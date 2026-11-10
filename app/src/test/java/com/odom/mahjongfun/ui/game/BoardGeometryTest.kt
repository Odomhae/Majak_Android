package com.odom.mahjongfun.ui.game

import com.odom.mahjongfun.core.tile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BoardGeometryTest {
    private val delta = 0.001f

    @Test
    fun tileHasThreeByFourAspectAndTwoCellWidth() {
        val g = BoardGeometry(listOf(tile(1, 0, 0)), cellWidth = 10f)
        val r = g.faceRect(tile(1, 0, 0))
        assertEquals(20f, r.width, delta)
        assertEquals(26.667f, r.height, 0.01f)
        assertEquals(0.75f, r.width / r.height, 0.001f)
    }

    @Test
    fun contentIsNormalizedToOriginAndIncludesSideDepth() {
        val tiles = listOf(tile(1, 4, 6), tile(2, 6, 6))
        val g = BoardGeometry(tiles, cellWidth = 10f)
        val a = g.faceRect(tiles[0])
        val b = g.faceRect(tiles[1])
        assertEquals(0f, a.left, delta)
        assertEquals(0f, a.top, delta)
        assertEquals(20f, b.left - a.left, delta) // 한 칸 옆 패 = 2 반 칸
        assertEquals(40f + g.sideDepth, g.contentWidth, delta)
        assertEquals(g.tileHeight + g.sideDepth, g.contentHeight, delta)
    }

    @Test
    fun upperLayersShiftUpAndRightByLayerOffset() {
        val bottom = tile(1, 0, 0, z = 0)
        val top = tile(2, 0, 0, z = 1)
        val g = BoardGeometry(listOf(bottom, top), cellWidth = 10f)
        val rb = g.faceRect(bottom)
        val rt = g.faceRect(top)
        assertEquals(g.layerOffset, rt.left - rb.left, delta)
        assertEquals(-g.layerOffset, rt.top - rb.top, delta)
        assertTrue(g.layerOffset > 0)
    }

    @Test
    fun drawOrderIsLayerThenRowThenRightToLeft() {
        val tiles = listOf(
            tile(1, 0, 0, z = 1), tile(2, 0, 2, z = 0), tile(3, 2, 0, z = 0), tile(4, 0, 0, z = 0),
        )
        val order = BoardGeometry(tiles, 10f).drawOrder.map { it.id }
        // z=0: y=0 에서 x 내림차순(3, 4), 그다음 y=2(2); z=1(1)이 맨 마지막
        assertEquals(listOf(3, 4, 2, 1), order)
    }

    @Test
    fun hitReturnsTheTopmostTileUnderThePoint() {
        val bottom = tile(1, 0, 0, z = 0)
        val top = tile(2, 0, 0, z = 1)
        val g = BoardGeometry(listOf(bottom, top), cellWidth = 10f)
        val inBoth = g.faceRect(top).let { it.left + it.width / 2 to it.top + it.height / 2 }
        assertEquals(2, g.hit(inBoth.first, inBoth.second) { true }?.id)
    }

    @Test
    fun hitSeesTheLowerTileWhereTheUpperOneDoesNotCover() {
        val bottom = tile(1, 0, 0, z = 0)
        val top = tile(2, 0, 0, z = 1)
        val g = BoardGeometry(listOf(bottom, top), cellWidth = 10f)
        val rb = g.faceRect(bottom)
        // 아래 패의 왼쪽 아래 모서리 근처: 위 패는 오른쪽 위로 밀려 있어 덮지 않는다.
        assertEquals(1, g.hit(rb.left + 1f, rb.bottom - 1f) { true }?.id)
    }

    @Test
    fun hitSkipsInactiveTilesAndMissesOutsideTiles() {
        val bottom = tile(1, 0, 0, z = 0)
        val top = tile(2, 0, 0, z = 1)
        val g = BoardGeometry(listOf(bottom, top), cellWidth = 10f)
        val r = g.faceRect(top)
        val cx = r.left + r.width / 2
        val cy = r.top + r.height / 2
        assertEquals(1, g.hit(cx, cy) { it.id != 2 }?.id)
        assertNull(g.hit(-5f, -5f) { true })
        assertNull(g.hit(cx, cy) { false })
    }

    @Test
    fun adjacentTilesDoNotShareHitArea() {
        val a = tile(1, 0, 0)
        val b = tile(2, 2, 0)
        val g = BoardGeometry(listOf(a, b), cellWidth = 10f)
        val ra = g.faceRect(a)
        assertEquals(1, g.hit(ra.right - 0.5f, ra.top + 5f) { true }?.id)
        assertEquals(2, g.hit(ra.right + 0.5f, ra.top + 5f) { true }?.id)
    }

    @Test
    fun fitCellWidthMakesTheBoardFitTheViewport() {
        val tiles = (0 until 6).map { tile(it, it * 2, 0) } + tile(10, 0, 2, z = 1)
        val fit = BoardGeometry.fitCellWidth(tiles, width = 300f, height = 400f)
        val g = BoardGeometry(tiles, fit)
        assertTrue(g.contentWidth <= 300f + delta)
        assertTrue(g.contentHeight <= 400f + delta)
        // 둘 중 한 방향은 꽉 찬다.
        assertTrue(g.contentWidth > 299f || g.contentHeight > 399f)
    }

    @Test
    fun fitIsLimitedByTheTighterDimension() {
        val wide = (0 until 10).map { tile(it, it * 2, 0) }
        val byWidth = BoardGeometry.fitCellWidth(wide, width = 200f, height = 1000f)
        val byHeight = BoardGeometry.fitCellWidth(wide, width = 1000f, height = 20f)
        // 한 줄 10장: 가로 20.15칸, 세로 2.8167칸(두께 포함)이 기준 크기
        assertEquals(200f / 20.15f, byWidth, 0.01f)
        assertEquals(20f / 2.8167f, byHeight, 0.01f)
        assertTrue(BoardGeometry(wide, byWidth).contentWidth <= 200f + delta)
        assertTrue(BoardGeometry(wide, byHeight).contentHeight <= 20f + delta)
    }
}
