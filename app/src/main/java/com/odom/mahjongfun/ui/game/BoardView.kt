package com.odom.mahjongfun.ui.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.odom.mahjongfun.core.engine.FreeTileRule
import com.odom.mahjongfun.core.model.Board
import com.odom.mahjongfun.core.model.Tile
import com.odom.mahjongfun.ui.theme.TileDisabledOverlay
import com.odom.mahjongfun.ui.theme.TileFace
import com.odom.mahjongfun.ui.theme.TileSelectedBorder
import com.odom.mahjongfun.ui.theme.TileShadow
import com.odom.mahjongfun.ui.theme.TileSide
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.sin

private const val REMOVE_MILLIS = 200
private const val SHAKE_MILLIS = 280
private const val SELECT_MILLIS = 120
private const val MAX_USER_SCALE = 2.5f
private const val CORNER_RATIO = 0.12f
private const val SELECT_SCALE = 0.05f
private const val REMOVE_SHRINK = 0.3f
private const val SHAKE_CYCLES = 3
private const val FIT_PADDING_DP = 8

/**
 * 게임 보드. Compose Canvas에 층 -> y -> x 순으로 그려 겹침을 표현하고, 터치는 위층부터 판정한다. (§10.3)
 *
 * 패 너비가 [BoardGeometry.MIN_TILE_WIDTH_DP]보다 작아져야 판이 들어가면, 그 최소 크기로 그리고
 * 핀치 줌과 드래그로 움직여 볼 수 있게 한다.
 */
@Composable
fun BoardView(
    board: Board,
    selectedId: Int?,
    shake: ShakeEvent?,
    onTileTap: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val tiles = remember(board.tiles) { board.tiles.values.toList() }
    var viewport by remember { mutableStateOf(IntSize.Zero) }

    val padding = with(density) { FIT_PADDING_DP.dp.toPx() }
    val minCell = with(density) { (BoardGeometry.MIN_TILE_WIDTH_DP / 2).dp.toPx() }
    val fitCell = remember(tiles, viewport) {
        if (viewport.width == 0 || viewport.height == 0) {
            0f
        } else {
            BoardGeometry.fitCellWidth(tiles, viewport.width - 2 * padding, viewport.height - 2 * padding)
        }
    }
    val zoomEnabled = fitCell > 0f && fitCell < minCell
    val cell = max(fitCell, minCell)
    val geometry = remember(tiles, cell) { BoardGeometry(tiles, cell) }

    var userScale by remember(tiles) { mutableFloatStateOf(1f) }
    var pan by remember(tiles) { mutableStateOf(Offset.Zero) }

    fun clampedPan(candidate: Offset, scale: Float): Offset {
        val slackX = max(0f, (geometry.contentWidth * scale - viewport.width) / 2)
        val slackY = max(0f, (geometry.contentHeight * scale - viewport.height) / 2)
        return Offset(candidate.x.coerceIn(-slackX, slackX), candidate.y.coerceIn(-slackY, slackY))
    }

    /** 콘텐츠 좌표 -> 화면 좌표: screen = content * scale + translation. 판은 항상 가운데에 놓고 pan만큼 민다. */
    fun translation(scale: Float): Offset = Offset(
        (viewport.width - geometry.contentWidth * scale) / 2 + pan.x,
        (viewport.height - geometry.contentHeight * scale) / 2 + pan.y,
    )

    val freeIds = remember(board) { FreeTileRule.freeTiles(board).mapTo(HashSet()) { it.id } }
    val painters = HashMap<Int, Painter>(allTileDrawables.size * 2).also { map ->
        for (res in allTileDrawables) map[res] = painterResource(res)
    }

    // 제거 애니메이션: 새로 치워진 패를 200ms 동안 페이드+축소한 뒤 그리기를 멈춘다.
    val fading = remember { mutableStateMapOf<Int, Float>() }
    val scope = rememberCoroutineScope()
    val previousRemoved = remember { arrayOf(board.removed) }
    LaunchedEffect(board.removed) {
        val newlyRemoved = board.removed - previousRemoved[0]
        previousRemoved[0] = board.removed
        for (id in newlyRemoved) {
            scope.launch {
                animate(0f, 1f, animationSpec = tween(REMOVE_MILLIS, easing = LinearEasing)) { value, _ ->
                    fading[id] = value
                }
                fading.remove(id)
            }
        }
    }

    // 선택하면 살짝 들린다. 막힌 패를 누르면 좌우로 흔들린다.
    val lift by animateFloatAsState(if (selectedId != null) 1f else 0f, tween(SELECT_MILLIS), label = "lift")
    val shakeProgress = remember { Animatable(1f) }
    LaunchedEffect(shake) {
        if (shake != null) {
            shakeProgress.snapTo(0f)
            shakeProgress.animateTo(1f, tween(SHAKE_MILLIS, easing = LinearEasing))
        }
    }

    val currentBoard by rememberUpdatedState(board)
    val currentTap by rememberUpdatedState(onTileTap)

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .onSizeChanged { viewport = it }
            .pointerInput(geometry, zoomEnabled, viewport) {
                if (zoomEnabled) {
                    detectTransformGestures { _, panChange, zoom, _ ->
                        val scale = (userScale * zoom).coerceIn(1f, MAX_USER_SCALE)
                        userScale = scale
                        pan = clampedPan(pan + panChange, scale)
                    }
                }
            }
            .pointerInput(geometry, viewport) {
                detectTapGestures { tap ->
                    val scale = if (zoomEnabled) userScale else 1f
                    val t = translation(scale)
                    val x = (tap.x - t.x) / scale
                    val y = (tap.y - t.y) / scale
                    val hit = geometry.hit(x, y) { !currentBoard.isRemoved(it.id) }
                    if (hit != null) currentTap(hit.id)
                }
            },
    ) {
        val scale = if (zoomEnabled) userScale else 1f
        val t = translation(scale)
        withTransform({
            translate(t.x, t.y)
            scale(scale, scale, pivot = Offset.Zero)
        }) {
            for (tile in geometry.drawOrder) {
                val removed = board.isRemoved(tile.id)
                val removeProgress = if (removed) fading[tile.id] ?: continue else 0f
                drawTile(
                    tile = tile,
                    geometry = geometry,
                    painter = painters.getValue(tile.face.drawableRes()),
                    free = tile.id in freeIds,
                    selected = tile.id == selectedId,
                    lift = if (tile.id == selectedId) lift else 0f,
                    removeProgress = removeProgress,
                    shakeOffset = if (shake?.tileId == tile.id) shakeOffset(shakeProgress.value, geometry) else 0f,
                    borderWidth = 3.dp.toPx(),
                )
            }
        }
    }
}

private fun shakeOffset(progress: Float, geometry: BoardGeometry): Float =
    sin(progress * 2 * PI.toFloat() * SHAKE_CYCLES) * geometry.cellWidth * 0.25f * (1f - progress)

private fun DrawScope.drawTile(
    tile: Tile,
    geometry: BoardGeometry,
    painter: Painter,
    free: Boolean,
    selected: Boolean,
    lift: Float,
    removeProgress: Float,
    shakeOffset: Float,
    borderWidth: Float,
) {
    val rect = geometry.faceRect(tile)
    val alpha = 1f - removeProgress
    val depth = geometry.sideDepth
    val corner = CornerRadius(rect.width * CORNER_RATIO)
    val faceSize = Size(rect.width, rect.height)
    val center = Offset(rect.left + rect.width / 2, rect.top + rect.height / 2)
    val tileScale = 1f + SELECT_SCALE * lift - REMOVE_SHRINK * removeProgress

    withTransform({
        translate(shakeOffset, -lift * depth)
        scale(tileScale, tileScale, pivot = center)
    }) {
        // 그림자는 들릴수록 더 멀리, 더 진하게 퍼진다.
        val shadowShift = depth * (0.8f + 1.2f * lift)
        drawRoundRect(
            color = TileShadow.scaled(alpha * (1f + 0.4f * lift)),
            topLeft = Offset(rect.left + shadowShift, rect.top + shadowShift),
            size = faceSize,
            cornerRadius = corner,
        )
        // 옆면(두께)
        drawRoundRect(
            color = TileSide.scaled(alpha),
            topLeft = Offset(rect.left + depth, rect.top + depth),
            size = faceSize,
            cornerRadius = corner,
        )
        drawRoundRect(
            color = TileFace.scaled(alpha),
            topLeft = Offset(rect.left, rect.top),
            size = faceSize,
            cornerRadius = corner,
        )
        translate(rect.left, rect.top) {
            with(painter) { draw(size = faceSize, alpha = alpha) }
        }
        if (!free) {
            drawRoundRect(
                color = TileDisabledOverlay.scaled(alpha),
                topLeft = Offset(rect.left, rect.top),
                size = faceSize,
                cornerRadius = corner,
            )
        }
        if (selected) {
            // 색만으로 구분하지 않도록 테두리 + 들림을 함께 쓴다. (색약 대응)
            drawRoundRect(
                color = TileSelectedBorder,
                topLeft = Offset(rect.left, rect.top),
                size = faceSize,
                cornerRadius = corner,
                style = Stroke(width = borderWidth),
            )
        }
    }
}

private fun Color.scaled(factor: Float): Color = copy(alpha = (alpha * factor).coerceIn(0f, 1f))
