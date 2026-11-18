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

    val allLineups: List<Lineup> get() = map.lineups + userLineups

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
