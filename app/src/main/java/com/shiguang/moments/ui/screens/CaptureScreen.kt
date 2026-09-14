package com.shiguang.moments.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.shiguang.moments.service.CaptureAccessibility
import com.shiguang.moments.ui.AppViewModel

/** 屏幕捕获落版：把无障碍抓到的聊天长文收进回忆库 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureScreen(nav: NavHostController, vm: AppViewModel) {
    val context = LocalContext.current
    var sender by remember { mutableStateOf("屏幕捕获") }
    var text by remember { mutableStateOf(CaptureAccessibility.pendingCapture) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("留档这段文字") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") }
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(20.dp)) {
            Text("把聊天界面里打动你的整段文字存下来", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(value = sender, onValueChange = { sender = it },
                label = { Text("与谁的对话") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(value = text, onValueChange = { text = it },
                label = { Text("文字内容（可直接编辑）") },
                minLines = 8, modifier = Modifier.fillMaxWidth().weight(1f))
            Spacer(Modifier.height(14.dp))
            Button(onClick = {
                if (text.isNotBlank()) {
                    vm.saveManual(sender, text, null)
                    Toast.makeText(context, "已收进回忆库 💙", Toast.LENGTH_SHORT).show()
                    nav.popBackStack()
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("收进回忆库") }
        }
    }
}