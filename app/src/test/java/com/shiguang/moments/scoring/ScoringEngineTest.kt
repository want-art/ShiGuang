package com.shiguang.moments.scoring

import com.shiguang.moments.data.models.ChatMessage
import com.shiguang.moments.data.models.MomentType
import com.shiguang.moments.data.models.Verdict
import com.shiguang.moments.prefs.Profile
import com.shiguang.moments.scoring.ScoringEngine.evaluate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ScoringEngineTest {

    private fun tsAt(hour: Int, minute: Int = 30): Long =
        Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, minute) }.timeInMillis

    private val loveProfile = Profile(
        themes = setOf("love", "friendship", "milestone"),
        remindThreshold = 50, autoThreshold = 78,
    )

    @Test
    fun `深夜星标恋人连发长文关键词_应自动保存`() {
        val text = "宝宝，想你了，今天是我们的纪念日，生日快乐！忙完就特别想跟你说晚安，舍不得你，认识你真好，我会一直陪你，遇见你真好。你也要照顾好自己，我要看着你开心，明天见！么么哒，永远相守。"
        val r = evaluate(
            ChatMessage("com.tencent.mm", "大熊", text, MomentType.LONG_TEXT, tsAt(23, 45)),
            loveProfile, isStar = true, burstCount = 4,
        )
        assertEquals(Verdict.AUTO, r.tier)
        assertTrue("得分应 ≥78，实际 ${r.score}", r.score >= 78)
    }

    @Test
    fun `深夜星标恋人发图带暖话_应提醒收藏`() {
        val r = evaluate(
            ChatMessage("com.tencent.mm", "阿花", "快看这个，想你了，舍不得你，认识你真好。", MomentType.IMAGE, tsAt(0, 10)),
            loveProfile, isStar = true, burstCount = 0,
        )
        assertEquals(Verdict.PROMPT, r.tier)
    }

    @Test
    fun `深夜朋友连发长话不带星标_提示而不自动存`() {
        val r = evaluate(
            ChatMessage("com.tencent.mm", "小飞", "好久不见！今天翻照片想起你，想你了，舍不得，我们约周末见吧，认识你真好。",
                MomentType.IMAGE, tsAt(23, 5)),
            loveProfile, isStar = false, burstCount = 4,
        )
        assertTrue("应在提醒档（≥50），实际 ${r.score}", r.score >= 50)
        assertTrue("不应自动存（<78），实际 ${r.score}", r.score < 78)
    }

    @Test
    fun `明显广告噪声_应当忽略`() {
        val r = evaluate(
            ChatMessage("com.tencent.mm", "优惠助手", "限时秒杀！红包福利 快来领取", MomentType.TEXT, tsAt(15, 0)),
            loveProfile, isStar = false, burstCount = 0,
        )
        assertTrue(r.score == 0)
        assertEquals(Verdict.IGNORED, r.tier)
    }

    @Test
    fun `平凡一句好的_应当忽略`() {
        val r = evaluate(
            ChatMessage("com.tencent.mm", "同事", "好的", MomentType.TEXT, tsAt(10, 0)),
            loveProfile, isStar = false, burstCount = 0,
        )
        assertEquals(Verdict.IGNORED, r.tier)
    }

    @Test
    fun `未选中的主题关键词_不加分`() {
        val rNo = evaluate(
            ChatMessage("com.tencent.mm", "小黄", "今天新工作搞定了！", MomentType.TEXT, tsAt(14, 0)),
            Profile(themes = emptySet(), remindThreshold = 50, autoThreshold = 78),
            isStar = false, burstCount = 0,
        )
        // 工作成长主题未选 → “搞定”不计分；无通用暖词 → 应为 0
        assertEquals(0, rNo.score)
    }
}