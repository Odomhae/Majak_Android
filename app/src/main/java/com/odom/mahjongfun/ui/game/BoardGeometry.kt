package com.odom.mahjongfun.ui.game

import com.odom.mahjongfun.core.model.Tile
import kotlin.math.min

data class TileRect(val left: Float, val top: Float, val width: Float, val height: Float) {
    val right: Float get() = left + width
    val bottom: Float get() = top + height

    fun contains(x: Float, y: Float): Boolean = x >= left && x < right && y >= top && y < bottom
}

/**
 * 판 좌표(반 칸 단위)를 화면 픽셀로 바꾸는 계산. Android 의존성이 없어 JVM 테스트로 검증한다.
 *
 * - [cellWidth]는 반 칸의 가로 픽셀. 패 가로 = 2칸, 패 세로 = 2칸 x [ASPECT] (에셋이 3:4).
 * - 층마다 오른쪽 위로 [LAYER_OFFSET_RATIO] x cellWidth 만큼 밀어 입체감을 낸다. (§10.3의 (4dp, -4dp)는 패 가로 40dp일 때의 값)
 * - 모든 좌표는 콘텐츠 왼쪽 위가 (0, 0)이 되게 평행이동해 둔다. 패 두께(옆면, 그림자)도 콘텐츠 크기에 포함한다.
 */
class BoardGeometry(tiles: List<Tile>, val cellWidth: Float) {
    val cellHeight: Float = cellWidth * ASPECT
    val tileWidth: Float = 2 * cellWidth
    val tileHeight: Float = 2 * cellHeight
    val layerOffset: Float = cellWidth * LAYER_OFFSET_RATIO

    /** 옆면(두께)이 face 오른쪽 아래로 튀어나오는 길이. */
    val sideDepth: Float = cellWidth * SIDE_DEPTH_RATIO

    private val originX: Float
    private val originY: Float
    val contentWidth: Float
    val contentHeight: Float

    /** 그리는 순서: 층 오름차순, 같은 층에서는 y 오름차순, x 내림차순 (§10.3). */
    val drawOrder: List<Tile> = tiles.sortedWith(compareBy<Tile>({ it.pos.z }, { it.pos.y }, { -it.pos.x }))

    init {
        require(tiles.isNotEmpty()) { "no tiles" }
        originX = tiles.minOf { rawX(it) }
        originY = tiles.minOf { rawY(it) }
        contentWidth = tiles.maxOf { rawX(it) } - originX + tileWidth + sideDepth
        contentHeight = tiles.maxOf { rawY(it) } - originY + tileHeight + sideDepth
    }

    private fun rawX(tile: Tile) = tile.pos.x * cellWidth + tile.pos.z * layerOffset
    private fun rawY(tile: Tile) = tile.pos.y * cellHeight - tile.pos.z * layerOffset

    fun faceRect(tile: Tile): TileRect =
        TileRect(rawX(tile) - originX, rawY(tile) - originY, tileWidth, tileHeight)

    /** (x, y) 위치에서 가장 위에 보이는 패. [isActive]가 false인 패(이미 치운 패)는 건너뛴다. */
    fun hit(x: Float, y: Float, isActive: (Tile) -> Boolean): Tile? {
        for (i in drawOrder.indices.reversed()) {
            val tile = drawOrder[i]
            if (isActive(tile) && faceRect(tile).contains(x, y)) return tile
        }
        return null
    }

    companion object {
        /** 에셋(120 x 160)과 같은 3:4 패 비율에서 반 칸 세로/가로. */
        const val ASPECT = 4f / 3f
        const val LAYER_OFFSET_RATIO = 0.2f
        const val SIDE_DEPTH_RATIO = 0.15f

        /** 패 너비 최소값(dp). 이보다 작아야 맞는 판은 이 크기로 그리고 핀치 줌/드래그로 본다. (§10.3) */
        const val MIN_TILE_WIDTH_DP = 40f

        /** 판 전체가 [width] x [height]에 들어가는 가장 큰 [cellWidth]. */
        fun fitCellWidth(tiles: List<Tile>, width: Float, height: Float): Float {
            val unit = BoardGeometry(tiles, 1f)
            return min(width / unit.contentWidth, height / unit.contentHeight)
        }
    }
}
