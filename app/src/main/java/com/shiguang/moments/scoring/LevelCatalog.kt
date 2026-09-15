package com.shiguang.moments.scoring

/**
 * 等级表：以「累计珍藏瞬间数」为 XP 增长曲线，分 6 段。
 * 每升一级会有专属文案与渐变色；等级越高，主题越暖。
 */
data class Level(
    val tier: Int,
    val name: String,
    val title: String,
    val emoji: String,
    val from: Int,        // 当前等级起点（含）
    val toInclusive: Int, // 当前等级终点（含）；最后一档为 Int.MAX_VALUE
)

object LevelCatalog {

    private val TABLE = listOf(
        Level(1,  "拾光嫩芽",     "刚种下一颗回忆的种子", "🌱",   from = 0,    toInclusive = 2),
        Level(2,  "拾光微光",     "第一缕光落在了笔尖",   "🌟",   from = 3,    toInclusive = 6),
        Level(3,  "拾光记录者",   "开始把日子轻轻收好",   "📒",   from = 7,    toInclusive = 12),
        Level(4,  "拾光守望者",   "学会了留意每句暖话",   "🌙",   from = 13,   toInclusive = 20),
        Level(5,  "拾光旅人",     "在生活里走走停停",     "🧭",   from = 21,   toInclusive = 30),
        Level(6,  "拾光守夜人",   "把深夜也酿成了温柔",   "🕯️",   from = 31,   toInclusive = 42),
        Level(7,  "拾光收集家",   "图片、文字、声音都归我", "📷", from = 43,   toInclusive = 58),
        Level(8,  "拾光时光师",   "拥有了属于你的小宇宙", "✨",   from = 59,   toInclusive = 78),
        Level(9,  "拾光回忆馆主", "这座馆子里有你的四季", "🏛️",   from = 79,   toInclusive = 104),
        Level(10, "拾光守星人",   "替日子守住每一颗星",   "🌌",   from = 105,  toInclusive = 140),
        Level(11, "拾光时光守护者","把回忆酿成了老酒",   "⏳",   from = 141,  toInclusive = 190),
        Level(12, "拾光永恒见证者","成为人间的记忆化身", "🌈",   from = 191,  toInclusive = Int.MAX_VALUE),
    )

    val ALL: List<Level> get() = TABLE

    fun levelFor(total: Int): Level = TABLE.lastOrNull { total >= it.from } ?: TABLE.first()

    /** 距下一级进度 [0, 1)；到达顶级返回 1.0 */
    fun progressToNext(total: Int): Float {
        val cur = levelFor(total)
        if (cur.toInclusive == Int.MAX_VALUE) return 1f
        val span = (cur.toInclusive - cur.from + 1).toFloat().coerceAtLeast(1f)
        val into = (total - cur.from + 1).toFloat().coerceAtLeast(0f)
        return (into / span).coerceIn(0f, 1f)
    }

    fun nextLevel(total: Int): Level? = TABLE.firstOrNull { total < it.from }
}