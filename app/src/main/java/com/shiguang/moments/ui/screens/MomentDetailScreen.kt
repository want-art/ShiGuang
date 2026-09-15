package com.shiguang.moments.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.shiguang.moments.data.models.MomentEntity
import com.shiguang.moments.ui.AppViewModel
import com.shiguang.moments.ui.components.Fmt
import com.shiguang.moments.ui.components.LocalImage
import com.shiguang.moments.ui.components.label
import java.io.File

/** 瞬间详情：完整文字 + 多图缩略 + 引语/备注编辑 + 星标/删除 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MomentDetailScreen(nav: NavHostController, vm: AppViewModel, id: Long) {
    val moments by vm.moments.collectAsStateWithLifecycle()
    val m = moments.firstOrNull { it.id == id }
    if (m == null) {
        LaunchedEffect(Unit) { nav.popBackStack() }
        return
    }

    var editQuote by remember { mutableStateOf(false) }
    var editNote by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var scheduleCapsule by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = 9),
    ) { uris -> if (uris.isNotEmpty()) vm.attachImage(id, uris) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("一段美好") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") }
                },
                actions = {
                        IconButton(onClick = { vm.toggleStar(id) }) {
                            Icon(Icons.Filled.Star, "星标",
                                tint = if (m.starred) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Filled.Delete, "删除", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
            )
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(m.sender, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                AssistChip(onClick = {}, label = { Text(m.type.label) })
            }
            Text(Fmt.fullDay(m.capturedAt) + " · " + Fmt.hm(m.capturedAt),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(14.dp))

            val paths = m.allImagePaths()
            if (paths.isNotEmpty()) {
                LocalImage(
                    data = File(paths.first()), contentDescription = "图片瞬间",
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)),
                )
                if (paths.size > 1) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        paths.drop(1).forEach { p ->
                            LocalImage(
                                data = File(p), contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(10.dp)),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            if (m.text.isNotBlank()) {
                Text(m.text, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(16.dp))
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { editQuote = true }) {
                    Icon(Icons.Filled.FormatQuote, "加引语", tint = MaterialTheme.colorScheme.primary)
                }
                TextButton(onClick = { editNote = true }) {
                    Icon(Icons.Filled.Note, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("加备注")
                }
                IconButton(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                    Icon(Icons.Filled.AddAPhoto, "补充图片", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = { scheduleCapsule = true }) {
                    Icon(Icons.Filled.Schedule, "藏进时光宝盒",
                        tint = if (m.capsuleRevealAt != null) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary)
                }
            }
            if (m.capsuleRevealAt != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    if (m.capsuleRevealedAt != null) "📦 时光宝盒已开启"
                    else "📦 预约 ${java.text.SimpleDateFormat("yyyy年M月d日", java.util.Locale.CHINA).format(java.util.Date(m.capsuleRevealAt!!))} 回到这里",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }

            m.quote?.let { q ->
                Text("「$q」", style = MaterialTheme.typography.titleMedium, fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(10.dp))
            }
            m.note?.let { n ->
                Text(n, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(10.dp))
            }

            if (m.themeTags.isNotBlank()) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    m.themeTags.split(",").filter { it.isNotBlank() }.forEach { tag ->
                        AssistChip(onClick = {}, label = { Text(tag) })
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }

    if (scheduleCapsule) {
        CapsuleDialog(
            alreadyScheduled = m.capsuleRevealAt != null,
            onPick = { revealAt ->
                vm.scheduleCapsule(id, revealAt)
                scheduleCapsule = false
            },
            onCancel = {
                if (m.capsuleRevealAt != null) vm.cancelCapsule(id)
                scheduleCapsule = false
            },
            onDismiss = { scheduleCapsule = false },
        )
    }

    if (editQuote) {
        TextDialog(title = "给这段美好写一句引语", initial = m.quote ?: "",
            onConfirm = { vm.setQuote(id, it); editQuote = false }, onDismiss = { editQuote = false })
    }
    if (editNote) {
        TextDialog(title = "备注点什么", initial = m.note ?: "",
            onConfirm = { vm.setNote(id, it); editNote = false }, onDismiss = { editNote = false })
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("删掉这段回忆？") },
            text = { Text("删除后不可恢复。") },
            confirmButton = {
                TextButton(onClick = { vm.deleteMoment(id); confirmDelete = false; nav.popBackStack() }) { Text("删除") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("取消") } },
        )
    }
}

@Composable
fun TextDialog(title: String, initial: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var v by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(value = v, onValueChange = { v = it }, minLines = 2,
                modifier = Modifier.fillMaxWidth())
        },
        confirmButton = { TextButton(onClick = { onConfirm(v) }) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

/** 时光宝盒：选择预约揭示时间 */
@Composable
private fun CapsuleDialog(
    alreadyScheduled: Boolean,
    onPick: (Long) -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit,
) {
    val now = System.currentTimeMillis()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("📦 藏进时光宝盒") },
        text = {
            Column {
                Text("选一个未来的日子，让这段回忆悄悄回来见你。", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(12.dp))
                var h = java.util.Calendar.getInstance().apply { add(java.util.Calendar.MONTH, 1) }
                listOf(
                    "1 个月后" to (now + 30L * 24 * 3600 * 1000),
                    "半年后" to (now + 180L * 24 * 3600 * 1000),
                    "一年后" to (now + 365L * 24 * 3600 * 1000),
                ).forEach { (label, ts) ->
                    OutlinedButton(onClick = { onPick(ts) }, modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                        Text("${label} · ${java.text.SimpleDateFormat("yyyy年M月d日", java.util.Locale.CHINA).format(java.util.Date(ts))}")
                    }
                }
            }
        },
        confirmButton = { TextButton(onDismiss) { Text("取消") } },
        dismissButton = if (alreadyScheduled) {
            { TextButton(onCancel) { Text("取消宝盒") } }
        } else null,
    )
}