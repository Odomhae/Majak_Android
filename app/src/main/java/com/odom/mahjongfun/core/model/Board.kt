package com.odom.mahjongfun.core.model

/**
 * 반 칸 단위 격자 좌표. 패 1장은 가로 2 x 세로 2칸을 차지한다.
 * z는 층이며 0이 바닥이다.
 */
data class TilePos(val x: Int, val y: Int, val z: Int)

data class Tile(val id: Int, val pos: TilePos, val face: TileFace)

/** 위치 -> 패 id 해시 인덱스. 판 구조는 변하지 않으므로 이동마다 새로 만들지 않고 공유한다. */
internal class PositionIndex(tiles: Collection<Tile>) {
    private val idByKey = HashMap<Long, Int>(tiles.size * 2)

    init {
        for (tile in tiles) {
            val (x, y, z) = tile.pos
            require(x in RANGE && y in RANGE && z in RANGE) { "position out of range: ${tile.pos}" }
            val previous = idByKey.put(key(x, y, z), tile.id)
            require(previous == null) { "tiles $previous and ${tile.id} share position ${tile.pos}" }
        }
    }

    fun idAt(x: Int, y: Int, z: Int): Int? = idByKey[key(x, y, z)]

    private companion object {
        const val OFFSET = 1 shl 19
        val RANGE = -OFFSET until OFFSET

        fun key(x: Int, y: Int, z: Int): Long =
            ((z + OFFSET).toLong() shl 40) or ((x + OFFSET).toLong() shl 20) or (y + OFFSET).toLong()
    }
}

/** 불변 판 상태. 이동마다 [remove]로 새 Board를 만든다. */
class Board private constructor(
    val tiles: Map<Int, Tile>,
    val removed: Set<Int>,
    private val index: PositionIndex,
) {
    constructor(tiles: Map<Int, Tile>, removed: Set<Int> = emptySet()) :
        this(tiles, removed, PositionIndex(tiles.values)) {
        validate()
    }

    constructor(tiles: Collection<Tile>) : this(tiles.associateBy { it.id }.also {
        require(it.size == tiles.size) { "duplicate tile id" }
    })

    private fun validate() {
        for ((id, tile) in tiles) require(id == tile.id) { "map key $id != tile id ${tile.id}" }
        require(tiles.keys.containsAll(removed)) { "removed contains unknown tile id" }
    }

    val remainingCount: Int get() = tiles.size - removed.size

    val isCleared: Boolean get() = remainingCount == 0

    fun isRemoved(id: Int): Boolean = id in removed

    /** 제거되지 않은 패 목록 (id 오름차순). */
    fun activeTiles(): List<Tile> =
        tiles.values.filter { it.id !in removed }.sortedBy { it.id }

    /** 해당 좌표에 남아 있는 패. 없거나 이미 제거됐으면 null. */
    fun tileAt(x: Int, y: Int, z: Int): Tile? {
        val id = index.idAt(x, y, z) ?: return null
        return if (id in removed) null else tiles.getValue(id)
    }

    /** 규칙 검사 없이 두 패를 제거한 새 판을 만든다. 규칙 검사는 [com.odom.mahjongfun.core.engine.MoveFinder.isValidMove]. */
    fun remove(a: Int, b: Int): Board {
        require(a != b) { "cannot remove the same tile twice: $a" }
        require(a in tiles && b in tiles) { "unknown tile id: $a or $b" }
        require(a !in removed && b !in removed) { "tile already removed: $a or $b" }
        return Board(tiles, removed + a + b, index)
    }
}
