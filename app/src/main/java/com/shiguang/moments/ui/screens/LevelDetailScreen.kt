package com.shiguang.moments.ui.screens

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.shiguang.moments.scoring.LevelCatalog
import com.shiguang.moments.ui.AppViewModel
import com.shiguang.moments.ui.components.LevelUpOverlay

/** 等级总览：当前段位 + 经验值进度 + 全部段位（未解锁灰态） */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LevelDetailScreen(nav: NavHostController, vm: AppViewModel) {
    val moments by vm.moments.collectAsStateWithLifecycle()
    val total = moments.size
    val level = LevelCatalog.levelFor(total)
    val next = LevelCatalog.nextLevel(total)
    val progress = LevelCatalog.progressToNext(total)
    val animatedProgress by animateFloatAsState(targetValue = progress, animationSpec = spring(), label = "xp")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("我的等级") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.padding(padding).fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                val t = androidx.compose.animation.core.rememberInfiniteTransition(label = "ld")
                val floatY by t.animateFloat(0f, -8f,
                    androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(2000), androidx.compose.animation.core.RepeatMode.Reverse), label = "fy")
                val sway by t.animateFloat(-4f, 4f,
                    androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(2600), androidx.compose.animation.core.RepeatMode.Reverse), label = "sw")
                Box(
                    Modifier.fillMaxWidth().background(
                        Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)),
                        RoundedCornerShape(24.dp),
                    ).padding(22.dp),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${level.emoji}", fontSize = 44.sp, modifier = Modifier.graphicsLayer {
                            translationY = floatY
                            rotationZ = sway
                        })
                        Spacer(Modifier.height(8.dp))
                        Text(level.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary)
                        Text(level.title, style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f))
                        Spacer(Modifier.height(14.dp))
                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.25f),
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            if (next == null) "已集满全部经验 🎉"
                            else "经验 $total / ${next.from} · 还差 ${next.from - total} 段升级到 ${next.name}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                        Spacer(Modifier.height(10.dp))
                        Text("每珍藏一段 = 1 经验点", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f))
                    }
                }
            }

            item {
                Text("成长之路", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp))
            }

            items(LevelCatalog.ALL.size) { i ->
                LevelRow(i, total)
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun LevelRow(index: Int, total: Int) {
    val catalog = LevelCatalog.ALL
    val lv = catalog[index]
    val unlocked = total >= lv.from
    val isCurrent = index < catalog.size - 1 &&
        total >= lv.from && total < catalog[index + 1].from
    val nextStart = if (index < catalog.size - 1) catalog[index + 1].from else lv.from

    Row(
        Modifier
            .fillMaxWidth()
            .background(
                if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                else if (unlocked) MaterialTheme.colorScheme.surfaceContainerLow
                else MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.6f),
                RoundedCornerShape(14.dp),
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(if (unlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), CircleShape),
            contentAlignment = Alignment.Center,
        ) { Text(lv.emoji, fontSize = 20.sp) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Lv.${lv.tier}", style = MaterialTheme.typography.labelSmall,
                    color = if (unlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                Spacer(Modifier.width(6.dp))
                Text(lv.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                if (isCurrent) {
                    Spacer(Modifier.width(6.dp))
                    Text("· 当前", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary)
                }
            }
            Text(if (unlocked) lv.title else "已锁定", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (total >= lv.from) {
            Text("✓", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
        }
    }
}