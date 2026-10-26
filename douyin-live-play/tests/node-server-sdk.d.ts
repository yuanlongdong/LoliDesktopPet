type LiveRoomContext = {
  appId?: string;
  roomId?: string;
};
type CloudOpenApiRequest = {
  url: string;
  method: "POST" | "GET";
  headers: Record<string, string>;
  data: Record<string, unknown>;
};
type CloudOpenApiResult = {
  err_no?: number;
  err_msg?: string;
  data?: unknown;
};
declare module "@open-dy/node-server-sdk" {
  export const dySDK: {
    context(context: any): {
      getContext(): LiveRoomContext;
      openApiInvoke(request: CloudOpenApiRequest): Promise<CloudOpenApiResult>;
    };
  };
}
