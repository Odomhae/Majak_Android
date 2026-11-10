package com.odom.mahjongfun.core.model

import com.odom.mahjongfun.core.boardOf
import com.odom.mahjongfun.core.tile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class BoardAndLayoutTest {
    @Test
    fun removeReturnsNewBoardAndKeepsOriginal() {
        val board = boardOf(tile(1, 0, 0), tile(2, 2, 0), tile(3, 4, 0))
        val next = board.remove(1, 2)

        assertEquals(3, board.remainingCount)
        assertEquals(1, next.remainingCount)
        assertEquals(setOf(1, 2), next.removed)
        assertEquals(listOf(3), next.activeTiles().map { it.id })
    }

    @Test
    fun boardIsClearedWhenAllRemoved() {
        val board = boardOf(tile(1, 0, 0), tile(2, 2, 0))
        assertFalse(board.isCleared)
        assertTrue(board.remove(1, 2).isCleared)
    }

    @Test
    fun tileAtIgnoresRemovedTiles() {
        val board = boardOf(tile(1, 0, 0), tile(2, 2, 0), tile(3, 4, 0))
        assertEquals(1, board.tileAt(0, 0, 0)?.id)
        assertNull(board.tileAt(0, 0, 1))
        assertNull(board.remove(1, 2).tileAt(0, 0, 0))
    }

    @Test
    fun removeRejectsInvalidArguments() {
        val board = boardOf(tile(1, 0, 0), tile(2, 2, 0), tile(3, 4, 0)).remove(1, 2)
        assertThrows(IllegalArgumentException::class.java) { board.remove(3, 3) }
        assertThrows(IllegalArgumentException::class.java) { board.remove(1, 3) }
        assertThrows(IllegalArgumentException::class.java) { board.remove(3, 99) }
    }

    @Test
    fun boardRejectsSharedPositionAndDuplicateId() {
        assertThrows(IllegalArgumentException::class.java) { boardOf(tile(1, 0, 0), tile(2, 0, 0)) }
        assertThrows(IllegalArgumentException::class.java) { boardOf(tile(1, 0, 0), tile(1, 2, 0)) }
    }

    @Test
    fun boardRejectsUnknownRemovedId() {
        val tiles = listOf(tile(1, 0, 0), tile(2, 2, 0)).associateBy { it.id }
        assertThrows(IllegalArgumentException::class.java) { Board(tiles, setOf(9)) }
    }

    @Test
    fun layoutRequiresEvenPositionCount() {
        assertThrows(IllegalArgumentException::class.java) {
            Layout("odd", "odd", 1, listOf(TilePos(0, 0, 0), TilePos(2, 0, 0), TilePos(4, 0, 0)))
        }
    }

    @Test
    fun layoutRejectsDuplicateAndOverlappingPositions() {
        assertThrows(IllegalArgumentException::class.java) {
            Layout("dup", "dup", 1, listOf(TilePos(0, 0, 0), TilePos(0, 0, 0)))
        }
        // 같은 층에서 반 칸만 어긋나면 패가 겹친다.
        assertThrows(IllegalArgumentException::class.java) {
            Layout("overlap", "overlap", 1, listOf(TilePos(0, 0, 0), TilePos(1, 0, 0)))
        }
        assertThrows(IllegalArgumentException::class.java) {
            Layout("overlapY", "overlapY", 1, listOf(TilePos(0, 0, 0), TilePos(0, 1, 0)))
        }
    }

    @Test
    fun layoutAllowsHalfOffsetAcrossLayersAndAdjacentTiles() {
        val layout = Layout(
            "ok", "ok", 1,
            listOf(TilePos(0, 0, 0), TilePos(2, 0, 0), TilePos(1, 1, 1), TilePos(2, 1, 0).copy(y = 2)),
        )
        assertEquals(4, layout.positions.size)
    }
}
