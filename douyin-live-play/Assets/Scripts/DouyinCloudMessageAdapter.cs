using System;
using System.Collections.Generic;
using ByteDance.LiveOpenSdk;
using ByteDance.LiveOpenSdk.DyCloud;
using Douyin.LiveOpenSDK.Samples;
using UnityEngine;

namespace LoliDesktopPet.DouyinLivePlay
{
    /// <summary>
    /// Adapter for the official LiveOpenSDK + its SampleDyCloudManager sample.
    /// Copy the SDK sample into Assets first, then bind this component to that manager.
    /// Cloud callbacks contain a JSON array, so each event is converted individually.
    /// </summary>
    public sealed class DouyinCloudMessageAdapter : MonoBehaviour
    {
        [SerializeField] private PetLiveReactionController reactionController;

        private SampleDyCloudManager manager;

        [Serializable]
        private sealed class PlatformPayload
        {
            public string msg_id;
            public string msg_type_str;
            public long timestamp;
            public string nickname;
            public string content;
            public int like_num;
            public int gift_num;
            public bool test;
        }

        [Serializable]
        private sealed class PayloadArray
        {
            public PlatformPayload[] items;
        }

        /// <summary>
        /// Call once after SampleDyCloudManager has been initialized and its WebSocket connected.
        /// </summary>
        public void Bind(SampleDyCloudManager source)
        {
            Unbind();
            if (source == null)
            {
                Debug.LogError("[PetLiveAdapter] Cannot bind a null Douyin Cloud manager.");
                return;
            }

            manager = source;
            manager.OnDyCloudMessage += HandleCloudMessage;
        }

        public void Unbind()
        {
            if (manager != null) manager.OnDyCloudMessage -= HandleCloudMessage;
            manager = null;
        }

        private void OnDestroy()
        {
            Unbind();
        }

        private void HandleCloudMessage(DyCloudSocketMessage message)
        {
            if (message == null) return;

            try
            {
                if (reactionController == null)
                {
                    Debug.LogError("[PetLiveAdapter] Assign PetLiveReactionController in Inspector.");
                    return;
                }

                foreach (PlatformPayload payload in ParsePayloads(message.Data))
                {
                    if (payload == null || payload.test) continue;

                    string type = !string.IsNullOrWhiteSpace(payload.msg_type_str)
                        ? payload.msg_type_str
                        : message.MsgType;

                    string eventId = !string.IsNullOrWhiteSpace(payload.msg_id)
                        ? payload.msg_id
                        : message.MsgId;

                    string normalizedType = PetLiveEvent.NormalizeType(type);
                    if (string.IsNullOrWhiteSpace(eventId) || string.IsNullOrWhiteSpace(normalizedType))
                        continue;

                    int count = 1;
                    if (normalizedType == "live_like" && payload.like_num > 0) count = payload.like_num;
                    else if (normalizedType == "live_gift" && payload.gift_num > 0) count = payload.gift_num;

                    var item = new PetLiveEvent(
                        eventId,
                        normalizedType,
                        payload.nickname,
                        count,
                        payload.content,
                        payload.timestamp,
                        payload.test);

                    reactionController.TryEnqueue(item);
                }
            }
            catch (Exception ex)
            {
                Debug.LogError("[PetLiveAdapter] Failed to process cloud event: " + ex.Message);
            }
            finally
            {
                // Acknowledge the outer gateway message after processing to prevent repeated delivery.
                // The SDK sample exposes this exact acknowledgement API.
                if (!string.IsNullOrWhiteSpace(message.MsgId) && !string.IsNullOrWhiteSpace(message.MsgType))
                {
                    try
                    {
                        LiveOpenSdk.MessageAckService.ReportAck(message.MsgId, message.MsgType);
                    }
                    catch (Exception ex)
                    {
                        Debug.LogWarning("[PetLiveAdapter] Event ack failed: " + ex.Message);
                    }
                }
            }
        }

        private static PlatformPayload[] ParsePayloads(string json)
        {
            if (string.IsNullOrWhiteSpace(json)) return Array.Empty<PlatformPayload>();
            string trimmed = json.Trim();

            if (trimmed.StartsWith("[", StringComparison.Ordinal))
            {
                string wrapped = "{\"items\":" + trimmed + "}";
                PayloadArray result = JsonUtility.FromJson<PayloadArray>(wrapped);
                return result != null && result.items != null
                    ? result.items
                    : Array.Empty<PlatformPayload>();
            }

            PlatformPayload single = JsonUtility.FromJson<PlatformPayload>(trimmed);
            return single != null ? new[] { single } : Array.Empty<PlatformPayload>();
        }
    }
}
