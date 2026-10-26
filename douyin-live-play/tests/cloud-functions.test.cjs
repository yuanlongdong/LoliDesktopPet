const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const Module = require("node:module");
const test = require("node:test");
const ts = require("typescript");

const root = path.resolve(__dirname, "..");
const cloudDir = path.join(root, "cloud");
const sources = ["start_game.ts", "live_data_callback.ts", "websocket_callback.ts", "finish_game.ts"];

function loadFunction(name) {
  const file = path.join(cloudDir, name);
  const source = fs.readFileSync(file, "utf8");
  const compiled = ts.transpileModule(source, {
    compilerOptions: { target: ts.ScriptTarget.ES2020, module: ts.ModuleKind.CommonJS, strict: true },
    fileName: file,
    reportDiagnostics: true
  });
  const errors = (compiled.diagnostics || []).filter(x => x.category === ts.DiagnosticCategory.Error);
  assert.equal(errors.length, 0, errors.map(x => ts.flattenDiagnosticMessageText(x.messageText, "\n")).join("\n"));

  const compiledPath = file.replace(/\.ts$/, ".test.cjs");
  const instance = new Module(compiledPath, module);
  instance.filename = compiledPath;
  instance.paths = Module._nodeModulePaths(path.dirname(compiledPath));
  instance._compile(compiled.outputText, compiledPath);
  return instance.exports.default;
}

function context({ headers = {}, liveContext = { appId: "app-test", roomId: "room-test" }, responses = [] } = {}) {
  let index = 0;
  const value = {
    headers, liveContext, calls: [], logs: [], errors: [],
    log(...args) { this.logs.push(args); },
    error(...args) { this.errors.push(args); },
    async openApiInvoke(request) {
      this.calls.push(request);
      const result = responses[index++];
      if (result instanceof Error) throw result;
      return result === undefined ? { err_no: 0, err_msg: "ok" } : result;
    }
  };
  return value;
}

const originalLoad = Module._load;
Module._load = function(request, parent, isMain) {
  if (request === "@open-dy/node-server-sdk") {
    return { dySDK: { context(ctx) {
      return { getContext: () => ctx.liveContext, openApiInvoke: req => ctx.openApiInvoke(req) };
    } } };
  }
  return originalLoad.call(this, request, parent, isMain);
};

test("all cloud functions pass strict TypeScript checks against the isolated SDK contract", () => {
  const files = [...sources.map(name => path.join(cloudDir, name)), path.join(__dirname, "node-server-sdk.d.ts")];
  const program = ts.createProgram(files, {
    target: ts.ScriptTarget.ES2020, module: ts.ModuleKind.CommonJS,
    moduleResolution: ts.ModuleResolutionKind.Node10, strict: true, noEmit: true,
    skipLibCheck: true, lib: ["lib.es2020.d.ts"]
  });
  const errors = ts.getPreEmitDiagnostics(program).filter(x => x.category === ts.DiagnosticCategory.Error);
  assert.equal(errors.length, 0, errors.map(x =>
    ts.flattenDiagnosticMessageText(x.messageText, "\n")
  ).join("\n"));
});

test("start_game opens comment, like and gift tasks", async () => {
  const ctx = context();
  const result = await loadFunction("start_game.ts")({}, ctx);
  assert.equal(result.err_no, 0);
  assert.deepEqual(ctx.calls.map(x => x.data.msg_type), ["live_comment", "live_like", "live_gift"]);
});

test("start_game rolls back only tasks which successfully started", async () => {
  const ctx = context({ responses: [
    { err_no: 0 }, { err_no: 99, err_msg: "planned test failure" }, { err_no: 0 },
    { err_no: 0 }, { err_no: 0 }
  ] });
  const result = await loadFunction("start_game.ts")({}, ctx);
  assert.equal(result.err_no, 1);
  assert.deepEqual(ctx.calls.map(x => x.url.endsWith("/stop") ? "stop:" + x.data.msg_type : "start:" + x.data.msg_type), [
    "start:live_comment", "start:live_like", "start:live_gift", "stop:live_comment", "stop:live_gift"
  ]);
});

test("callback forwards each event separately with its own ID", async () => {
  const ctx = context({ headers: { "x-anchor-openid": "anchor-test", "x-msg-type": "live_like" } });
  const result = await loadFunction("live_data_callback.ts")([
    { msg_id: "like-1", msg_type_str: "live_like" },
    { msg_id: "like-2", msg_type_str: "live_like" }
  ], ctx);
  assert.equal(result, "success");
  assert.deepEqual(ctx.calls.map(x => x.data.msg_id), ["like-1", "like-2"]);
  assert.deepEqual(ctx.calls.map(x => JSON.parse(x.data.data).map(e => e.msg_id)), [["like-1"], ["like-2"]]);
  assert.ok(ctx.calls.every(x => x.headers["X-TT-WS-OPENIDS"] === "[\"anchor-test\"]"));
});

test("callback throws when the gateway rejects an event", async () => {
  const ctx = context({
    headers: { "x-anchor-openid": "anchor-test", "x-msg-type": "live_gift" },
    responses: [{ err_no: 9, err_msg: "rejected" }]
  });
  await assert.rejects(loadFunction("live_data_callback.ts")([{ msg_id: "gift-1" }], ctx), /Failed to forward 1 live event/);
});

test("callback blocks requests missing platform headers", async () => {
  const ctx = context({ headers: {} });
  assert.equal(await loadFunction("live_data_callback.ts")([{ msg_id: "x" }], ctx), "missing required headers");
  assert.equal(ctx.calls.length, 0);
});

test("finish_game stops all subscribed event tasks", async () => {
  const ctx = context();
  const result = await loadFunction("finish_game.ts")({}, ctx);
  assert.equal(result.err_no, 0);
  assert.ok(ctx.calls.every(x => x.url.endsWith("/stop")));
  assert.deepEqual(ctx.calls.map(x => x.data.msg_type), ["live_comment", "live_like", "live_gift"]);
});

test("websocket_callback handles lifecycle events without echoing user payload", async () => {
  const ctx = context({ headers: { "x-tt-event-type": "connect" } });
  const result = await loadFunction("websocket_callback.ts")({ arbitrary: "viewer text" }, ctx);
  assert.equal(result, "success");
  assert.ok(ctx.logs.some(args => String(args[0]).includes("connected")));
  assert.ok(!ctx.logs.some(args => args.join(" ").includes("viewer text")));
});

test("finish_game reports partial stop failures", async () => {
  const ctx = context({ responses: [
    { err_no: 0 }, { err_no: 7, err_msg: "cannot stop" }, { err_no: 0 }
  ] });
  const result = await loadFunction("finish_game.ts")({}, ctx);
  assert.equal(result.err_no, 1);
  assert.equal(result.data.live_like.ok, false);
});
