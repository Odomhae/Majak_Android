package com.odom.mahjongfun.core

import com.odom.mahjongfun.core.model.Board
import com.odom.mahjongfun.core.model.Tile
import com.odom.mahjongfun.core.model.TileFace
import com.odom.mahjongfun.core.model.TilePos

val Dot1: TileFace = TileFace.Dot(1)
val Dot2: TileFace = TileFace.Dot(2)
val Dot3: TileFace = TileFace.Dot(3)

fun tile(id: Int, x: Int, y: Int, z: Int = 0, face: TileFace = Dot1) = Tile(id, TilePos(x, y, z), face)

fun boardOf(vararg tiles: Tile) = Board(tiles.toList())
