package com.shiguang.moments.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring.StiffnessLow
import androidx.compose.animation.core.animateFloatAsState
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
import com.shiguang.moments.data.models.MomentEntity
import com.shiguang.moments.ui.AppViewModel
import com.shiguang.moments.ui.components.EmptyState
import com.shiguang.moments.ui.components.Fmt
import com.shiguang.moments.ui.components.LocalImage
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

private val CoverA = Color(0xFF9A4560)
private val CoverB = Color(0xFFC76B2B)
private val RAINBOW = listOf(
    Color(0xFFE57373), Color(0xFFFFB74D), Color(0xFFFFF176),
    Color(0xFFAED581), Color(0xFF4DD0E1), Color(0xFF7986CB), Color(0xFFBA68C8),
)

/**
 * 随机翻卡：3D 双面翻转 + 内容渐显 + 彩虹烟火。
 * 这一版去掉了所有“把动画写进 padding/参数”导致的每帧重排（白屏/卡顿的元凶）；
 * 烟火也只画在卡片区域，纯绘制不触发布局。
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

    val animatable = remember { Animatable(0f) }
    LaunchedEffect(revealed) {
        lock = true
        animatable.animateTo(if (revealed) 180f else 0f, spring(dampingRatio = 0.82f, stiffness = StiffnessLow))
        lock = false
        if (revealed) burstSeed++
    }
    val flipDeg = animatable.value

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
                "收藏第一段美好，才能抽出第一张惊喜", Modifier.padding(padding), emoji = "🃏")
        } else {
            val m = current ?: moments.last()
            Column(
                Modifier.padding(padding).fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(12.dp))
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Box(
                        Modifier.fillMaxWidth().graphicsLayer {
                            rotationY = flipDeg
                            cameraDistance = 8f * density
                        },
                    ) {
                        if (flipDeg < 90f) {
                            CoverCard(onClick = { if (!lock) revealed = true })
                        } else {
                            Box(Modifier.graphicsLayer { rotationY = 180f }) {
                                ContentCard(m = m, revealed = revealed)
                            }
                        }
                    }
                    // 烟火：只画在卡片区域，纯 Canvas，不参与布局
                    if (burstSeed > 0) {
                        Box(Modifier.matchParentSize()) { RainbowBurst(seed = burstSeed) }
                    }
                }

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
                    Button(onClick = { if (!lock) vm.toggleStar(m.id) }) {
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
private fun CoverCard(onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
        colors = CardDefaults.cardColors(containerColor = CoverA),
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
            .clickable(onClick = onClick),
    ) {
        Box(
            Modifier.fillMaxSize().background(Brush.linearGradient(listOf(CoverA, CoverB))),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("💌", fontSize = 58.sp)
                Spacer(Modifier.height(14.dp))
                Text("拆开今天的回忆", fontSize = 24.sp, fontWeight = FontWeight.Bold,
                    color = Color.White, letterSpacing = 2.sp)
                Spacer(Modifier.height(8.dp))
                Text("点一下信封", color = Color.White.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun ContentCard(m: MomentEntity, revealed: Boolean) {
    val senderA by animateFloatAsState(if (revealed) 1f else 0f, tween(300, delayMillis = 40), label = "s")
    val mediaA by animateFloatAsState(if (revealed) 1f else 0f, tween(340, delayMillis = 240), label = "m")
    val textA by animateFloatAsState(if (revealed) 1f else 0f, tween(340, delayMillis = 420), label = "t")
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
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
                LocalImage(
                    data = File(paths.first()), contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(170.dp).clip(RoundedCornerShape(16.dp))
                        .graphicsLayer { alpha = mediaA },
                )
                if (paths.size > 1) {
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.graphicsLayer { alpha = mediaA }) {
                        paths.drop(1).take(4).forEach { p ->
                            LocalImage(data = File(p), contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(42.dp).clip(RoundedCornerShape(8.dp)))
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(m.text.ifBlank { "[${m.type.label}]" }, style = MaterialTheme.typography.bodyLarge, maxLines = 6,
                modifier = Modifier.graphicsLayer { alpha = textA })
            m.quote?.let { q ->
                Spacer(Modifier.height(10.dp))
                Text("「$q」", color = MaterialTheme.colorScheme.primary,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
            }
        }
    }
}

/** 翻完后迸一圈小彩虹（一次性；纯 Canvas，不参与布局） */
@Composable
private fun RainbowBurst(seed: Int) {
    val ring = remember(seed) { Animatable(0f) }
    LaunchedEffect(seed) { ring.animateTo(1f, tween(950)) }
    Canvas(Modifier.fillMaxSize()) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val p = ring.value
        val eased = 1f - (1f - p) * (1f - p)
        for (i in 0 until 14) {
            val angle = Math.toRadians((i * 25.7 + seed * 7.0)).toFloat()
            val dist = (96f + (i % 5) * 14f) * eased
            val x = cx + kotlin.math.cos(angle.toDouble()).toFloat() * dist
            val y = cy + kotlin.math.sin(angle.toDouble()).toFloat() * (dist * 0.82f)
            val fading = (1f - p).coerceIn(0f, 1f)
            drawCircle(
                color = RAINBOW[i % RAINBOW.size].copy(alpha = fading),
                radius = (7f + (i % 3) * 3f) * (1f + p * 0.6f),
                center = Offset(x, y),
            )
        }
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