package com.odom.mahjongfun.core.engine

import com.odom.mahjongfun.core.model.TilePos
import kotlin.random.Random

/** 놓는 순서상 한 쌍. 위치 목록의 인덱스이며, 제거 순서는 이 순서의 역순이다. */
data class PlannedPair(val first: Int, val second: Int)

/**
 * 역방향 생성. 완성된 판에서 거꾸로 한 쌍씩 쌓는다고 보고, 쌓는 순서(= 제거 순서의 역순)를 만든다.
 *
 * 한 쌍을 놓은 직후의 판에서 그 두 패가 자유로우면, 그 쌍을 마지막에 놓은 순서의 역순으로 제거할 수 있다.
 * 면(face) 배정은 이 계획과 독립이므로, 같은 face 쌍만 배정하면 항상 풀 수 있는 판이 된다.
 *
 * 막다른 길을 줄이기 위한 규칙:
 * - 아래층 패 위치가 모두 채워진 뒤에야 그 위 패를 놓을 수 있다 (위에 덮인 자리는 영영 못 채우기 때문).
 * - 같은 줄에서 빈 자리 양쪽(좌우로 이어지는 어느 쪽이든)에 이미 놓인 패가 있으면 그 자리는 영영 못 놓는다.
 *   (빈 구간의 마지막으로 채우는 패는 항상 양옆이 막힌다.) 그런 자리가 생기는 선택은 버린다.
 *   결과적으로 한 줄은 먼저 놓은 패에서 바깥쪽으로만 이어 붙일 수 있다.
 * 그래도 막히면 [plan]이 처음부터 다시 시도한다.
 */
class ReversePlanner(private val positions: List<TilePos>) {
    private val size = positions.size
    private val below: Array<IntArray>
    private val lefts: Array<IntArray>
    private val rights: Array<IntArray>

    /** lefts/rights를 끝까지 따라가 닿는 모든 자리 (왼쪽/오른쪽 방향 전이 폐포). */
    private val leftReach: Array<IntArray>
    private val rightReach: Array<IntArray>

    init {
        require(size % 2 == 0) { "position count must be even: $size" }
        val index = HashMap<TilePos, Int>(size * 2)
        positions.forEachIndexed { i, p -> require(index.put(p, i) == null) { "duplicate position: $p" } }

        fun lookup(x: Int, y: Int, z: Int): Int? = index[TilePos(x, y, z)]

        below = Array(size) { i ->
            val (x, y, z) = positions[i]
            val found = ArrayList<Int>()
            for (dx in -1..1) for (dy in -1..1) lookup(x + dx, y + dy, z - 1)?.let(found::add)
            found.toIntArray()
        }
        lefts = Array(size) { i -> sideNeighbors(positions[i], -SIDE, ::lookup) }
        rights = Array(size) { i -> sideNeighbors(positions[i], SIDE, ::lookup) }
        leftReach = Array(size) { closure(it, lefts) }
        rightReach = Array(size) { closure(it, rights) }
    }

    private fun closure(start: Int, edges: Array<IntArray>): IntArray {
        val seen = BooleanArray(size)
        val queue = ArrayDeque<Int>()
        queue.addAll(edges[start].toList())
        val result = ArrayList<Int>()
        while (queue.isNotEmpty()) {
            val v = queue.removeFirst()
            if (seen[v]) continue
            seen[v] = true
            result += v
            queue.addAll(edges[v].toList())
        }
        return result.toIntArray()
    }

    private fun sideNeighbors(pos: TilePos, dx: Int, lookup: (Int, Int, Int) -> Int?): IntArray {
        val found = ArrayList<Int>()
        for (dy in -1..1) lookup(pos.x + dx, pos.y + dy, pos.z)?.let(found::add)
        return found.toIntArray()
    }

    /** 최대 [maxAttempts]번 시도해서 계획을 만든다. 모두 막히면 null. */
    fun plan(random: Random, maxAttempts: Int = DEFAULT_MAX_ATTEMPTS): List<PlannedPair>? {
        repeat(maxAttempts) {
            attempt(random)?.let { return it }
        }
        return null
    }

    private fun attempt(random: Random): List<PlannedPair>? {
        val placed = BooleanArray(size)
        val result = ArrayList<PlannedPair>(size / 2)
        val candidates = IntArray(size)
        var remaining = size

        while (remaining > 0) {
            var count = 0
            for (i in 0 until size) {
                if (!placed[i] && below[i].all { placed[it] }) candidates[count++] = i
            }
            shuffle(candidates, count, random)

            var first = -1
            var second = -1
            search@ for (x in 0 until count) {
                for (y in x + 1 until count) {
                    if (canPlacePair(placed, candidates[x], candidates[y])) {
                        first = candidates[x]
                        second = candidates[y]
                        break@search
                    }
                }
            }
            if (first < 0) return null

            placed[first] = true
            placed[second] = true
            remaining -= 2
            result += PlannedPair(first, second)
        }
        return result
    }

    private fun canPlacePair(placed: BooleanArray, a: Int, b: Int): Boolean {
        placed[a] = true
        placed[b] = true
        val ok = sideOpen(placed, a) && sideOpen(placed, b) &&
            noDoomedSpot(placed, a) && noDoomedSpot(placed, b)
        placed[a] = false
        placed[b] = false
        return ok
    }

    /** 놓인 상태에서 좌 또는 우 중 한쪽이 비어 있는가 (= 이 패가 자유로운가). */
    private fun sideOpen(placed: BooleanArray, i: Int): Boolean =
        !lefts[i].any { placed[it] } || !rights[i].any { placed[it] }

    /** i를 놓아서, 이미 놓인 패에 갇혀 영영 놓을 수 없게 된 빈 자리가 생기지 않았는가. */
    private fun noDoomedSpot(placed: BooleanArray, i: Int): Boolean {
        for (e in leftReach[i]) if (isDoomed(placed, e)) return false
        for (e in rightReach[i]) if (isDoomed(placed, e)) return false
        return true
    }

    private fun isDoomed(placed: BooleanArray, e: Int): Boolean =
        !placed[e] && leftReach[e].any { placed[it] } && rightReach[e].any { placed[it] }

    companion object {
        const val DEFAULT_MAX_ATTEMPTS = 50
        private const val SIDE = 2

        /** 플랫폼 구현에 기대지 않는 Fisher-Yates. 같은 시드면 항상 같은 순서가 나와야 한다. */
        internal fun shuffle(array: IntArray, count: Int, random: Random) {
            for (i in count - 1 downTo 1) {
                val j = random.nextInt(i + 1)
                val tmp = array[i]
                array[i] = array[j]
                array[j] = tmp
            }
        }
    }
}
