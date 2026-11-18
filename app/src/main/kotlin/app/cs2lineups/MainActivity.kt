package app.cs2lineups

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.cs2lineups.ui.AppTheme
import app.cs2lineups.ui.LineupApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        setContent {
            var mapId by remember {
                mutableStateOf(prefs.getString("map", MAPS[0].first)?.takeIf { id -> MAPS.any { it.first == id } } ?: MAPS[0].first)
            }
            AppTheme {
                // key() gives each map fresh screen state (grenade, selection, position).
                key(mapId) {
                    val repo = remember { LineupRepository(applicationContext, mapId) }
                    LineupApp(repo, MAPS) { mapId = it; prefs.edit().putString("map", it).apply() }
                }
            }
        }
    }

    companion object {
        /** Premier + FACEIT pools (id to display name); each needs data/maps/<id>.json. */
        val MAPS = listOf(
            "de_dust2" to "Dust II", "de_mirage" to "Mirage", "de_inferno" to "Inferno",
            "de_nuke" to "Nuke", "de_ancient" to "Ancient", "de_anubis" to "Anubis", "de_cache" to "Cache",
        )
    }
}
