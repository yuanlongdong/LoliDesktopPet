import { dySDK } from "@open-dy/node-server-sdk";

type PlatformEvent = {
  msg_id?: string;
  test?: boolean;
};

export default async function (params: PlatformEvent[] | unknown, context: any) {
  const anchorOpenId = context.headers?.["x-anchor-openid"] ?? context.headers?.["X-Anchor-OpenID"];
  const msgType = context.headers?.["x-msg-type"] ?? context.headers?.["X-Msg-Type"];

  if (!anchorOpenId || !msgType) {
    context.error("live_data_callback missing required platform headers");
    return "missing required headers";
  }

  if (!Array.isArray(params) || params.length === 0) {
    context.log("live_data_callback received an empty event batch");
    return "success";
  }

  const first = params.find((item) => item && typeof item === "object" && "msg_id" in item) as PlatformEvent | undefined;
  if (!first?.msg_id) {
    context.error("live_data_callback payload contains no msg_id");
    return "invalid payload";
  }

  // Keep the platform's original event payload. The Unity adapter handles batches and
  // deliberately ignores platform-marked test events instead of treating them as real.
  const gatewayMessage = {
    msg_id: first.msg_id,
    msg_type: String(msgType),
    data: JSON.stringify(params)
  };

  try {
    const serviceContext = dySDK.context(context);
    const result = await serviceContext.openApiInvoke({
      url: "http://ws-push.dyc.ivolces.com/ws/live_interaction/push_data",
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "X-TT-WS-OPENIDS": JSON.stringify([anchorOpenId])
      },
      data: gatewayMessage
    });
    context.log("live event batch forwarded to Douyin Cloud gateway", {
      msg_type: msgType,
      count: params.length,
      result
    });
    return "success";
  } catch (error) {
    context.error("Could not forward live event batch", String(msgType), error);
    throw error;
  }
}
