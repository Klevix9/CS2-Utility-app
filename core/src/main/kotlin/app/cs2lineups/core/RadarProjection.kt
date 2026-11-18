package app.cs2lineups.core

/** Converts between world units and radar coordinates (0..1 normalized, top-left origin). */
class RadarProjection(private val radar: Radar) {
    private val sizeWorld = radar.scale * radar.sizePx

    /** World position to normalized radar position (0..1 on both axes). */
    fun toRadar(pos: WorldPos): Pair<Double, Double> =
        (pos.x - radar.posX) / sizeWorld to (radar.posY - pos.y) / sizeWorld

    /** Normalized radar position (0..1) to world position. */
    fun toWorld(u: Double, v: Double): WorldPos =
        WorldPos(radar.posX + u * sizeWorld, radar.posY - v * sizeWorld)

    /** Converts a distance on the radar (normalized) to world units. */
    fun radarDistanceToWorld(d: Double): Double = d * sizeWorld
}
