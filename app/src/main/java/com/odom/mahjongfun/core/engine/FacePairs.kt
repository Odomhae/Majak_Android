package com.odom.mahjongfun.core.engine

import com.odom.mahjongfun.core.model.TileFace
import com.odom.mahjongfun.core.model.TileSet
import com.odom.mahjongfun.core.model.matchGroup
import kotlin.random.Random

/** 풀 세트 144장을 짝 72쌍으로 나눈 뒤 필요한 만큼 뽑는다. 144장 미만 배치는 항상 쌍 단위로 쓴다. */
object FacePairs {
    const val MAX_PAIRS = TileSet.FULL_SIZE / 2

    /** [count]쌍을 무작위로 뽑는다. 같은 시드면 같은 결과. */
    fun pick(count: Int, random: Random): List<Pair<TileFace, TileFace>> {
        require(count in 0..MAX_PAIRS) { "pair count must be 0..$MAX_PAIRS: $count" }
        val all = allPairs(random)
        val order = IntArray(all.size) { it }
        ReversePlanner.shuffle(order, order.size, random)
        return List(count) { all[order[it]] }
    }

    private fun allPairs(random: Random): List<Pair<TileFace, TileFace>> {
        val pairs = ArrayList<Pair<TileFace, TileFace>>(MAX_PAIRS)
        // 그룹을 안정적인 순서로 돌기 위해 첫 등장 순서를 유지한다.
        val groups = LinkedHashMap<Any, MutableList<TileFace>>()
        for (face in TileSet.fullSet()) groups.getOrPut(face.matchGroup) { ArrayList() }.add(face)
        for (faces in groups.values) {
            // 꽃/계절은 어떤 두 장이든 짝이 되므로 섞어서 짝지으면 조합이 다양해진다.
            val order = IntArray(faces.size) { it }
            ReversePlanner.shuffle(order, order.size, random)
            for (k in order.indices step 2) pairs += faces[order[k]] to faces[order[k + 1]]
        }
        return pairs
    }
}
