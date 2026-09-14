package com.shiguang.moments.ui.screens

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.shiguang.moments.ui.AppViewModel
import com.shiguang.moments.ui.components.EmptyState
import com.shiguang.moments.ui.components.Fmt
import com.shiguang.moments.ui.components.MomentCard
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** 日历回望：按月看哪些日子有美好瞬间；点某天回溯那天 */
@Composable
fun CalendarScreen(nav: NavHostController, vm: AppViewModel, modifier: Modifier = Modifier) {
    val moments by vm.moments.collectAsStateWithLifecycle()
    val now = Calendar.getInstance()
    var year by remember { mutableIntStateOf(now.get(Calendar.YEAR)) }
    var month by remember { mutableIntStateOf(now.get(Calendar.MONTH)) }
    var selectedDay by remember { mutableIntStateOf(now.get(Calendar.DAY_OF_MONTH)) }

    val todayKey = Fmt.dayKey(System.currentTimeMillis())
    val monthly = moments.filter {
        val c = Calendar.getInstance().apply { timeInMillis = it.capturedAt }
        c.get(Calendar.YEAR) == year && c.get(Calendar.MONTH) == month
    }
    val countByDay: Map<Int, Int> = monthly.groupingBy {
        Calendar.getInstance().apply { timeInMillis = it.capturedAt }.get(Calendar.DAY_OF_MONTH)
    }.eachCount()

    val first = Calendar.getInstance().apply { set(year, month, 1) }
    val daysInMonth = first.getActualMaximum(Calendar.DAY_OF_MONTH)
    val weekOffset = (first.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7
    val effectiveDay = minOf(selectedDay, daysInMonth)
    val rows = (weekOffset + daysInMonth + 6) / 7

    val selectedMoments = moments.filter {
        val c = Calendar.getInstance().apply { timeInMillis = it.capturedAt }
        c.get(Calendar.YEAR) == year && c.get(Calendar.MONTH) == month && c.get(Calendar.DAY_OF_MONTH) == effectiveDay
    }.sortedByDescending { it.capturedAt }

    if (moments.isEmpty()) {
        EmptyState(Icons.Filled.CalendarMonth, "日历上还没有标记",
            "收藏的瞬间会像星星一样落在日历上", modifier)
        return
    }

    Column(modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { run { month--; if (month < 0) { month = 11; year-- } } }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "上月")
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$year 年 ${month + 1} 月",
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("本月记录 ${monthly.size} 段", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { run { month++; if (month > 11) { month = 0; year++ } } }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "下月")
            }
        }

        Row(Modifier.fillMaxWidth()) {
            listOf("一", "二", "三", "四", "五", "六", "日").forEach { d ->
                Text(d, Modifier.weight(1f), textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(4.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            userScrollEnabled = false,
            modifier = Modifier.height((rows * 52).dp + 8.dp),
        ) {
            gridItems((0 until weekOffset).map { -it }) { Box(Modifier.height(46.dp)) }
            gridItems((1..daysInMonth).toList()) { day ->
                val count = countByDay[day] ?: 0
                val selected = day == effectiveDay
                val isToday = Fmt.dayKey(makeTs(year, month, day)) == todayKey
                val alpha = when {
                    count == 0 -> 0f
                    count == 1 -> 0.35f
                    count <= 3 -> 0.55f
                    else -> 0.85f
                }
                Box(Modifier.padding(3.dp).fillMaxWidth().height(46.dp), contentAlignment = Alignment.Center) {
                    Box(
                        Modifier
                            .size(40.dp)
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary
                                else if (count > 0) MaterialTheme.colorScheme.primary.copy(alpha = alpha)
                                else Color.Transparent,
                                CircleShape,
                            )
                            .clickable { selectedDay = day },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("$day",
                            color = when {
                                selected -> MaterialTheme.colorScheme.onPrimary
                                count > 0 -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.onSurface
                            },
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(Fmt.fullDay(makeTs(year, month, selectedDay)) + "  ·  ${selectedMoments.size} 段",
                style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            FilledTonalButton(onClick = { nav.navigate("yearago") }) { Text("那年今日") }
        }
        Spacer(Modifier.height(8.dp))

        LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (selectedMoments.isEmpty()) {
                item {
                    Text("这一天没有收藏，也许本就平凡得刚好。", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                items(selectedMoments, key = { it.id }) { m ->
                    MomentCard(m, onOpen = { nav.navigate("moment/${m.id}") })
                }
            }
            item { Spacer(Modifier.height(20.dp)) }
        }
    }
}

private fun makeTs(year: Int, month: Int, day: Int): Long =
    Calendar.getInstance().apply {
        clear()
        set(year, month, day, 12, 0, 0)
    }.timeInMillis