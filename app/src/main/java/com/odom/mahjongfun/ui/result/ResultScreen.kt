package com.odom.mahjongfun.ui.result

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.odom.mahjongfun.R
import com.odom.mahjongfun.ui.components.PlaceholderScreen

@Composable
fun ResultScreen(stage: Int, onNext: () -> Unit, onHome: () -> Unit) {
    PlaceholderScreen(
        title = stringResource(R.string.result_title, stage),
        actions = listOf(
            stringResource(R.string.result_next) to onNext,
            stringResource(R.string.result_home) to onHome,
        )
    )
}
