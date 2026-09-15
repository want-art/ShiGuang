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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.shiguang.moments.ui.AppViewModel
import com.shiguang.moments.ui.components.EmptyState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** 月度报告：这个月的记录、常和谁、心情分布、最暖的一句 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyReportScreen(nav: NavHostController, vm: AppViewModel) {
    val moments by vm.moments.collectAsStateWithLifecycle()
    val moods by vm.moods.collectAsStateWithLifecycle()

    val now = Calendar.getInstance()
    val monthKey = now.get(Calendar.YEAR) * 100 + (now.get(Calendar.MONTH) + 1)
    val monthMoments = remember(moments, monthKey) {
        moments.filter { m ->
            val c = Calendar.getInstance().apply { timeInMillis = m.capturedAt }
            c.get(Calendar.YEAR) * 100 + (c.get(Calendar.MONTH) + 1) == monthKey
        }.sortedByDescending { it.capturedAt }
    }
    val monthMoods = remember(moods, monthKey) {
        moods.filter { m ->
            val c = Calendar.getInstance().apply { timeInMillis = m.ts }
            c.get(Calendar.YEAR) * 100 + (c.get(Calendar.MONTH) + 1) == monthKey
        }
    }

    val title = SimpleDateFormat("yyyy年M月", Locale.CHINA).format(Date())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$title · 小结") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") }
                },
            )
        },
    ) { padding ->
        if (monthMoments.isEmpty()) {
            EmptyState(Icons.Filled.BarChart, "这个月还没有记录",
                "记录几段，月底就能看到属于你的小结", Modifier.padding(padding))
        } else {
            LazyColumn(
                Modifier.padding(padding).fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { HeaderStats(monthMoments, monthMoods) }
                item { SectionCard("常和谁") { TopPeople(monthMoments) } }
                if (monthMoods.isNotEmpty()) {
                    item { SectionCard("这个月的心情") { MoodRows(monthMoods) } }
                }
                item { SectionCard("最暖的一句") { WarmQuote(monthMoments) } }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun HeaderStats(list: List<com.shiguang.moments.data.models.MomentEntity>, moods: List<com.shiguang.moments.data.models.MoodEntity>) {
    val imgCount = list.sumOf { it.allImagePaths().size }
    val starCount = list.count { it.starred }
    val daysRecorded = list.map { com.shiguang.moments.ui.components.Fmt.dayKey(it.capturedAt) }.toSet().size
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Stat("${list.size}", "记录", Modifier.weight(1f))
        Stat("$imgCount", "图片", Modifier.weight(1f))
        Stat("$daysRecorded", "有记录的天", Modifier.weight(1f))
        Stat("$starCount", "星标", Modifier.weight(1f))
        Stat("${moods.size}", "心情", Modifier.weight(1f))
    }
}

@Composable
private fun Stat(value: String, label: String, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = modifier,
    ) {
        Column(Modifier.padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.tertiary)
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun TopPeople(list: List<com.shiguang.moments.data.models.MomentEntity>) {
    val ranked = list.groupingBy { it.sender }.eachCount().entries.sortedByDescending { it.value }.take(6)
    ranked.forEach { (name, count) ->
        Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text("× $count", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MoodRows(moods: List<com.shiguang.moments.data.models.MoodEntity>) {
    val counts = moods.groupingBy { it.emoji }.eachCount()
    counts.entries.forEach { (emoji, count) ->
        Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("$emoji  × $count", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun WarmQuote(list: List<com.shiguang.moments.data.models.MomentEntity>) {
    val pool = list.filter { it.text.isNotBlank() }
    val pick = pool.let {
        if (it.isEmpty()) null else it[(it.first().id.toInt() + it.size) % it.size]
    }
    if (pick == null) {
        Text("这个月还没有留下文字", style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    } else {
        Column {
            Text("「${pick.text.take(60)}${if (pick.text.length > 60) "…" else ""}」",
                style = MaterialTheme.typography.bodyLarge, fontStyle = FontStyle.Italic)
            Spacer(Modifier.height(6.dp))
            Text("— ${pick.sender} · ${com.shiguang.moments.ui.components.Fmt.dayStringShort(pick.capturedAt)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}