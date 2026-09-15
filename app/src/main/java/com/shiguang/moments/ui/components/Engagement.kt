package com.shiguang.moments.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.composed
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shiguang.moments.scoring.Level
import kotlinx.coroutines.delay

/** 等级卡：渐变背景 + 经验值 + 进度条 + 详情；点击可进入等级详情页 */
@Composable
fun LevelCard(
    level: Level,
    progress: Float,
    totalMoments: Int,
    streak: Int,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val next = com.shiguang.moments.scoring.LevelCatalog.nextLevel(totalMoments)
    val needNext = (next?.from ?: totalMoments) - totalMoments
    // 等级图案：轻微上下浮动 + 缓缓呼吸，显得有生命
    val levelT = rememberInfiniteTransition(label = "lv")
    val bobY by levelT.animateFloat(0f, -6f, infiniteRepeatable(tween(1800), RepeatMode.Reverse), label = "bob")
    val bobScale by levelT.animateFloat(1f, 1.08f, infiniteRepeatable(tween(1800), RepeatMode.Reverse), label = "bobS")
    Card(
        modifier = modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
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
                        .graphicsLayer { translationY = bobY; scaleX = bobScale; scaleY = bobScale }
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
                    Text("经验 ${totalMoments}${if (next != null) " / ${next.from}" else " · 满级"} · " + level.title,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f))
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.18f),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (next == null) "已是最高等级"
                        else "再攒 $needNext 段经验升级 ${next.emoji} ${next.name}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                if (onClick != null) {
                    Spacer(Modifier.width(8.dp))
                    Text("查看 →", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly) {
            StatPill("$totalMoments", "经验值 XP")
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

/* ============ 通用动效工具 ============ */

/** 按压缩放反馈：按下缩一点，松手弹回 */
fun Modifier.pressScale(scale: Float = 0.955f): Modifier = composed {
    val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val s by animateFloatAsState(
        targetValue = if (pressed) scale else 1f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMedium),
        label = "press",
    )
    this.graphicsLayer { scaleX = s; scaleY = s }
}

/** 首页顶部大卡的"毛玻璃流光"：两团渐变光点缓缓流转 */
@Composable
fun FlowingGlass(
    base: Color,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val t = rememberInfiniteTransition(label = "flow")
    val x0 by t.animateFloat(0f, 1f, infiniteRepeatable(tween(5200), RepeatMode.Reverse), label = "x")
    val y0 by t.animateFloat(0.1f, 0.7f, infiniteRepeatable(tween(5200), RepeatMode.Reverse), label = "y")
    Box(
        modifier.drawBehind {
            val w = size.width; val h = size.height
            drawRect(
                Brush.linearGradient(
                    colors = listOf(base, accent.copy(alpha = 0.9f), base),
                    start = androidx.compose.ui.geometry.Offset(w * x0, 0f),
                    end = androidx.compose.ui.geometry.Offset(w * (1f - y0), h),
                ),
            )
        },
    )
}

/** 渐次入场：每个 item 错开 delay 淡入上浮（一次性） */
@Composable
fun RevealItem(index: Int, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val shown = remember { mutableStateOf(false) }
    val a by animateFloatAsState(
        targetValue = if (shown.value) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow),
        label = "revealA",
    )
    val y by animateFloatAsState(
        targetValue = if (shown.value) 0f else 26f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow),
        label = "revealY",
    )
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(index * 70L)
        shown.value = true
    }
    Box(modifier.graphicsLayer { alpha = a; translationY = y }) { content() }
}

/** 情绪化空状态：大 emoji + 柔和底圈 + 一句话 */
@Composable
fun FriendlyEmpty(emoji: String, title: String, subtitle: String, modifier: Modifier = Modifier) {
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
            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
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
                    // padding 必须非负：clamp 防崩
                    .padding(
                        start = (left + offset / 3f).coerceAtLeast(0f).dp,
                        top = (top - offset).coerceAtLeast(0f).dp,
                    )
                    .size(((i % 5 + 6).dp))
                    .alpha(0.6f - (offset / 720f).coerceAtMost(0.5f))
                    .clip(CircleShape)
                    .background(color, CircleShape),
            )
        }
    }
}

/** 保存成功反馈：爱心 + 温暖文案，从底部弹起、轻晃后淡出 */
@Composable
fun XpToast(text: String?, onShown: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    var last by remember { mutableStateOf<String?>(null) }
    var seed by remember { mutableStateOf(0) }

    val popScale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.7f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow),
        label = "pop",
    )
    val lift by animateFloatAsState(
        targetValue = if (visible) 0f else 24f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium),
        label = "lift",
    )
    val fade by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(if (visible) 220 else 400),
        label = "fade",
    )
    val weak by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow),
        label = "lift2",
    )
    val heartScale by animateFloatAsState(
        targetValue = if (visible) 1.18f else 0.9f,
        animationSpec = tween(600),
        label = "heart",
    )

    LaunchedEffect(text) {
        if (text != null && text != last) {
            last = text
            seed++
            visible = true
            delay(1900)
            visible = false
            delay(350)
            onShown()
        }
    }
    if (visible || fade > 0f) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            Card(
                shape = RoundedCornerShape(22.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier
                    .padding(bottom = 88.dp + lift.dp)
                    .graphicsLayer {
                        scaleX = popScale; scaleY = popScale
                        alpha = fade
                        rotationZ = (weak - 0.5f) * 2f * 2f // 轻晃
                    },
            ) {
                Row(Modifier.padding(horizontal = 18.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(30.dp).graphicsLayer { scaleX = heartScale; scaleY = heartScale }, contentAlignment = Alignment.Center) {
                        Text("💙", fontSize = 22.sp)
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(text ?: "", style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("+1 XP · 已珍藏", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f))
                    }
                }
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

/** 心情印章：一键打卡。表情横向可滑动；选中时弹簧放大 + 触感确认 */
private val MOOD_EMOJIS = listOf("😊", "😀", "🥰", "😌", "😴", "🤗", "😐", "😕", "😢", "😡", "🤩", "🥳")

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun MoodStampCard(
    todayEmoji: String?,
    onPick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    var flash by remember { mutableStateOf(0) } // 点击闪光计数（触发动画）
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val scale by animateFloatAsState(
        targetValue = if (flash > 0) 1f else 0.94f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
        label = "moodScale",
    )
    LaunchedEffect(flash) { if (flash > 0) delay(220) }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp).graphicsLayer { scaleX = scale; scaleY = scale }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                            if (todayEmoji != null) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                            else MaterialTheme.colorScheme.surfaceVariant,
                            CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(todayEmoji ?: "💭", fontSize = 22.sp)
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(if (todayEmoji == null) "点一下记下今天的心情" else "今天的心情",
                        style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(if (todayEmoji == null) "按下方的表情，1 秒盖章" else "已盖章 · 再点可改",
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (todayEmoji != null) {
                    AssistChip(onClick = { expanded = !expanded },
                        label = { Text("改一下") })
                }
            }
            AnimatedVisibility(visible = expanded) {
                // 横向可滑：LazyRow + 每个表情 48dp 大点按区域，弹簧反馈
                LazyRow(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(MOOD_EMOJIS, key = { it }) { e ->
                        val chosen = e == todayEmoji
                        val eb by animateFloatAsState(
                            targetValue = if (chosen) 1.15f else 1f,
                            animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium),
                            label = "emoji$e",
                        )
                        Box(
                            Modifier
                                .size(48.dp)
                                .graphicsLayer { scaleX = eb; scaleY = eb }
                                .clip(CircleShape)
                                .background(
                                    if (chosen) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                                    else Color.Transparent,
                                    CircleShape,
                                )
                                .clickable {
                                    expanded = false
                                    flash++
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    onPick(e)
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(e, fontSize = 28.sp)
                        }
                    }
                }
                Text("左右滑动看看更多", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (expanded || todayEmoji == null) {
                // 未盖章时默认就展示可滑表情条，引导立刻上手
                if (!expanded && todayEmoji == null) {
                    LazyRow(
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(MOOD_EMOJIS.take(10), key = { it }) { e ->
                            Box(
                                Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                                    .clickable {
                                        flash++
                                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                        onPick(e)
                                    },
                                contentAlignment = Alignment.Center,
                            ) { Text(e, fontSize = 20.sp) }
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