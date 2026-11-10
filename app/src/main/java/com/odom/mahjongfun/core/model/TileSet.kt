package com.odom.mahjongfun.core.model

import com.odom.mahjongfun.core.model.TileFace.Bamboo
import com.odom.mahjongfun.core.model.TileFace.Character
import com.odom.mahjongfun.core.model.TileFace.Dot
import com.odom.mahjongfun.core.model.TileFace.Dragon
import com.odom.mahjongfun.core.model.TileFace.Flower
import com.odom.mahjongfun.core.model.TileFace.Season
import com.odom.mahjongfun.core.model.TileFace.Wind

object TileSet {
    const val FULL_SIZE = 144

    /** 풀 세트 144장: 수패 3종 x 9 x 4, 풍 4 x 4, 삼원 3 x 4, 꽃 4, 계절 4. */
    fun fullSet(): List<TileFace> = buildList {
        repeat(4) {
            for (rank in 1..9) {
                add(Character(rank))
                add(Dot(rank))
                add(Bamboo(rank))
            }
            WindDirection.entries.forEach { add(Wind(it)) }
            DragonColor.entries.forEach { add(Dragon(it)) }
        }
        for (n in 1..4) {
            add(Flower(n))
            add(Season(n))
        }
    }
}
