package com.odom.mahjongfun.ui.game

import java.util.Locale

/** 경과 시간을 "m:ss"로. 60분 이상은 "h:mm:ss". */
fun formatElapsed(millis: Long): String {
    val totalSeconds = (millis.coerceAtLeast(0) / 1000)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds)
    else String.format(Locale.ROOT, "%d:%02d", minutes, seconds)
}
