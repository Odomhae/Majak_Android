package com.odom.mahjongfun.core.engine

import com.odom.mahjongfun.core.boardOf
import com.odom.mahjongfun.core.tile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FreeTileRuleTest {
    @Test
    fun loneTileIsFree() {
        assertTrue(FreeTileRule.isFree(boardOf(tile(1, 0, 0)), 1))
    }

    @Test
    fun tileBoxedInOnBothSidesIsNotFree() {
        val board = boardOf(tile(1, 0, 0), tile(2, 2, 0), tile(3, 4, 0))
        assertTrue(FreeTileRule.isFree(board, 1))
        assertFalse(FreeTileRule.isFree(board, 2))
        assertTrue(FreeTileRule.isFree(board, 3))
    }

    @Test
    fun tileWithOneOpenSideIsFree() {
        val board = boardOf(tile(1, 0, 0), tile(2, 2, 0))
        assertTrue(FreeTileRule.isFree(board, 1))
        assertTrue(FreeTileRule.isFree(board, 2))
    }

    @Test
    fun halfRowOffsetNeighborsBlockSide() {
        // 가운데 패(id 1)의 오른쪽에는 y가 +-1 어긋난 패가 두 장, 왼쪽에는 한 장.
        val blockedRight = boardOf(
            tile(1, 0, 0),
            tile(2, 2, -1), // 오른쪽 위쪽 반 칸
            tile(3, -2, 1), // 왼쪽 아래쪽 반 칸
        )
        // 양쪽 다 y 겹침이라 둘 다 막힘.
        assertFalse(FreeTileRule.isFree(blockedRight, 1))
    }

    @Test
    fun neighborTwoRowsAwayDoesNotBlockSide() {
        // y 차이가 2면 겹치지 않는다.
        val board = boardOf(tile(1, 0, 0), tile(2, 2, 2), tile(3, -2, -2))
        assertTrue(FreeTileRule.isFree(board, 1))
    }

    @Test
    fun tileDirectlyAboveCoversTile() {
        val board = boardOf(tile(1, 0, 0, 0), tile(2, 0, 0, 1))
        assertFalse(FreeTileRule.isFree(board, 1))
        assertTrue(FreeTileRule.isFree(board, 2))
    }

    @Test
    fun halfOffsetTileAboveCoversTile() {
        for (dx in -1..1) for (dy in -1..1) {
            val board = boardOf(tile(1, 0, 0, 0), tile(2, dx, dy, 1))
            assertFalse("offset ($dx,$dy) should cover", FreeTileRule.isFree(board, 1))
        }
    }

    @Test
    fun tileAboveFullyOffsetDoesNotCover() {
        for ((dx, dy) in listOf(2 to 0, -2 to 0, 0 to 2, 0 to -2, 2 to 2, -2 to -2)) {
            val board = boardOf(tile(1, 0, 0, 0), tile(2, dx, dy, 1))
            assertTrue("offset ($dx,$dy) should not cover", FreeTileRule.isFree(board, 1))
        }
    }

    @Test
    fun tileTwoLayersAboveDoesNotCover() {
        val board = boardOf(tile(1, 0, 0, 0), tile(2, 0, 0, 2))
        assertTrue(FreeTileRule.isFree(board, 1))
    }

    @Test
    fun tileOnLowerLayerDoesNotBlockSideOrCover() {
        val board = boardOf(tile(1, 0, 0, 1), tile(2, 2, 0, 0), tile(3, -2, 0, 0))
        assertTrue(FreeTileRule.isFree(board, 1))
    }

    @Test
    fun removedTilesAreIgnored() {
        val board = boardOf(tile(1, 0, 0), tile(2, 2, 0), tile(3, 4, 0), tile(4, 0, 0, 1), tile(5, 6, 0, 0))
        assertFalse(FreeTileRule.isFree(board, 3)) // 양옆이 막힘
        // 4(위)와 5(오른쪽)를 치우면 1은 자유, 3은 오른쪽이 열려 자유.
        val next = board.remove(4, 5)
        assertTrue(FreeTileRule.isFree(next, 1))
        assertTrue(FreeTileRule.isFree(next, 3))
    }

    @Test
    fun removedTileIsNeverFree() {
        val board = boardOf(tile(1, 0, 0), tile(2, 2, 0)).remove(1, 2)
        assertFalse(FreeTileRule.isFree(board, 1))
        assertFalse(FreeTileRule.isFree(board, 99)) // 없는 id
    }

    @Test
    fun freeTilesListsOnlyFreeOnesInIdOrder() {
        val board = boardOf(
            tile(5, 4, 0), tile(1, 0, 0), tile(3, 2, 0), // 한 줄: 1, 3, 5
            tile(2, 2, 0, 1), // 3 위를 덮음
        )
        // 1: 왼쪽 열림 -> 자유. 3: 덮임. 5: 오른쪽 열림 -> 자유. 2: 맨 위 -> 자유.
        assertEquals(listOf(1, 2, 5), FreeTileRule.freeTiles(board).map { it.id })
    }

    @Test
    fun stackedPyramidOnlyTopIsFreeUntilRemoved() {
        // 한 줄 2장 위에 반 칸 어긋난 1장이 걸쳐 있는 구조.
        val board = boardOf(tile(1, 0, 0), tile(2, 2, 0), tile(3, 1, 0, 1))
        assertEquals(listOf(3), FreeTileRule.freeTiles(board).map { it.id })
    }
}
