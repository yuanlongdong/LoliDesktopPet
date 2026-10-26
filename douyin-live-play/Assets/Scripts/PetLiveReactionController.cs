using System;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;

namespace LoliDesktopPet.DouyinLivePlay
{
    /// <summary>
    /// Normalized live event delivered by the official Douyin Cloud adapter.
    /// This class deliberately does not generate simulated events.
    /// </summary>
    public sealed class PetLiveEvent
    {
        public string EventId;
        public string Type;
        public string Nickname;
        public int Count;
        public string Comment;
        public long TimestampMs;
        public bool IsPlatformTest;

        public PetLiveEvent(
            string eventId,
            string type,
            string nickname,
            int count,
            string comment,
            long timestampMs,
            bool isPlatformTest)
        {
            EventId = eventId ?? string.Empty;
            Type = NormalizeType(type);
            Nickname = CleanNickname(nickname);
            Count = Mathf.Clamp(count, 1, 9999);
            Comment = comment ?? string.Empty;
            TimestampMs = timestampMs;
            IsPlatformTest = isPlatformTest;
        }

        public static string NormalizeType(string value)
        {
            if (string.IsNullOrWhiteSpace(value)) return string.Empty;
            string type = value.Trim().ToLowerInvariant();
            if (type == "live_like" || type == "like") return "live_like";
            if (type == "live_comment" || type == "comment") return "live_comment";
            if (type == "live_gift" || type == "gift") return "live_gift";
            if (type == "live_follow" || type == "follow") return "live_follow";
            return string.Empty;
        }

        public static string CleanNickname(string value)
        {
            if (string.IsNullOrWhiteSpace(value)) return "朋友";
            var chars = value.Trim().ToCharArray();
            var cleaned = new System.Text.StringBuilder(chars.Length);
            foreach (char c in chars)
            {
                if (!char.IsControl(c) && c != '\\' && c != '<' && c != '>') cleaned.Append(c);
            }
            string result = cleaned.ToString().Trim();
            if (result.Length > 16) result = result.Substring(0, 16);
            return string.IsNullOrWhiteSpace(result) ? "朋友" : result;
        }
    }

    /// <summary>
    /// Drives an Animator and a local speech bubble from verified live-play events.
    /// Configure Animator triggers with the names documented in douyin-live-play/README.md.
    /// </summary>
    public sealed class PetLiveReactionController : MonoBehaviour
    {
        [Header("Character")]
        [SerializeField] private Animator petAnimator;

        [Header("Optional response bubble")]
        [SerializeField] private CanvasGroup bubbleGroup;
        [SerializeField] private Text bubbleText;
        [SerializeField, Min(0.1f)] private float bubbleDurationSeconds = 2.8f;

        [Header("Rate control")]
        [SerializeField, Min(0f)] private float minimumReactionGapSeconds = 0.45f;
        [SerializeField, Min(1)] private int queueCapacity = 64;

        private readonly object gate = new object();
        private readonly Queue<PetLiveEvent> pending = new Queue<PetLiveEvent>();
        private readonly Queue<string> seenOrder = new Queue<string>();
        private readonly HashSet<string> seenIds = new HashSet<string>();
        private readonly HashSet<string> triggerNames = new HashSet<string>();

        private float nextReactionAt;
        private float bubbleUntil;
        private bool hasBubble;

        private void Awake()
        {
            if (petAnimator != null)
            {
                foreach (AnimatorControllerParameter parameter in petAnimator.parameters)
                {
                    if (parameter.type == AnimatorControllerParameterType.Trigger)
                        triggerNames.Add(parameter.name);
                }
            }

            if (bubbleText != null) bubbleText.supportRichText = false;
            HideBubble();
        }

        /// <summary>
        /// Called only by the official live-event adapter after parsing a platform message.
        /// Returns false for malformed, duplicate, unsupported or platform-test messages.
        /// </summary>
        public bool TryEnqueue(PetLiveEvent liveEvent)
        {
            if (liveEvent == null || string.IsNullOrWhiteSpace(liveEvent.EventId)) return false;
            if (liveEvent.IsPlatformTest) return false;
            if (string.IsNullOrWhiteSpace(liveEvent.Type)) return false;

            lock (gate)
            {
                if (seenIds.Contains(liveEvent.EventId)) return false;
                if (pending.Count >= Mathf.Clamp(queueCapacity, 1, 256))
                {
                    // Prefer dropping noisy likes over allowing an unbounded backlog.
                    if (liveEvent.Type == "live_like") return false;
                    pending.Dequeue();
                }

                seenIds.Add(liveEvent.EventId);
                seenOrder.Enqueue(liveEvent.EventId);
                while (seenOrder.Count > 512)
                    seenIds.Remove(seenOrder.Dequeue());

                pending.Enqueue(liveEvent);
                return true;
            }
        }

        private void Update()
        {
            if (hasBubble && Time.unscaledTime >= bubbleUntil) HideBubble();
            if (Time.unscaledTime < nextReactionAt) return;

            PetLiveEvent next = null;
            lock (gate)
            {
                if (pending.Count > 0) next = pending.Dequeue();
            }
            if (next == null) return;

            ApplyReaction(next);
            nextReactionAt = Time.unscaledTime + Mathf.Max(0f, minimumReactionGapSeconds);
        }

        private void ApplyReaction(PetLiveEvent e)
        {
            string name = PetLiveEvent.CleanNickname(e.Nickname);
            int count = Mathf.Clamp(e.Count, 1, 9999);
            string trigger = "Comment";
            string line = "看到你的弹幕啦！";

            switch (e.Type)
            {
                case "live_like":
                    trigger = "Like";
                    line = "谢谢 " + name + " 点赞 ×" + count + "！";
                    break;

                case "live_comment":
                    trigger = CommentCommandTrigger(e.Comment);
                    line = CommentCommandLine(trigger, name);
                    break;

                case "live_gift":
                    trigger = count >= 10 ? "BigGift" : "Gift";
                    line = "谢谢 " + name + " 送来的礼物 ×" + count + "！";
                    break;

                case "live_follow":
                    trigger = "Follow";
                    line = "欢迎 " + name + "！";
                    break;
            }

            FireTrigger(trigger);
            ShowBubble(line);
        }

        private static string CommentCommandTrigger(string comment)
        {
            // Do not echo arbitrary viewer text onto the live screen.
            // Only known, safe commands can request a specific character action.
            string value = (comment ?? string.Empty).Trim();
            if (value.Contains("跳舞")) return "Dance";
            if (value.Contains("比心")) return "Heart";
            if (value.Contains("挥手")) return "Wave";
            if (value.Contains("抱抱")) return "Hug";
            return "Comment";
        }

        private static string CommentCommandLine(string trigger, string nickname)
        {
            switch (trigger)
            {
                case "Dance": return "收到 " + nickname + " 的指令，跳一段！";
                case "Heart": return "给 " + nickname + " 比个心！";
                case "Wave": return "向 " + nickname + " 挥挥手！";
                case "Hug": return "给 " + nickname + " 一个抱抱！";
                default: return "看到 " + nickname + " 的弹幕啦！";
            }
        }

        private void FireTrigger(string trigger)
        {
            if (petAnimator == null || string.IsNullOrEmpty(trigger)) return;
            if (!triggerNames.Contains(trigger))
            {
                Debug.LogWarning("[PetLiveReaction] Animator trigger not configured: " + trigger);
                return;
            }
            petAnimator.ResetTrigger(trigger);
            petAnimator.SetTrigger(trigger);
        }

        private void ShowBubble(string message)
        {
            if (bubbleText != null) bubbleText.text = message;
            if (bubbleGroup != null)
            {
                bubbleGroup.alpha = 1f;
                bubbleGroup.interactable = false;
                bubbleGroup.blocksRaycasts = false;
                bubbleGroup.gameObject.SetActive(true);
            }
            hasBubble = true;
            bubbleUntil = Time.unscaledTime + Mathf.Max(0.5f, bubbleDurationSeconds);
        }

        private void HideBubble()
        {
            if (bubbleGroup != null)
            {
                bubbleGroup.alpha = 0f;
                bubbleGroup.interactable = false;
                bubbleGroup.blocksRaycasts = false;
                bubbleGroup.gameObject.SetActive(false);
            }
            hasBubble = false;
        }
    }
}
