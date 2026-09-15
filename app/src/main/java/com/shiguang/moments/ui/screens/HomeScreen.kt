package com.shiguang.moments.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.shiguang.moments.ui.AppViewModel
import com.shiguang.moments.ui.components.FlowingGlass
import com.shiguang.moments.ui.components.Fmt
import com.shiguang.moments.ui.components.LevelCard
import com.shiguang.moments.ui.components.LevelUpOverlay
import com.shiguang.moments.ui.components.MoodStampCard
import com.shiguang.moments.ui.components.RevealItem
import com.shiguang.moments.ui.components.XpToast
import com.shiguang.moments.ui.components.pressScale
import com.shiguang.moments.util.DailyPrompts
import com.shiguang.moments.util.KeyUtil
import com.shiguang.moments.util.Streak
import java.io.File

@Composable
fun HomeScreen(nav: NavHostController, vm: AppViewModel, modifier: Modifier = Modifier) {
    val moments by vm.moments.collectAsStateWithLifecycle()
    val moods by vm.moods.collectAsStateWithLifecycle()
    val profile by vm.profile.collectAsStateWithLifecycle()
    val level by vm.level.collectAsStateWithLifecycle()
    val levelProgress by vm.levelProgress.collectAsStateWithLifecycle()

    val todayKey = Fmt.dayKey(System.currentTimeMillis())
    val todayMood = moods.firstOrNull { it.day == KeyUtil.currentDayLong() }
    val todayCount = moments.count { Fmt.dayKey(it.capturedAt) == todayKey }
    val yearAgoCount = moments.count { m ->
        val c = java.util.Calendar.getInstance().apply { timeInMillis = m.capturedAt }
        val now = java.util.Calendar.getInstance()
        c.get(java.util.Calendar.MONTH) == now.get(java.util.Calendar.MONTH) &&
            c.get(java.util.Calendar.DAY_OF_MONTH) == now.get(java.util.Calendar.DAY_OF_MONTH) &&
            c.get(java.util.Calendar.YEAR) < now.get(java.util.Calendar.YEAR)
    }
    val streak = Streak.of(moments, moods)
    val currentWeek = KeyUtil.currentWeekId()
    val weekMoments = moments.filter { KeyUtil.weekId(it.capturedAt) == currentWeek }
    val showMagazine = weekMoments.isNotEmpty() && profile.lastMagazineWeekId != currentWeek

    var showLevelUp by remember { mutableStateOf(false) }
    val toastFlow by vm.toast.collectAsStateWithLifecycle(initialValue = null)
    val levelUpFlow by vm.levelUpEvent.collectAsStateWithLifecycle(initialValue = null)
    LaunchedEffect(levelUpFlow) { if (levelUpFlow != null) showLevelUp = true }

    Box(modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("拾光", fontSize = 28.sp, fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary)
                        Text("${DailyPrompts.displayDate()} · 今天 ${todayCount} 段",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { nav.navigate("search") }) {
                        Icon(Icons.Filled.Search, "搜索回忆")
                    }
                }
            }

            // 写优先：最上面的醒目入口
            item { RevealItem(0) { WriteEntry(onClick = { nav.navigate("new") }) } }

            item { RevealItem(1) { LevelCard(level = level, progress = levelProgress, totalMoments = moments.size, streak = streak,
                onClick = { nav.navigate("levels") }) } }

            item { RevealItem(2) { MoodStampCard(todayEmoji = todayMood?.emoji, onPick = { vm.recordMood(it) }) } }

            if (showMagazine) {
                item { RevealItem(3) { WeeklyMagazineCard(weekMoments = weekMoments, onKeep = { vm.setMagazineShown(currentWeek) }) } }
            }

            item { RevealItem(4) { LuckyHero(nav) } }

            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                    Row(Modifier
                        .fillMaxWidth()
                        .clickable { nav.navigate("yearago") }
                        .padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.AutoAwesome, null, tint = MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.width(10.dp))
                        Text("那年今日", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.weight(1f))
                        Text(if (yearAgoCount > 0) "往年今天有 $yearAgoCount 段回忆" else "今天还没有往年回忆",
                            style = MaterialTheme.typography.labelMedium)
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.size(16.dp))
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { nav.navigate("timeline") },
                ) {
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("已收藏 ${moments.size} 段美好",
                            style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.weight(1f))
                        Text("去时间线看全部 →", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }

        XpToast(text = toastFlow, onShown = {})
        LevelUpOverlay(level = level, visible = showLevelUp, onDismiss = { showLevelUp = false })

        // 悬浮「＋」随手记
        FloatingActionButton(
            onClick = { nav.navigate("new") },
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 96.dp).pressScale(),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ) { Icon(Icons.Filled.Add, "随手记") }
    }
}

@Composable
private fun WriteEntry(onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier
            .fillMaxWidth()
            .pressScale()
            .clickable(onClick = onClick),
    ) {
        Box {
            FlowingGlass(
                base = MaterialTheme.colorScheme.secondaryContainer,
                accent = MaterialTheme.colorScheme.tertiaryContainer,
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
            Box(
                Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Filled.Edit, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(22.dp)) }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("随手记一笔", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("把此刻变成以后的回忆", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("✍️", fontSize = 30.sp)
            }
        }
    }
}

@Composable
private fun LuckyHero(nav: NavHostController) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(22.dp))
            .clickable { nav.navigate("lucky") },
        contentAlignment = Alignment.Center,
    ) {
        FlowingGlass(base = MaterialTheme.colorScheme.primary, accent = MaterialTheme.colorScheme.tertiary)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("✨ 随机翻开一段回忆", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(4.dp))
            Text("像抽卡一样，回到某个被记住的瞬间", color = Color.White.copy(alpha = 0.85f))
        }
    }
}

private val MAG_TITLES = listOf(
    "本周也在好好生活", "这一周的光", "把日子过成了诗",
    "攒下的小确幸", "平凡里有星光", "本周的温柔记录",
)

/** 本周杂志：每周末生成一次，封面+标题+mini 图集，收下即关闭 */
@Composable
private fun WeeklyMagazineCard(weekMoments: List<com.shiguang.moments.data.models.MomentEntity>, onKeep: () -> Unit) {
    val cover = weekMoments
        .mapNotNull { it.allImagePaths().firstOrNull() }
        .firstOrNull()
    val imgCount = weekMoments.sumOf { it.allImagePaths().size }
    val title = remember(weekMoments) {
        if (weekMoments.isEmpty()) "本周" else MAG_TITLES[Math.floorMod(weekMoments.first().id.toInt(), MAG_TITLES.size)]
    }
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            if (cover != null) {
                AsyncImage(
                    model = File(cover), contentDescription = null,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                )
            }
            Column(Modifier.padding(14.dp)) {
                Text("📖 本周报纸", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.tertiary)
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("本周 ${weekMoments.size} 段记录${if (imgCount > 0) " · $imgCount 张图" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (weekMoments.isNotEmpty() && weekMoments.first().text.isNotBlank()) {
                        Text("「${weekMoments.first().text.take(24)}…」",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f).padding(end = 8.dp))
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                    androidx.compose.material3.OutlinedButton(onClick = onKeep) { Text("收下") }
                }
            }
        }
    }
}