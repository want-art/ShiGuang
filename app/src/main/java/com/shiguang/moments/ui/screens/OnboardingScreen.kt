package com.shiguang.moments.ui.screens

import android.Manifest
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shiguang.moments.prefs.Profile
import com.shiguang.moments.scoring.ThemeCatalog
import com.shiguang.moments.service.KeepAliveService
import com.shiguang.moments.service.PermissionGate
import com.shiguang.moments.ui.AppViewModel

/** 开机引导：选你在意的主题 → 常用 App → 授权 → 完成 */
@Composable
fun OnboardingScreen(onDone: () -> Unit, vm: AppViewModel = viewModel()) {
    val context = LocalContext.current
    var step by remember { mutableIntStateOf(0) }
    var themes by remember { mutableStateOf(setOf<String>()) }
    var apps by remember { mutableStateOf(setOf(Profile.WEIXIN, Profile.QQ)) }
    var askedPost by remember { mutableStateOf(false) }

    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {}
    val mediaLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    val postNotifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    val a11yLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {}

    if (step == 3 && !askedPost) {
        askedPost = true
        if (android.os.Build.VERSION.SDK_INT >= 33) postNotifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    val notifGranted = PermissionGate.hasNotificationAccess(context)

    Column(
        Modifier.fillMaxSize().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        LinearProgressIndicator(progress = { (step + 1) / 5f }, Modifier.fillMaxWidth())
        Spacer(Modifier.height(28.dp))

        when (step) {
            0 -> {
                Spacer(Modifier.weight(1f))
                Text("拾光", fontSize = 44.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(10.dp))
                Text("不经意地，记录聊天里的美好瞬间", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text("长文、一张图、一段语音，都可能成为多年后的珍藏。\n一切只存在你的手机里，不上传、不联网。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.weight(1f))
                Button({ step = 1 }, Modifier.fillMaxWidth().height(52.dp)) { Text("开始", fontSize = 16.sp) }
            }
            1 -> {
                Text("你最在意什么？", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text("选中的主题，会被优先识别为值得记录的瞬间（可多选）",
                    style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(16.dp))
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ThemeCatalog.ALL.chunked(2).forEach { pair ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            pair.forEach { t ->
                                FilterChip(
                                    selected = t.slug in themes,
                                    onClick = { themes = if (t.slug in themes) themes - t.slug else themes + t.slug },
                                    label = { Text(t.label) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Button({ step = 2 }, Modifier.fillMaxWidth().height(50.dp), enabled = themes.isNotEmpty()) {
                    Text("下一步")
                }
            }
            2 -> {
                Text("主要用哪些聊天 App？", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text("拾光通过「通知」识别它们", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(16.dp))
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = Profile.WEIXIN in apps,
                            onClick = { apps = if (Profile.WEIXIN in apps) apps - Profile.WEIXIN else apps + Profile.WEIXIN },
                            label = { Text("微信") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        FilterChip(
                            selected = Profile.QQ in apps,
                            onClick = { apps = if (Profile.QQ in apps) apps - Profile.QQ else apps + Profile.QQ },
                            label = { Text("QQ") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                Button({ step = 3 }, Modifier.fillMaxWidth().height(50.dp), enabled = apps.isNotEmpty()) {
                    Text("下一步")
                }
            }
            3 -> {
                Text("授权一下，守护就能开始", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(14.dp))
                PermRow(
                    icon = { Icon(Icons.Filled.NotificationsActive, null, tint = MaterialTheme.colorScheme.primary) },
                    title = "通知使用权",
                    sub = "识别聊天里值得记录的内容",
                    granted = notifGranted,
                    onAction = {
                        notifLauncher.launch(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
                    },
                )
                Spacer(Modifier.height(10.dp))
                PermRow(
                    icon = { Icon(Icons.Filled.PhotoLibrary, null, tint = MaterialTheme.colorScheme.primary) },
                    title = "相册权限",
                    sub = "把长按保存的图自动归到对应瞬间（可选）",
                    granted = PermissionGate.hasMediaRead(context),
                    onAction = {
                        mediaLauncher.launch(
                            if (android.os.Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_IMAGES
                            else Manifest.permission.READ_EXTERNAL_STORAGE,
                        )
                    },
                )
                Spacer(Modifier.height(10.dp))
                PermRow(
                    icon = { Icon(Icons.Filled.Star, null, tint = MaterialTheme.colorScheme.primary) },
                    title = "无障碍捕获",
                    sub = "聊天界面的长文也能整段留档（可选）",
                    granted = isA11yEnabled(context),
                    onAction = { a11yLauncher.launch(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                )
                Spacer(Modifier.height(10.dp))
                Text("一切识别都在手机本地完成，全程不联网。",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.weight(1f))
                Button({
                    vm.setThemes(themes)
                    vm.setApps(apps)
                    vm.setListening(true)
                    vm.setOnboarded(true)
                    try { context.startForegroundService(Intent(context, KeepAliveService::class.java)) } catch (_: Throwable) {}
                    Toast.makeText(context, "守护已开启 🌙", Toast.LENGTH_SHORT).show()
                    onDone()
                }, Modifier.fillMaxWidth().height(52.dp)) {
                    Text("开始守护", fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
private fun PermRow(
    icon: @Composable () -> Unit,
    title: String,
    sub: String,
    granted: Boolean,
    onAction: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon()
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(sub, style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (granted) {
            Text("已开启", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
        } else {
            OutlinedButton(onClick = onAction) { Text("去开启") }
        }
    }
}

private fun isA11yEnabled(context: android.content.Context): Boolean {
    val ourServices =
        context.packageManager.getInstalledPackages(android.content.pm.PackageManager.GET_SERVICES)
            .firstOrNull { it.packageName == context.packageName }
            ?.services?.joinToString(",") { context.packageName + "/" + it.name } ?: ""
    val enabled = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
    ) ?: ""
    return enabled.contains(ourServices.split(",").firstOrNull() ?: "") && enabled.isNotEmpty()
}