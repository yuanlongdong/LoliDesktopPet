using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using ByteDance.LiveOpenSdk;
using ByteDance.LiveOpenSdk.DyCloud;
using Douyin.LiveOpenSDK.Samples;
using UnityEngine;

namespace LoliDesktopPet.DouyinLivePlay
{
    /// <summary>
    /// Coordinates the official Unity LiveOpenSDK sample manager with the project's
    /// /start_game, /finish_game and /websocket_callback Douyin Cloud functions.
    /// The client contains identifiers only, never app secrets or access tokens.
    /// </summary>
    public sealed class DouyinLivePlayBootstrap : MonoBehaviour
    {
        private const int WebSocketOpenTimeoutSeconds = 15;

        [Header("Open platform identifiers (not secrets)")]
        [SerializeField] private string appId;
        [SerializeField] private string cloudEnvId;
        [SerializeField] private string cloudServiceId;
        [SerializeField] private bool sdkDebug;

        [Header("Scene wiring")]
        [SerializeField] private DouyinCloudMessageAdapter messageAdapter;

        private SampleLiveOpenSdkManager sdkManager;
        private SampleDyCloudManager cloudManager;
        private Task startupTask;
        private TaskCompletionSource<bool> socketOpened;
        private bool started;
        private bool cleaningUp;
        private bool startRequested;
        private bool destroying;

        [Serializable]
        private sealed class CloudFunctionResponse
        {
            public int err_no;
            public string err_msg;
        }

        private void Start()
        {
            startupTask = StartAsync();
        }

        private async Task StartAsync()
        {
            if (string.IsNullOrWhiteSpace(appId) ||
                string.IsNullOrWhiteSpace(cloudEnvId) ||
                string.IsNullOrWhiteSpace(cloudServiceId))
            {
                Debug.LogError("[DouyinLiveBootstrap] Set App ID, Cloud Env ID, and Cloud Service ID in Inspector.");
                enabled = false;
                return;
            }

            if (messageAdapter == null)
            {
                Debug.LogError("[DouyinLiveBootstrap] Assign DouyinCloudMessageAdapter in Inspector.");
                enabled = false;
                return;
            }

            try
            {
                // The official sample initializes LiveOpenSdk and sets the AppId.
                sdkManager = new SampleLiveOpenSdkManager { AppId = appId };
                sdkManager.OnCreate();
                if (destroying) return;

                cloudManager = sdkManager.DyCloudManager;
                cloudManager.EnvId = cloudEnvId;
                cloudManager.ServiceId = cloudServiceId;
                cloudManager.IsDebug = sdkDebug;
                cloudManager.connectPath = "/websocket_callback";

                // Subscribe before the socket starts so the first event cannot race the subscription.
                messageAdapter.Bind(cloudManager);

                // Do not use SampleDyCloudManager.Init here: the SDK sample catches and logs
                // initialization exceptions without rethrowing. Await the public SDK API directly
                // so a failed initialization aborts this startup sequence.
                if (LiveOpenSdk.DyCloudApi == null)
                    throw new InvalidOperationException("LiveOpenSdk.DyCloudApi is unavailable after SDK initialization.");

                var initParams = new DyCloudInitParams
                {
                    EnvId = cloudEnvId,
                    DefaultServiceId = cloudServiceId,
                    IsDebug = sdkDebug,
                    DebugIpAddress = string.Empty
                };
                await LiveOpenSdk.DyCloudApi.InitializeAsync(initParams);
                if (destroying) return;

                startRequested = true;
                string startResult = await CallCloudFunctionAsync("/start_game");
                if (destroying) return;

                CloudFunctionResponse parsed = JsonUtility.FromJson<CloudFunctionResponse>(startResult);
                if (parsed == null || parsed.err_no != 0)
                {
                    string detail = parsed != null ? parsed.err_msg : startResult;
                    throw new InvalidOperationException("Douyin Cloud /start_game failed: " + detail);
                }

                started = true;
                await ConnectAndVerifyWebSocketAsync();
                if (destroying) return;

                Debug.Log("[DouyinLiveBootstrap] WebSocket OnOpen received. The transport is connected; real event delivery still requires verification in an authorized Douyin live room.");
            }
            catch (Exception ex)
            {
                if (!destroying)
                    Debug.LogError("[DouyinLiveBootstrap] Startup failed: " + ex.Message);
                await CleanupAsync(startRequested || started);
            }
        }

        private async Task<string> CallCloudFunctionAsync(string path)
        {
            // Use the public response API so HTTP failures cannot be mistaken for an empty
            // or misleading function body.
            var response = await LiveOpenSdk.DyCloudApi.CallContainerAsync(
                path,
                cloudServiceId,
                "POST",
                string.Empty,
                new Dictionary<string, string>());

            if (response == null)
                throw new InvalidOperationException("Douyin Cloud " + path + " returned no HTTP response.");

            if (response.StatusCode < 200 || response.StatusCode >= 300)
                throw new InvalidOperationException(
                    "Douyin Cloud " + path + " HTTP " + response.StatusCode + ": " + response.Body);

            return response.Body ?? string.Empty;
        }

        private async Task ConnectAndVerifyWebSocketAsync()
        {
            var webSocket = LiveOpenSdk.DyCloudApi.WebSocket;
            socketOpened = new TaskCompletionSource<bool>(TaskCreationOptions.RunContinuationsAsynchronously);

            // SampleDyCloudManager.ConnectWebSocket logs and swallows connection exceptions,
            // so independently wait for the SDK's actual OnOpen event.
            webSocket.OnOpen += HandleSocketOpen;
            webSocket.OnError += HandleSocketError;

            try
            {
                await cloudManager.ConnectWebSocket();
                if (destroying) return;

                Task timeout = Task.Delay(WebSocketOpenTimeoutSeconds * 1000);
                Task completed = await Task.WhenAny(socketOpened.Task, timeout);
                if (completed != socketOpened.Task)
                    throw new TimeoutException(
                        "WebSocket OnOpen was not received within " + WebSocketOpenTimeoutSeconds +
                        " seconds. Check Cloud Env/Service IDs, callback path, SDK logs and Douyin permissions.");

                await socketOpened.Task;
            }
            finally
            {
                webSocket.OnOpen -= HandleSocketOpen;
                webSocket.OnError -= HandleSocketError;
                socketOpened = null;
            }
        }

        private void HandleSocketOpen()
        {
            socketOpened?.TrySetResult(true);
        }

        private void HandleSocketError(IDyCloudWebSocketError error)
        {
            // The SDK may reconnect automatically. Only fail immediately for a terminal error;
            // otherwise the open timeout provides the final guard.
            if (error == null || error.WillReconnect != true)
            {
                string detail = error != null ? error.RawMessage : "unknown WebSocket error";
                socketOpened?.TrySetException(
                    new InvalidOperationException("Douyin Cloud WebSocket terminal error: " + detail));
            }
        }

        private async Task CleanupAsync(bool stopTasks)
        {
            if (cleaningUp) return;
            cleaningUp = true;

            if (messageAdapter != null) messageAdapter.Unbind();

            try
            {
                if (cloudManager != null)
                {
                    if (stopTasks)
                    {
                        try
                        {
                            string result = await CallCloudFunctionAsync("/finish_game");
                            CloudFunctionResponse parsed = JsonUtility.FromJson<CloudFunctionResponse>(result);
                            if (parsed == null || parsed.err_no != 0)
                            {
                                string detail = parsed != null ? parsed.err_msg : result;
                                Debug.LogWarning("[DouyinLiveBootstrap] /finish_game returned an error: " + detail);
                            }
                            else
                            {
                                Debug.Log("[DouyinLiveBootstrap] /finish_game: " + result);
                            }
                        }
                        catch (Exception ex)
                        {
                            Debug.LogWarning("[DouyinLiveBootstrap] Could not stop live tasks cleanly: " + ex.Message);
                        }
                    }

                    cloudManager.CloseWebSocket();
                }
            }
            catch (Exception ex)
            {
                Debug.LogWarning("[DouyinLiveBootstrap] Shutdown warning: " + ex.Message);
            }
            finally
            {
                if (sdkManager != null) sdkManager.OnDestroy();
                sdkManager = null;
                cloudManager = null;
            }
        }

        private async void OnDestroy()
        {
            destroying = true;

            // Let an in-flight startup request settle before stopping its tasks, avoiding
            // a finish_game call racing ahead of start_game.
            if (startupTask != null)
            {
                try { await startupTask; }
                catch (Exception ex) { Debug.LogWarning("[DouyinLiveBootstrap] Startup shutdown warning: " + ex.Message); }
            }

            await CleanupAsync(started || startRequested);
        }
    }
}
