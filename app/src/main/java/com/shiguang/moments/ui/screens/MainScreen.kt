package com.shiguang.moments.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.shiguang.moments.ui.AppViewModel

private data class TabDef(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val TABS = listOf(
    TabDef("今天", Icons.Filled.Home),
    TabDef("日历", Icons.Filled.CalendarMonth),
    TabDef("相册", Icons.Filled.PhotoLibrary),
    TabDef("我", Icons.Filled.Person),
)

/** 主框架：四个底部标签页容纳全部回忆形态的入口 */
@Composable
fun MainScreen(nav: NavHostController, vm: AppViewModel) {
    var selected by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                TABS.forEachIndexed { i, tab ->
                    NavigationBarItem(
                        selected = selected == i,
                        onClick = { selected = i },
                        icon = { Icon(tab.icon, null) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { padding ->
        when (selected) {
            0 -> HomeScreen(nav, vm, Modifier.padding(padding))
            1 -> CalendarScreen(nav, vm, Modifier.padding(padding))
            2 -> GalleryScreen(nav, vm, Modifier.padding(padding))
            3 -> SettingsScreen(nav, vm, Modifier.padding(padding))
        }
    }
}