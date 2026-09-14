package com.shiguang.moments.scoring

import com.shiguang.moments.data.models.ChatMessage
import com.shiguang.moments.data.models.MomentType
import com.shiguang.moments.data.models.ScoreResult
import com.shiguang.moments.data.models.Verdict
import com.shiguang.moments.prefs.Profile
import java.util.Calendar

/**
 * 判分引擎（纯 Kotlin，可 JVM 单测）。
 * 输入一条消息 + 偏好档案 + 上下文信号，输出 0~100 与档位。
 */
object ScoringEngine {

    const val W_IMAGE = 14
    const val W_VOICE = 12
    const val W_STICKER = 6
    const val W_LONG = 18
    const val W_DEEP_NIGHT = 10
    const val W_BURST = 8
    const val W_STAR = 15
    const val W_LENGTHY = 8

    /** 明显的广告/系统推送噪声：命中直接归零 */
    val NOISE_MARKERS = listOf(
        "领取", "红包" , "收款", "支付", "验证码", "将体验", "开通", "会员", "优惠", "促销",
        "广告", "推送", "公众号", "公众平台", "服务通知", "活动来袭", "限时", "秒杀", "关注",
        "福利", "敬请期待", "订阅号", "账单", "扣费", "签到", "兑换码", "下载",
    )

    fun isDeepNight(epoch: Long): Boolean {
        val cal = Calendar.getInstance().apply { timeInMillis = epoch }
        val h = cal.get(Calendar.HOUR_OF_DAY)
        return h >= 23 || h <= 2
    }

    fun evaluate(msg: ChatMessage, profile: Profile, isStar: Boolean, burstCount: Int): ScoreResult {
        val reasons = mutableListOf<String>()
        val text = msg.text ?: ""

        // 噪声防护
        for (n in NOISE_MARKERS) if (text.contains(n, ignoreCase = true)) {
            return ScoreResult(0, Verdict.IGNORED, listOf("疑似广告/系统推送（含“$n”）"))
        }

        var score = 0

        // 消息类型
        when (msg.type) {
            MomentType.IMAGE -> { score += W_IMAGE; reasons += "朋友分享了图片" }
            MomentType.VOICE -> { score += W_VOICE; reasons += "收到一段语音" }
            MomentType.STICKER -> { score += W_STICKER; reasons += "刷表情" }
            MomentType.LONG_TEXT -> { score += W_LONG; reasons += "收到一段长文" }
            MomentType.TEXT -> if (text.length >= 40) { score += W_LENGTHY; reasons += "较长的消息" }
            else -> {}
        }
        // 长文长度再叠一点
        if (msg.type == MomentType.LONG_TEXT && text.length >= 80) score += 6

        // 特别的人
        if (isStar) { score += W_STAR; reasons += "特别的「$msg.sender」" }

        // 深夜
        if (isDeepNight(msg.sentAt)) { score += W_DEEP_NIGHT; reasons += "深夜时分" }

        // 对方高密度连发
        if (burstCount >= 3) { score += W_BURST; reasons += "$msg.sender 连发了 $burstCount 条" }

        // 关键词（只加“选了该主题”的词；通用暖词始终有效）
        val hitThemes = LinkedHashSet<String>()
        for (theme in ThemeCatalog.ALL) {
            if (theme.slug !in profile.themes) continue
            for (kw in theme.keywords) if (text.contains(kw)) {
                hitThemes += theme.slug
                reasons += "提到「$kw」"
                break // 每个主题计一次即可
            }
        }
        score += minOf(hitThemes.size * 4, 20)

        var generalHits = 0
        for (kw in ThemeCatalog.GENERAL.distinct()) {
            if (text.contains(kw)) {
                generalHits++
                reasons += "戳中「$kw」"
            }
        }
        score += minOf(generalHits * 5, 15)

        score = score.coerceIn(0, 100)

        val tier = when {
            score >= profile.autoThreshold -> Verdict.AUTO
            score >= profile.remindThreshold -> Verdict.PROMPT
            else -> Verdict.IGNORED
        }
        return ScoreResult(score, tier, reasons)
    }

    fun evaluateWithDefaults(
        text: String,
        type: com.shiguang.moments.data.models.MomentType,
        sentAt: Long,
        isStar: Boolean,
        burstCount: Int,
    ): ScoreResult = evaluate(
        ChatMessage(app = "", sender = "测试", text = text, type = type, sentAt = sentAt),
        Profile(themes = setOf("love"), remindThreshold = 50, autoThreshold = 78),
        isStar, burstCount,
    )
}