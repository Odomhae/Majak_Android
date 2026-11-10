package com.odom.mahjongfun.ui.game

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.odom.mahjongfun.R
import com.odom.mahjongfun.ui.components.PlaceholderScreen

@Composable
fun GameScreen(stage: Int, onClear: () -> Unit, onBack: () -> Unit) {
    PlaceholderScreen(
        title = stringResource(R.string.stage_format, stage),
        actions = listOf(
            stringResource(R.string.game_clear_debug) to onClear,
            stringResource(R.string.back) to onBack,
        )
    )
}
