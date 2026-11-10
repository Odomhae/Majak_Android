package com.odom.mahjongfun.ui.navigation

object Routes {
    const val ARG_STAGE = "stage"

    const val SPLASH = "splash"
    const val HOME = "home"
    const val LEVEL_SELECT = "levelSelect"
    const val GAME = "game/{$ARG_STAGE}"
    const val RESULT = "result/{$ARG_STAGE}"
    const val SETTINGS = "settings"

    fun game(stage: Int) = "game/$stage"
    fun result(stage: Int) = "result/$stage"
}
