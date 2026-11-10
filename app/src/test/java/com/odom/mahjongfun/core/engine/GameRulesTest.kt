package com.odom.mahjongfun.core.engine

import com.odom.mahjongfun.core.Dot1
import com.odom.mahjongfun.core.Dot2
import com.odom.mahjongfun.core.Dot3
import com.odom.mahjongfun.core.boardOf
import com.odom.mahjongfun.core.tile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class GameRulesTest {
    /** 한 줄 4장. 양 끝(1, 4)만 자유로움. 1-4는 Dot1, 2-3은 Dot2 짝. */
    private fun row() = boardOf(
        tile(1, 0, 0, face = Dot1),
        tile(2, 2, 0, face = Dot2),
        tile(3, 4, 0, face = Dot2),
        tile(4, 6, 0, face = Dot1),
    )

    /** 서로 떨어져 있어 전부 자유로운 4장. 1-3은 Dot1, 2-4는 Dot2 짝. */
    private fun spread() = boardOf(
        tile(1, 0, 0, face = Dot1),
        tile(2, 4, 0, face = Dot2),
        tile(3, 8, 0, face = Dot1),
        tile(4, 12, 0, face = Dot2),
    )

    @Test
    fun tappingFreeTileSelectsIt() {
        val result = GameRules.tap(GameRules.start(row()), 1)
        assertEquals(TapEvent.SELECTED, result.event)
        assertEquals(1, result.state.selected)
    }

    @Test
    fun tappingSelectedTileAgainDeselects() {
        val selected = GameRules.tap(GameRules.start(row()), 1).state
        val result = GameRules.tap(selected, 1)
        assertEquals(TapEvent.DESELECTED, result.event)
        assertNull(result.state.selected)
    }

    @Test
    fun tappingBlockedTileDoesNothingAndKeepsSelection() {
        val selected = GameRules.tap(GameRules.start(row()), 1).state
        val result = GameRules.tap(selected, 2)
        assertEquals(TapEvent.BLOCKED, result.event)
        assertSame(selected, result.state)
        assertEquals(1, result.state.selected)
    }

    @Test
    fun tappingBlockedTileWithNothingSelectedChangesNothing() {
        val start = GameRules.start(row())
        val result = GameRules.tap(start, 3)
        assertEquals(TapEvent.BLOCKED, result.event)
        assertSame(start, result.state)
    }

    @Test
    fun matchingSecondTileRemovesBothAndRecordsHistory() {
        val selected = GameRules.tap(GameRules.start(row()), 1).state
        val result = GameRules.tap(selected, 4)

        assertEquals(TapEvent.REMOVED, result.event)
        assertEquals(setOf(1, 4), result.state.board.removed)
        assertNull(result.state.selected)
        assertEquals(listOf(Move(1, 4)), result.state.history)
        assertEquals(GameStatus.PLAYING, result.state.status)
    }

    @Test
    fun removalOrderDoesNotMatterForHistoryPairOrdering() {
        val selected = GameRules.tap(GameRules.start(row()), 4).state
        val result = GameRules.tap(selected, 1)
        assertEquals(listOf(Move(1, 4)), result.state.history)
    }

    @Test
    fun nonMatchingFreeTileMovesTheSelection() {
        val selected = GameRules.tap(GameRules.start(spread()), 1).state
        val result = GameRules.tap(selected, 2) // Dot1 선택 후 Dot2
        assertEquals(TapEvent.SELECTION_MOVED, result.event)
        assertEquals(2, result.state.selected)
        assertTrue(result.state.board.removed.isEmpty())
    }

    @Test
    fun clearingTheLastPairEndsTheGame() {
        val board = boardOf(tile(1, 0, 0), tile(2, 2, 0))
        val state = GameRules.tap(GameRules.tap(GameRules.start(board), 1).state, 2).state
        assertEquals(GameStatus.CLEARED, state.status)
        assertTrue(state.board.isCleared)
    }

    @Test
    fun tapsAreIgnoredOnceTheGameIsOver() {
        val board = boardOf(tile(1, 0, 0), tile(2, 2, 0))
        val cleared = GameRules.tap(GameRules.tap(GameRules.start(board), 1).state, 2).state
        val result = GameRules.tap(cleared, 1)
        assertEquals(TapEvent.IGNORED, result.event)
        assertSame(cleared, result.state)
    }

    @Test
    fun removingLeavesNoMovesIsStuck() {
        // 1-4를 치우면 남는 2(Dot2)와 3(Dot3)는 둘 다 자유롭지만 짝이 아니다.
        val board = boardOf(
            tile(1, 0, 0, face = Dot1),
            tile(2, 2, 0, face = Dot2),
            tile(3, 4, 0, face = Dot3),
            tile(4, 6, 0, face = Dot1),
        )
        val state = GameRules.tap(GameRules.tap(GameRules.start(board), 1).state, 4).state
        assertEquals(GameStatus.STUCK, state.status)
        assertEquals(2, state.board.remainingCount)
    }

    @Test
    fun tapsAreIgnoredWhenStuck() {
        val board = boardOf(
            tile(1, 0, 0, face = Dot1), tile(2, 2, 0, face = Dot2),
            tile(3, 4, 0, face = Dot3), tile(4, 6, 0, face = Dot1),
        )
        val stuck = GameRules.tap(GameRules.tap(GameRules.start(board), 1).state, 4).state
        assertEquals(TapEvent.IGNORED, GameRules.tap(stuck, 2).event)
    }

    @Test
    fun startingOnABoardWithoutMovesIsStuck() {
        val board = boardOf(
            tile(1, 0, 0, face = Dot1), tile(2, 2, 0, face = Dot2),
            tile(3, 4, 0, face = Dot1), tile(4, 6, 0, face = Dot2),
        )
        assertEquals(GameStatus.STUCK, GameRules.start(board).status)
    }

    @Test
    fun tappingUnknownOrRemovedTileIsIgnored() {
        val removed = GameRules.tap(GameRules.tap(GameRules.start(row()), 1).state, 4).state
        assertEquals(TapEvent.IGNORED, GameRules.tap(removed, 99).event)
        assertEquals(TapEvent.IGNORED, GameRules.tap(removed, 1).event)
    }

    @Test
    fun undoRestoresTheLastPairAndClearsSelection() {
        val removed = GameRules.tap(GameRules.tap(GameRules.start(row()), 1).state, 4).state
        val reselected = GameRules.tap(removed, 2).state
        val undone = GameRules.undo(reselected)

        assertTrue(undone.board.removed.isEmpty())
        assertNull(undone.selected)
        assertTrue(undone.history.isEmpty())
        assertFalse(undone.canUndo)
    }

    @Test
    fun undoCanBeRepeatedAllTheWayBack() {
        var state = GameRules.start(row())
        state = GameRules.tap(GameRules.tap(state, 1).state, 4).state
        state = GameRules.tap(GameRules.tap(state, 2).state, 3).state
        assertEquals(GameStatus.CLEARED, state.status)

        state = GameRules.undo(state)
        assertEquals(GameStatus.PLAYING, state.status)
        assertEquals(setOf(1, 4), state.board.removed)

        state = GameRules.undo(state)
        assertTrue(state.board.removed.isEmpty())
        assertSame(state, GameRules.undo(state)) // 더 되돌릴 것이 없으면 그대로
    }

    @Test
    fun undoGetsOutOfAStuckPosition() {
        val board = boardOf(
            tile(1, 0, 0, face = Dot1), tile(2, 2, 0, face = Dot2),
            tile(3, 4, 0, face = Dot3), tile(4, 6, 0, face = Dot1),
        )
        val stuck = GameRules.tap(GameRules.tap(GameRules.start(board), 1).state, 4).state
        val undone = GameRules.undo(stuck)
        assertEquals(GameStatus.PLAYING, undone.status)
        assertEquals(4, undone.board.remainingCount)
    }

    @Test
    fun boardRestoreRejectsTilesThatWereNotRemoved() {
        val board = row()
        try {
            board.restore(1, 4)
        } catch (_: IllegalArgumentException) {
            return
        }
        throw AssertionError("expected IllegalArgumentException")
    }
}
