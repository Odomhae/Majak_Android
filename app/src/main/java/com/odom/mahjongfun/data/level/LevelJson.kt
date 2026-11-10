package com.odom.mahjongfun.data.level

import com.odom.mahjongfun.core.model.Layout
import com.odom.mahjongfun.core.model.TilePos
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class PosDto(val x: Int, val y: Int, val z: Int)

@Serializable
private data class LayoutDto(
    val id: String,
    val name: String,
    val difficulty: Int,
    val positions: List<PosDto>,
)

@Serializable
private data class LevelsDto(val stages: Map<Int, String>)

/** assets의 layouts 폴더 JSON 파일들과 levels.json 파싱. Android 의존성이 없어 JVM 테스트에서 그대로 쓴다. */
object LevelJson {
    private val json = Json { ignoreUnknownKeys = true }

    /** 짝수 개수, 중복 위치, 같은 층 겹침은 [Layout] 생성 시 검증되어 위반하면 예외가 난다. */
    fun parseLayout(text: String): Layout {
        val dto = json.decodeFromString<LayoutDto>(text)
        return Layout(dto.id, dto.name, dto.difficulty, dto.positions.map { TilePos(it.x, it.y, it.z) })
    }

    fun parseStages(text: String): Map<Int, String> = json.decodeFromString<LevelsDto>(text).stages
}
