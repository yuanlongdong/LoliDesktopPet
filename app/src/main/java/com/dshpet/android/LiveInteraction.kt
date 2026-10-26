package com.dshpet.android

import android.os.SystemClock
import java.util.ArrayDeque
import java.util.LinkedHashSet

/**
 * 平台无关的直播互动事件。真实数据源必须先校验、再转换为此模型。
 * 当前仓库尚未连接抖音事件源。
 */
data class LiveEvent(
    val eventId: String,
    val type: Type,
    val nickname: String = "朋友",
    val count: Int = 1,
    val occurredAtMs: Long = System.currentTimeMillis(),
) {
    enum class Type { LIKE, FOLLOW, GIFT, GIFT_COMBO, BIG_GIFT, COMMENT_MENTION }

    fun safeNickname(): String = nickname
        .replace(Regex("[\\r\\n\\t]"), " ")
        .trim()
        .take(16)
        .ifBlank { "朋友" }

    fun safeCount(): Int = count.coerceIn(1, 9999)
}

/** 有界事件队列：去重、限长；不允许直播重连后无限重复播放旧事件。 */
class LiveEventQueue(
    private val capacity: Int = 40,
    private val dedupeCapacity: Int = 256,
) {
    private val queue = ArrayDeque<LiveEvent>()
    private val seen = LinkedHashSet<String>()

    @Synchronized
    fun offer(event: LiveEvent): Boolean {
        val id = event.eventId.trim().take(128)
        if (id.isEmpty() || !seen.add(id)) return false
        while (seen.size > dedupeCapacity) seen.remove(seen.first())
        val pending = (queue.toList() + event.copy(eventId = id))
            .sortedByDescending { priorityOf(it.type) }
            .take(capacity.coerceAtLeast(1))
        queue.clear()
        queue.addAll(pending)
        return pending.any { it.eventId == id }
    }

    @Synchronized
    fun poll(): LiveEvent? = if (queue.isEmpty()) null else queue.removeFirst()

    @Synchronized
    fun clear() = queue.clear()

    @Synchronized
    fun size(): Int = queue.size

    private fun priorityOf(type: LiveEvent.Type): Int = when (type) {
        LiveEvent.Type.BIG_GIFT -> 10
        LiveEvent.Type.GIFT_COMBO -> 5
        LiveEvent.Type.GIFT -> 4
        LiveEvent.Type.FOLLOW -> 3
        LiveEvent.Type.COMMENT_MENTION -> 2
        LiveEvent.Type.LIKE -> 1
    }
}

data class PetReaction(val message: String, val actionSlug: String, val priority: Int)

/** 将平台事件映射到现有动画配置，不依赖特定直播平台。 */
object PetReactionMapper {
    fun map(event: LiveEvent, config: PetConfig): PetReaction {
        val name = event.safeNickname()
        val count = event.safeCount()
        val (message, priority, preferredCategory) = when (event.type) {
            LiveEvent.Type.LIKE -> Triple("收到 $count 个点赞啦，谢谢大家！", 1, listOf("玩耍", "小动作"))
            LiveEvent.Type.FOLLOW -> Triple("欢迎 $name 来到直播间！", 3, listOf("文字", "小动作"))
            LiveEvent.Type.GIFT -> Triple("谢谢 $name 送来的礼物！", 4, listOf("文字", "玩耍"))
            LiveEvent.Type.GIFT_COMBO -> Triple("$name 连续送出礼物 × $count，开心值叠满啦！", 5, listOf("玩耍", "文字"))
            LiveEvent.Type.BIG_GIFT -> Triple("感谢 $name 的惊喜礼物！全场庆祝！", 10, listOf("玩耍", "文字"))
            LiveEvent.Type.COMMENT_MENTION -> Triple("$name，我看到你的弹幕啦！", 2, listOf("文字", "小动作"))
        }
        val categories = preferredCategory.mapNotNull { key ->
            config.categories.firstOrNull { it.id.contains(key, ignoreCase = true) }
        }
        val preferredActions = categories.flatMap { it.actions }
        val fallback = config.clicks + config.categories.flatMap { it.actions } + config.idle
        val slug = (preferredActions.ifEmpty { fallback }).distinct().randomOrNull() ?: ""
        return PetReaction(message, slug, priority)
    }
}

/** 简单限流工具，供评论/点赞事件源使用，时间由单调时钟计算。 */
class LiveEventRateLimiter(private val intervalMs: Long) {
    private var lastAcceptedAt = 0L
    @Synchronized
    fun allow(nowMs: Long = SystemClock.uptimeMillis()): Boolean {
        if (lastAcceptedAt != 0L && nowMs - lastAcceptedAt < intervalMs) return false
        lastAcceptedAt = nowMs
        return true
    }
}
