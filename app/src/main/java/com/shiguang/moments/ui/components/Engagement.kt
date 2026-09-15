package com.shiguang.moments.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shiguang.moments.scoring.Level
import kotlinx.coroutines.delay

/** 等级卡：渐变背景 + 进度条 + 升级详情 */
@Composable
fun LevelCard(level: Level, progress: Float, totalMoments: Int, streak: Int, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.tertiaryContainer),
                    ),
                )
                .padding(18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(58.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(level.emoji, fontSize = 28.sp)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(level.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(8.dp))
                        Text("Lv.${level.tier}", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    Text(level.title, style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f))
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(progress = { progress.coerceIn(0f, 1f) }, Modifier.fillMaxWidth())
                    Spacer(Modifier.height(4.dp))
                    val next = com.shiguang.moments.scoring.LevelCatalog.nextLevel(totalMoments)
                    Text(
                        if (next == null) "已是最高等级" else "再记 ${(level.toInclusive.coerceAtMost(next.from - 1) - totalMoments).coerceAtLeast(0)} 段到 ${next.emoji} ${next.name}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly) {
            StatPill("$totalMoments", "珍藏瞬间")
            StatPill("$streak", "连续天数" + if (streak >= 3) " 🔥" else "")
            StatPill("Lv.${level.tier}", "等级")
        }
    }
}

@Composable
private fun StatPill(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** 升级全屏浮层：粒子背景 + 大字 + 旋转图标 + 自动消失 */
@Composable
fun LevelUpOverlay(level: Level, visible: Boolean, onDismiss: () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(400)) + slideInVertically(initialOffsetY = { it / 4 }, animationSpec = tween(500)),
        exit = fadeOut(tween(300)) + slideOutVertically(targetOffsetY = { -it / 4 }, animationSpec = tween(300)),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.55f))
                .clickable(enabled = true) { onDismiss() },
            contentAlignment = Alignment.Center,
        ) {
            Particles(level.tier)
            Card(
                shape = RoundedCornerShape(28.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
                modifier = Modifier
                    .padding(32.dp)
                    .clickable(enabled = false) { },
            ) {
                Column(
                    Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("✨", fontSize = 42.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("升级了！", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(6.dp))
                    Text("${level.emoji} Lv.${level.tier} ${level.name}",
                        style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(level.title, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center)
                    Spacer(Modifier.height(16.dp))
                    Text("点任意位置关闭", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline)
                }
            }
        }
    }
    // 自动 2.6s 后关闭
    LaunchedEffect(visible, level.tier) {
        if (visible) {
            delay(2600)
            onDismiss()
        }
    }
}

/** 彩色粒子围绕升级卡片缓慢上升 */
@Composable
private fun Particles(seed: Int) {
    val transition = rememberInfiniteTransition(label = "p")
    val t by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(3500, easing = LinearEasing), RepeatMode.Restart), label = "t")
    val palette = listOf(
        Color(0xFFE57373), Color(0xFFF06292), Color(0xFFFFB74D),
        Color(0xFFAED581), Color(0xFF4DD0E1), Color(0xFF7986CB),
    )
    Box(Modifier.fillMaxSize()) {
        repeat(14) { i ->
            val left = ((i * 73 + seed * 11) % 320) - 160
            val top = ((i * 91 + seed * 7) % 520) - 260
            val color = palette[(i + seed) % palette.size]
            val offset = (t * 360f - i * 23f) % 360f
            Box(
                Modifier
                    .align(Alignment.Center)
                    .padding(start = (left + offset / 3f).dp, top = (top - offset).dp)
                    .size(((i % 5 + 6).dp))
                    .alpha(0.6f - (offset / 720f).coerceAtMost(0.5f))
                    .clip(CircleShape)
                    .background(color, CircleShape),
            )
        }
    }
}

/** 右下角飘出的「+1 XP」轻提示 */
@Composable
fun XpToast(text: String?, onShown: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    var last by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(text) {
        if (text != null && text != last) {
            last = text
            visible = true
            delay(1300)
            visible = false
            delay(300)
            onShown()
        }
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(180)) + slideInVertically(initialOffsetY = { it }, animationSpec = tween(220)),
        exit = fadeOut(tween(220)) + slideOutVertically(targetOffsetY = { it }, animationSpec = tween(220)),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            Card(
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.padding(bottom = 92.dp),
            ) {
                Text(text ?: "", color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            }
        }
    }
}

/** 极轻的呼吸渐变背景（用于英雄卡片上方淡淡浮动） */
@Composable
fun BreathingTint(colors: List<Color>, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "b")
    val shift by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(6000, easing = LinearEasing), RepeatMode.Reverse), label = "s")
    Box(modifier
        .background(
            Brush.verticalGradient(
                listOf(colors.first().copy(alpha = 0.4f + 0.3f * shift),
                       colors.getOrElse(1) { colors.first() }.copy(alpha = 0.15f - 0.1f * shift)),
            ),
        )
    )
}

/** 心情印章：一键打卡。默认 9 个 emoji；点击展开 grid */
private val MOOD_EMOJIS = listOf("😊", "🥰", "😌", "😴", "😐", "😕", "😢", "😡", "🤩")

@Composable
fun MoodStampCard(
    todayEmoji: String?,
    onPick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Favorite, null, tint = MaterialTheme.colorScheme.tertiary)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("今天今天", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(if (todayEmoji == null) "点一下记下今天的心情" else "今天的心意：$todayEmoji",
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                AssistChip(onClick = { expanded = !expanded },
                    label = { Text(if (todayEmoji == null) "打卡" else "改一下") })
            }
            AnimatedVisibility(visible = expanded) {
                Row(Modifier.padding(top = 10.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MOOD_EMOJIS.forEach { e ->
                        Box(
                            Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (todayEmoji == e) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                    else Color.Transparent,
                                    CircleShape,
                                )
                                .clickable {
                                    onPick(e)
                                    expanded = false
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(e, fontSize = 22.sp)
                        }
                    }
                }
            }
        }
    }
}

/** 时光宝盒预告小徽章（贴于详情/首页） */
@Composable
fun CapsuleBadge(label: String, modifier: Modifier = Modifier) {
    Box(
        modifier
        .clip(RoundedCornerShape(12.dp))
        .background(MaterialTheme.colorScheme.tertiaryContainer, RoundedCornerShape(12.dp))
        .padding(horizontal = 8.dp, vertical = 4.dp),
    ) { Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onTertiaryContainer) }
}