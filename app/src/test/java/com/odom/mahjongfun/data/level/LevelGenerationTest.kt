package com.odom.mahjongfun.data.level

import com.odom.mahjongfun.TestAssets
import com.odom.mahjongfun.core.assertReplayClears
import com.odom.mahjongfun.core.engine.BoardGenerator
import com.odom.mahjongfun.core.engine.BoardGeneratorTest
import com.odom.mahjongfun.core.engine.ReversePlanner
import com.odom.mahjongfun.core.engine.SolveStatus
import com.odom.mahjongfun.core.engine.Solver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import kotlin.random.Random

/** CLAUDE.md §5.3, §11: 1~200단계 전부 생성하고 Solver로 풀이 가능함을 확인한다. */
class LevelGenerationTest {
    // 일부 판은 풀이기가 100만 노드 안팎을 쓴다. 풀이기 한도이지 판의 문제가 아니다. (docs/DECISIONS.md)
    private val TEST_MAX_NODES = 2_000_000

    @Test
    fun allStagesFrom1To200AreGeneratedAndSolvable() {
        var maxNodes = 0
        for (stage in 1..200) {
            val layout = TestAssets.catalog.layoutFor(stage)
            val board = BoardGenerator.generate(layout, stage, verify = false)
            assertEquals("stage $stage tile count", layout.positions.size, board.tiles.size)

            val result = Solver.solve(board, TEST_MAX_NODES)
            assertEquals("stage $stage (${layout.id}) status, nodes=${result.nodes}", SolveStatus.SOLVED, result.status)
            assertReplayClears(board, result.moves)
            maxNodes = maxOf(maxNodes, result.nodes)
        }
        println("max solver nodes over stages 1..200 = $maxNodes")
    }

    @Test
    fun endlessStagesAreAlsoGeneratedAndSolvable() {
        for (stage in listOf(201, 250, 333, 500, 999, 1234)) {
            val layout = TestAssets.catalog.layoutFor(stage)
            val board = BoardGenerator.generate(layout, stage, verify = false)
            val result = Solver.solve(board, TEST_MAX_NODES)
            assertEquals("stage $stage (${layout.id})", SolveStatus.SOLVED, result.status)
            assertReplayClears(board, result.moves)
        }
    }

    /** 모든 배치 x 여러 시드에서 계획기가 막히지 않고, 계획이 실제 엔진 규칙으로 제거 가능한 순서인지 확인한다. */
    @Test
    fun plannerSucceedsOnEveryLayoutForManySeeds() {
        for (layout in TestAssets.layouts) {
            val planner = ReversePlanner(layout.positions)
            for (seed in 0L until 20L) {
                val plan = planner.plan(Random(seed))
                assertNotNull("no plan for ${layout.id} seed $seed", plan)
                BoardGeneratorTest.assertPlanRemovable(layout, plan!!)
            }
        }
    }
}
