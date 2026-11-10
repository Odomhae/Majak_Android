package com.odom.mahjongfun.ui.game

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.odom.mahjongfun.core.engine.BoardGenerator
import com.odom.mahjongfun.core.engine.GameRules
import com.odom.mahjongfun.core.engine.GameState
import com.odom.mahjongfun.core.engine.GameStatus
import com.odom.mahjongfun.core.engine.TapEvent
import com.odom.mahjongfun.core.model.Board
import com.odom.mahjongfun.core.util.Clock
import com.odom.mahjongfun.data.level.LevelSource
import com.odom.mahjongfun.di.DefaultDispatcher
import com.odom.mahjongfun.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** 막힌 패를 눌렀을 때 흔들 패. 같은 패를 연달아 눌러도 다시 흔들리도록 [nonce]를 올린다. */
data class ShakeEvent(val tileId: Int, val nonce: Int)

data class GameUiState(
    val stage: Int,
    val loading: Boolean = true,
    val failed: Boolean = false,
    val game: GameState? = null,
    val elapsedMillis: Long = 0,
    val paused: Boolean = false,
    val foreground: Boolean = true,
    val shake: ShakeEvent? = null,
) {
    val remainingTiles: Int get() = game?.board?.remainingCount ?: 0

    /** 시간이 흐르는 중인가. 일시정지, 앱 백그라운드, 로딩, 클리어·막힘 상태에서는 멈춘다. */
    val clockRunning: Boolean
        get() = !loading && !paused && foreground && game?.status == GameStatus.PLAYING
}

@HiltViewModel
class GameViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val levels: LevelSource,
    private val clock: Clock,
    @DefaultDispatcher private val generationDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val stage: Int = savedStateHandle.get<Int>(Routes.ARG_STAGE) ?: 1

    private val _ui = MutableStateFlow(GameUiState(stage))
    val ui: StateFlow<GameUiState> = _ui.asStateFlow()

    private var initialBoard: Board? = null

    init {
        load()
        viewModelScope.launch { runClock() }
    }

    fun retry() {
        _ui.update { GameUiState(stage) }
        load()
    }

    private fun load() {
        viewModelScope.launch {
            try {
                val board = withContext(generationDispatcher) {
                    val layout = levels.catalog().layoutFor(stage)
                    BoardGenerator.generate(layout, stage)
                }
                initialBoard = board
                _ui.update { it.copy(loading = false, failed = false, game = GameRules.start(board)) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _ui.update { it.copy(loading = false, failed = true) }
            }
        }
    }

    fun onTileTap(id: Int) {
        _ui.update { state ->
            val game = state.game ?: return@update state
            if (state.paused || state.loading) return@update state
            val result = GameRules.tap(game, id)
            val shake = if (result.event == TapEvent.BLOCKED) {
                ShakeEvent(id, (state.shake?.nonce ?: 0) + 1)
            } else {
                state.shake
            }
            state.copy(game = result.state, shake = shake)
        }
    }

    fun undo() {
        _ui.update { state ->
            val game = state.game ?: return@update state
            if (state.paused || state.loading) return@update state
            state.copy(game = GameRules.undo(game))
        }
    }

    /** 처음부터 다시. 같은 판(같은 시드)에서 시간도 0부터 시작한다. */
    fun restart() {
        val board = initialBoard ?: return
        _ui.update {
            it.copy(game = GameRules.start(board), elapsedMillis = 0, paused = false, shake = null)
        }
    }

    fun pause() {
        _ui.update { if (it.loading) it else it.copy(paused = true) }
    }

    fun resume() {
        _ui.update { it.copy(paused = false) }
    }

    fun setForeground(foreground: Boolean) {
        _ui.update { it.copy(foreground = foreground) }
    }

    /** 시계가 흐를 때만 경과 시간을 쌓는다. 멈춰 있던 동안의 시간은 버린다. */
    private suspend fun runClock() {
        var last = clock.nowMillis()
        while (true) {
            delay(TICK_MILLIS)
            val now = clock.nowMillis()
            val delta = now - last
            last = now
            _ui.update { if (it.clockRunning) it.copy(elapsedMillis = it.elapsedMillis + delta) else it }
        }
    }

    private companion object {
        const val TICK_MILLIS = 250L
    }
}
