package com.shiguang.moments.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.shiguang.moments.ui.AppViewModel

/** 全屏「新建回忆」：专注写作 + 快捷（日期/加图/写给自己/大保存） */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewMomentScreen(nav: NavHostController, vm: AppViewModel) {
    val context = LocalContext.current
    val profile by vm.profile.collectAsStateWithLifecycle()

    var sender by remember { mutableStateOf("") }
    LaunchedEffect(profile.nickname) {
        if (sender.isBlank()) sender = profile.nickname.takeIf { it.isNotBlank() } ?: "我"
    }
    var note by remember { mutableStateOf("") }
    var imageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var atMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var atDayLabel by remember { mutableStateOf("今天") }
    var showDatePicker by remember { mutableStateOf(false) }
    var selfMode by remember { mutableStateOf(false) }
    val prompt = remember { com.shiguang.moments.util.DailyPrompts.promptForToday() }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = 9),
    ) { uris -> if (uris.isNotEmpty()) imageUris = imageUris + uris }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("随手记") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                placeholder = { Text(if (selfMode) "给今天的自己说一句……" else "这一刻想留住什么？") },
                minLines = 6,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = sender,
                onValueChange = { if (!selfMode) sender = it },
                label = { Text(if (selfMode) "写给自己" else "与谁的瞬间（可空）") },
                singleLine = true,
                enabled = !selfMode,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(14.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                AssistChip(onClick = { showDatePicker = true }, label = { Text("📅 ${if (!selfMode) atDayLabel else "今天"}") })
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                    Icon(Icons.Filled.CameraAlt, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp))
                    Text(if (imageUris.isEmpty()) "加图" else "再加（${imageUris.size}）")
                }
                Spacer(Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = selfMode, onCheckedChange = {
                        selfMode = it
                        if (it) sender = "我"
                    })
                    Spacer(Modifier.width(4.dp))
                    Text("写给自己", style = MaterialTheme.typography.labelMedium)
                }
            }

            if (imageUris.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    imageUris.take(6).forEach { uri ->
                        AsyncImage(
                            model = uri, contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(62.dp).clip(RoundedCornerShape(8.dp)),
                        )
                    }
                    if (imageUris.size > 6) {
                        Box(Modifier.size(62.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center) { Text("+${imageUris.size - 6}") }
                    }
                }
            }

            Spacer(Modifier.height(22.dp))

            Button(
                onClick = {
                    when {
                        selfMode && note.isNotBlank() -> { vm.writeToSelf(note.trim()) }
                        else -> vm.saveManual(sender, note, imageUris, atMillis)
                    }
                    nav.popBackStack()
                },
                enabled = (note.isNotBlank() || imageUris.isNotEmpty()) || (selfMode && note.isNotBlank()),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth().height(54.dp),
            ) { Text("收进回忆 💙", fontWeight = FontWeight.SemiBold) }

            if (prompt.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text("今日一题：${prompt}", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showDatePicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = com.shiguang.moments.util.KeyUtil.dayBoundaryStart(System.currentTimeMillis()),
            yearRange = 2020..(java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton({
                    state.selectedDateMillis?.let { selMs ->
                        val cal = java.util.Calendar.getInstance()
                        cal.timeInMillis = selMs
                        cal.set(java.util.Calendar.HOUR_OF_DAY, 12)
                        cal.set(java.util.Calendar.MINUTE, 0)
                        cal.set(java.util.Calendar.SECOND, 0)
                        atMillis = cal.timeInMillis
                        atDayLabel = java.text.SimpleDateFormat("M月d日", java.util.Locale.CHINA)
                            .format(java.util.Date(atMillis))
                        showDatePicker = false
                    }
                }) { Text("确定") }
            },
            dismissButton = { TextButton({ showDatePicker = false }) { Text("取消") } },
        ) { DatePicker(state = state) }
    }
}