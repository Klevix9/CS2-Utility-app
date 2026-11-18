package app.cs2lineups.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A point in CS2 world units, as printed by the `getpos` console command (x, y). */
@Serializable
data class WorldPos(val x: Double, val y: Double) {
    fun distanceTo(other: WorldPos): Double {
        val dx = x - other.x
        val dy = y - other.y
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }
}

@Serializable
enum class Grenade {
    @SerialName("smoke") SMOKE,
    @SerialName("flash") FLASH,
    @SerialName("he") HE,
    @SerialName("molotov") MOLOTOV,
}

@Serializable
enum class Side {
    @SerialName("t") T,
    @SerialName("ct") CT,
    @SerialName("both") BOTH,
}

@Serializable
enum class Stance {
    @SerialName("stand") STAND,
    @SerialName("crouch") CROUCH,
}

@Serializable
enum class Movement {
    @SerialName("still") STILL,
    @SerialName("jump") JUMP,
    @SerialName("run") RUN,
    @SerialName("walk") WALK,
    @SerialName("run_jump") RUN_JUMP,
}

@Serializable
enum class Click {
    /** Full-strength throw (left click). */
    @SerialName("left") LEFT,
    /** Short underhand throw (right click). */
    @SerialName("right") RIGHT,
    /** Medium throw (both buttons). */
    @SerialName("both") BOTH,
}

/** Where to aim. [pitch]/[yaw] are the `setang` values from `getpos`, when known. */
@Serializable
data class Aim(
    val description: String,
    val pitch: Double? = null,
    val yaw: Double? = null,
)

@Serializable
data class Lineup(
    val id: String,
    val title: String,
    val grenade: Grenade,
    val side: Side = Side.BOTH,
    /** Where the player stands to throw. */
    val from: WorldPos,
    val fromName: String,
    /** Height of the throw spot (third `setpos` value), when recorded. */
    val fromZ: Double? = null,
    /** Where the grenade lands. */
    val target: WorldPos,
    val targetName: String,
    val stance: Stance = Stance.STAND,
    val movement: Movement = Movement.STILL,
    val click: Click = Click.LEFT,
    val aim: Aim,
    /** Optional screenshot of the aim point, relative to the data folder. */
    val image: String? = null,
    val notes: String? = null,
    /** True for seeded entries whose coordinates and aim still need recording in-game. */
    val placeholder: Boolean = false,
    /** Id of the unrecorded lineup this one was recorded to replace, if any. */
    val replaces: String? = null,
) {
    /** Level of the throw spot. Unknown height counts as upper. */
    fun fromLevel(radar: Radar): Level =
        if (radar.lowerBelowZ != null && fromZ != null && fromZ < radar.lowerBelowZ) Level.LOWER else Level.UPPER
}

/**
 * Radar overview parameters. Values come from the map's overview file
 * (game/csgo/resource/overviews/<map>.txt): pos_x, pos_y, scale.
 */
@Serializable
data class Radar(
    val image: String,
    val posX: Double,
    val posY: Double,
    val scale: Double,
    val sizePx: Int = 1024,
    /** Radar image for the lower level of multi-level maps (same posX/posY/scale), if any. */
    val lowerImage: String? = null,
    /** Positions with a height (z) below this are on the lower level. */
    val lowerBelowZ: Double? = null,
) {
    val hasLowerLevel: Boolean get() = lowerImage != null && lowerBelowZ != null
}

/** Which radar image a point belongs to on multi-level maps. */
enum class Level { UPPER, LOWER }

/** One walkable region of the drawn map, as a polygon in world units. */
@Serializable
data class Area(
    /** "floor", "site" or "spawn". */
    val kind: String = "floor",
    val points: List<WorldPos>,
)

/** A place name shown on the drawn map. */
@Serializable
data class Callout(val name: String, val pos: WorldPos)

/** An original, simplified drawing of the map, used when no radar image is set. */
@Serializable
data class Layout(
    val areas: List<Area> = emptyList(),
    val callouts: List<Callout> = emptyList(),
)

@Serializable
data class MapData(
    val id: String,
    val displayName: String,
    val radar: Radar,
    val layout: Layout? = null,
    val lineups: List<Lineup>,
)
