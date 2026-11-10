package com.odom.mahjongfun.core.engine

import com.odom.mahjongfun.core.model.Board

enum class GameStatus {
    PLAYING,

    /** 남은 패가 없다. */
    CLEARED,

    /** 남은 패는 있는데 제거할 수 있는 짝이 하나도 없다. */
    STUCK,
}

/**
 * 한 판의 진행 상태. 불변이며 [GameRules]가 다음 상태를 만든다.
 * @property selected 첫 번째로 고른 패. 없으면 null.
 * @property history 제거한 짝을 시간순으로 쌓은 되돌리기 스택.
 */
data class GameState(
    val board: Board,
    val selected: Int? = null,
    val history: List<Move> = emptyList(),
    val status: GameStatus = GameStatus.PLAYING,
) {
    val canUndo: Boolean get() = history.isNotEmpty()
}

/** 탭 한 번의 결과. UI가 효과(흔들림, 소리 등)를 고르는 데 쓴다. */
enum class TapEvent {
    /** 아무 일 없음 (진행 중이 아니거나, 없거나 이미 치운 패). */
    IGNORED,

    /** 자유롭지 않은 패를 눌렀다. 상태는 그대로. */
    BLOCKED,

    SELECTED,
    DESELECTED,

    /** 다른 패를 눌러 선택을 옮겼다 (짝이 아니었음). */
    SELECTION_MOVED,

    /** 짝이 맞아 두 패를 치웠다. */
    REMOVED,
}

data class TapResult(val state: GameState, val event: TapEvent)

/** CLAUDE.md §4.4 게임 상태 흐름. */
object GameRules {
    fun start(board: Board): GameState = GameState(board, status = statusOf(board))

    fun tap(state: GameState, id: Int): TapResult {
        if (state.status != GameStatus.PLAYING) return TapResult(state, TapEvent.IGNORED)
        val board = state.board
        val tile = board.tiles[id]
        if (tile == null || board.isRemoved(id)) return TapResult(state, TapEvent.IGNORED)
        if (!FreeTileRule.isFree(board, tile)) return TapResult(state, TapEvent.BLOCKED)

        val selected = state.selected
        return when {
            selected == null -> TapResult(state.copy(selected = id), TapEvent.SELECTED)
            selected == id -> TapResult(state.copy(selected = null), TapEvent.DESELECTED)
            MoveFinder.isValidMove(board, selected, id) -> TapResult(remove(state, selected, id), TapEvent.REMOVED)
            else -> TapResult(state.copy(selected = id), TapEvent.SELECTION_MOVED)
        }
    }

    /** 직전 제거를 취소한다. 무제한, 연속 가능, 감점 없음. 기록이 없으면 그대로. */
    fun undo(state: GameState): GameState {
        val last = state.history.lastOrNull() ?: return state
        return GameState(
            board = state.board.restore(last.first, last.second),
            selected = null,
            history = state.history.dropLast(1),
            status = GameStatus.PLAYING,
        )
    }

    private fun remove(state: GameState, a: Int, b: Int): GameState {
        val next = state.board.remove(a, b)
        return GameState(
            board = next,
            selected = null,
            history = state.history + Move(minOf(a, b), maxOf(a, b)),
            status = statusOf(next),
        )
    }

    private fun statusOf(board: Board): GameStatus = when {
        board.isCleared -> GameStatus.CLEARED
        !MoveFinder.hasMove(board) -> GameStatus.STUCK
        else -> GameStatus.PLAYING
    }
}
