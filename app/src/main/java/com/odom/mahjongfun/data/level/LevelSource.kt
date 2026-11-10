package com.odom.mahjongfun.data.level

/** 배치/단계 데이터를 주는 곳. ViewModel 테스트에서 Fake로 바꿔 끼우기 위한 인터페이스. */
interface LevelSource {
    suspend fun catalog(): LevelCatalog
}
