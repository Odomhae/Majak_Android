package com.odom.mahjongfun.ui.game

import org.junit.Assert.assertEquals
import org.junit.Test

class TimeFormatTest {
    @Test
    fun formatsMinutesAndSeconds() {
        assertEquals("0:00", formatElapsed(0))
        assertEquals("0:09", formatElapsed(9_999))
        assertEquals("1:05", formatElapsed(65_000))
        assertEquals("59:59", formatElapsed(3_599_000))
    }

    @Test
    fun formatsHoursAndClampsNegative() {
        assertEquals("1:00:00", formatElapsed(3_600_000))
        assertEquals("2:03:04", formatElapsed(7_384_000))
        assertEquals("0:00", formatElapsed(-5))
    }
}
