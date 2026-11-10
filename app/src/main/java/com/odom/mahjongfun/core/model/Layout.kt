package com.odom.mahjongfun.core.model

import kotlin.math.abs

/** 패 배치. 위치 개수는 반드시 짝수이고, 같은 층에서 패끼리 겹치면 안 된다. */
data class Layout(
    val id: String,
    val name: String,
    val difficulty: Int,
    val positions: List<TilePos>,
) {
    init {
        require(positions.size % 2 == 0) { "layout $id: position count must be even: ${positions.size}" }
        require(positions.toSet().size == positions.size) { "layout $id: duplicate positions" }
        for (i in positions.indices) {
            for (j in i + 1 until positions.size) {
                val a = positions[i]
                val b = positions[j]
                require(a.z != b.z || abs(a.x - b.x) >= 2 || abs(a.y - b.y) >= 2) {
                    "layout $id: tiles overlap on the same layer: $a, $b"
                }
            }
        }
    }
}
