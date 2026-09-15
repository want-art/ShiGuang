package com.shiguang.moments.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.shiguang.moments.AppRouting
import com.shiguang.moments.ui.screens.HomeScreen
import com.shiguang.moments.ui.screens.LevelDetailScreen
import com.shiguang.moments.ui.screens.LuckyCardScreen
import com.shiguang.moments.ui.screens.MainScreen
import com.shiguang.moments.ui.screens.MomentDetailScreen
import com.shiguang.moments.ui.screens.MonthlyReportScreen
import com.shiguang.moments.ui.screens.OnboardingScreen
import com.shiguang.moments.ui.screens.SearchScreen
import com.shiguang.moments.ui.screens.TimelineScreen
import com.shiguang.moments.ui.screens.YearAgoScreen

@Composable
fun AppRoot(viewModel: AppViewModel = viewModel()) {
    val nav = rememberNavController()
    val profile by viewModel.profile.collectAsStateWithLifecycle()

    LaunchedEffect(profile.onboarded) {
        if (profile.onboarded) {
            nav.navigate("main") { popUpTo(0) { inclusive = true } }
        }
    }

    LaunchedEffect(AppRouting.target.value) {
        val t = AppRouting.target.value ?: return@LaunchedEffect
        AppRouting.target.value = null
        try {
            when {
                t == "lucky" -> nav.navigate("lucky") { launchSingleTop = true }
                t == "main" -> nav.navigate("main") { popUpTo(0) { inclusive = true }; launchSingleTop = true }
                t.startsWith("moment/") -> nav.navigate(t) { launchSingleTop = true }
            }
        } catch (_: Exception) { }
    }

    NavHost(navController = nav, startDestination = "onboarding") {
        composable("onboarding") {
            OnboardingScreen(onDone = {
                nav.navigate("main") { popUpTo(0) { inclusive = true } }
            })
        }
        composable("main") { MainScreen(nav, viewModel) }
        composable("timeline") { TimelineScreen(nav, viewModel) }
        composable("search") { SearchScreen(nav, viewModel) }
        composable("yearago") { YearAgoScreen(nav, viewModel) }
        composable("levels") { LevelDetailScreen(nav, viewModel) }
        composable("monthly") { MonthlyReportScreen(nav, viewModel) }
        composable("lucky") { LuckyCardScreen(nav, viewModel) }
        composable(
            "moment/{id}",
            arguments = listOf(navArgument("id") { type = NavType.LongType }),
        ) {
            val id = it.arguments?.getLong("id") ?: 0L
            MomentDetailScreen(nav, viewModel, id)
        }
    }
}