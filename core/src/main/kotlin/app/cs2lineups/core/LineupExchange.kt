package app.cs2lineups.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray

/** File format for sharing lineups between players (and for adding them to the bundled data). */
@Serializable
data class LineupFile(val map: String, val lineups: List<Lineup>)

object LineupExchange {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = false }

    fun encode(mapId: String, lineups: List<Lineup>): String =
        json.encodeToString(LineupFile.serializer(), LineupFile(mapId, lineups))

    /**
     * Reads lineups from an exported file, a full map file, or a bare list.
     * Throws [IllegalArgumentException] when the text holds none of those.
     */
    fun decode(text: String): List<Lineup> {
        val root = runCatching { json.parseToJsonElement(text) }
            .getOrElse { throw IllegalArgumentException("Not a lineup file") }
        val list = when (root) {
            is JsonArray -> root
            is JsonObject -> root["lineups"]?.jsonArray ?: throw IllegalArgumentException("No lineups in file")
            else -> throw IllegalArgumentException("Not a lineup file")
        }
        return json.decodeFromJsonElement(ListSerializer(Lineup.serializer()), list)
    }
}
