package com.shiguang.moments.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
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
import com.shiguang.moments.ui.components.Fmt
import com.shiguang.moments.ui.components.MomentCard
import java.util.Calendar

/** 那年今日：往年同月同日的回忆 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YearAgoScreen(nav: NavHostController, vm: AppViewModel) {
    val moments by vm.moments.collectAsStateWithLifecycle()
    val now = Calendar.getInstance()
    val yearAgo = moments.filter { m ->
        val c = Calendar.getInstance().apply { timeInMillis = m.capturedAt }
        c.get(Calendar.MONTH) == now.get(Calendar.MONTH) &&
            c.get(Calendar.DAY_OF_MONTH) == now.get(Calendar.DAY_OF_MONTH) &&
            c.get(Calendar.YEAR) < now.get(Calendar.YEAR)
    }.sortedByDescending { it.capturedAt }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("那年今日 · ${now.get(Calendar.MONTH) + 1}月${now.get(Calendar.DAY_OF_MONTH)}日") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") }
                },
            )
        },
    ) { padding ->
        if (yearAgo.isEmpty()) {
            EmptyState(Icons.Filled.AutoAwesome, "往年今天还没有惊喜",
                "从现在开始收藏，明年的今天就会有的", Modifier.padding(padding))
        } else {
            LazyColumn(
                Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Text("原来过去的今天，也发生过这些：", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                items(yearAgo, key = { it.id }) { m ->
                    MomentCard(m, onOpen = { nav.navigate("moment/${m.id}") })
                }
                item { Spacer(Modifier.height(20.dp)) }
            }
        }
    }
}