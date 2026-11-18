package app.cs2lineups.core

import kotlinx.serialization.json.Json

object MapParser {
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(text: String): MapData {
        val map = json.decodeFromString(MapData.serializer(), text)
        validate(map)
        return map
    }

    fun encode(map: MapData): String =
        Json { prettyPrint = true; encodeDefaults = false }.encodeToString(MapData.serializer(), map)

    private fun validate(map: MapData) {
        val dupes = map.lineups.groupBy { it.id }.filterValues { it.size > 1 }.keys
        require(dupes.isEmpty()) { "Duplicate lineup ids in ${map.id}: $dupes" }
        val proj = RadarProjection(map.radar)
        val inside = { p: WorldPos -> proj.toRadar(p).let { (u, v) -> u in 0.0..1.0 && v in 0.0..1.0 } }
        map.layout?.let { layout ->
            require(layout.areas.all { a -> a.points.size >= 3 && a.points.all(inside) }) { "${map.id}: layout area outside the radar" }
            require(layout.callouts.all { inside(it.pos) }) { "${map.id}: callout outside the radar" }
        }
        for (l in map.lineups) {
            for ((label, p) in listOf("from" to l.from, "target" to l.target)) {
                val (u, v) = proj.toRadar(p)
                require(u in 0.0..1.0 && v in 0.0..1.0) {
                    "Lineup ${l.id}: $label $p is outside the ${map.id} radar"
                }
            }
        }
    }
}
