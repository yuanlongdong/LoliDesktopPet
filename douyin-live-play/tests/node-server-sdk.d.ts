declare module "@open-dy/node-server-sdk" {
  export const dySDK: {
    context(context: any): {
      getContext(): any;
      openApiInvoke(request: any): Promise<any>;
    };
  };
}
