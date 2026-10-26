import { dySDK } from "@open-dy/node-server-sdk";

type PlatformEvent = {
  msg_id?: string;
};

type PushResult = {
  err_no?: number;
  err_msg?: string;
};

export default async function (params: PlatformEvent[] | unknown, context: any) {
  const headers = context.headers ?? {};
  const anchorOpenId = headers["x-anchor-openid"] ?? headers["X-Anchor-OpenID"];
  const msgType = headers["x-msg-type"] ?? headers["X-Msg-Type"];
  const source = headers["x-tt-source"] ?? headers["X-TT-Source"];

  if (!anchorOpenId || !msgType) {
    context.error("live_data_callback missing required platform headers");
    return "missing required headers";
  }

  // The callback is configured as an internal Douyin Cloud route. If the platform
  // provides x-tt-source, log only its presence for diagnostics, never viewer content.
  if (!Array.isArray(params) || params.length === 0) {
    context.log("live_data_callback received an empty event batch", { hasPlatformSource: Boolean(source) });
    return "success";
  }

  const events = params.filter(
    (item): item is PlatformEvent =>
      Boolean(item) && typeof item === "object" && typeof (item as PlatformEvent).msg_id === "string"
  );

  if (events.length === 0) {
    context.error("live_data_callback payload contains no valid msg_id");
    return "invalid payload";
  }

  const serviceContext = dySDK.context(context);
  const failures: string[] = [];

  // Push each platform instruction separately, with its own msg_id. This lets the
  // Unity SDK report fulfillment against the exact instruction after rendering.
  for (const event of events) {
    const gatewayMessage = {
      msg_id: event.msg_id,
      msg_type: String(msgType),
      data: JSON.stringify([event])
    };

    try {
      const result = (await serviceContext.openApiInvoke({
        url: "http://ws-push.dyc.ivolces.com/ws/live_interaction/push_data",
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "X-TT-WS-OPENIDS": JSON.stringify([anchorOpenId])
        },
        data: gatewayMessage
      })) as PushResult;

      if (result?.err_no !== 0) {
        failures.push(event.msg_id ?? "unknown");
        context.error("Douyin Cloud gateway rejected an event", {
          msg_type: msgType,
          msg_id: event.msg_id,
          err_no: result?.err_no,
          err_msg: result?.err_msg
        });
      }
    } catch (error) {
      failures.push(event.msg_id ?? "unknown");
      context.error("Could not forward live event", {
        msg_type: msgType,
        msg_id: event.msg_id,
        error: String(error)
      });
    }
  }

  context.log("live event batch processed", {
    msg_type: msgType,
    count: events.length,
    failed_count: failures.length
  });

  // Non-success allows the cloud runtime's configured retry policy to take effect.
  if (failures.length > 0) {
    throw new Error("Failed to forward " + failures.length + " live event(s)");
  }

  return "success";
}
