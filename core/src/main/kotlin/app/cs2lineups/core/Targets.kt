package app.cs2lineups.core

/** A landing spot, with every lineup that lands there for one grenade type. */
data class Target(
    val id: String,
    val name: String,
    val pos: WorldPos,
    val grenade: Grenade,
    val lineups: List<Lineup>,
)

object Targets {
    /** Lineups whose landing spots are this close (world units) count as the same spot. */
    const val MERGE_DISTANCE = 250.0

    /** How far you'd walk to a throw spot for a landing spot to count as reachable. */
    const val REACH = 900.0

    /** Groups lineups into landing spots, per grenade type. */
    fun group(lineups: List<Lineup>, mergeDistance: Double = MERGE_DISTANCE): List<Target> {
        val groups = mutableListOf<MutableList<Lineup>>()
        for (l in lineups) {
            val home = groups.firstOrNull { g ->
                g[0].grenade == l.grenade && centre(g).distanceTo(l.target) <= mergeDistance
            }
            if (home != null) home.add(l) else groups.add(mutableListOf(l))
        }
        return groups.map { g ->
            Target(
                id = "${g[0].grenade.name.lowercase()}:${g[0].id}",
                name = g.groupingBy { it.targetName }.eachCount().maxBy { it.value }.key,
                pos = centre(g),
                grenade = g[0].grenade,
                lineups = g.toList(),
            )
        }
    }

    /** True when at least one throw spot for [target] is within [reach] of [me]. */
    fun reachable(target: Target, me: WorldPos, reach: Double = REACH): Boolean =
        target.lineups.any { it.from.distanceTo(me) <= reach }

    /** Throw options for [target], closest to [me] first when known. */
    fun throwOptions(target: Target, me: WorldPos?): List<LineupMatch> =
        target.lineups
            .map { LineupMatch(it, me?.let(it.from::distanceTo) ?: 0.0) }
            .sortedBy { it.distance }

    private fun centre(g: List<Lineup>) =
        WorldPos(g.sumOf { it.target.x } / g.size, g.sumOf { it.target.y } / g.size)
}
