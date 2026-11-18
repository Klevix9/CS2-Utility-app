package app.cs2lineups.core

data class LineupMatch(val lineup: Lineup, val distance: Double)

object LineupFinder {
    /**
     * Lineups of [grenade] thrown from near [tapped], closest first.
     * [maxDistance] is in world units (a player is about 32 units wide).
     */
    fun near(
        map: MapData,
        tapped: WorldPos,
        grenade: Grenade? = null,
        side: Side? = null,
        maxDistance: Double = 500.0,
        limit: Int = 10,
    ): List<LineupMatch> =
        map.lineups.asSequence()
            .filter { grenade == null || it.grenade == grenade }
            .filter { side == null || it.side == Side.BOTH || it.side == side }
            .map { LineupMatch(it, it.from.distanceTo(tapped)) }
            .filter { it.distance <= maxDistance }
            .sortedBy { it.distance }
            .take(limit)
            .toList()
}
