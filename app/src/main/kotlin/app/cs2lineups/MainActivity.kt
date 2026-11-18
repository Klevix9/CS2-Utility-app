package app.cs2lineups

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import app.cs2lineups.ui.AppTheme
import app.cs2lineups.ui.LineupApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repo = LineupRepository(applicationContext, "de_dust2")
        setContent {
            AppTheme {
                LineupApp(repo)
            }
        }
    }
}
