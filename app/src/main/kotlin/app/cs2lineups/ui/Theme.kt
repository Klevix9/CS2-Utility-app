package app.cs2lineups.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Map and brand colours: dusty sand on near-black, with an amber accent. */
object Palette {
    val ground = Color(0xFF0E0D0B)
    val floor = Color(0xFF3A3328)
    val site = Color(0xFF4D3C22)
    val siteEdge = Color(0xFFF0B54A)
    val spawn = Color(0xFF2F3833)
    val callout = Color(0xFFECE6DD)
    val draft = Color(0xFF6FD3C7)
    val warn = Color(0xFFFFB74D)
    val mine = Color(0xFF6FD3C7)
}

private val scheme = darkColorScheme(
    primary = Color(0xFFF0B54A),
    onPrimary = Color(0xFF2B1D00),
    primaryContainer = Color(0xFF4A3712),
    onPrimaryContainer = Color(0xFFFFDEA6),
    secondary = Color(0xFF6FD3C7),
    onSecondary = Color(0xFF00201C),
    secondaryContainer = Color(0xFF1F3A36),
    onSecondaryContainer = Color(0xFFB8EFE7),
    background = Color(0xFF11100E),
    onBackground = Color(0xFFECE6DD),
    surface = Color(0xFF11100E),
    onSurface = Color(0xFFECE6DD),
    surfaceVariant = Color(0xFF26231F),
    onSurfaceVariant = Color(0xFFA8A096),
    surfaceContainer = Color(0xFF1A1815),
    surfaceContainerLow = Color(0xFF161512),
    surfaceContainerHigh = Color(0xFF221F1B),
    surfaceContainerHighest = Color(0xFF2B2823),
    outline = Color(0xFF4A453E),
    outlineVariant = Color(0xFF332F2A),
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, content = content)
}
