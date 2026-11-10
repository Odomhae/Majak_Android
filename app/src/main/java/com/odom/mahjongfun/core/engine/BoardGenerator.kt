package com.odom.mahjongfun.core.engine

import com.odom.mahjongfun.core.model.Board
import com.odom.mahjongfun.core.model.Layout
import com.odom.mahjongfun.core.model.Tile
import kotlin.random.Random

/**
 * 풀이가 보장되는 판 생성. 시드가 단계 번호이므로 같은 단계는 모든 기기에서 같은 판이 나온다.
 *
 * 1. [ReversePlanner]로 거꾸로 쌓는 순서를 만들고
 * 2. 쌓는 순서의 각 쌍에 같은 face 쌍을 배정한 뒤
 * 3. [Solver]로 검증한다. 풀 수 없다고 판명되면(구성상 일어나지 않아야 한다) 다음 시드 변형으로 다시 만든다.
 */
object BoardGenerator {
    const val SALT = 0x4D4A5L
    const val MAX_VARIANTS = 8
    const val VERIFY_NODES = 20_000

    /** 단계 번호와 변형 번호로 만드는 시드. 변형 0이 기본 시드 `stage * 31 + SALT`. */
    fun seedFor(stage: Int, variant: Int = 0): Long =
        stage.toLong() * 31 + SALT + variant * VARIANT_STEP

    /**
     * @param verify true면 [Solver]로 짧게([VERIFY_NODES] 노드) 검증한다. 구성상 풀이가 보장되므로 한도 내에 결론이
     * 안 나면(TIMEOUT) 그대로 쓴다. 풀이기가 어려운 판은 휴대폰에서 수 초가 걸릴 수 있어 한도를 작게 둔다.
     * 1~200단계는 테스트에서 큰 한도로 전부 풀림을 확인한다.
     * @throws IllegalStateException 모든 시드 변형에서 생성에 실패한 경우
     */
    fun generate(layout: Layout, stage: Int, verify: Boolean = true): Board {
        require(stage >= 1) { "stage must be >= 1: $stage" }
        val planner = ReversePlanner(layout.positions)
        for (variant in 0 until MAX_VARIANTS) {
            val random = Random(seedFor(stage, variant))
            val board = build(layout, planner.plan(random) ?: continue, random)
            if (!verify || Solver.solve(board, VERIFY_NODES).status != SolveStatus.UNSOLVABLE) return board
        }
        error("failed to generate stage $stage with layout ${layout.id}")
    }

    private fun build(layout: Layout, plan: List<PlannedPair>, random: Random): Board {
        val faces = FacePairs.pick(plan.size, random)
        val tiles = ArrayList<Tile>(layout.positions.size)
        plan.forEachIndexed { step, pair ->
            val (faceA, faceB) = faces[step]
            tiles += Tile(pair.first, layout.positions[pair.first], faceA)
            tiles += Tile(pair.second, layout.positions[pair.second], faceB)
        }
        return Board(tiles.sortedBy { it.id })
    }

    private const val VARIANT_STEP = 7919L
}
