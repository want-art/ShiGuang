package com.shiguang.moments.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.shiguang.moments.data.repo.ImageStore
import com.shiguang.moments.prefs.Profile
import com.shiguang.moments.scoring.ThemeCatalog
import com.shiguang.moments.ui.AppViewModel
import kotlinx.coroutines.launch
import java.io.File

/** 「我」：个人页 · 主题偏好 · 守护设置 · 星标联系人 · 数据导出 */
@Composable
fun SettingsScreen(nav: NavHostController, vm: AppViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val profile by vm.profile.collectAsStateWithLifecycle()
    val moments by vm.moments.collectAsStateWithLifecycle()
    var wipeConfirm by remember { mutableStateOf(false) }
    var editProfile by remember { mutableStateOf(false) }

    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) scope.launch {
            val path = ImageStore.copyToLocal(context, uri)
            if (path != null) vm.setProfile(profile.nickname, profile.signature, path)
        }
    }

    val htmlExporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/html")) { uri ->
        if (uri != null) scope.launch { vm.export(uri, true); Toast.makeText(context, "HTML 已导出", Toast.LENGTH_SHORT).show() }
    }
    val jsonExporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch { vm.export(uri, false); Toast.makeText(context, "JSON 已导出", Toast.LENGTH_SHORT).show() }
    }

    Column(modifier.fillMaxSize()) {
        Row(Modifier.padding(horizontal = 20.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("我", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }

        LazyColumn(
            Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { ProfileCard(profile, onEdit = { editProfile = true }, onAvatarPick = { avatarPicker.launch("image/*") }) }

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

            item { SectionTitle("守护") }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Column(Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                        SettingRow("夜间勿扰（23–07）", desc = "夜里不主动推送回忆", "📦", trailing = {
                            Switch(profile.quietSleeping, { vm.setQuiet(it, 23, 7) })
                        }) {}
                        SettingRow("每晚 21:30 随机回忆", desc = "翻一张卡回到某个瞬间", "🌙", trailing = {
                            Switch(profile.dailyReviewEnabled, { vm.setDailyReview(it, 21, 30) })
                        }) {}
                    }
                }
            }

            item { SectionTitle("重要的人") }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    val senders = moments.groupingBy { it.sender }.eachCount()
                        .entries.sortedByDescending { it.value }.take(12).map { it.key }
                    if (senders.isEmpty()) {
                        Text("收藏几段后，这里会出现常出现的人，可把重要的人标星。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(14.dp))
                    } else {
                        Column(Modifier.padding(6.dp)) {
                            senders.forEach { name ->
                                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Star, null,
                                        tint = if (name in profile.starSetFor("manual")) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(10.dp))
                                    Text(name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                    val star = name in profile.starSetFor("manual")
                                    Switch(star, { vm.setContactStar(name, "manual", it) })
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
                            Text("本机共 ${moments.size} 段回忆 · 全部离线",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
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

    if (editProfile) {
        ProfileEditorDialog(profile, onDismiss = { editProfile = false }, onSave = { n, s, a ->
            vm.setProfile(n, s, a)
            editProfile = false
        })
    }
}

@Composable
private fun ProfileCard(profile: Profile, onEdit: () -> Unit, onAvatarPick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                    .clickable { onAvatarPick() },
                contentAlignment = Alignment.Center,
            ) {
                if (!profile.avatarPath.isNullOrBlank() && File(profile.avatarPath).exists()) {
                    AsyncImage(
                        model = File(profile.avatarPath), contentDescription = "头像",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Text(profile.nickname.take(1), style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(profile.nickname, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(2.dp))
                Text(profile.signature.ifBlank { "给这段人生留一句话（可空）" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onAvatarPick) {
                        Icon(Icons.Filled.PhotoCamera, null, Modifier.size(14.dp)); Spacer(Modifier.width(4.dp)); Text("换头像")
                    }
                    OutlinedButton(onClick = onEdit) { Text("编辑资料") }
                }
            }
        }
    }
}

@Composable
private fun ProfileEditorDialog(profile: Profile, onDismiss: () -> Unit, onSave: (String, String, String?) -> Unit) {
    var n by remember { mutableStateOf(profile.nickname) }
    var s by remember { mutableStateOf(profile.signature) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑我的资料") },
        text = {
            Column {
                OutlinedTextField(n, { n = it }, label = { Text("昵称") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(s, { s = it }, label = { Text("个性签名") },
                    minLines = 2, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { TextButton({ onSave(n, s, profile.avatarPath) }) { Text("保存") } },
        dismissButton = { TextButton(onDismiss) { Text("取消") } },
    )
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
    emoji: String = "",
    trailing: (@Composable () -> Unit)? = null,
    onClick: () -> Unit = {},
) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(if (emoji.isEmpty()) title else "$emoji $title", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(desc, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (trailing != null) trailing()
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