package com.shiguang.moments.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import coil.compose.AsyncImage
import com.shiguang.moments.data.models.MomentEntity
import com.shiguang.moments.ui.AppViewModel
import com.shiguang.moments.ui.components.EmptyState
import com.shiguang.moments.ui.components.Fmt
import com.shiguang.moments.ui.components.label
import java.io.File

private val HEALING = listOf(
    "有些人来过，就把光留了下来。",
    "记得的瞬间多了，日子日子就变得重量。",
    "把生活拆开看，快乐都在细节里。",
    "愿你后来想起，都还能笑出来。",
    "时间的尘埃里也藏着星星。",
    "这一秒会老，但它被你留住了。",
)

/**
 * 随机翻牌回忆卡：双面同时渲染会导致叠影；
 * 修复点 = 单面渲染 + 背面预旋转 180° 抵消镜像 + opacity 切换。
 */
@Composable
fun LuckyCardScreen(nav: NavHostController, vm: AppViewModel) {
    val moments by vm.moments.collectAsStateWithLifecycle()
    var current by remember { mutableStateOf<MomentEntity?>(null) }
    var flipped by remember { mutableStateOf(false) }

    LaunchedEffect(moments.size) {
        if (current == null && moments.isNotEmpty()) current = moments.random()
    }
    val rotation by animateFloatAsState(
        targetValue = if (flipped) 180f else 0f,
        // 弹簧回弹：翻到 90° 附近微微顿一下，质感更自然
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
        label = "flip",
    )

    Scaffold(topBar = {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") }
            Spacer(Modifier.weight(1f))
            Text("今日惊喜", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = {
                if (moments.isNotEmpty()) { current = moments.random(); flipped = false }
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
                Spacer(Modifier.height(20.dp))
                Box(
                    Modifier.weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    // 整体容器旋转：cameraDistance 给一点 3D 透视
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                rotationY = rotation
                                cameraDistance = 14f * density
                            }
                            .clickable(enabled = moments.isNotEmpty()) { flipped = !flipped },
                    ) {
                        // 单面渲染：旋转<90° 显示封面；≥90° 显示背面
                        // 背面预旋转 180° 抵消镜像
                        if (rotation < 90f) {
                            CardCover()
                        } else {
                            CardFace(m)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    HEALING[Math.floorMod(m.id.toInt(), HEALING.size)],
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = {
                        if (moments.isNotEmpty()) { current = moments.random(); flipped = false }
                    }) {
                        Icon(Icons.Filled.Shuffle, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("换一张")
                    }
                    Button(onClick = { vm.toggleStar(m.id) }) {
                        Icon(Icons.Filled.Star, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (m.starred) "取消星标" else "点亮星标")
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun CardCover() {
    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(420.dp)
                .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary))),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🃏", fontSize = 44.sp)
                Spacer(Modifier.height(12.dp))
                Text("拾光", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Color.White,
                    letterSpacing = 6.sp)
                Spacer(Modifier.height(6.dp))
                Text("点一下，翻开今天的记忆", color = Color.White.copy(alpha = 0.85f))
            }
        }
    }
}

@Composable
private fun CardFace(m: MomentEntity) {
    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = Modifier.graphicsLayer { rotationY = 180f }, // 抵消容器翻转，让文字正向
    ) {
        Column(Modifier.fillMaxWidth().height(420.dp).padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
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
                    modifier = Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(16.dp)),
                )
                if (paths.size > 1) {
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        paths.drop(1).take(4).forEach { p ->
                            AsyncImage(
                                model = File(p), contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(
                m.text.ifBlank { "[${m.type.label}]" },
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 6,
            )
            if (!m.quote.isNullOrBlank()) {
                Spacer(Modifier.height(10.dp))
                Text("「${m.quote}」", color = MaterialTheme.colorScheme.primary,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
            }
        }
    }
}