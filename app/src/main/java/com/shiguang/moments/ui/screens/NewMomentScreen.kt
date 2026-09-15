package com.shiguang.moments.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.shiguang.moments.ui.AppViewModel

/** 全屏「新建回忆」：便签纸质感编辑器 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewMomentScreen(nav: NavHostController, vm: AppViewModel) {
    val context = LocalContext.current
    val profile by vm.profile.collectAsStateWithLifecycle()

    var sender by remember { mutableStateOf("") }
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
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            // 便签纸主编辑器
            PaperEditor(
                value = note,
                onValueChange = { note = it },
                placeholder = if (selfMode) "给今天的自己说一句……" else "这一刻想留住什么？",
            )

            Spacer(Modifier.height(14.dp))

            // 与谁（便签式胶囊输入）
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AssistChip(onClick = { showDatePicker = true }, label = { Text("📅 ${if (!selfMode) atDayLabel else "今天"}") })
                OutlinedPillButton(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                    Icon(Icons.Filled.CameraAlt, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp))
                    Text(if (imageUris.isEmpty()) "加图" else "再加（${imageUris.size}）")
                }
                Spacer(Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = selfMode, onCheckedChange = { selfMode = it }, modifier = Modifier
                        .clip(RoundedCornerShape(24.dp)))
                    Spacer(Modifier.width(4.dp))
                    Text("写给自己", style = MaterialTheme.typography.labelMedium)
                }
            }

            // 与谁的胶囊输入
            Spacer(Modifier.height(12.dp))
            CapsuleTextInput(
                value = sender,
                onValueChange = { if (!selfMode) sender = it },
                label = if (selfMode) "写给自己" else "和谁有关（可空）",
                enabled = !selfMode,
            )

            if (imageUris.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    imageUris.take(6).forEach { uri ->
                        AsyncImage(model = uri, contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(64.dp).clip(RoundedCornerShape(10.dp)))
                    }
                    if (imageUris.size > 6) {
                        Box(Modifier.size(64.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center) { Text("+${imageUris.size - 6}") }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // 渐变保存大按钮
            GradientSaveButton(
                enabled = note.isNotBlank() || imageUris.isNotEmpty(),
                text = if (selfMode) "收进回忆，写给自己 💛" else "收进回忆 💙",
                onClick = {
                    when {
                        selfMode && note.isNotBlank() -> vm.writeToSelf(note.trim())
                        else -> vm.saveManual(sender, note, imageUris, atMillis)
                    }
                    nav.popBackStack()
                },
            )

            if (prompt.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Text("💡 $prompt", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(28.dp))
        }
    }

    if (showDatePicker) {
        DatePickDialog(
            onPick = { ms ->
                val cal = java.util.Calendar.getInstance()
                cal.timeInMillis = ms
                atMillis = cal.timeInMillis
                atDayLabel = java.text.SimpleDateFormat("M月d日", java.util.Locale.CHINA).format(java.util.Date(atMillis))
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickDialog(onPick: (Long) -> Unit, onDismiss: () -> Unit) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = com.shiguang.moments.util.KeyUtil.dayBoundaryStart(System.currentTimeMillis()),
        yearRange = 2020..(java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)),
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton({
                state.selectedDateMillis?.let { selMs ->
                    val cal = java.util.Calendar.getInstance()
                    cal.timeInMillis = selMs
                    cal.set(java.util.Calendar.HOUR_OF_DAY, 12)
                    cal.set(java.util.Calendar.MINUTE, 0)
                    cal.set(java.util.Calendar.SECOND, 0)
                    onPick(cal.timeInMillis)
                }
            }) { Text("确定") }
        },
        dismissButton = { TextButton(onDismiss) { Text("取消") } },
    ) { DatePicker(state = state) }
}

/** 手写便签感的主编辑区 */
@Composable
private fun PaperEditor(value: String, onValueChange: (String) -> Unit, placeholder: String) {
    Card(
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(24.dp),
        elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = TextStyle(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 17.sp,
                    lineHeight = 26.sp,
                ),
                modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp),
                decorationBox = { inner ->
                    Box {
                        if (value.isEmpty()) {
                            Text(placeholder,
                                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, lineHeight = 26.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                        }
                        inner()
                    }
                },
            )
        }
        // 底部一行淡淡的"便签折角"装饰
        Box(
            Modifier.fillMaxWidth().height(4.dp).background(
                Brush.horizontalGradient(listOf(
                    Color.Transparent,
                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.25f),
                    Color.Transparent,
                ))),
        )
    }
}

@Composable
private fun CapsuleTextInput(value: String, onValueChange: (String) -> Unit, label: String, enabled: Boolean) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().height(44.dp),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) {
                        Text(label, style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                    }
                    inner()
                }
            },
        )
    }
}

@Composable
private fun OutlinedPillButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) { androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) { content() } }
}

@Composable
private fun GradientSaveButton(enabled: Boolean, text: String, onClick: () -> Unit) {
    val bg = Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary))
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .graphicsLayer { alpha = if (enabled) 1f else 0.45f }
            .clickable(enabled = enabled, onClick = onClick)
            .height(56.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
    }
}