package com.odom.mahjongfun.ui.game

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.odom.mahjongfun.R
import com.odom.mahjongfun.core.engine.GameStatus
import com.odom.mahjongfun.ui.theme.OnBg
import kotlinx.coroutines.delay

/** 마지막 패가 사라지는 애니메이션이 보이도록 결과 화면으로 넘기기 전에 잠깐 기다린다. */
private const val CLEAR_DELAY_MILLIS = 500L

@Composable
fun GameScreen(
    onClear: () -> Unit,
    onBack: () -> Unit,
    viewModel: GameViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val game = ui.game

    // 앱이 백그라운드에 있는 동안은 시간을 멈춘다.
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.setForeground(false) }
    LifecycleEventEffect(Lifecycle.Event.ON_START) { viewModel.setForeground(true) }

    // 뒤로 가기는 바로 나가지 않고 일시정지 창을 먼저 연다.
    BackHandler(enabled = !ui.paused && !ui.loading && !ui.failed) { viewModel.pause() }

    val cleared = game?.status == GameStatus.CLEARED
    LaunchedEffect(cleared) {
        if (cleared) {
            delay(CLEAR_DELAY_MILLIS)
            onClear()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding(),
    ) {
        TopBar(
            stage = ui.stage,
            remaining = ui.remainingTiles,
            elapsedMillis = ui.elapsedMillis,
            onPause = viewModel::pause,
            pauseEnabled = !ui.loading && !ui.failed,
        )
        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            when {
                ui.failed -> LoadFailed(onRetry = viewModel::retry, onBack = onBack)
                ui.loading || game == null -> CircularProgressIndicator(color = OnBg)
                // 일시정지 중에는 판을 가린다. 시간이 멈춘 채 판을 훑어보는 것을 막는다.
                ui.paused -> Text(stringResource(R.string.game_paused_title), style = MaterialTheme.typography.titleLarge)
                else -> BoardView(
                    board = game.board,
                    selectedId = game.selected,
                    shake = ui.shake,
                    onTileTap = viewModel::onTileTap,
                )
            }
        }
        BottomBar(canUndo = game?.canUndo == true && !ui.paused, onUndo = viewModel::undo)
    }

    if (ui.paused) {
        PauseDialog(onResume = viewModel::resume, onRestart = viewModel::restart, onHome = onBack)
    }
    if (game?.status == GameStatus.STUCK && !ui.paused) {
        StuckDialog(onUndo = viewModel::undo, onRestart = viewModel::restart)
    }
}

@Composable
private fun TopBar(
    stage: Int,
    remaining: Int,
    elapsedMillis: Long,
    onPause: () -> Unit,
    pauseEnabled: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(stringResource(R.string.stage_format, stage), style = MaterialTheme.typography.titleLarge)
        Column(horizontalAlignment = Alignment.End) {
            Text(stringResource(R.string.game_remaining, remaining), style = MaterialTheme.typography.bodyLarge)
            Text(formatElapsed(elapsedMillis), style = MaterialTheme.typography.bodyLarge)
        }
        IconButton(onClick = onPause, enabled = pauseEnabled, modifier = Modifier.size(56.dp)) {
            Icon(
                painter = painterResource(R.drawable.ic_pause),
                contentDescription = stringResource(R.string.game_pause),
                tint = OnBg,
                modifier = Modifier.size(32.dp),
            )
        }
    }
}

@Composable
private fun BottomBar(canUndo: Boolean, onUndo: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        Button(
            onClick = onUndo,
            enabled = canUndo,
            modifier = Modifier.heightIn(min = 56.dp),
        ) {
            Icon(painterResource(R.drawable.ic_undo), contentDescription = null, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.game_undo), style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun LoadFailed(onRetry: () -> Unit, onBack: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.game_load_failed), style = MaterialTheme.typography.bodyLarge)
        Button(onClick = onRetry, modifier = Modifier.heightIn(min = 56.dp)) {
            Text(stringResource(R.string.game_retry), style = MaterialTheme.typography.labelLarge)
        }
        TextButton(onClick = onBack, modifier = Modifier.heightIn(min = 56.dp)) {
            Text(stringResource(R.string.back), style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun PauseDialog(onResume: () -> Unit, onRestart: () -> Unit, onHome: () -> Unit) {
    AlertDialog(
        onDismissRequest = onResume,
        title = { Text(stringResource(R.string.game_paused_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onResume, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                    Text(stringResource(R.string.game_resume), style = MaterialTheme.typography.labelLarge)
                }
                TextButton(onClick = onRestart, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                    Text(stringResource(R.string.game_restart), style = MaterialTheme.typography.labelLarge)
                }
                TextButton(onClick = onHome, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                    Text(stringResource(R.string.game_exit), style = MaterialTheme.typography.labelLarge)
                }
            }
        },
        confirmButton = {},
    )
}

/** 막힘 다이얼로그. 셔플 버튼은 마일스톤 5(셔플)·8(광고)에서 추가한다. */
@Composable
private fun StuckDialog(onUndo: () -> Unit, onRestart: () -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text(stringResource(R.string.game_stuck_title)) },
        text = { Text(stringResource(R.string.game_stuck_message), style = MaterialTheme.typography.bodyLarge) },
        confirmButton = {
            Button(onClick = onUndo, modifier = Modifier.heightIn(min = 56.dp)) {
                Text(stringResource(R.string.game_undo), style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = {
            TextButton(onClick = onRestart, modifier = Modifier.heightIn(min = 56.dp)) {
                Text(stringResource(R.string.game_restart), style = MaterialTheme.typography.labelLarge)
            }
        },
    )
}
