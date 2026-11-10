package com.odom.mahjongfun.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.odom.mahjongfun.R
import com.odom.mahjongfun.ui.components.PlaceholderScreen

@Composable
fun HomeScreen(onContinue: () -> Unit, onLevelSelect: () -> Unit, onSettings: () -> Unit) {
    PlaceholderScreen(
        title = stringResource(R.string.app_name),
        actions = listOf(
            stringResource(R.string.home_continue) to onContinue,
            stringResource(R.string.home_level_select) to onLevelSelect,
            stringResource(R.string.home_settings) to onSettings,
        )
    )
}
