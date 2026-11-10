package com.odom.mahjongfun.core

import com.odom.mahjongfun.core.engine.Move
import com.odom.mahjongfun.core.engine.MoveFinder
import com.odom.mahjongfun.core.model.Board
import com.odom.mahjongfun.core.model.Tile
import com.odom.mahjongfun.core.model.TileFace
import com.odom.mahjongfun.core.model.TilePos
import org.junit.Assert.assertTrue

val Dot1: TileFace = TileFace.Dot(1)
val Dot2: TileFace = TileFace.Dot(2)
val Dot3: TileFace = TileFace.Dot(3)

fun tile(id: Int, x: Int, y: Int, z: Int = 0, face: TileFace = Dot1) = Tile(id, TilePos(x, y, z), face)

fun boardOf(vararg tiles: Tile) = Board(tiles.toList())

/** 풀이를 엔진 규칙(자유 패, 짝)으로 한 수씩 재생해 모두 유효하고 판이 비는지 확인한다. */
fun assertReplayClears(board: Board, moves: List<Move>) {
    var current = board
    moves.forEachIndexed { step, move ->
        assertTrue("step $step: invalid move $move", MoveFinder.isValidMove(current, move.first, move.second))
        current = current.remove(move.first, move.second)
    }
    assertTrue("board not cleared after ${moves.size} moves, ${current.remainingCount} left", current.isCleared)
}
