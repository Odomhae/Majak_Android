package com.odom.mahjongfun.core.model

enum class WindDirection { EAST, SOUTH, WEST, NORTH }

enum class DragonColor { WHITE, GREEN, RED }

/** 패의 종류. 짝 규칙은 [matches] 참고. */
sealed interface TileFace {
    data class Character(val rank: Int) : TileFace {
        init { require(rank in 1..9) { "rank must be 1..9: $rank" } }
    }

    data class Dot(val rank: Int) : TileFace {
        init { require(rank in 1..9) { "rank must be 1..9: $rank" } }
    }

    data class Bamboo(val rank: Int) : TileFace {
        init { require(rank in 1..9) { "rank must be 1..9: $rank" } }
    }

    data class Wind(val direction: WindDirection) : TileFace

    data class Dragon(val color: DragonColor) : TileFace

    data class Flower(val number: Int) : TileFace {
        init { require(number in 1..4) { "number must be 1..4: $number" } }
    }

    data class Season(val number: Int) : TileFace {
        init { require(number in 1..4) { "number must be 1..4: $number" } }
    }
}

private object FlowerGroup
private object SeasonGroup

/** 서로 짝이 될 수 있는 패는 같은 그룹 키를 갖는다. 꽃 4종은 한 그룹, 계절 4종은 한 그룹. */
internal val TileFace.matchGroup: Any
    get() = when (this) {
        is TileFace.Flower -> FlowerGroup
        is TileFace.Season -> SeasonGroup
        else -> this
    }

/** 같은 face끼리 짝. 단, 꽃패 4종은 서로 모두 짝, 계절패 4종도 서로 모두 짝. */
fun TileFace.matches(other: TileFace): Boolean = matchGroup == other.matchGroup
