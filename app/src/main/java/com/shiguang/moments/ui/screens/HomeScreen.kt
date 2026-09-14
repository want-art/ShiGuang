package com.shiguang.moments.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.shiguang.moments.ui.AppViewModel
import com.shiguang.moments.ui.components.Fmt
import com.shiguang.moments.ui.components.MomentCard
import java.util.Calendar

@Composable
fun HomeScreen(nav: NavHostController, vm: AppViewModel, modifier: Modifier = Modifier) {
    val moments by vm.moments.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val todayKey = Fmt.dayKey(System.currentTimeMillis())
    val todayCount = moments.count { Fmt.dayKey(it.capturedAt) == todayKey && it.source != "manual" }
    val yearAgoCount = moments.count { m ->
        val c = Calendar.getInstance().apply { timeInMillis = m.capturedAt }
        val now = Calendar.getInstance()
        c.get(Calendar.MONTH) == now.get(Calendar.MONTH) && c.get(Calendar.DAY_OF_MONTH) == now.get(Calendar.DAY_OF_MONTH) && c.get(Calendar.YEAR) < now.get(Calendar.YEAR)
    }

    var sender by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var imgUri by remember { mutableStateOf<Uri?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> imgUri = uri }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("拾光", fontSize = 30.sp, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary)
                    Text(greeting(), style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { nav.navigate("search") }) {
                    Icon(Icons.Filled.Search, "搜索回忆")
                }
            }
        }

        item { StatsRow(total = moments.size, today = todayCount, starred = moments.count { it.starred }) }

        item { LuckyHero(nav) }

        item { QuickAddCard(sender, { sender = it }, note, { note = it }, imgUri, { imgUri = it },
            onPick = { picker.launch("image/*") },
            onSave = {
                if (sender.isNotBlank() || note.isNotBlank() || imgUri != null) {
                    vm.saveManual(sender, note, imgUri)
                    sender = ""; note = ""; imgUri = null
                    Toast.makeText(context, "已收进回忆库 💙", Toast.LENGTH_SHORT).show()
                }
            },
        ) }

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

        if (moments.isEmpty()) {
            item {
                Text("还没有收藏任何瞬间。聊天里遇到打动你的话，点一下「值得记录」的通知就能留住。",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            val preview = moments.take(8)
            items(preview, key = { it.id }) { m ->
                MomentCard(m, onOpen = { nav.navigate("moment/${m.id}") })
            }
        }
    }
}

@Composable
private fun StatsRow(total: Int, today: Int, starred: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatChip("$total", "珍藏瞬间", Modifier.weight(1f))
        StatChip("$today", "今日新生", Modifier.weight(1f))
        StatChip("$starred", "星标之选", Modifier.weight(1f))
    }
}

@Composable
private fun StatChip(value: String, label: String, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun LuckyHero(nav: NavHostController) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(150.dp)
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
    imgUri: Uri?, onImg: (Uri?) -> Unit,
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
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = onPick) {
                    Icon(Icons.Filled.CameraAlt, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("加张图")
                }
                if (imgUri != null) {
                    Spacer(Modifier.width(10.dp))
                    AsyncImage(
                        model = imgUri, contentDescription = null,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(8.dp)),
                    )
                    IconButton(onClick = { onImg(null) }) { Text("✕") }
                }
                Spacer(Modifier.weight(1f))
                Button(onClick = onSave) { Text("收进回忆库") }
            }
        }
    }
}

private fun greeting(): String {
    val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        h < 5 -> "夜深了，记得好好睡"
        h < 11 -> "早上好，今天也有好事情发生"
        h < 14 -> "中午好，歇一歇看看回忆"
        h < 18 -> "下午好，来翻翻过去的小美好"
        else -> "晚上好，今天过得怎么样？"
    }
}