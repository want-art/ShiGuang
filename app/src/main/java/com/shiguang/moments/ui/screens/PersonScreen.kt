package com.shiguang.moments.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.shiguang.moments.ui.AppViewModel
import com.shiguang.moments.ui.components.EmptyState
import com.shiguang.moments.ui.components.MomentCard
import java.net.URLDecoder

/** 某个人的全部记录（从设置-联系人点入） */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonScreen(nav: NavHostController, vm: AppViewModel, rawName: String) {
    val name = URLDecoder.decode(rawName, "UTF-8")
    val moments by vm.moments.collectAsStateWithLifecycle()
    val list = moments.filter { it.sender == name }.sortedByDescending { it.capturedAt }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(name) },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") }
                },
            )
        },
    ) { padding ->
        if (list.isEmpty()) {
            EmptyState(Icons.Filled.Person, "还没有 TA 的记录",
                "收藏几段关于 TA 的瞬间，这里就会亮起来", Modifier.padding(padding))
        } else {
            LazyColumn(
                Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Text("与 ${name} 的 ${list.size} 段回忆", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                items(list, key = { it.id }) { m ->
                    MomentCard(m, onOpen = { nav.navigate("moment/${m.id}") })
                }
                item { Spacer(Modifier.height(20.dp)) }
            }
        }
    }
}