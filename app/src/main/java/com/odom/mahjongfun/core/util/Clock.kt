package com.odom.mahjongfun.core.util

/** 게임 로직은 System.currentTimeMillis()를 직접 쓰지 않고 이 인터페이스를 주입받는다. */
interface Clock {
    fun nowMillis(): Long
}

object SystemClock : Clock {
    override fun nowMillis(): Long = System.currentTimeMillis()
}
