package com.odom.mahjongfun.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.odom.mahjongfun.R
import com.odom.mahjongfun.ui.components.PlaceholderScreen

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    PlaceholderScreen(
        title = stringResource(R.string.settings_title),
        actions = listOf(stringResource(R.string.back) to onBack)
    )
}
