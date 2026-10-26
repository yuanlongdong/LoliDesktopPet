import { dySDK } from "@open-dy/node-server-sdk";

type ApiResult = {
  err_no?: number;
  err_msg?: string;
};

const MESSAGE_TYPES = ["live_comment", "live_like", "live_gift"];

export default async function (_params: unknown, context: any) {
  const serviceContext = dySDK.context(context);
  const liveContext = serviceContext.getContext();
  const appId = liveContext?.appId;
  const roomId = liveContext?.roomId;

  if (!appId || !roomId) {
    context.error("finish_game missing Douyin live context");
    return { err_no: 1, err_msg: "缺少抖音直播间上下文，无法结束互动任务" };
  }

  const results: Record<string, { ok: boolean; err_no?: number; err_msg?: string }> = {};

  for (const msgType of MESSAGE_TYPES) {
    try {
      const result = (await serviceContext.openApiInvoke({
        url: "https://webcast-bytedance-com.openapi.dyc.ivolces.com/api/live_data/task/stop",
        method: "POST",
        headers: { "Content-Type": "application/json" },
        data: { roomid: roomId, appid: appId, msg_type: msgType }
      })) as ApiResult;

      const ok = result?.err_no === 0;
      results[msgType] = { ok, err_no: result?.err_no, err_msg: result?.err_msg };
      if (!ok) context.error("Failed to stop live data task", msgType, result?.err_msg);
    } catch (error) {
      results[msgType] = { ok: false, err_msg: String(error) };
      context.error("Failed to stop live data task", msgType, error);
    }
  }

  const allStopped = MESSAGE_TYPES.every((type) => results[type]?.ok);
  return {
    err_no: allStopped ? 0 : 1,
    err_msg: allStopped ? "互动事件推送任务已停止" : "部分互动事件停止失败，请检查抖音云日志",
    data: results
  };
}
