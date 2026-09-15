package com.shiguang.moments.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring.StiffnessMedium
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.shiguang.moments.data.models.MomentEntity
import com.shiguang.moments.ui.AppViewModel
import com.shiguang.moments.ui.components.EmptyState
import com.shiguang.moments.ui.components.Fmt
import com.shiguang.moments.ui.components.label
import java.io.File

private val HEALING = listOf(
    "有些人来过，就把光留了下来。",
    "记得的瞬间多了，日子就有了重量。",
    "把生活拆开看，快乐都在细节里。",
    "愿你后来想起，都还能笑出来。",
    "时间的尘埃里也藏着星星。",
    "这一秒会老，但它被你留住了。",
)

/** 彩虹粒子色板 */
private val RAINBOW = listOf(
    Color(0xFFE57373), Color(0xFFFFB74D), Color(0xFFFFF176),
    Color(0xFFAED581), Color(0xFF4DD0E1), Color(0xFF7986CB), Color(0xFFBA68C8),
)

/**
 * 翻卡 = 标准"双面单渲染"3D 翻转（绝不叠面、点击锁定防连点）+ 翻到位后内容逐块淡入。
 * 周围迸一圈轻量彩虹粒子（一次性 Canvas，≤14 粒，不拖帧）。
 */
@Composable
fun LuckyCardScreen(nav: NavHostController, vm: AppViewModel) {
    val moments by vm.moments.collectAsStateWithLifecycle()
    var current by remember { mutableStateOf<MomentEntity?>(null) }
    var revealed by remember { mutableStateOf(false) }
    var lock by remember { mutableStateOf(false) }
    var burstSeed by remember { mutableStateOf(0) }

    LaunchedEffect(moments.size) {
        if (current == null && moments.isNotEmpty()) current = moments.random()
    }

    // 用 Animatable 驱动：0→180 平滑翻转
    val animatable = remember { Animatable(0f) }
    LaunchedEffect(revealed) {
        lock = true
        animatable.animateTo(if (revealed) 180f else 0f, spring(dampingRatio = 0.78f, stiffness = StiffnessMedium))
        lock = false
        if (revealed) burstSeed++
    }
    val flipDeg = animatable.value

    // 待机呼吸（仅未翻时让封面活起来）
    val idle = rememberInfiniteTransition(label = "idle")
    val idleBob by idle.animateFloat(-6f, 1f, infiniteRepeatable(tween(2400)), label = "bob")
    val glowAlpha by idle.animateFloat(0.4f, 0.85f, infiniteRepeatable(tween(2800)), label = "glow")
    val twinkle by idle.animateFloat(0f, 1f, infiniteRepeatable(tween(2000)), label = "tw")

    Scaffold(topBar = {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") }
            Spacer(Modifier.weight(1f))
            Text("今日惊喜", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = {
                if (moments.isNotEmpty() && !lock) { current = moments.random(); revealed = false }
            }) { Icon(Icons.Filled.Shuffle, "再抽一张") }
        }
    }) { padding ->
        if (moments.isEmpty()) {
            EmptyState(Icons.AutoMirrored.Filled.Notes, "回忆卡包还空着",
                "收藏第一段美好，才能抽出第一张惊喜", Modifier.padding(padding))
        } else {
            val m = current ?: moments.last()
            Column(
                Modifier.padding(padding).fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(16.dp))
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    // 光晕 / 星光（始终在，轻）
                    Box(
                        Modifier.size(300.dp)
                            .graphicsLayer { alpha = 0.3f + 0.5f * glowAlpha }
                            .background(Brush.radialGradient(listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), Color.Transparent)), CircleShape),
                    )
                    Box(
                        Modifier.align(Alignment.TopStart)
                            .padding(start = (26 + twinkle * 60).dp, top = (26 - twinkle * 16).dp)
                            .size(10.dp)
                            .graphicsLayer { alpha = 0.3f + 0.6f * twinkle }
                            .background(MaterialTheme.colorScheme.tertiary, CircleShape),
                    )
                    Box(
                        Modifier.align(Alignment.BottomEnd)
                            .padding(end = (32 - twinkle * 46).dp, bottom = (42 + twinkle * 22).dp)
                            .size(8.dp)
                            .graphicsLayer { alpha = 0.6f - 0.3f * twinkle }
                            .background(MaterialTheme.colorScheme.secondary, CircleShape),
                    )

                    // 翻卡容器：双面单渲染
                    Box(
                        Modifier.fillMaxWidth().graphicsLayer {
                            rotationY = flipDeg
                            cameraDistance = 8f * density
                        },
                    ) {
                        if (flipDeg < 90f) {
                            CoverCard(
                                idleBob = idleBob,
                                onClick = { if (!lock) { revealed = true } },
                            )
                        } else {
                            // 内容面预先反 180°，抵消容器旋转，让文字正向
                            Box(Modifier.graphicsLayer { rotationY = 180f }) {
                                ContentCard(m = m, revealed = revealed, burstSeed = burstSeed)
                            }
                        }
                    }
                }

                // 彩虹粒子爆发（一次性，画在最上层，不随卡转）
                if (burstSeed > 0) RainbowBurst(seed = burstSeed)

                Spacer(Modifier.height(12.dp))
                Text(HEALING[Math.floorMod(m.id.toInt(), HEALING.size)],
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = {
                        if (moments.isNotEmpty() && !lock) { current = moments.random(); revealed = false }
                    }) {
                        Icon(Icons.Filled.Shuffle, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("换一张")
                    }
                    Button(onClick = { vm.toggleStar(m.id) }) {
                        Icon(Icons.Filled.Star, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp))
                        Text(if (m.starred) "取消星标" else "点亮星标")
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun CoverCard(idleBob: Float, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
            .graphicsLayer { translationY = idleBob }
            .clickable(onClick = onClick),
    ) {
        Box(
            Modifier.fillMaxSize().background(
                Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary))),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("💌", fontSize = 56.sp)
                Spacer(Modifier.height(14.dp))
                Text("拆开今天的回忆", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.height(6.dp))
                Text("点一下信封", color = Color.White.copy(alpha = 0.85f))
            }
        }
    }
}

@Composable
private fun ContentCard(m: MomentEntity, revealed: Boolean, burstSeed: Int) {
    val senderA by animateFloatAsState(targetValue = if (revealed) 1f else 0f, animationSpec = tween(260, delayMillis = 20), label = "s")
    val mediaA by animateFloatAsState(targetValue = if (revealed) 1f else 0f, animationSpec = tween(300, delayMillis = 180), label = "m")
    val textA by animateFloatAsState(targetValue = if (revealed) 1f else 0f, animationSpec = tween(300, delayMillis = 360), label = "t")
    val quoteA by animateFloatAsState(targetValue = if (revealed) 1f else 0f, animationSpec = tween(260, delayMillis = 520), label = "q")

    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = Modifier.graphicsLayer { alpha = senderA },
    ) {
        Column(Modifier.fillMaxWidth().height(400.dp).padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.graphicsLayer { alpha = senderA }) {
                Text(m.sender, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text(Fmt.fullDay(m.capturedAt), style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            val paths = m.allImagePaths()
            if (paths.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                AsyncImage(
                    model = File(paths.first()), contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(16.dp))
                        .graphicsLayer { alpha = mediaA },
                )
                if (paths.size > 1) {
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.graphicsLayer { alpha = mediaA }) {
                        paths.drop(1).take(4).forEach { p ->
                            AsyncImage(model = File(p), contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)))
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(m.text.ifBlank { "[${m.type.label}]" }, style = MaterialTheme.typography.bodyLarge, maxLines = 5,
                modifier = Modifier.graphicsLayer { alpha = textA })
            m.quote?.let { q ->
                Spacer(Modifier.height(10.dp))
                Text("「$q」", color = MaterialTheme.colorScheme.primary,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    modifier = Modifier.graphicsLayer { alpha = quoteA })
            }
        }
    }
}

/** 翻到位后，周围迸一圈小彩虹粒子（一次性、轻量） */
@Composable
private fun RainbowBurst(seed: Int) {
    val ring = remember(seed) { Animatable(0f) }
    LaunchedEffect(seed) { ring.animateTo(1f, tween(950)) }
    Canvas(Modifier.fillMaxSize()) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val p = ring.value
        val eased = 1f - (1f - p) * (1f - p) // easeOut
        for (i in 0 until 14) {
            val angle = Math.toRadians((i * 25.7 + seed * 7.0)).toFloat()
            val base = 96f + (i % 5) * 14f
            val dist = base * eased
            val x = cx + kotlin.math.cos(angle.toDouble()).toFloat() * dist
            val y = cy + kotlin.math.sin(angle.toDouble()).toFloat() * (dist * 0.82f)
            val fading = (1f - p).coerceIn(0f, 1f)
            val size = (7f + (i % 3) * 3f) * (1f + p * 0.6f)
            drawCircle(
                color = RAINBOW[i % RAINBOW.size].copy(alpha = fading),
                radius = size,
                center = Offset(x, y),
            )
        }
        // 前一瞬向外扩散的细圈
        if (p < 0.85f) {
            drawCircle(
                color = Color.White.copy(alpha = 0.35f * (1f - p)),
                radius = 40f + p * 170f,
                center = Offset(cx, cy),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5f),
            )
        }
    }
}