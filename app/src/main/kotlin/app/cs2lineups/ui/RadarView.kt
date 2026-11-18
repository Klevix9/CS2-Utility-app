package app.cs2lineups.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import app.cs2lineups.core.Lineup
import app.cs2lineups.core.MapData
import app.cs2lineups.core.RadarProjection
import app.cs2lineups.core.WorldPos

/**
 * The radar map. Shows every throw spot for the current filter as a faint dot,
 * the tapped position with its search radius, and the matching lineups as
 * lines from throw spot to landing spot.
 */
@Composable
fun RadarView(
    map: MapData,
    radarImage: ImageBitmap?,
    visibleLineups: List<Lineup>,
    matches: List<Lineup>,
    selected: Lineup?,
    tapped: WorldPos?,
    searchRadius: Double,
    /** Points being placed while adding a lineup: throw spot, then landing spot. */
    draft: List<WorldPos> = emptyList(),
    onTap: (WorldPos) -> Unit,
    modifier: Modifier = Modifier,
) {
    val proj = RadarProjection(map.radar)
    val currentOnTap by rememberUpdatedState(onTap)
    Box(
        modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(Color(0xFF1B1F23)),
    ) {
        Canvas(
            Modifier
                .fillMaxSize()
                .pointerInput(map.id) {
                    detectTapGestures { o ->
                        currentOnTap(proj.toWorld(o.x.toDouble() / size.width, o.y.toDouble() / size.height))
                    }
                },
        ) {
            if (radarImage != null) {
                drawImage(radarImage, dstSize = IntSize(size.width.toInt(), size.height.toInt()))
            } else {
                drawGrid()
            }

            fun at(p: WorldPos): Offset {
                val (u, v) = proj.toRadar(p)
                return Offset((u * size.width).toFloat(), (v * size.height).toFloat())
            }

            val matchIds = matches.map { it.id }.toSet()
            for (l in visibleLineups) {
                if (l.id !in matchIds) drawCircle(l.grenade.color.copy(alpha = 0.35f), 4.dp.toPx(), at(l.from))
            }

            tapped?.let {
                val r = (searchRadius / proj.radarDistanceToWorld(1.0) * size.width).toFloat()
                drawCircle(Color.White.copy(alpha = 0.08f), r, at(it))
                drawCircle(Color.White.copy(alpha = 0.5f), r, at(it), style = Stroke(1.dp.toPx()))
            }

            for (l in matches) {
                val strong = selected == null || selected.id == l.id
                val c = l.grenade.color.copy(alpha = if (strong) 1f else 0.4f)
                drawLine(
                    c, at(l.from), at(l.target), strokeWidth = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f)),
                )
                drawCircle(c, 5.dp.toPx(), at(l.from))
                drawCircle(c, 9.dp.toPx(), at(l.target), style = Stroke(2.dp.toPx()))
            }

            if (draft.size == 2) {
                drawLine(Color.Cyan, at(draft[0]), at(draft[1]), strokeWidth = 2.dp.toPx())
            }
            draft.forEachIndexed { i, p ->
                if (i == 0) drawCircle(Color.Cyan, 6.dp.toPx(), at(p))
                else drawCircle(Color.Cyan, 9.dp.toPx(), at(p), style = Stroke(2.dp.toPx()))
            }

            tapped?.let {
                drawCircle(Color.White, 6.dp.toPx(), at(it))
                drawCircle(Color.Black, 6.dp.toPx(), at(it), style = Stroke(1.5.dp.toPx()))
            }
        }
        if (radarImage == null) {
            Text(
                "No radar image yet: add data/${map.radar.image}",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.align(Alignment.BottomCenter).padding(8.dp),
            )
        }
    }
}

private fun DrawScope.drawGrid() {
    val lines = 8
    for (i in 1 until lines) {
        val f = i / lines.toFloat()
        drawLine(Color.White.copy(alpha = 0.06f), Offset(f * size.width, 0f), Offset(f * size.width, size.height))
        drawLine(Color.White.copy(alpha = 0.06f), Offset(0f, f * size.height), Offset(size.width, f * size.height))
    }
}
