package com.odom.mahjongfun.core.engine

import com.odom.mahjongfun.TestAssets
import com.odom.mahjongfun.core.assertReplayClears
import com.odom.mahjongfun.core.model.Board
import com.odom.mahjongfun.core.model.Layout
import com.odom.mahjongfun.core.model.Tile
import com.odom.mahjongfun.core.model.TileFace
import com.odom.mahjongfun.core.model.TilePos
import com.odom.mahjongfun.core.model.matchGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class BoardGeneratorTest {
    private val small = Layout(
        "small", "small", 1,
        listOf(
            TilePos(0, 0, 0), TilePos(2, 0, 0), TilePos(4, 0, 0), TilePos(6, 0, 0),
            TilePos(0, 2, 0), TilePos(2, 2, 0), TilePos(4, 2, 0), TilePos(6, 2, 0),
            TilePos(2, 0, 1), TilePos(4, 0, 1), TilePos(2, 2, 1), TilePos(4, 2, 1),
        ),
    )

    private fun faceKey(board: Board) = board.tiles.values.sortedBy { it.id }.map { it.face }

    @Test
    fun generatesBoardWithAllLayoutPositionsAndPairedFaces() {
        val board = BoardGenerator.generate(small, stage = 1)

        assertEquals(small.positions.size, board.tiles.size)
        assertEquals(small.positions.toSet(), board.tiles.values.map { it.pos }.toSet())
        val groups = board.tiles.values.groupingBy { it.face.matchGroup }.eachCount()
        assertTrue("every face group must have an even count", groups.values.all { it % 2 == 0 })
    }

    @Test
    fun sameStageGivesSameBoard() {
        val a = BoardGenerator.generate(small, stage = 7)
        val b = BoardGenerator.generate(small, stage = 7)
        assertEquals(faceKey(a), faceKey(b))
    }

    @Test
    fun differentStagesGiveDifferentBoards() {
        val boards = (1..6).map { faceKey(BoardGenerator.generate(small, stage = it)) }.toSet()
        assertTrue("expected variety across stages, got ${boards.size}", boards.size > 1)
    }

    @Test
    fun generatedBoardIsSolvableAndSolutionReplays() {
        for (stage in 1..30) {
            val board = BoardGenerator.generate(small, stage)
            val result = Solver.solve(board)
            assertEquals("stage $stage", SolveStatus.SOLVED, result.status)
            assertReplayClears(board, result.moves)
        }
    }

    @Test
    fun seedIsStageTimesThirtyOnePlusSalt() {
        assertEquals(5L * 31 + BoardGenerator.SALT, BoardGenerator.seedFor(5))
        assertNotEquals(BoardGenerator.seedFor(5), BoardGenerator.seedFor(5, variant = 1))
    }

    @Test
    fun rejectsInvalidStage() {
        try {
            BoardGenerator.generate(small, stage = 0)
        } catch (_: IllegalArgumentException) {
            return
        }
        throw AssertionError("expected IllegalArgumentException")
    }

    @Test
    fun facePairsAreMatchingAndCoverFullSetWhenAskedForAll() {
        val pairs = FacePairs.pick(FacePairs.MAX_PAIRS, Random(1))
        assertEquals(72, pairs.size)
        assertTrue(pairs.all { (a, b) -> a.matchGroup == b.matchGroup })
        val faces = pairs.flatMap { listOf(it.first, it.second) }
        assertEquals(144, faces.size)
        assertEquals(TileFaceCounts.full, faces.groupingBy { it }.eachCount())
    }

    @Test
    fun facePairsAreDeterministicPerSeed() {
        assertEquals(FacePairs.pick(20, Random(3)), FacePairs.pick(20, Random(3)))
    }

    /** 계획기 결과가 엔진의 실제 규칙에서 제거 가능한 순서인지 독립적으로 재생해 본다. */
    @Test
    fun plannerOrderReplaysAsLegalRemovalsOnTheRealEngine() {
        for (layout in TestAssets.layouts.filter { it.positions.size <= 60 }) {
            val plan = ReversePlanner(layout.positions).plan(Random(11))
            assertNotNull("no plan for ${layout.id}", plan)
            assertPlanRemovable(layout, plan!!)
        }
    }

    companion object {
        /** 계획의 역순(= 제거 순서)으로 판에서 한 쌍씩 뺄 때 두 패가 항상 자유로운지 확인한다. */
        fun assertPlanRemovable(layout: Layout, plan: List<PlannedPair>) {
            assertEquals(layout.positions.size / 2, plan.size)
            val tiles = layout.positions.mapIndexed { i, p -> Tile(i, p, TileFace.Dot(1)) }
            var board = Board(tiles)
            for (pair in plan.asReversed()) {
                assertTrue(
                    "${layout.id}: tiles ${pair.first},${pair.second} not both free",
                    FreeTileRule.isFree(board, pair.first) && FreeTileRule.isFree(board, pair.second),
                )
                board = board.remove(pair.first, pair.second)
            }
            assertTrue(board.isCleared)
        }
    }

    private object TileFaceCounts {
        val full: Map<TileFace, Int> = com.odom.mahjongfun.core.model.TileSet.fullSet().groupingBy { it }.eachCount()
    }
}
