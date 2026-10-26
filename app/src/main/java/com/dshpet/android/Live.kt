package com.dshpet.android

import android.os.SystemClock
import org.json.JSONObject
import kotlin.random.Random

/**
 * 直播联动：把直播间的事件（礼物 / 关注 / 点赞 / 弹幕 / 进场）翻译成桌宠的反应。
 *
 * 这是一条纯本地的「事件 → 反应」管道，事件来源是可插拔的：
 *  - 内置「测试：来一条」演示（不接桥也能看到效果）；
 *  - 或由 [LiveBridge] 从 tools/live-bridge 桥转发真实直播间事件（TCP JSON-lines，见
 *    tools/live-bridge/protocol.md）。
 *
 * 反应 = 说一句对应台词 + 播一段庆祝动画 + Q 弹一下。每个类型都有独立冷却，
 * 直播弹幕很密时不会刷屏；大额礼物会插队优先反应。
 */

/** 事件类型。`jsonName` 是桥上 JSON 里的 type 值，`category` 是台词库里的分类。 */
enum class LiveType(val jsonName: String, val category: String) {
    GIFT("gift", "gift"),
    GIFT_BIG("gift_big", "giftBig"),
    FOLLOW("follow", "follow"),
    LIKE("like", "like"),
    COMMENT("comment", "comment"),
    ENTER("enter", "enter"),
    ;

    companion object {
        /** 只认白名单里的类型，其它一律返回 null（防御外来 JSON） */
        fun from(s: String): LiveType? = values().firstOrNull { it.jsonName == s }
    }
}

/** 一条直播事件。字段都是可选，解析时做了容错。 */
data class LiveEvent(
    val type: LiveType,
    val user: String = "",
    val item: String = "",     // 礼物名
    val count: Int = 1,        // 数量/连击
    val amount: Double = 0.0,  // 抖币数；gift 且 >= bigGiftThreshold 会升格成大额礼物
    val text: String = "",     // 弹幕原文
)

/** 把桥转发的 JSON 行解析成事件；格式不对或类型不认识返回 null。 */
fun parseLiveEvent(line: String): LiveEvent? {
    val root = try {
        JSONObject(line)
    } catch (_: Throwable) {
        return null
    }
    val type = LiveType.from(root.optString("type")) ?: return null
    return LiveEvent(
        type = type,
        user = root.optString("user", ""),
        item = root.optString("item", ""),
        count = root.optInt("count", 1).coerceAtLeast(1),
        amount = root.optDouble("amount", 0.0).coerceAtLeast(0.0),
        text = root.optString("text", ""),
    )
}

/**
 * 反应引擎：决定「这条事件要不要反应、怎么反应」。
 *
 * @param pet 被驱动的桌宠
 * @param cfg 动画配置（用来挑庆祝动画）
 * @param say 说台词的回调，由 Service 接住塞进气泡（注意：这里不设 20s 总冷却，
 *            直播反应靠 [cooldownMs] 各自的冷却来控制节奏）
 */
class LiveReactor(
    private val pet: Pet,
    private val cfg: PetConfig,
    private val say: (String) -> Unit,
) {

    /** 每个类型的冷却（毫秒）。大额礼物最敏感，点赞/进场最克制。 */
    private val cooldownMs = mapOf(
        LiveType.GIFT to 2500L,
        LiveType.GIFT_BIG to 900L,
        LiveType.FOLLOW to 8000L,
        LiveType.LIKE to 20_000L,
        LiveType.COMMENT to 6000L,
        LiveType.ENTER to 15_000L,
    )
    private val lastAt = HashMap<LiveType, Long>()

    /** 抖币数 >= 这个值就当作「大额礼物」优先反应 */
    private val bigGiftThreshold = 100.0

    /** 弹幕命中关键词就播个庆祝动作，当作「听到了」。后续可做成设置项。 */
    private val keywordSet = listOf("关注", "唱歌", "跳舞", "唱首", "感谢", "666", "喜欢", "爱了")

    /** 清理用：目前无需释放资源，保留扩展位。 */
    fun release() {}

    /**
     * 处理一条直播事件。返回 true 表示「真的反应了」（测试/演示时用于提示）。
     */
    fun onEvent(raw: LiveEvent): Boolean {
        var e = raw
        if (e.type == LiveType.GIFT && e.amount >= bigGiftThreshold) {
            e = e.copy(type = LiveType.GIFT_BIG)
        }
        val now = SystemClock.uptimeMillis()
        val cd = cooldownMs[e.type] ?: 4000L
        if (now - (lastAt[e.type] ?: 0L) < cd) return false
        lastAt[e.type] = now

        when (e.type) {
            LiveType.GIFT -> {
                pet.bounce()
                say(e.category)
            }
            LiveType.GIFT_BIG -> {
                pet.bounce()
                celebrate()?.let { pet.playAction(it) }
                say(e.category)
            }
            LiveType.FOLLOW -> {
                pet.bounce()
                celebrate()?.let { pet.playAction(it) }
                say(e.category)
            }
            LiveType.LIKE -> {
                pet.bounce()
                say(e.category)
            }
            LiveType.COMMENT -> {
                if (keywordSet.any { it in e.text }) {
                    celebrate()?.let { pet.playAction(it) }
                } else {
                    pet.bounce()
                }
                say(e.category)
            }
            LiveType.ENTER -> say(e.category)
        }
        return true
    }

    /** 挑一段「庆祝」动画：优先「玩耍」分类，其次点击回应。没有就返回 null（不强求）。 */
    private fun celebrate(): String? {
        cfg.categories
            .firstOrNull { it.id.contains("玩耍") && it.actions.isNotEmpty() }
            ?.let { return it.actions.random() }
        if (cfg.clicks.isNotEmpty()) return cfg.clicks.random()
        return null
    }
}

/** 生成一条随机演示事件（设置页「测试：来一条」用），让用户不接桥也能看到反应效果。 */
fun randomDemoEvent(): LiveEvent {
    val gifts = listOf("小心心", "粉丝灯牌", "玫瑰", "棒棒糖", "火箭")
    val users = listOf("小可爱", "皮皮虾", "奶茶三分糖", "阿伟", "小鹿", "只想白嫖")
    val comments = listOf("主播真好看", "求关注", "唱首歌吧", "666", "笑死我了", "爱了爱了", "感谢主播")
    val user = users.random()
    return when (val type = LiveType.values().random()) {
        LiveType.GIFT -> LiveEvent(
            type, user = user, item = gifts.random(), count = Random.nextInt(1, 9),
        )
        LiveType.GIFT_BIG -> LiveEvent(
            type, user = user, item = "火箭", count = 1, amount = 1000.0,
        )
        LiveType.FOLLOW -> LiveEvent(type, user = user)
        LiveType.LIKE -> LiveEvent(type, user = user, count = Random.nextInt(10, 99))
        LiveType.COMMENT -> LiveEvent(type, user = user, text = comments.random())
        LiveType.ENTER -> LiveEvent(type, user = user)
    }
}
