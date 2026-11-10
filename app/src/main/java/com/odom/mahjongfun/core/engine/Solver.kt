package com.odom.mahjongfun.core.engine

import com.odom.mahjongfun.core.model.Board
import com.odom.mahjongfun.core.model.TilePos
import com.odom.mahjongfun.core.model.matchGroup
import kotlin.random.Random

enum class SolveStatus { SOLVED, UNSOLVABLE, TIMEOUT }

/**
 * @property moves SOLVED일 때 제거 순서대로의 풀이.
 * @property nodes 탐색한 상태 수.
 */
data class SolveResult(val status: SolveStatus, val moves: List<Move>, val nodes: Int)

/**
 * 검증용 자동 풀이기. DFS + 실패 상태 메모이제이션(제거 집합 비트셋).
 *
 * - 한 번에 많은 패를 풀어 주는 수를 먼저 시도하고, 남은 패가 2장뿐인 면에서 한 장이 다른 한 장을
 *   덮고 있으면(영원히 못 푸는 데드락) 바로 가지를 친다.
 * - 한 갈래에 갇히지 않도록 예산을 키워 가며 정렬에 흔들림을 준 재시작을 한다.
 *   끝까지 탐색해 실패한 상태는 정렬과 무관하게 사실이므로 라운드 사이에 유지한다.
 * - 시간 대신 탐색 노드 수로 제한한다. 게임 로직에서 시계를 직접 쓰지 않고, 테스트 결과를 결정적으로 하기 위해서다.
 *   한도를 넘으면 [SolveStatus.TIMEOUT] (풀이 가능 여부 모름).
 */
object Solver {
    const val DEFAULT_MAX_NODES = 200_000

    private const val FIRST_ROUND_NODES = 1_500
    private const val ROUND_GROWTH = 1
    private const val JITTER_SEED = 0x5EEDL

    fun solve(board: Board, maxNodes: Int = DEFAULT_MAX_NODES): SolveResult =
        Search(board, maxNodes).run()

    private class StateKey(val bits: LongArray) {
        override fun equals(other: Any?) = other is StateKey && bits.contentEquals(other.bits)
        override fun hashCode() = bits.contentHashCode()
    }

    private class Candidate(val first: Int, val second: Int, val score: Int)

    private class Search(board: Board, private val maxNodes: Int) {
        private val tiles = board.activeTiles()
        private val size = tiles.size

        private val above: Array<IntArray>
        private val lefts: Array<IntArray>
        private val rights: Array<IntArray>

        /** i를 치우면 새로 자유로워질 수 있는 후보: i가 덮고 있는 패, 좌우 이웃. */
        private val touched: Array<IntArray>

        /** covers[y][x]: x가 (직접 또는 건너서) y의 위에 있다. */
        private val covers: Array<BooleanArray>
        private val group = IntArray(size)
        private val groupSize: IntArray
        private val weight = IntArray(size)

        private val removed = BooleanArray(size)
        private val bits = LongArray((size + 63) / 64)
        private val remainingInGroup: IntArray
        private val deadStates = HashSet<StateKey>()
        private val path = ArrayList<Move>()

        private var nodes = 0
        private var roundLimit = 0
        private var roundNodes = 0
        private var timedOut = false
        private var jitter: Random? = null

        init {
            val index = HashMap<TilePos, Int>(size * 2)
            tiles.forEachIndexed { i, t -> index[t.pos] = i }
            fun at(x: Int, y: Int, z: Int): Int? = index[TilePos(x, y, z)]

            above = Array(size) { i ->
                val (x, y, z) = tiles[i].pos
                val found = ArrayList<Int>()
                for (dx in -1..1) for (dy in -1..1) at(x + dx, y + dy, z + 1)?.let(found::add)
                found.toIntArray()
            }
            lefts = Array(size) { i -> side(tiles[i].pos, -2, ::at) }
            rights = Array(size) { i -> side(tiles[i].pos, 2, ::at) }

            val below = Array(size) { ArrayList<Int>() }
            for (i in 0 until size) for (j in above[i]) below[j] += i
            touched = Array(size) { i ->
                (below[i] + lefts[i].toList() + rights[i].toList()).distinct().toIntArray()
            }

            covers = Array(size) { y -> transitiveAbove(y) }

            val groupIds = HashMap<Any, Int>()
            for (i in 0 until size) group[i] = groupIds.getOrPut(tiles[i].face.matchGroup) { groupIds.size }
            groupSize = IntArray(groupIds.size)
            for (i in 0 until size) groupSize[group[i]]++
            remainingInGroup = groupSize.copyOf()

            // 많은 패를 막고 있는 패를 먼저 치우는 쪽이 막다른 길에 덜 빠진다.
            for (i in 0 until size) {
                for (j in above[i]) weight[j]++
                for (j in lefts[i]) weight[j]++
                for (j in rights[i]) weight[j]++
            }
        }

        private fun side(pos: TilePos, dx: Int, at: (Int, Int, Int) -> Int?): IntArray {
            val found = ArrayList<Int>()
            for (dy in -1..1) at(pos.x + dx, pos.y + dy, pos.z)?.let(found::add)
            return found.toIntArray()
        }

        private fun transitiveAbove(start: Int): BooleanArray {
            val seen = BooleanArray(size)
            val stack = ArrayDeque<Int>()
            above[start].forEach { stack.addLast(it) }
            while (stack.isNotEmpty()) {
                val v = stack.removeLast()
                if (seen[v]) continue
                seen[v] = true
                above[v].forEach { stack.addLast(it) }
            }
            return seen
        }

        fun run(): SolveResult {
            if (size == 0) return SolveResult(SolveStatus.SOLVED, emptyList(), 0)

            var round = 0
            var budget = FIRST_ROUND_NODES
            while (nodes < maxNodes) {
                roundLimit = minOf(budget, maxNodes - nodes)
                roundNodes = 0
                timedOut = false
                jitter = if (round == 0) null else Random(JITTER_SEED + round)

                if (dfs(0)) return SolveResult(SolveStatus.SOLVED, path.toList(), nodes)
                if (!timedOut) return SolveResult(SolveStatus.UNSOLVABLE, emptyList(), nodes)

                round++
                budget *= ROUND_GROWTH
            }
            return SolveResult(SolveStatus.TIMEOUT, emptyList(), nodes)
        }

        private fun dfs(removedBefore: Int): Boolean {
            if (removedBefore == size) return true
            if (roundNodes >= roundLimit) {
                timedOut = true
                return false
            }
            roundNodes++
            nodes++
            if (StateKey(bits) in deadStates) return false

            val entryKey = StateKey(bits.copyOf())
            val pathMark = path.size
            val forced = ArrayList<Int>()
            val removedCount = removedBefore + applyForcedMoves(forced)

            if (removedCount == size) return true
            if (!hasDeadlockedPair()) {
                for (move in orderedMoves()) {
                    setRemoved(move.first, true)
                    setRemoved(move.second, true)
                    path += toMove(move)
                    if (dfs(removedCount + 2)) return true
                    path.removeAt(path.lastIndex)
                    setRemoved(move.first, false)
                    setRemoved(move.second, false)
                    if (timedOut) break
                }
            }

            undoForcedMoves(forced, pathMark)
            if (!timedOut) deadStates += entryKey
            return false
        }

        /**
         * 어떤 면의 남은 패가 전부 지금 자유로우면 그 패들은 순서와 짝 지음에 상관없이 지금 치워도 손해가 없다.
         * (치우는 것은 다른 패를 풀어 줄 뿐이고, 이 패들은 서로하고만 짝이 되기 때문이다.)
         * 분기 없이 바로 치우고, 치운 패 인덱스를 [applied]에 쌓아 돌려놓을 수 있게 한다.
         * @return 치운 패 수
         */
        private fun applyForcedMoves(applied: MutableList<Int>): Int {
            var count = 0
            val freeInGroup = IntArray(groupSize.size)
            while (true) {
                freeInGroup.fill(0)
                val free = ArrayList<Int>()
                for (i in 0 until size) if (isFree(i)) {
                    free += i
                    freeInGroup[group[i]]++
                }
                var changed = false
                val pending = IntArray(groupSize.size) { -1 }
                for (i in free) {
                    val g = group[i]
                    if (remainingInGroup[g] == 0 || freeInGroup[g] != remainingInGroup[g]) continue
                    if (pending[g] < 0) {
                        pending[g] = i
                    } else {
                        val a = pending[g]
                        pending[g] = -1
                        setRemoved(a, true)
                        setRemoved(i, true)
                        path += toMove(Candidate(a, i, 0))
                        applied += a
                        applied += i
                        count += 2
                        changed = true
                    }
                }
                if (!changed) return count
            }
        }

        private fun undoForcedMoves(applied: List<Int>, pathMark: Int) {
            for (k in applied.indices.reversed()) setRemoved(applied[k], false)
            while (path.size > pathMark) path.removeAt(path.lastIndex)
        }

        /** 남은 패가 2장뿐인 면에서 한 장이 다른 한 장을 덮고 있으면 둘 다 영원히 못 치운다. */
        private fun hasDeadlockedPair(): Boolean {
            for (g in remainingInGroup.indices) if (remainingInGroup[g] == 2) {
                var first = -1
                var second = -1
                for (i in 0 until size) if (!removed[i] && group[i] == g) {
                    if (first < 0) first = i else { second = i; break }
                }
                if (covers[first][second] || covers[second][first]) return true
            }
            return false
        }

        private fun orderedMoves(): List<Candidate> {
            val free = ArrayList<Int>()
            for (i in 0 until size) if (isFree(i)) free += i
            val moves = ArrayList<Candidate>()
            for (x in free.indices) for (y in x + 1 until free.size) {
                val a = free[x]
                val b = free[y]
                if (group[a] == group[b]) moves += Candidate(a, b, score(a, b))
            }
            moves.sortWith(compareByDescending<Candidate> { it.score }.thenBy { it.first }.thenBy { it.second })
            return moves
        }

        /** 이 수로 새로 자유로워지는 패가 많을수록, 막고 있던 패일수록 높은 점수. */
        private fun score(a: Int, b: Int): Int {
            var unlocked = 0
            removed[a] = true
            removed[b] = true
            for (t in touched[a]) if (!removed[t] && isFree(t)) unlocked++
            for (t in touched[b]) if (!removed[t] && isFree(t)) unlocked++
            removed[a] = false
            removed[b] = false
            val noise = jitter?.nextInt(JITTER_RANGE) ?: 0
            return unlocked * UNLOCK_WEIGHT + weight[a] + weight[b] + noise
        }

        private fun isFree(i: Int): Boolean =
            !removed[i] && above[i].all { removed[it] } &&
                (lefts[i].all { removed[it] } || rights[i].all { removed[it] })

        private fun setRemoved(i: Int, value: Boolean) {
            removed[i] = value
            val mask = 1L shl (i and 63)
            bits[i ushr 6] = if (value) bits[i ushr 6] or mask else bits[i ushr 6] and mask.inv()
            remainingInGroup[group[i]] += if (value) -1 else 1
        }

        private fun toMove(c: Candidate): Move {
            val a = tiles[c.first].id
            val b = tiles[c.second].id
            return if (a < b) Move(a, b) else Move(b, a)
        }
    }

    private const val UNLOCK_WEIGHT = 4
    private const val JITTER_RANGE = 1000
}
