using System;
using System.Threading.Tasks;
using ByteDance.LiveOpenSdk.Utilities;
using Douyin.LiveOpenSDK.Samples;
using UnityEngine;

namespace LoliDesktopPet.DouyinLivePlay
{
    /// <summary>
    /// Coordinates the official Unity LiveOpenSDK sample manager with the project's
    /// /start_game, /finish_game and /websocket_callback Douyin Cloud functions.
    /// Configure public App ID, Douyin Cloud Env ID / Service ID, adapter and actor in Inspector.
    /// Never put app secrets or access tokens in this Unity client.
    /// </summary>
    public sealed class DouyinLivePlayBootstrap : MonoBehaviour
    {
        [Header("Open platform identifiers (not secrets)")]
        [SerializeField] private string appId;
        [SerializeField] private string cloudEnvId;
        [SerializeField] private string cloudServiceId;
        [SerializeField] private bool sdkDebug;

        [Header("Scene wiring")]
        [SerializeField] private DouyinCloudMessageAdapter messageAdapter;

        private SampleLiveOpenSdkManager sdkManager;
        private SampleDyCloudManager cloudManager;
        private bool started;
        private bool cleaningUp;

        [Serializable]
        private sealed class CloudFunctionResponse
        {
            public int err_no;
            public string err_msg;
        }

        private async void Start()
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
                sdkManager = new SampleLiveOpenSdkManager { AppId = appId };
                sdkManager.OnCreate();

                cloudManager = sdkManager.DyCloudManager;
                cloudManager.EnvId = cloudEnvId;
                cloudManager.ServiceId = cloudServiceId;
                cloudManager.IsDebug = sdkDebug;
                cloudManager.connectPath = "/websocket_callback";

                // Subscribe before the socket starts to avoid losing an early first message.
                messageAdapter.Bind(cloudManager);

                await cloudManager.Init();

                string startResult = await cloudManager.CallContainer("/start_game", HttpType.POST);
                CloudFunctionResponse parsed = JsonUtility.FromJson<CloudFunctionResponse>(startResult);
                if (parsed == null || parsed.err_no != 0)
                {
                    string detail = parsed != null ? parsed.err_msg : startResult;
                    throw new InvalidOperationException("Douyin Cloud /start_game failed: " + detail);
                }

                await cloudManager.ConnectWebSocket();
                started = true;
                Debug.Log("[DouyinLiveBootstrap] Start request accepted. Check SDK logs and the real Douyin live room before treating the connection as verified.");
            }
            catch (Exception ex)
            {
                Debug.LogError("[DouyinLiveBootstrap] Startup failed: " + ex.Message);
                if (messageAdapter != null) messageAdapter.Unbind();
            }
        }

        private async void OnDestroy()
        {
            if (cleaningUp) return;
            cleaningUp = true;

            if (messageAdapter != null) messageAdapter.Unbind();

            try
            {
                if (cloudManager != null)
                {
                    if (started)
                    {
                        try
                        {
                            string result = await cloudManager.CallContainer("/finish_game", HttpType.POST);
                            Debug.Log("[DouyinLiveBootstrap] /finish_game result: " + result);
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
    }
}
