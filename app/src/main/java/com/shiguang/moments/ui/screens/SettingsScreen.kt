package com.shiguang.moments.ui.screens

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.HeartBroken
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.shiguang.moments.BuildConfig
import com.shiguang.moments.data.models.MomentType
import com.shiguang.moments.prefs.Profile
import com.shiguang.moments.scoring.ThemeCatalog
import com.shiguang.moments.service.KeepAliveService
import com.shiguang.moments.service.PermissionGate
import com.shiguang.moments.ui.AppViewModel
import kotlinx.coroutines.launch

/** 「我」：权限与守护 / 偏好档案 / 收藏判断 / 星标联系人 / 数据 / 调试 */
@Composable
fun SettingsScreen(nav: NavHostController, vm: AppViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val profile by vm.profile.collectAsStateWithLifecycle()
    val moments by vm.moments.collectAsStateWithLifecycle()
    var wipeConfirm by remember { mutableStateOf(false) }

    val mediaLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    val htmlExporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/html")) { uri ->
        if (uri != null) scope.launch { vm.export(uri, true); Toast.makeText(context, "HTML 已导出", Toast.LENGTH_SHORT).show() }
    }
    val jsonExporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch { vm.export(uri, false); Toast.makeText(context, "JSON 已导出", Toast.LENGTH_SHORT).show() }
    }

    Column(modifier.fillMaxSize()) {
        Row(Modifier.padding(horizontal = 20.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("拾光", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text("偏好与守护", style = MaterialTheme.typography.titleMedium)
        }

        LazyColumn(
            Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { SectionTitle("权限与守护") }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Column(Modifier.padding(8.dp)) {
                        SettingRow("通知监听", desc = "识别美好瞬间的入口",
                            trailing = { StatusOk(profile.listeningEnabled && PermissionGate.hasNotificationAccess(context)) }) {
                            if (PermissionGate.hasNotificationAccess(context)) vm.setListening(!profile.listeningEnabled)
                            else context.startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
                        }
                        SettingRow("相册自动归集", desc = "在微信长按保存的图自动挂到瞬间",
                            trailing = { Switch(profile.galleryImportEnabled, {
                                vm.setGallery(it)
                                if (it && !PermissionGate.hasMediaRead(context)) {
                                    mediaLauncher.launch(
                                        if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_IMAGES
                                        else Manifest.permission.READ_EXTERNAL_STORAGE,
                                    )
                                }
                            }) }) {}
                        SettingRow("聊天长文捕获（无障碍）", desc = "聊天界面的整段长文也能留档",
                            trailing = { Switch(profile.a11yEnabled, {
                                vm.setA11y(it)
                                if (it) context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                            }) }) {}
                        SettingRow("后台守护服务", desc = "提高常驻率 + 相册归集观察",
                            trailing = { OutlinedButton(
                                onClick = {
                                    try { context.startForegroundService(Intent(context, KeepAliveService::class.java)) }
                                    catch (_: Throwable) {}
                                    Toast.makeText(context, "守护已启动", Toast.LENGTH_SHORT).show()
                                },
                            ) { Text("立即启动") } }) {}
                    }
                }
            }

            item { SectionTitle("你在意什么") }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Column(Modifier.padding(12.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                            ThemeCatalog.ALL.take(6).forEach { ThemeChip(it.slug, it.label, profile.themes, vm) }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            ThemeCatalog.ALL.drop(6).forEach { ThemeChip(it.slug, it.label, profile.themes, vm) }
                        }
                    }
                }
            }

            item { SectionTitle("收藏判断") }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        ThresholdSlider("提醒收藏阈值", profile.remindThreshold, 20, 100) { vm.setRemindThreshold(it) }
                        ThresholdSlider("自动记录阈值（更高分才自动存）", profile.autoThreshold, 30, 100) { vm.setAutoThreshold(it) }
                        SettingRow("授权后自动记录", desc = "高分瞬间不用你点，直接入库",
                            trailing = { Switch(profile.autoSaveEnabled, { vm.setAutoSave(it) }) }) {}
                        SettingRow("夜间勿扰（23–07）", desc = "深夜自动存但不弹提醒",
                            trailing = { Switch(profile.quietSleeping, { vm.setQuiet(it, 23, 7) }) }) {}
                        SettingRow("每晚 21:30 抽一段回忆", desc = "准点推送到微笑",
                            trailing = { Switch(profile.dailyReviewEnabled, { vm.setDailyReview(it, 21, 30) }) }) {}
                    }
                }
            }

            item { SectionTitle("星标联系人") }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    val senders = moments.groupingBy { it.sender }.eachCount()
                        .entries.sortedByDescending { it.value }.take(12).map { it.key }
                    if (senders.isEmpty()) {
                        Text("收藏几段后，这里会出现常联系的人，可把重要的人标星。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(14.dp))
                    } else {
                        Column(Modifier.padding(6.dp)) {
                            senders.forEach { name ->
                                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Star, null,
                                        tint = if (name in profile.starSetFor(Profile.WEIXIN)) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(10.dp))
                                    Text(name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                                    val star = name in profile.starSetFor(Profile.WEIXIN) || name in profile.starSetFor(Profile.QQ)
                                    Switch(star, { vm.setContactStar(name, Profile.WEIXIN, it) })
                                }
                            }
                        }
                    }
                }
            }

            item { SectionTitle("数据") }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton({ htmlExporter.launch("shiguang-${System.currentTimeMillis()}.html") }, Modifier.weight(1f)) {
                                Icon(Icons.Filled.FileDownload, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("导出 HTML")
                            }
                            OutlinedButton({ jsonExporter.launch("shiguang-${System.currentTimeMillis()}.json") }, Modifier.weight(1f)) {
                                Icon(Icons.Filled.FileDownload, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("导出 JSON")
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("本机共 ${moments.size} 段回忆 · 全部离线", modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            TextButton({ wipeConfirm = true }, enabled = moments.isNotEmpty()) {
                                Icon(Icons.Filled.DeleteForever, null, Modifier.size(16.dp))
                                Text("清空")
                            }
                        }
                        Text("隐私底线：App 不申请联网权限，数据只在本机。",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            if (BuildConfig.DEBUG) {
                item { SectionTitle("调试 · 模拟消息") }
                item { DebugSimulatorCard(vm) }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    if (wipeConfirm) {
        AlertDialog(
            onDismissRequest = { wipeConfirm = false },
            title = { Text("清空所有回忆？") },
            text = { Text("将删除全部 ${moments.size} 段瞬间，不可恢复。") },
            confirmButton = { TextButton({ vm.wipeAll(); wipeConfirm = false }) { Text("清空") } },
            dismissButton = { TextButton({ wipeConfirm = false }) { Text("取消") } },
        )
    }
}

@Composable
private fun SectionTitle(t: String) {
    Text(t, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.tertiary, modifier = Modifier.padding(start = 6.dp, top = 4.dp))
}

@Composable
private fun SettingRow(
    title: String,
    desc: String,
    trailing: (@Composable () -> Unit)? = null,
    onClick: () -> Unit = {},
) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(desc, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (trailing != null) trailing()
    }
}

@Composable
private fun StatusOk(on: Boolean) {
    if (on) {
        Text("守护中", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.labelMedium)
    } else {
        Text("已停", color = MaterialTheme.colorScheme.outline, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun ThemeChip(slug: String, label: String, selected: Set<String>, vm: AppViewModel) {
    FilterChip(
        selected = slug in selected,
        onClick = { vm.setThemes(if (slug in selected) selected - slug else selected + slug) },
        label = { Text(label) },
    )
}

@Composable
private fun ThresholdSlider(label: String, value: Int, min: Int, max: Int, onChange: (Int) -> Unit) {
    Column(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
        Row {
            Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            Text("$value", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
        Slider(value = value.toFloat(), onValueChange = { onChange(it.toInt()) },
            valueRange = min.toFloat()..max.toFloat())
    }
}

@Composable
private fun DebugSimulatorCard(vm: AppViewModel) {
    val logs by vm.logs.collectAsStateWithLifecycle()
    var sender by remember { mutableStateOf("大熊") }
    var text by remember { mutableStateOf("想你了，什么时候回来一起吃饭") }
    var type by remember { mutableStateOf(MomentType.TEXT) }

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(12.dp)) {
            OutlinedTextField(sender, { sender = it }, label = { Text("联系人") }, singleLine = true,
                modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(text, { text = it }, label = { Text("消息内容") }, minLines = 2,
                modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MomentType.entries.forEach { t ->
                    FilterChip(selected = type == t, onClick = { type = t }, label = { Text(t.label) })
                }
            }
            Spacer(Modifier.height(8.dp))
            Button({ vm.simulateOnce(sender, text, type) }, Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Science, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("投喂一条模拟消息")
            }
            Spacer(Modifier.height(8.dp))
            Text("最近判定日志", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            logs.take(10).forEach { l ->
                Text("${l.ts % 100000} [${l.score} ${l.verdict}] ${l.sender}: ${l.text.take(24)} · ${l.processedAs}",
                    style = MaterialTheme.typography.labelSmall, fontSize = 11.sp,
                    modifier = Modifier.padding(vertical = 1.dp))
            }
        }
    }
}

// 让 MomentType.label 在调试卡可用
private val MomentType.label: String
    get() = when (this) {
        MomentType.TEXT -> "文字"; MomentType.LONG_TEXT -> "长文"; MomentType.IMAGE -> "图片"
        MomentType.VOICE -> "语音"; MomentType.STICKER -> "表情"; MomentType.OTHER -> "其他"
    }