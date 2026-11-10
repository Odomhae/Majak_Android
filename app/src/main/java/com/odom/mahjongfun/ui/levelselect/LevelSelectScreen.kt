package com.odom.mahjongfun.ui.levelselect

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.odom.mahjongfun.R
import com.odom.mahjongfun.ui.components.PlaceholderScreen

@Composable
fun LevelSelectScreen(onStage: (Int) -> Unit, onBack: () -> Unit) {
    PlaceholderScreen(
        title = stringResource(R.string.level_select_title),
        actions = listOf(
            stringResource(R.string.stage_format, 1) to { onStage(1) },
            stringResource(R.string.back) to onBack,
        )
    )
}
