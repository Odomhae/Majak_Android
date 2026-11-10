package com.odom.mahjongfun.ui.game

import com.odom.mahjongfun.core.model.DragonColor
import com.odom.mahjongfun.core.model.TileFace
import com.odom.mahjongfun.core.model.TileSet
import com.odom.mahjongfun.core.model.WindDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TileDrawablesTest {
    @Test
    fun everyFaceOfTheFullSetHasAUniqueDrawable() {
        val faces = TileSet.fullSet().distinct()
        assertEquals(42, faces.size) // 34종 + 꽃 4 + 계절 4
        val resources = faces.map { it.drawableRes() }
        assertEquals("each face needs its own picture", faces.size, resources.toSet().size)
        assertTrue(resources.all { it != 0 })
        assertEquals(resources.toSet(), allTileDrawables.toSet())
    }

    @Test
    fun differentRanksAndKindsMapToDifferentPictures() {
        assertNotEquals(TileFace.Dot(1).drawableRes(), TileFace.Dot(2).drawableRes())
        assertNotEquals(TileFace.Dot(1).drawableRes(), TileFace.Bamboo(1).drawableRes())
        assertNotEquals(TileFace.Character(1).drawableRes(), TileFace.Dot(1).drawableRes())
        assertNotEquals(
            TileFace.Wind(WindDirection.EAST).drawableRes(),
            TileFace.Wind(WindDirection.WEST).drawableRes(),
        )
        assertNotEquals(
            TileFace.Dragon(DragonColor.RED).drawableRes(),
            TileFace.Dragon(DragonColor.GREEN).drawableRes(),
        )
        // 꽃끼리는 서로 짝이지만 그림은 각자 다르다.
        assertNotEquals(TileFace.Flower(1).drawableRes(), TileFace.Flower(2).drawableRes())
    }
}
