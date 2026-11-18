package app.cs2lineups.core

/** Search and filter options for a list of lineups. */
data class LineupFilter(
    val query: String = "",
    /** Empty means every grenade type. */
    val grenades: Set<Grenade> = emptySet(),
    /** Lineups for [Side.BOTH] always match. Null means any side. */
    val side: Side? = null,
    val favouritesOnly: Boolean = false,
    val includeUnrecorded: Boolean = true,
) {
    val isDefault: Boolean get() = this == LineupFilter()

    fun matches(l: Lineup, favourites: Set<String> = emptySet()): Boolean =
        (grenades.isEmpty() || l.grenade in grenades) &&
            (side == null || l.side == Side.BOTH || l.side == side) &&
            (!favouritesOnly || l.id in favourites) &&
            (includeUnrecorded || !l.placeholder) &&
            matchesQuery(l)

    fun apply(lineups: List<Lineup>, favourites: Set<String> = emptySet()): List<Lineup> =
        lineups.filter { matches(it, favourites) }

    /** Every word of the query must appear in the title, spot names, aim text, notes or grenade name. */
    private fun matchesQuery(l: Lineup): Boolean {
        val words = query.lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (words.isEmpty()) return true
        val haystack = listOfNotNull(l.title, l.fromName, l.targetName, l.aim.description, l.notes, l.grenade.name)
            .joinToString(" ").lowercase()
        return words.all { it in haystack }
    }
}

/** The player's starred lineups, stored as a set of lineup ids. */
object Favourites {
    fun toggle(favourites: Set<String>, id: String): Set<String> =
        if (id in favourites) favourites - id else favourites + id

    /** Newline-separated, so it fits in one preference string. */
    fun encode(favourites: Set<String>): String = favourites.sorted().joinToString("\n")

    fun decode(text: String?): Set<String> =
        text.orEmpty().lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toSet()

    /** Drops ids that no longer exist (e.g. a deleted own lineup). */
    fun prune(favourites: Set<String>, lineups: List<Lineup>): Set<String> {
        val ids = lineups.mapTo(HashSet()) { it.id }
        return favourites.filterTo(LinkedHashSet()) { it in ids }
    }
}
