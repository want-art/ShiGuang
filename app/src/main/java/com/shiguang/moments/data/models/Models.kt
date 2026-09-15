package com.shiguang.moments.data.models

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** 消息类型 */
enum class MomentType { TEXT, LONG_TEXT, IMAGE, VOICE, STICKER, OTHER }

/** 判定档位 */
enum class Verdict { IGNORED, PROMPT, AUTO }

/** 一条解析后的聊天消息（监听器输出 / 调试模拟器输入） */
data class ChatMessage(
    val app: String,      // 包名，如 com.tencent.mm
    val sender: String,
    val text: String,
    val type: MomentType,
    val sentAt: Long,     // epoch millis
)

/** 评分结果 */
data class ScoreResult(
    val score: Int,
    val tier: Verdict,
    val reasons: List<String>,
)

/** 已收藏的美好瞬间 */
@Entity(tableName = "moments", indices = [
    Index("capturedAt"),
    Index("sender"),
    Index("app"),
    Index("starred"),
    Index("capsuleRevealAt"),
])
data class MomentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val capturedAt: Long,          // 原聊天消息时间
    val createdAt: Long,           // 收藏时间
    val app: String,
    val sender: String,
    val type: MomentType,
    val text: String,
    val imagePath: String? = null, // 兼容历史：首张图片本地绝对路径
    val imagePaths: String = "",   // 多图：CSV，本地绝对路径
    val mediaSourceId: String? = null, // 相册归集去重（首图）
    val quote: String? = null,     // 闪亮引语
    val note: String? = null,      // 备注
    val themeTags: String = "",    // CSV：love,friendship,…
    val score: Int = 0,
    val starred: Boolean = false,
    val source: String = "auto",   // auto | tap | sim | a11y | gallery
    val capsuleRevealAt: Long? = null,    // 时光宝盒预约揭示时间
    val capsuleRevealedAt: Long? = null,  // 实际揭示时间（避免重复通知）
) {
    /** 主图：多图 CSV 的首张；否则取旧 imagePath */
    fun primaryImagePath(): String? =
        if (imagePaths.isNotBlank()) imagePaths.split(",").firstOrNull()?.trim()?.takeIf { it.isNotBlank() }
        else imagePath
    /** 全部图片（去空、去重） */
    fun allImagePaths(): List<String> {
        val list = if (imagePaths.isNotBlank()) imagePaths.split(",").map { it.trim() }.filter { it.isNotBlank() }
        else listOfNotNull(imagePath)
        return list.distinct()
    }
    val isCapsuleOpen: Boolean
        get() = capsuleRevealAt != null && capsuleRevealedAt != null
}

/** 每日心情（一键打卡） */
@Entity(tableName = "moods", indices = [Index(value = ["day"], unique = true)])
data class MoodEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val emoji: String,
    val ts: Long,
    val day: Long,             // 同一天的 dayKey（yyyy-MM-dd 的数值）
)

/** 联系人画像（星标联系人 + 关系统计） */
@Entity(tableName = "contacts", indices = [Index("name"), Index("app")])
data class ContactEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val app: String,
    val isStar: Boolean = false,
    val lastSeen: Long = 0,
    val moments: Int = 0,
)

/** 监听日志（用于校准与调试） */
@Entity(tableName = "logs", indices = [Index("ts")])
data class LogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ts: Long,
    val app: String,
    val sender: String,
    val text: String,
    val type: MomentType,
    val score: Int,
    val verdict: Verdict,
    val reason: String,
    val processedAs: String,   // prompt | auto | none
)

/** 待收藏的“值得记录”通知载荷（点击通知时据此入库） */
@Entity(tableName = "prompts")
data class PromptEntity(
    @PrimaryKey val pid: String,
    val payloadJson: String,
    val createdAt: Long,
    val saved: Boolean = false,
)