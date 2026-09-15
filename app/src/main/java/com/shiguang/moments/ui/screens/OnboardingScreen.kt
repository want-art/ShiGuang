package com.shiguang.moments.ui.screens

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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shiguang.moments.scoring.ThemeCatalog
import com.shiguang.moments.ui.AppViewModel

/** 开机引导（手动模式）：选你在意的主题 → 完成。 */
@Composable
fun OnboardingScreen(onDone: () -> Unit, vm: AppViewModel = viewModel()) {
    var step by remember { mutableIntStateOf(0) }
    var themes by remember { mutableStateOf(setOf("family", "friendship", "love")) }

    Column(
        Modifier.fillMaxSize().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        LinearProgressIndicator(progress = { (step + 1) / 2f }, Modifier.fillMaxWidth())
        Spacer(Modifier.height(28.dp))

        when (step) {
            0 -> {
                Spacer(Modifier.weight(1f))
                Text("拾光", fontSize = 48.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(12.dp))
                Text("为愿意记录生活的人而生", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text(
                    "把聊天里打动你的话、让你心动的那张图、深夜听到的那段语音，\n一一悄悄收进这本时光日记。\n一切只存在你的手机里，不上传、不联网。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.weight(1f))
                Button({ step = 1 }, Modifier.fillMaxWidth().height(52.dp)) { Text("开始", fontSize = 16.sp) }
            }
            1 -> {
                Text("你最在意什么？", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text("选中主题的瞬间，会被打上特别的标签（多选，之后可改）",
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
                Button({
                    vm.setThemes(themes)
                    vm.setOnboarded(true)
                    onDone()
                }, Modifier.fillMaxWidth().height(52.dp)) {
                    Text("进入拾光 ✨", fontSize = 16.sp)
                }
            }
        }
    }
}