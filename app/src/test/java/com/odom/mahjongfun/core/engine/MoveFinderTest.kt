package com.odom.mahjongfun.core.engine

import com.odom.mahjongfun.core.Dot1
import com.odom.mahjongfun.core.Dot2
import com.odom.mahjongfun.core.Dot3
import com.odom.mahjongfun.core.boardOf
import com.odom.mahjongfun.core.model.TileFace
import com.odom.mahjongfun.core.tile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class MoveFinderTest {
    @Test
    fun findsPairOfFreeMatchingTiles() {
        val board = boardOf(tile(1, 0, 0, face = Dot1), tile(2, 2, 0, face = Dot1))
        assertEquals(listOf(Move(1, 2)), MoveFinder.findMoves(board))
        assertEquals(1, MoveFinder.count(board))
    }

    @Test
    fun blockedMiddleTilesAreNotPaired() {
        // 한 줄 4장: 양 끝만 자유. 끝이 서로 다르면 안쪽 짝이 있어도 이동 불가.
        val board = boardOf(
            tile(1, 0, 0, face = Dot1),
            tile(2, 2, 0, face = Dot2),
            tile(3, 4, 0, face = Dot2),
            tile(4, 6, 0, face = Dot3),
        )
        assertEquals(0, MoveFinder.count(board))
        assertFalse(MoveFinder.hasMove(board))
        assertNull(MoveFinder.findHint(board))
    }

    @Test
    fun removingOuterPairExposesInnerPair() {
        val board = boardOf(
            tile(1, 0, 0, face = Dot1),
            tile(2, 2, 0, face = Dot2),
            tile(3, 4, 0, face = Dot2),
            tile(4, 6, 0, face = Dot1),
        )
        assertEquals(listOf(Move(1, 4)), MoveFinder.findMoves(board))
        val next = board.remove(1, 4)
        assertEquals(listOf(Move(2, 3)), MoveFinder.findMoves(next))
    }

    @Test
    fun countsAllPairsInGroupOfFour() {
        // 같은 패 4장이 모두 자유 -> C(4,2) = 6쌍
        val board = boardOf(
            tile(1, 0, 0), tile(2, 0, 2), tile(3, 0, 4), tile(4, 0, 6),
        )
        assertEquals(6, MoveFinder.count(board))
        assertEquals(6, MoveFinder.findMoves(board).size)
    }

    @Test
    fun differentFlowersPairWithEachOther() {
        val board = boardOf(
            tile(1, 0, 0, face = TileFace.Flower(1)),
            tile(2, 0, 2, face = TileFace.Flower(4)),
            tile(3, 0, 4, face = TileFace.Season(1)),
            tile(4, 0, 6, face = TileFace.Season(3)),
        )
        assertEquals(listOf(Move(1, 2), Move(3, 4)), MoveFinder.findMoves(board))
    }

    @Test
    fun flowerAndSeasonDoNotPair() {
        val board = boardOf(
            tile(1, 0, 0, face = TileFace.Flower(1)),
            tile(2, 0, 2, face = TileFace.Season(1)),
        )
        assertEquals(0, MoveFinder.count(board))
    }

    @Test
    fun countMatchesFindMovesSize() {
        val board = boardOf(
            tile(1, 0, 0, face = Dot1), tile(2, 0, 2, face = Dot1), tile(3, 0, 4, face = Dot1),
            tile(4, 0, 6, face = Dot2), tile(5, 0, 8, face = Dot2),
            tile(6, 0, 10, face = TileFace.Flower(2)), tile(7, 0, 12, face = TileFace.Flower(3)),
        )
        assertEquals(MoveFinder.findMoves(board).size, MoveFinder.count(board))
        assertEquals(3 + 1 + 1, MoveFinder.count(board))
    }

    @Test
    fun hintIsDeterministicSmallestPair() {
        val board = boardOf(
            tile(9, 0, 0, face = Dot2), tile(8, 0, 2, face = Dot2),
            tile(3, 0, 4, face = Dot1), tile(2, 0, 6, face = Dot1),
        )
        assertEquals(Move(2, 3), MoveFinder.findHint(board))
        assertEquals(MoveFinder.findHint(board), MoveFinder.findHint(board))
    }

    @Test
    fun coveredTilesAreNotOffered() {
        val board = boardOf(
            tile(1, 0, 0, 0, Dot1),
            tile(2, 4, 0, 0, Dot1),
            tile(3, 0, 0, 1, Dot2), // 1을 덮음
        )
        assertEquals(0, MoveFinder.count(board))
    }

    @Test
    fun isValidMoveChecksFreedomMatchAndIdentity() {
        val board = boardOf(
            tile(1, 0, 0, face = Dot1),
            tile(2, 2, 0, face = Dot2),
            tile(3, 4, 0, face = Dot2),
            tile(4, 6, 0, face = Dot1),
        )
        assertTrue(MoveFinder.isValidMove(board, 1, 4))
        assertTrue(MoveFinder.isValidMove(board, 4, 1))
        assertFalse("not free", MoveFinder.isValidMove(board, 2, 3))
        assertFalse("different face", MoveFinder.isValidMove(board, 1, 2))
        assertFalse("same tile", MoveFinder.isValidMove(board, 1, 1))
        assertFalse("unknown id", MoveFinder.isValidMove(board, 1, 99))
        assertFalse("already removed", MoveFinder.isValidMove(board.remove(1, 4), 1, 4))
    }

    @Test
    fun moveRequiresOrderedIds() {
        assertThrows(IllegalArgumentException::class.java) { Move(2, 1) }
    }

    @Test
    fun clearedBoardHasNoMoves() {
        val board = boardOf(tile(1, 0, 0), tile(2, 2, 0)).remove(1, 2)
        assertEquals(0, MoveFinder.count(board))
        assertFalse(MoveFinder.hasMove(board))
    }
}
