package com.odom.mahjongfun.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.odom.mahjongfun.ui.game.GameScreen
import com.odom.mahjongfun.ui.home.HomeScreen
import com.odom.mahjongfun.ui.levelselect.LevelSelectScreen
import com.odom.mahjongfun.ui.result.ResultScreen
import com.odom.mahjongfun.ui.settings.SettingsScreen
import com.odom.mahjongfun.ui.splash.SplashScreen

private val stageArg = listOf(navArgument(Routes.ARG_STAGE) { type = NavType.IntType })

@Composable
fun AppNavHost(modifier: Modifier = Modifier) {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Routes.SPLASH, modifier = modifier) {
        composable(Routes.SPLASH) {
            SplashScreen(onReady = {
                nav.navigate(Routes.HOME) { popUpTo(Routes.SPLASH) { inclusive = true } }
            })
        }
        composable(Routes.HOME) {
            HomeScreen(
                onContinue = { nav.navigate(Routes.game(1)) },
                onLevelSelect = { nav.navigate(Routes.LEVEL_SELECT) },
                onSettings = { nav.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.LEVEL_SELECT) {
            LevelSelectScreen(
                onStage = { nav.navigate(Routes.game(it)) },
                onBack = { nav.popBackStack() },
            )
        }
        composable(Routes.GAME, arguments = stageArg) { entry ->
            val stage = entry.arguments?.getInt(Routes.ARG_STAGE) ?: 1
            GameScreen(
                onClear = { nav.navigate(Routes.result(stage)) { popUpTo(Routes.HOME) } },
                onBack = { nav.popBackStack() },
            )
        }
        composable(Routes.RESULT, arguments = stageArg) { entry ->
            val stage = entry.arguments?.getInt(Routes.ARG_STAGE) ?: 1
            ResultScreen(
                stage = stage,
                onNext = { nav.navigate(Routes.game(stage + 1)) { popUpTo(Routes.HOME) } },
                onHome = { nav.popBackStack(Routes.HOME, inclusive = false) },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { nav.popBackStack() })
        }
    }
}
