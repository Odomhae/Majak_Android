package com.odom.mahjongfun.core.engine

import com.odom.mahjongfun.core.model.Board
import com.odom.mahjongfun.core.model.Tile

/**
 * 자유로운 패 판정. 아래 둘을 모두 만족해야 한다.
 * 1. 위가 비어 있음: z+1 층에 x, y 모두 +-1 이내로 겹치는 패가 없음.
 * 2. 좌 또는 우 중 한쪽이 비어 있음: 같은 층에서 x-2 또는 x+2 위치(y는 +-1 겹침)에 패가 없음.
 *
 * 제거된 패는 판정 대상에서 제외한다. 위치 인덱스로 O(1) 조회한다.
 */
object FreeTileRule {
    fun isFree(board: Board, tile: Tile): Boolean {
        if (board.isRemoved(tile.id)) return false
        return !isCovered(board, tile) && (!hasNeighbor(board, tile, -SIDE) || !hasNeighbor(board, tile, SIDE))
    }

    fun isFree(board: Board, id: Int): Boolean {
        val tile = board.tiles[id] ?: return false
        return isFree(board, tile)
    }

    /** 자유로운 패 목록 (id 오름차순). */
    fun freeTiles(board: Board): List<Tile> = board.activeTiles().filter { isFree(board, it) }

    private fun isCovered(board: Board, tile: Tile): Boolean {
        val (x, y, z) = tile.pos
        for (dx in -1..1) for (dy in -1..1) {
            if (board.tileAt(x + dx, y + dy, z + 1) != null) return true
        }
        return false
    }

    private fun hasNeighbor(board: Board, tile: Tile, dx: Int): Boolean {
        val (x, y, z) = tile.pos
        for (dy in -1..1) {
            if (board.tileAt(x + dx, y + dy, z) != null) return true
        }
        return false
    }

    private const val SIDE = 2
}
