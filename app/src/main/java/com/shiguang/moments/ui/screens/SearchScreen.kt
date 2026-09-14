package com.shiguang.moments.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.shiguang.moments.ui.AppViewModel
import com.shiguang.moments.ui.components.MomentCard

/** 检索：关键词 + 联系人 + 星标过滤 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(nav: NavHostController, vm: AppViewModel) {
    val moments by vm.moments.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    var person by remember { mutableStateOf("全部") }
    var onlyStar by remember { mutableStateOf(false) }

    val topSenders = moments.groupingBy { it.sender }
        .eachCount().entries.sortedByDescending { it.value }.take(8).map { it.key }

    val filtered = moments.filter { m ->
        val q = query.trim()
        (q.isEmpty() ||
            m.text.contains(q) || m.note?.contains(q) == true ||
            m.quote?.contains(q) == true || m.sender.contains(q) || m.themeTags.contains(q)) &&
            (person == "全部" || m.sender == person) &&
            (!onlyStar || m.starred)
    }.sortedByDescending { it.capturedAt }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("检索回忆") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") }
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                OutlinedTextField(
                    value = query, onValueChange = { query = it },
                    placeholder = { Text("搜一句话、一个人……") },
                    leadingIcon = { Icon(Icons.Filled.Search, null) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
                Spacer(Modifier.width(8.dp))
                FilterChip(
                    selected = onlyStar,
                    onClick = { onlyStar = !onlyStar },
                    label = { Text("★") },
                )
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(selected = person == "全部", onClick = { person = "全部" }, label = { Text("全部") })
                topSenders.forEach { s ->
                    FilterChip(selected = person == s, onClick = { person = s }, label = { Text(s) })
                }
            }
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (filtered.isEmpty()) {
                    item { Text("没有找到相关回忆。", color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(8.dp)) }
                } else {
                    items(filtered, key = { it.id }) { m ->
                        MomentCard(m, onOpen = { nav.navigate("moment/${m.id}") })
                    }
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}