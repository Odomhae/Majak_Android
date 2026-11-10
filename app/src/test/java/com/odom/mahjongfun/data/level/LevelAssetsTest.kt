package com.odom.mahjongfun.data.level

import com.odom.mahjongfun.TestAssets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class LevelAssetsTest {
    private val layouts = TestAssets.layouts
    private val byId = layouts.associateBy { it.id }
    private val catalog = TestAssets.catalog

    @Test
    fun layoutCountIsBetween30And40() {
        assertTrue("layouts: ${layouts.size}", layouts.size in 30..40)
    }

    @Test
    fun everyLayoutFileLoadsWithEvenCountAndMatchingFileName() {
        // Layout 생성자가 짝수 개수, 중복 위치, 같은 층 겹침을 검증하므로 로딩이 되면 통과한 것이다.
        for ((file, layout) in TestAssets.layoutFiles.zip(layouts)) {
            assertEquals(file.nameWithoutExtension, layout.id)
            assertEquals("${layout.id} must have an even tile count", 0, layout.positions.size % 2)
            assertTrue("${layout.id} too large", layout.positions.size <= 144)
            assertTrue("${layout.id} difficulty", layout.difficulty in 1..5)
        }
        assertEquals(layouts.size, byId.size)
    }

    @Test
    fun everyStageUpTo200MapsToAnExistingLayout() {
        for (stage in 1..200) {
            val id = TestAssets.stages[stage]
            assertTrue("stage $stage -> $id", id != null && id in byId)
        }
    }

    @Test
    fun sameLayoutDoesNotRepeatWithinTenStages() {
        for (stage in 1..200) {
            val id = catalog.layoutFor(stage).id
            for (next in stage + 1..minOf(stage + 9, 200)) {
                assertNotEquals("stage $stage and $next share $id", id, catalog.layoutFor(next).id)
            }
        }
    }

    @Test
    fun layoutSizeAndLayerCountFollowTheDifficultyCurve() {
        // (단계 범위, 패 수 범위, 층 수 범위) -- CLAUDE.md §5.2
        val tiers = listOf(
            Triple(1..10, 24..40, 1..2),
            Triple(11..50, 40..80, 2..3),
            Triple(51..120, 80..120, 3..4),
            Triple(121..200, 120..144, 4..5),
        )
        for ((stages, tiles, layers) in tiers) {
            for (stage in stages) {
                val layout = catalog.layoutFor(stage)
                assertTrue("stage $stage ${layout.id}: ${layout.positions.size} tiles", layout.positions.size in tiles)
                val layerCount = layout.positions.maxOf { it.z } + 1
                assertTrue("stage $stage ${layout.id}: $layerCount layers", layerCount in layers)
            }
        }
    }

    @Test
    fun classicTurtleIsInTheLastTier() {
        val turtle = byId.getValue("turtle_classic")
        assertEquals(144, turtle.positions.size)
        assertTrue((121..200).any { catalog.layoutFor(it).id == turtle.id })
    }

    @Test
    fun stagesAfter200UseOnlyDifficulty4Or5AndAreStable() {
        for (stage in 201..1000) {
            val layout = catalog.layoutFor(stage)
            assertTrue("stage $stage ${layout.id}", layout.difficulty >= 4)
            assertEquals(layout.id, catalog.layoutFor(stage).id)
        }
        assertTrue("endless stages should use several layouts", (201..400).map { catalog.layoutFor(it).id }.toSet().size > 3)
    }

    @Test
    fun catalogRejectsMissingStageOrUnknownLayout() {
        val first = layouts.first().id
        assertThrows(IllegalArgumentException::class.java) {
            LevelCatalog(layouts, (1..199).associateWith { first })
        }
        assertThrows(IllegalArgumentException::class.java) {
            LevelCatalog(layouts, (1..200).associateWith { "nope" })
        }
    }

    @Test
    fun parsesLayoutJsonAndRejectsOddCount() {
        val ok = """{"id":"t","name":"t","difficulty":1,"positions":[{"x":0,"y":0,"z":0},{"x":2,"y":0,"z":0}]}"""
        assertEquals(2, LevelJson.parseLayout(ok).positions.size)
        val odd = """{"id":"t","name":"t","difficulty":1,"positions":[{"x":0,"y":0,"z":0}]}"""
        assertThrows(IllegalArgumentException::class.java) { LevelJson.parseLayout(odd) }
    }

    @Test
    fun parsesStagesJson() {
        assertEquals(mapOf(1 to "a", 2 to "b"), LevelJson.parseStages("""{"stages":{"1":"a","2":"b"}}"""))
    }
}
