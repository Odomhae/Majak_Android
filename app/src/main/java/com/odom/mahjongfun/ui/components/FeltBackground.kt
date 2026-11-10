package com.odom.mahjongfun.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import com.odom.mahjongfun.ui.theme.BgFeltBottom
import com.odom.mahjongfun.ui.theme.BgFeltTop

/** 모든 화면 공통 초록 펠트 세로 그라데이션 배경. */
@Composable
fun FeltBackground(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BgFeltTop, BgFeltBottom))),
        content = { content() }
    )
}
