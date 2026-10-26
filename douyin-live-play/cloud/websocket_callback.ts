export default async function (_params: unknown, context: any) {
  const eventType = context.headers?.["x-tt-event-type"] ?? "unknown";
  switch (eventType) {
    case "connect":
      context.log("Douyin Cloud client connected");
      break;
    case "disconnect":
      context.log("Douyin Cloud client disconnected");
      break;
    case "uplink":
      // Do not log free-form viewer/comment data here.
      context.log("Douyin Cloud client sent an uplink message");
      break;
    default:
      context.log("Douyin Cloud WebSocket lifecycle callback", eventType);
      break;
  }
  return "success";
}
