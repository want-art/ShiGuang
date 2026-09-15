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
        Level(1, "拾光新人",     "刚开启的拾光之旅", "🌱",
            from = 0,     toInclusive = 4),
        Level(2, "拾光记录者",   "开始愿意把美好记下来", "📒",
            from = 5,     toInclusive = 14),
        Level(3, "拾光守望者",   "已经能主动察觉",     "🌙",
            from = 15,    toInclusive = 39),
        Level(4, "拾光守夜人",   "把深夜也变温柔",     "🕯️",
            from = 40,    toInclusive = 99),
        Level(5, "拾光时光师",   "拥有了一片小宇宙",   "✨",
            from = 100,   toInclusive = 249),
        Level(6, "拾光往事收集者","成为一座会呼吸的纪念馆","📚",
            from = 250,   toInclusive = Int.MAX_VALUE),
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