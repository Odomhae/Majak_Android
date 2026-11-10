package com.odom.mahjongfun.core.engine

import com.odom.mahjongfun.core.model.Board
import com.odom.mahjongfun.core.model.matchGroup

/** 제거 가능한 짝. 항상 `first < second`. */
data class Move(val first: Int, val second: Int) {
    init { require(first < second) { "Move must satisfy first < second: $first, $second" } }
}

/** 지금 제거할 수 있는 짝을 찾는다. 힌트와 막힘 감지에서 쓴다. */
object MoveFinder {
    /** 가능한 모든 짝, (first, second) 오름차순. */
    fun findMoves(board: Board): List<Move> {
        val moves = ArrayList<Move>()
        for (group in freeGroups(board)) {
            for (i in group.indices) for (j in i + 1 until group.size) {
                moves += Move(group[i], group[j])
            }
        }
        return moves.sortedWith(compareBy({ it.first }, { it.second }))
    }

    /** 가능한 짝의 수. 패 쌍을 만들지 않고 그룹별 조합 수만 센다. */
    fun count(board: Board): Int = freeGroups(board).sumOf { it.size * (it.size - 1) / 2 }

    fun hasMove(board: Board): Boolean = freeGroups(board).any { it.size >= 2 }

    /** 힌트로 보여줄 짝. 항상 같은 판이면 같은 짝(가장 작은 id)을 준다. */
    fun findHint(board: Board): Move? = findMoves(board).firstOrNull()

    /** 두 패가 지금 제거 가능한 짝인지(서로 다른 패, 둘 다 자유, face가 맞음) 검사한다. */
    fun isValidMove(board: Board, a: Int, b: Int): Boolean {
        if (a == b) return false
        val ta = board.tiles[a] ?: return false
        val tb = board.tiles[b] ?: return false
        return ta.face.matchGroup == tb.face.matchGroup &&
            FreeTileRule.isFree(board, ta) &&
            FreeTileRule.isFree(board, tb)
    }

    /** 자유로운 패 id를 짝 그룹별로 묶는다 (그룹 안은 id 오름차순). */
    private fun freeGroups(board: Board): Collection<List<Int>> =
        FreeTileRule.freeTiles(board)
            .groupBy({ it.face.matchGroup }, { it.id })
            .values
}
