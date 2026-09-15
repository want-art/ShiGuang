package com.shiguang.moments.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.shiguang.moments.data.models.MomentEntity
import com.shiguang.moments.data.models.MomentType
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Fmt {
    private val d = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)
    private val full = SimpleDateFormat("yyyy年M月d日 EEEE", Locale.CHINA)
    private val hm = SimpleDateFormat("HH:mm", Locale.CHINA)
    private val short = SimpleDateFormat("yyyy年M月d日", Locale.CHINA)
    fun dayKey(ts: Long) = d.format(Date(ts))
    fun fullDay(ts: Long) = full.format(Date(ts))
    fun dayStringShort(ts: Long) = short.format(Date(ts))
    fun hm(ts: Long) = hm.format(Date(ts))
}

val MomentType.label: String
    get() = when (this) {
        MomentType.TEXT -> "文字"
        MomentType.LONG_TEXT -> "长文"
        MomentType.IMAGE -> "图片"
        MomentType.VOICE -> "语音"
        MomentType.STICKER -> "表情"
        MomentType.OTHER -> "瞬间"
    }

val MomentType.icon: ImageVector
    get() = when (this) {
        MomentType.IMAGE -> Icons.Filled.Image
        MomentType.VOICE -> Icons.Filled.Mic
        MomentType.STICKER -> Icons.Filled.EmojiEmotions
        else -> Icons.AutoMirrored.Filled.Notes
    }

@Composable
fun MomentCard(m: MomentEntity, onOpen: (Long) -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onOpen(m.id) },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            Icon(
                m.type.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(20.dp),
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(m.sender, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    Text(Fmt.hm(m.capturedAt), style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (m.starred) {
                        Icon(Icons.Filled.Star, null, tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.padding(start = 4.dp).size(14.dp))
                    }
                }
                Spacer(Modifier.height(6.dp))
                if (m.text.isNotBlank()) {
                    Text(
                        m.text,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                } else if (m.imagePath == null) {
                    Text("[${m.type.label}]", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (m.allImagePaths().isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    AsyncImage(
                        model = File(m.allImagePaths().first()),
                        contentDescription = "图片瞬间",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .height(180.dp),
                    )
                    if (m.allImagePaths().size > 1) {
                        Spacer(Modifier.height(4.dp))
                        Text("+${m.allImagePaths().size - 1} 张图", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, subtitle: String, modifier: Modifier = Modifier, emoji: String = "🌙") {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier
                .size(104.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerLow, CircleShape),
            contentAlignment = Alignment.Center,
        ) { Text(emoji, fontSize = 46.sp) }
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(subtitle, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}