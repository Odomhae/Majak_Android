package com.odom.mahjongfun.ui.game

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.odom.mahjongfun.TestAssets
import com.odom.mahjongfun.core.engine.FreeTileRule
import com.odom.mahjongfun.core.engine.GameStatus
import com.odom.mahjongfun.core.engine.MoveFinder
import com.odom.mahjongfun.core.util.Clock
import com.odom.mahjongfun.data.level.LevelCatalog
import com.odom.mahjongfun.data.level.LevelSource
import com.odom.mahjongfun.ui.navigation.Routes
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelTest {
    private val scheduler = TestCoroutineScheduler()
    private val dispatcher = StandardTestDispatcher(scheduler)
    private val created = mutableListOf<GameViewModel>()

    private class TestClock(private val scheduler: TestCoroutineScheduler) : Clock {
        override fun nowMillis(): Long = scheduler.currentTime
    }

    private class FakeLevels(var failing: Boolean = false) : LevelSource {
        override suspend fun catalog(): LevelCatalog {
            check(!failing) { "boom" }
            return TestAssets.catalog
        }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** 시계 루프는 끝나지 않으므로, runTest가 남은 작업을 소진하다 멈추지 않게 본문 뒤에 직접 취소한다. */
    private fun vmTest(body: suspend TestScope.() -> Unit) = runTest(scheduler) {
        try {
            body()
        } finally {
            created.forEach { it.viewModelScope.cancel() }
        }
    }

    private fun create(levels: LevelSource = FakeLevels(), stage: Int = 1): GameViewModel {
        val vm = GameViewModel(
            SavedStateHandle(mapOf(Routes.ARG_STAGE to stage)),
            levels,
            TestClock(scheduler),
            dispatcher,
        )
        created += vm
        return vm
    }

    private fun GameViewModel.loaded(): GameViewModel {
        scheduler.runCurrent()
        assertFalse("still loading", ui.value.loading)
        return this
    }

    @Test
    fun loadsTheStageBoardAndStartsPlaying() = vmTest {
        val vm = create(stage = 3)
        assertTrue(vm.ui.value.loading)

        vm.loaded()

        val ui = vm.ui.value
        assertFalse(ui.failed)
        assertEquals(3, ui.stage)
        val game = ui.game
        assertNotNull(game)
        assertEquals(GameStatus.PLAYING, game!!.status)
        assertEquals(TestAssets.catalog.layoutFor(3).positions.size, ui.remainingTiles)
    }

    @Test
    fun sameStageGivesTheSameBoard() = vmTest {
        val a = create(stage = 5).loaded().ui.value.game!!.board.tiles
        val b = create(stage = 5).loaded().ui.value.game!!.board.tiles
        assertEquals(a, b)
    }

    @Test
    fun failedLoadShowsErrorAndRetryRecovers() = vmTest {
        val levels = FakeLevels(failing = true)
        val vm = create(levels)
        scheduler.runCurrent()
        assertTrue(vm.ui.value.failed)
        assertFalse(vm.ui.value.loading)

        levels.failing = false
        vm.retry()
        assertTrue(vm.ui.value.loading)
        scheduler.runCurrent()
        assertFalse(vm.ui.value.failed)
        assertNotNull(vm.ui.value.game)
    }

    @Test
    fun matchingTwoFreeTilesRemovesThemAndUndoRestoresThem() = vmTest {
        val vm = create().loaded()
        val start = vm.ui.value.game!!.board
        val move = MoveFinder.findHint(start)!!

        vm.onTileTap(move.first)
        assertEquals(move.first, vm.ui.value.game!!.selected)
        vm.onTileTap(move.second)

        val after = vm.ui.value.game!!
        assertEquals(start.remainingCount - 2, after.board.remainingCount)
        assertTrue(after.canUndo)

        vm.undo()
        assertEquals(start.remainingCount, vm.ui.value.game!!.board.remainingCount)
    }

    @Test
    fun tappingABlockedTileShakesItEachTime() = vmTest {
        val vm = create().loaded()
        val board = vm.ui.value.game!!.board
        val blocked = board.activeTiles().first { !FreeTileRule.isFree(board, it) }

        assertNull(vm.ui.value.shake)
        vm.onTileTap(blocked.id)
        assertEquals(ShakeEvent(blocked.id, 1), vm.ui.value.shake)
        vm.onTileTap(blocked.id)
        assertEquals(ShakeEvent(blocked.id, 2), vm.ui.value.shake)
        assertNull(vm.ui.value.game!!.selected)
    }

    @Test
    fun clockCountsOnlyWhileRunning() = vmTest {
        val vm = create().loaded()
        assertEquals(0L, vm.ui.value.elapsedMillis)

        scheduler.advanceTimeBy(1_000)
        scheduler.runCurrent()
        assertEquals(1_000L, vm.ui.value.elapsedMillis)
    }

    @Test
    fun pausingAndBackgroundingStopTheClock() = vmTest {
        val vm = create().loaded()
        scheduler.advanceTimeBy(1_000)
        scheduler.runCurrent()
        assertEquals(1_000L, vm.ui.value.elapsedMillis)

        vm.pause()
        scheduler.advanceTimeBy(5_000)
        scheduler.runCurrent()
        assertEquals("paused time must not count", 1_000L, vm.ui.value.elapsedMillis)

        vm.resume()
        vm.setForeground(false)
        scheduler.advanceTimeBy(5_000)
        scheduler.runCurrent()
        assertEquals("background time must not count", 1_000L, vm.ui.value.elapsedMillis)

        vm.setForeground(true)
        scheduler.advanceTimeBy(1_000)
        scheduler.runCurrent()
        assertEquals(2_000L, vm.ui.value.elapsedMillis)
    }

    @Test
    fun tapsAreIgnoredWhilePaused() = vmTest {
        val vm = create().loaded()
        val move = MoveFinder.findHint(vm.ui.value.game!!.board)!!

        vm.pause()
        vm.onTileTap(move.first)
        assertNull(vm.ui.value.game!!.selected)

        vm.resume()
        vm.onTileTap(move.first)
        assertEquals(move.first, vm.ui.value.game!!.selected)
    }

    @Test
    fun restartResetsBoardAndTimeButKeepsTheSameStage() = vmTest {
        val vm = create().loaded()
        val initial = vm.ui.value.game!!.board
        val move = MoveFinder.findHint(initial)!!
        vm.onTileTap(move.first)
        vm.onTileTap(move.second)
        scheduler.advanceTimeBy(2_000)
        scheduler.runCurrent()
        vm.pause()

        vm.restart()

        val ui = vm.ui.value
        assertEquals(initial.remainingCount, ui.game!!.board.remainingCount)
        assertEquals(initial.tiles, ui.game!!.board.tiles)
        assertFalse(ui.game!!.canUndo)
        assertEquals(0L, ui.elapsedMillis)
        assertFalse(ui.paused)
    }
}
