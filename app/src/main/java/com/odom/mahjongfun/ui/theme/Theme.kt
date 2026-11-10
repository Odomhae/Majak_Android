package com.odom.mahjongfun.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

// 다크/라이트 모드와 무관하게 항상 같은 초록 펠트 테마를 쓴다. (CLAUDE.md §10.2)
private val FeltColorScheme = darkColorScheme(
    primary = Accent,
    onPrimary = BgFeltBottom,
    secondary = TileSide,
    onSecondary = BgFeltBottom,
    background = BgFeltBottom,
    onBackground = OnBg,
    surface = BgFeltBottom,
    onSurface = OnBg,
    surfaceVariant = BgFeltTop,
    onSurfaceVariant = OnBg,
)

@Composable
fun MahjongFunTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FeltColorScheme,
        typography = Typography,
        content = content
    )
}
