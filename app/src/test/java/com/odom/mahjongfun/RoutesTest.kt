package com.odom.mahjongfun

import com.odom.mahjongfun.ui.navigation.Routes
import org.junit.Assert.assertEquals
import org.junit.Test

class RoutesTest {
    @Test
    fun stageRoutesAreBuiltFromStageNumber() {
        assertEquals("game/7", Routes.game(7))
        assertEquals("result/12", Routes.result(12))
    }
}
