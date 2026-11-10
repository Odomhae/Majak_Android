package com.odom.mahjongfun.core.engine

import com.odom.mahjongfun.TestAssets
import com.odom.mahjongfun.core.Dot1
import com.odom.mahjongfun.core.Dot2
import com.odom.mahjongfun.core.Dot3
import com.odom.mahjongfun.core.assertReplayClears
import com.odom.mahjongfun.core.boardOf
import com.odom.mahjongfun.core.model.TileFace
import com.odom.mahjongfun.core.tile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SolverTest {
    @Test
    fun emptyBoardIsSolved() {
        val result = Solver.solve(boardOf(tile(1, 0, 0), tile(2, 2, 0)).remove(1, 2))
        assertEquals(SolveStatus.SOLVED, result.status)
        assertTrue(result.moves.isEmpty())
    }

    @Test
    fun solvesNestedPairsAndReturnsReplayableMoves() {
        // 바깥 쌍을 치워야 안쪽 쌍이 드러난다.
        val board = boardOf(
            tile(1, 0, 0, face = Dot1),
            tile(2, 2, 0, face = Dot2),
            tile(3, 4, 0, face = Dot2),
            tile(4, 6, 0, face = Dot1),
        )
        val result = Solver.solve(board)
        assertEquals(SolveStatus.SOLVED, result.status)
        assertEquals(listOf(Move(1, 4), Move(2, 3)), result.moves)
        assertReplayClears(board, result.moves)
    }

    @Test
    fun solvesStackedBoardTopLayerFirst() {
        val board = boardOf(
            tile(1, 0, 0, 0, Dot1),
            tile(2, 4, 0, 0, Dot1),
            tile(3, 0, 0, 1, Dot2), // 1 위
            tile(4, 4, 0, 1, Dot2), // 2 위
        )
        val result = Solver.solve(board)
        assertEquals(SolveStatus.SOLVED, result.status)
        assertEquals(listOf(Move(3, 4), Move(1, 2)), result.moves)
        assertReplayClears(board, result.moves)
    }

    @Test
    fun coveredPairsCannotBeUsedEvenIfTheyMatch() {
        // 위층 3, 4는 서로 다른 face라 짝이 안 되고, 아래층 1, 2는 덮여 있어 짝이 돼도 못 쓴다.
        val board = boardOf(
            tile(1, 0, 0, 0, Dot1),
            tile(2, 4, 0, 0, Dot1),
            tile(3, 0, 0, 1, Dot2),
            tile(4, 4, 0, 1, Dot3),
        )
        assertEquals(SolveStatus.UNSOLVABLE, Solver.solve(board).status)
    }

    @Test
    fun reportsUnsolvableWhenNoPairCanEverBeFree() {
        // 한 줄 4장, 양 끝(자유)이 서로 다른 face
        val board = boardOf(
            tile(1, 0, 0, face = Dot1),
            tile(2, 2, 0, face = Dot2),
            tile(3, 4, 0, face = Dot1),
            tile(4, 6, 0, face = Dot2),
        )
        val result = Solver.solve(board)
        assertEquals(SolveStatus.UNSOLVABLE, result.status)
        assertTrue(result.moves.isEmpty())
    }

    @Test
    fun picksTheRightPairOutOfFourIdenticalFreeTiles() {
        // Dot1 두 쌍과 Dot2 두 쌍을 잘못 짝지으면 막힌다. 어느 순서든 결국 풀려야 한다.
        val board = boardOf(
            tile(1, 0, 0, face = Dot1),
            tile(2, 2, 0, face = Dot2),
            tile(3, 4, 0, face = Dot1),
            tile(4, 6, 0, face = Dot2),
            tile(5, 0, 2, face = Dot1),
            tile(6, 2, 2, face = Dot2),
            tile(7, 4, 2, face = Dot1),
            tile(8, 6, 2, face = Dot2),
        )
        val result = Solver.solve(board)
        assertEquals(SolveStatus.SOLVED, result.status)
        assertReplayClears(board, result.moves)
    }

    @Test
    fun flowersWithDifferentNumbersAreSolvedAsPairs() {
        val board = boardOf(
            tile(1, 0, 0, face = TileFace.Flower(1)),
            tile(2, 2, 0, face = TileFace.Flower(3)),
            tile(3, 0, 2, face = TileFace.Season(2)),
            tile(4, 2, 2, face = TileFace.Season(4)),
        )
        val result = Solver.solve(board)
        assertEquals(SolveStatus.SOLVED, result.status)
        assertReplayClears(board, result.moves)
    }

    @Test
    fun timesOutWhenNodeBudgetIsExhausted() {
        // 강제 수만으로는 안 풀리는 큰 판은 노드 1개로는 끝낼 수 없다.
        val turtle = TestAssets.layouts.first { it.id == "turtle_classic" }
        val board = BoardGenerator.generate(turtle, stage = 1, verify = false)

        val result = Solver.solve(board, maxNodes = 1)

        assertEquals(SolveStatus.TIMEOUT, result.status)
        assertTrue(result.moves.isEmpty())
        assertEquals(SolveStatus.SOLVED, Solver.solve(board, maxNodes = 2_000_000).status)
    }

    @Test
    fun removesAllFreeTilesOfAFaceWithoutBranching() {
        // 같은 면 4장이 전부 자유로우면 어떤 순서로 짝지어도 되므로 탐색 노드 1개로 끝난다.
        val board = boardOf(
            tile(1, 0, 0), tile(2, 0, 2), tile(3, 0, 4), tile(4, 0, 6),
        )
        val result = Solver.solve(board)
        assertEquals(SolveStatus.SOLVED, result.status)
        assertEquals(1, result.nodes)
        assertReplayClears(board, result.moves)
    }

    @Test
    fun detectsDeadlockWhenLastPairOfAFaceCoversItself() {
        // 마지막 남은 같은 면 두 장이 위아래로 겹쳐 있으면 영영 못 푼다.
        val board = boardOf(
            tile(1, 0, 0, 0, Dot1),
            tile(2, 0, 0, 1, Dot1),
            tile(3, 4, 0, 0, Dot2),
            tile(4, 8, 0, 0, Dot2),
        )
        assertEquals(SolveStatus.UNSOLVABLE, Solver.solve(board).status)
    }
}
