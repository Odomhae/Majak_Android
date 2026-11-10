package com.odom.mahjongfun.ui.game

import androidx.annotation.DrawableRes
import com.odom.mahjongfun.R
import com.odom.mahjongfun.core.model.DragonColor
import com.odom.mahjongfun.core.model.TileFace
import com.odom.mahjongfun.core.model.TileSet
import com.odom.mahjongfun.core.model.WindDirection

private val characters = intArrayOf(
    R.drawable.tile_char_1, R.drawable.tile_char_2, R.drawable.tile_char_3,
    R.drawable.tile_char_4, R.drawable.tile_char_5, R.drawable.tile_char_6,
    R.drawable.tile_char_7, R.drawable.tile_char_8, R.drawable.tile_char_9,
)
private val dots = intArrayOf(
    R.drawable.tile_dot_1, R.drawable.tile_dot_2, R.drawable.tile_dot_3,
    R.drawable.tile_dot_4, R.drawable.tile_dot_5, R.drawable.tile_dot_6,
    R.drawable.tile_dot_7, R.drawable.tile_dot_8, R.drawable.tile_dot_9,
)
private val bamboos = intArrayOf(
    R.drawable.tile_bamboo_1, R.drawable.tile_bamboo_2, R.drawable.tile_bamboo_3,
    R.drawable.tile_bamboo_4, R.drawable.tile_bamboo_5, R.drawable.tile_bamboo_6,
    R.drawable.tile_bamboo_7, R.drawable.tile_bamboo_8, R.drawable.tile_bamboo_9,
)
private val flowers = intArrayOf(
    R.drawable.tile_flower_1, R.drawable.tile_flower_2, R.drawable.tile_flower_3, R.drawable.tile_flower_4,
)
private val seasons = intArrayOf(
    R.drawable.tile_season_1, R.drawable.tile_season_2, R.drawable.tile_season_3, R.drawable.tile_season_4,
)

/** 패 그림(글자·무늬만 있는 투명 배경 벡터)의 리소스 id. 패 몸통(면, 옆면)은 Canvas로 그린다. */
@DrawableRes
fun TileFace.drawableRes(): Int = when (this) {
    is TileFace.Character -> characters[rank - 1]
    is TileFace.Dot -> dots[rank - 1]
    is TileFace.Bamboo -> bamboos[rank - 1]
    is TileFace.Wind -> when (direction) {
        WindDirection.EAST -> R.drawable.tile_wind_e
        WindDirection.SOUTH -> R.drawable.tile_wind_s
        WindDirection.WEST -> R.drawable.tile_wind_w
        WindDirection.NORTH -> R.drawable.tile_wind_n
    }
    is TileFace.Dragon -> when (color) {
        DragonColor.WHITE -> R.drawable.tile_dragon_white
        DragonColor.GREEN -> R.drawable.tile_dragon_green
        DragonColor.RED -> R.drawable.tile_dragon_red
    }
    is TileFace.Flower -> flowers[number - 1]
    is TileFace.Season -> seasons[number - 1]
}

/** 풀 세트에 나오는 모든 패 그림의 리소스 id (중복 없음). */
val allTileDrawables: List<Int> = TileSet.fullSet().distinct().map { it.drawableRes() }
