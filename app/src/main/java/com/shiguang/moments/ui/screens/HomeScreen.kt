package com.shiguang.moments.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.shiguang.moments.ui.AppViewModel
import com.shiguang.moments.ui.components.Fmt
import com.shiguang.moments.ui.components.LevelCard
import com.shiguang.moments.ui.components.LevelUpOverlay
import com.shiguang.moments.ui.components.MomentCard
import com.shiguang.moments.ui.components.MoodStampCard
import com.shiguang.moments.ui.components.XpToast
import com.shiguang.moments.util.DailyPrompts
import com.shiguang.moments.util.KeyUtil
import com.shiguang.moments.util.Streak
import java.io.File

@Composable
fun HomeScreen(nav: NavHostController, vm: AppViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
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

    var sender by remember { mutableStateOf(profile.nickname.takeIf { it.isNotBlank() } ?: "我") }
    LaunchedEffect(profile.nickname) { sender = profile.nickname.takeIf { it.isNotBlank() } ?: "我" }
    var note by remember { mutableStateOf("") }
    var imageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    val prompt = remember { DailyPrompts.promptForToday() }
    var showLevelUp by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 9),
    ) { uris -> if (uris.isNotEmpty()) imageUris = imageUris + uris }

    val toastFlow by vm.toast.collectAsStateWithLifecycle(initialValue = null)
    val levelUpFlow by vm.levelUpEvent.collectAsStateWithLifecycle(initialValue = null)
    LaunchedEffect(levelUpFlow) {
        if (levelUpFlow != null) showLevelUp = true
    }

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
                        Text("Hi，${profile.nickname.take(8)} · 今天 ${todayCount} 段",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { nav.navigate("search") }) {
                        Icon(Icons.Filled.Search, "搜索回忆")
                    }
                }
            }
            item { LevelCard(level = level, progress = levelProgress, totalMoments = moments.size, streak = streak) }

            item { MoodStampCard(todayEmoji = todayMood?.emoji, onPick = { vm.recordMood(it) }) }

            if (showMagazine) {
                item { WeeklyMagazineCard(weekMoments = weekMoments, onKeep = { vm.setMagazineShown(currentWeek) }) }
            }

            item { DailyPromptCard(prompt = prompt, onUse = { note = it }) }

            item { LuckyHero(nav) }

            item {
                QuickAddCard(
                    sender = sender, onSender = { sender = it },
                    note = note, onNote = { note = it },
                    imageUris = imageUris, onImageUris = { imageUris = it },
                    onPick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    onSave = {
                        if (sender.isNotBlank() || note.isNotBlank() || imageUris.isNotEmpty()) {
                            vm.saveManual(sender, note, imageUris)
                            note = ""
                            imageUris = emptyList()
                            Toast.makeText(context, "已收进回忆库 💙", Toast.LENGTH_SHORT).show()
                        }
                    },
                )
            }

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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("最近的美好", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { nav.navigate("timeline") }) { Text("完整时间线 →") }
                }
            }
            val preview = moments.take(8)
            if (preview.isEmpty()) {
                item {
                    Text("还没有收藏任何瞬间。在上面随手记下第一笔，把心动的瞬间留住。",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                items(preview, key = { it.id }) { m ->
                    MomentCard(m, onOpen = { nav.navigate("moment/${m.id}") })
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }

        XpToast(text = toastFlow, onShown = {})
        LevelUpOverlay(level = level, visible = showLevelUp, onDismiss = { showLevelUp = false })
    }
}

@Composable
private fun DailyPromptCard(prompt: String, onUse: (String) -> Unit) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onUse(prompt) },
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Lightbulb, null, tint = MaterialTheme.colorScheme.tertiary)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(DailyPrompts.displayDate(), style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer)
                Text(prompt, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer, fontStyle = FontStyle.Italic)
            }
            AssistChip(onClick = { onUse(prompt) }, label = { Text("写下来") })
        }
    }
}

@Composable
private fun LuckyHero(nav: NavHostController) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(140.dp)
            .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)), RoundedCornerShape(22.dp))
            .clickable { nav.navigate("lucky") },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("✨ 随机翻开一段回忆", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(6.dp))
            Text("像抽卡一样，回到某个被记住的瞬间", color = Color.White.copy(alpha = 0.85f))
        }
    }
}

@Composable
private fun QuickAddCard(
    sender: String, onSender: (String) -> Unit,
    note: String, onNote: (String) -> Unit,
    imageUris: List<Uri>, onImageUris: (List<Uri>) -> Unit,
    onPick: () -> Unit, onSave: () -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(16.dp)) {
            Text("随手记一笔", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(value = sender, onValueChange = onSender, label = { Text("与谁的瞬间（可空）") },
                singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = note, onValueChange = onNote, label = { Text("记点什么…") },
                minLines = 2, modifier = Modifier.fillMaxWidth())
            if (imageUris.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    imageUris.take(4).forEach { uri ->
                        AsyncImage(
                            model = uri, contentDescription = null,
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(8.dp)),
                        )
                    }
                    if (imageUris.size > 4) {
                        Box(Modifier.size(54.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center) {
                            Text("+${imageUris.size - 4}", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = onPick) {
                    Icon(Icons.Filled.CameraAlt, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp))
                    Text(if (imageUris.isEmpty()) "加图（可多选）" else "再加")
                }
                Spacer(Modifier.weight(1f))
                Button(onClick = onSave) { Text("收进回忆库") }
            }
        }
    }
}

private fun greeting(): String {
    val h = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when {
        h < 5 -> "夜深了，记得好好睡"
        h < 11 -> "早上好，今天也有好事情发生"
        h < 14 -> "中午好，歇一歇看看回忆"
        h < 18 -> "下午好，来翻翻过去的小美好"
        else -> "晚上好，今天过得怎么样？"
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
                    OutlinedButton(onClick = onKeep) { Text("收下") }
                }
            }
        }
    }
}