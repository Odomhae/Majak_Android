package com.odom.mahjongfun.core.model

import com.odom.mahjongfun.core.model.TileFace.Bamboo
import com.odom.mahjongfun.core.model.TileFace.Character
import com.odom.mahjongfun.core.model.TileFace.Dot
import com.odom.mahjongfun.core.model.TileFace.Dragon
import com.odom.mahjongfun.core.model.TileFace.Flower
import com.odom.mahjongfun.core.model.TileFace.Season
import com.odom.mahjongfun.core.model.TileFace.Wind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TileFaceTest {
    @Test
    fun sameFaceMatches() {
        assertTrue(Dot(5).matches(Dot(5)))
        assertTrue(Wind(WindDirection.EAST).matches(Wind(WindDirection.EAST)))
        assertTrue(Dragon(DragonColor.RED).matches(Dragon(DragonColor.RED)))
    }

    @Test
    fun differentFacesDoNotMatch() {
        assertFalse(Dot(5).matches(Dot(6)))
        assertFalse(Dot(1).matches(Bamboo(1)))
        assertFalse(Character(1).matches(Dot(1)))
        assertFalse(Wind(WindDirection.EAST).matches(Wind(WindDirection.WEST)))
        assertFalse(Dragon(DragonColor.RED).matches(Dragon(DragonColor.GREEN)))
    }

    @Test
    fun allFlowersMatchEachOther() {
        for (a in 1..4) for (b in 1..4) assertTrue("flower $a vs $b", Flower(a).matches(Flower(b)))
    }

    @Test
    fun allSeasonsMatchEachOther() {
        for (a in 1..4) for (b in 1..4) assertTrue("season $a vs $b", Season(a).matches(Season(b)))
    }

    @Test
    fun flowerDoesNotMatchSeason() {
        assertFalse(Flower(1).matches(Season(1)))
        assertFalse(Season(2).matches(Flower(2)))
    }

    @Test
    fun matchesIsSymmetric() {
        val faces = TileSet.fullSet().distinct()
        for (a in faces) for (b in faces) assertEquals(a.matches(b), b.matches(a))
    }

    @Test
    fun outOfRangeRankIsRejected() {
        assertThrowsIllegalArgument { Dot(0) }
        assertThrowsIllegalArgument { Bamboo(10) }
        assertThrowsIllegalArgument { Flower(5) }
        assertThrowsIllegalArgument { Season(0) }
    }

    @Test
    fun fullSetHas144TilesAndCanBePairedCompletely() {
        val faces = TileSet.fullSet()
        assertEquals(TileSet.FULL_SIZE, faces.size)
        // 그룹마다 장수가 짝수여야 144장 전부 짝을 지어 없앨 수 있다.
        val groupSizes = faces.groupingBy { it.matchGroup }.eachCount()
        assertTrue(groupSizes.values.all { it % 2 == 0 })
        // 수패 27종 + 풍 4 + 삼원 3 = 34종 각 4장, 꽃 1그룹 4장, 계절 1그룹 4장
        assertEquals(36, groupSizes.size)
        assertTrue(groupSizes.values.all { it == 4 })
    }

    private fun assertThrowsIllegalArgument(block: () -> Unit) {
        try {
            block()
        } catch (_: IllegalArgumentException) {
            return
        }
        throw AssertionError("expected IllegalArgumentException")
    }
}
