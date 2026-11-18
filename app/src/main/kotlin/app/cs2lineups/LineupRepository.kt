package app.cs2lineups

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import app.cs2lineups.core.Lineup
import app.cs2lineups.core.LineupExchange
import app.cs2lineups.core.RadarProjection
import app.cs2lineups.core.MapData
import app.cs2lineups.core.MapParser
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

/**
 * Bundled lineups (from /data, read-only) plus the player's own lineups and
 * radar image, saved in the app's private storage.
 */
class LineupRepository(private val context: Context, mapId: String) {
    val map: MapData = MapParser.parse(
        context.assets.open("maps/$mapId.json").bufferedReader().use { it.readText() }
    )

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    private val listSerializer = ListSerializer(Lineup.serializer())
    private val userFile = File(context.filesDir, "lineups/${map.id}.json")
    private val radarFile = File(context.filesDir, "radars/${map.id}.png")

    val userLineups = mutableStateListOf<Lineup>().apply {
        if (userFile.exists()) {
            runCatching { json.decodeFromString(listSerializer, userFile.readText()) }
                .onSuccess { addAll(it) }
        }
    }

    var radarImage by mutableStateOf(loadRadar())
        private set

    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    /** Whether to show bundled lineups that haven't been recorded in-game yet. */
    var showUnrecorded by mutableStateOf(prefs.getBoolean("showUnrecorded", true))
        private set

    fun setShowUnrecorded(show: Boolean) {
        showUnrecorded = show
        prefs.edit().putBoolean("showUnrecorded", show).apply()
    }

    val allLineups: List<Lineup>
        get() {
            val replaced = userLineups.mapNotNull { it.replaces }.toSet()
            return map.lineups.filter { it.id !in replaced && (showUnrecorded || !it.placeholder) } + userLineups
        }

    /** Writes the player's own lineups to a file they picked. Returns how many were written. */
    fun exportTo(uri: Uri): Int {
        val list = userLineups.map { it.copy(image = null) }
        context.contentResolver.openOutputStream(uri, "wt")!!.bufferedWriter().use {
            it.write(LineupExchange.encode(map.id, list))
        }
        return list.size
    }

    /** Adds lineups from a shared file. Returns how many were added or updated. */
    fun importFrom(uri: Uri): Int {
        val text = context.contentResolver.openInputStream(uri)!!.bufferedReader().use { it.readText() }
        val bundledIds = map.lineups.map { it.id }.toSet()
        val incoming = LineupExchange.decode(text)
            .filter { RadarProjection(map.radar).toRadar(it.from).let { (u, v) -> u in 0.0..1.0 && v in 0.0..1.0 } }
            .map { l ->
                val id = if (l.id in bundledIds || !l.id.startsWith("user-")) newId() else l.id
                l.copy(id = id, image = null, placeholder = false)
            }
        for (l in incoming) {
            val i = userLineups.indexOfFirst { it.id == l.id }
            if (i >= 0) userLineups[i] = l else userLineups.add(l)
        }
        persist()
        return incoming.size
    }

    fun isUserLineup(l: Lineup) = userLineups.any { it.id == l.id }

    fun newId() = "user-" + UUID.randomUUID().toString().take(8)

    fun save(lineup: Lineup) {
        val i = userLineups.indexOfFirst { it.id == lineup.id }
        if (i >= 0) userLineups[i] = lineup else userLineups.add(lineup)
        persist()
    }

    fun delete(lineup: Lineup) {
        userLineups.removeAll { it.id == lineup.id }
        lineup.image?.takeIf { it.startsWith(USER_PREFIX) }?.let { userImageFile(it).delete() }
        persist()
    }

    /** Copies a picked image into app storage and returns its image path for a lineup. */
    fun importScreenshot(uri: Uri): String? {
        val name = "$USER_PREFIX${UUID.randomUUID().toString().take(12)}.img"
        return if (copy(uri, userImageFile(name))) name else null
    }

    fun setRadar(uri: Uri) {
        if (copy(uri, radarFile)) radarImage = loadRadar()
    }

    fun clearRadar() {
        radarFile.delete()
        radarImage = loadRadar()
    }

    val hasCustomRadar: Boolean get() = radarFile.exists()

    /** Loads a lineup image: player screenshots from storage, others from bundled data. */
    fun loadImage(path: String): ImageBitmap? = runCatching {
        if (path.startsWith(USER_PREFIX)) decode(userImageFile(path))
        else context.assets.open(path).use { BitmapFactory.decodeStream(it)?.asImageBitmap() }
    }.getOrNull()

    /** Player-set radar first, then the bundled one, else null (grid fallback). */
    private fun loadRadar(): ImageBitmap? =
        (if (radarFile.exists()) decode(radarFile) else null) ?: loadImage(map.radar.image)

    private fun decode(f: File): ImageBitmap? = BitmapFactory.decodeFile(f.path)?.asImageBitmap()

    private fun userImageFile(path: String) = File(context.filesDir, "images/${path.removePrefix(USER_PREFIX)}")

    private fun copy(uri: Uri, dest: File): Boolean = runCatching {
        dest.parentFile?.mkdirs()
        context.contentResolver.openInputStream(uri)!!.use { input ->
            dest.outputStream().use { input.copyTo(it) }
        }
        true
    }.getOrDefault(false)

    private fun persist() {
        userFile.parentFile?.mkdirs()
        userFile.writeText(json.encodeToString(listSerializer, userLineups.toList()))
    }

    companion object {
        const val USER_PREFIX = "user:"
    }
}
