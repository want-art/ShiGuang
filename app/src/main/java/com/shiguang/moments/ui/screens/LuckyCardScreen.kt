package com.shiguang.moments.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
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

/** 封面固定浓饱和渐变（深浅模式都清晰） */
private val CoverA = Color(0xFF9A4560)
private val CoverB = Color(0xFFC76B2B)

/**
 * 随机翻卡（最稳版）：
 * 不用 3D 旋转/Animatable——纯 AnimatedContent 淡入淡出+缩放，
 * 天然稳定；封面浓饱和、白字，任何主题都清楚。
 */
@Composable
fun LuckyCardScreen(nav: NavHostController, vm: AppViewModel) {
    val moments by vm.moments.collectAsStateWithLifecycle()
    var current by remember { mutableStateOf<MomentEntity?>(null) }
    var revealed by remember { mutableStateOf(false) }

    LaunchedEffect(moments.size) {
        if (current == null && moments.isNotEmpty()) current = moments.random()
    }

    // 待机呼吸（只作用于封面侧）
    val idle = rememberInfiniteTransition(label = "idle")
    val idleBob by idle.animateFloat(-6f, 1f, infiniteRepeatable(tween(2400)), label = "bob")
    val glowAlpha by idle.animateFloat(0.35f, 0.8f, infiniteRepeatable(tween(2800)), label = "glow")
    val twinkle by idle.animateFloat(0f, 1f, infiniteRepeatable(tween(2000)), label = "tw")

    Scaffold(topBar = {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") }
            Spacer(Modifier.weight(1f))
            Text("今日惊喜", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = {
                if (moments.isNotEmpty()) { current = moments.random(); revealed = false }
            }) { Icon(Icons.Filled.Shuffle, "再抽一张") }
        }
    }) { padding ->
        if (moments.isEmpty()) {
            EmptyState(Icons.AutoMirrored.Filled.Notes, "回忆卡包还空着",
                "收藏第一段美好，才能抽出第一张惊喜", Modifier.padding(padding), emoji = "🃏")
        } else {
            val m = current ?: moments.last()
            Column(
                Modifier.padding(padding).fillMaxSize().padding(24.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.7f),
                                Color.Transparent,
                            ),
                        ),
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(14.dp))
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    // 暖色光晕（提升氛围、避免过白）
                    Box(
                        Modifier.size(360.dp).graphicsLayer { alpha = 0.35f + 0.45f * glowAlpha }
                            .background(Brush.radialGradient(
                                listOf(CoverB.copy(alpha = 0.35f), Color.Transparent)), CircleShape),
                    )
                    // 星光
                    Box(Modifier.align(Alignment.TopStart)
                        .padding(start = (26 + twinkle * 60).dp, top = (26 - twinkle * 16).dp)
                        .size(10.dp).graphicsLayer { alpha = 0.3f + 0.6f * twinkle }
                        .background(Color(0xFFFFD54F), CircleShape))
                    Box(Modifier.align(Alignment.BottomEnd)
                        .padding(end = (30 - twinkle * 46).dp, bottom = (42 + twinkle * 22).dp)
                        .size(8.dp).graphicsLayer { alpha = 0.6f - 0.3f * twinkle }
                        .background(Color(0xFFFF8A80), CircleShape))

                    // 核心：切换揭卡（无 3D，天然稳）
                    AnimatedContent(
                        targetState = revealed,
                        transitionSpec = {
                            if (targetState) {
                                (fadeIn(tween(300)) + scaleIn(initialScale = 0.86f, animationSpec = tween(340)))
                                    .togetherWith(fadeOut(tween(240)) + scaleOut(targetScale = 1.06f, animationSpec = tween(240)))
                            } else {
                                (fadeIn(tween(260)) + scaleIn(initialScale = 1.06f, animationSpec = tween(280)))
                                    .togetherWith(fadeOut(tween(220)))
                            }
                        },
                        label = "card",
                    ) { shown ->
                        if (!shown) {
                            CoverCard(idleBob = idleBob, onClick = { revealed = true })
                        } else {
                            ContentCard(m = m, vm = vm)
                        }
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
                        if (moments.isNotEmpty()) { current = moments.random(); revealed = false }
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
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
        colors = CardDefaults.cardColors(containerColor = CoverA),
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
            .graphicsLayer { translationY = idleBob }
            .clickable(onClick = onClick),
    ) {
        Box(
            Modifier.fillMaxSize().background(
                Brush.linearGradient(listOf(CoverA, CoverB))),
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
private fun ContentCard(m: MomentEntity, vm: AppViewModel) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
    ) {
        Column(Modifier.fillMaxWidth().height(400.dp).padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
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
                    modifier = Modifier.fillMaxWidth().height(175.dp).clip(RoundedCornerShape(16.dp)),
                )
                if (paths.size > 1) {
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        paths.drop(1).take(4).forEach { p ->
                            LocalImage(data = File(p), contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)))
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(m.text.ifBlank { "[${m.type.label}]" }, style = MaterialTheme.typography.bodyLarge, maxLines = 6)
            m.quote?.let { q ->
                Spacer(Modifier.height(10.dp))
                Text("「$q」", color = MaterialTheme.colorScheme.primary,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
            }
        }
    }
}