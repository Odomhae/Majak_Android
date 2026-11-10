package com.odom.mahjongfun.data.level

import com.odom.mahjongfun.core.model.Layout

/**
 * 단계 번호 -> 배치. 1~[MAPPED_STAGES]단계는 levels.json 매핑을 따르고,
 * 그 이후는 난이도 [MIN_ENDLESS_DIFFICULTY] 이상 배치 중에서 단계 번호 해시로 고른다.
 */
class LevelCatalog(layouts: List<Layout>, private val stageLayoutIds: Map<Int, String>) {
    private val layoutsById: Map<String, Layout>
    private val endlessPool: List<Layout>

    init {
        require(layouts.isNotEmpty()) { "no layouts" }
        layoutsById = layouts.associateBy { it.id }
        require(layoutsById.size == layouts.size) { "duplicate layout id" }
        for (stage in 1..MAPPED_STAGES) {
            val id = stageLayoutIds[stage] ?: throw IllegalArgumentException("stage $stage has no layout")
            require(id in layoutsById) { "stage $stage refers to unknown layout '$id'" }
        }
        // 파일 열거 순서와 무관하게 같은 결과가 나오도록 id로 정렬한다.
        endlessPool = layouts.filter { it.difficulty >= MIN_ENDLESS_DIFFICULTY }.sortedBy { it.id }
        require(endlessPool.isNotEmpty()) { "no layouts with difficulty >= $MIN_ENDLESS_DIFFICULTY" }
    }

    val layouts: Collection<Layout> get() = layoutsById.values

    fun layoutFor(stage: Int): Layout {
        require(stage >= 1) { "stage must be >= 1: $stage" }
        if (stage <= MAPPED_STAGES) return layoutsById.getValue(stageLayoutIds.getValue(stage))
        return endlessPool[Math.floorMod(stage * HASH_MULTIPLIER, endlessPool.size)]
    }

    companion object {
        const val MAPPED_STAGES = 200
        const val MIN_ENDLESS_DIFFICULTY = 4

        /** Knuth 곱셈 해시 상수(2654435761)를 Int로 감은 값. 값이 바뀌면 201단계 이후 배치가 달라지므로 고정한다. */
        private const val HASH_MULTIPLIER = -1640531535
    }
}
