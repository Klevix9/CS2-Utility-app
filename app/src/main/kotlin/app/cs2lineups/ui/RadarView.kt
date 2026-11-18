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
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.cs2lineups.core.Grenade
import app.cs2lineups.core.Lineup
import app.cs2lineups.core.MapData
import app.cs2lineups.core.RadarProjection
import app.cs2lineups.core.Target
import app.cs2lineups.core.WorldPos

/**
 * The radar map.
 *
 * Browsing: a grenade icon on every landing spot (faded when nothing near
 * [me] can reach it). With a [selectedTarget], its throw spots show as
 * numbered markers with dashed lines to where the grenade lands.
 * Adding a lineup ([picking] = true): every tap is a map position.
 */
@Composable
fun RadarView(
    map: MapData,
    radarImage: ImageBitmap?,
    targets: List<Target>,
    reachable: (Target) -> Boolean,
    selectedTarget: Target?,
    options: List<Lineup>,
    me: WorldPos?,
    picking: Boolean,
    draft: List<WorldPos>,
    onMapTap: (WorldPos) -> Unit,
    onTargetTap: (Target) -> Unit,
    onOptionTap: (Lineup) -> Unit,
    modifier: Modifier = Modifier,
) {
    val proj = RadarProjection(map.radar)
    val textMeasurer = rememberTextMeasurer()
    val state by rememberUpdatedState(
        TapState(targets, selectedTarget, options, picking, onMapTap, onTargetTap, onOptionTap)
    )

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
                        val s = state
                        fun px(p: WorldPos): Offset {
                            val (u, v) = proj.toRadar(p)
                            return Offset((u * size.width).toFloat(), (v * size.height).toFloat())
                        }
                        val world = proj.toWorld(o.x.toDouble() / size.width, o.y.toDouble() / size.height)
                        if (s.picking) {
                            s.onMapTap(world); return@detectTapGestures
                        }
                        val hit = 22.dp.toPx()
                        val option = s.options.minByOrNull { (px(it.from) - o).getDistance() }
                            ?.takeIf { (px(it.from) - o).getDistance() <= hit }
                        val target = s.targets.minByOrNull { (px(it.pos) - o).getDistance() }
                            ?.takeIf { (px(it.pos) - o).getDistance() <= hit }
                        when {
                            option != null -> s.onOptionTap(option)
                            target != null -> s.onTargetTap(target)
                            else -> s.onMapTap(world)
                        }
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

            me?.let {
                drawCircle(Color.White, 6.dp.toPx(), at(it))
                drawCircle(Color.Black, 6.dp.toPx(), at(it), style = Stroke(1.5.dp.toPx()))
            }

            for (t in targets) {
                if (t.id == selectedTarget?.id) continue
                val alpha = if (selectedTarget != null) 0.25f else if (reachable(t)) 1f else 0.3f
                drawGrenadeIcon(textMeasurer, t.grenade, at(t.pos), alpha)
            }

            selectedTarget?.let { t ->
                val c = t.grenade.color
                options.forEachIndexed { i, l ->
                    drawLine(
                        c.copy(alpha = 0.8f), at(l.from), at(t.pos), strokeWidth = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f)),
                    )
                    drawCircle(Color.White, 11.dp.toPx(), at(l.from))
                    drawCircle(c, 11.dp.toPx(), at(l.from), style = Stroke(2.dp.toPx()))
                    drawCentredText(textMeasurer, "${i + 1}", at(l.from), Color.Black)
                }
                drawCircle(Color.White, 17.dp.toPx(), at(t.pos), style = Stroke(2.dp.toPx()))
                drawGrenadeIcon(textMeasurer, t.grenade, at(t.pos), 1f)
            }

            if (draft.size == 2) {
                drawLine(Color.Cyan, at(draft[0]), at(draft[1]), strokeWidth = 2.dp.toPx())
            }
            draft.forEachIndexed { i, p ->
                if (i == 0) drawCircle(Color.Cyan, 6.dp.toPx(), at(p))
                else drawCircle(Color.Cyan, 9.dp.toPx(), at(p), style = Stroke(2.dp.toPx()))
            }
        }
        if (radarImage == null) {
            Text(
                "No radar image yet: ⋮ → Set radar image",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.align(Alignment.BottomCenter).padding(8.dp),
            )
        }
    }
}

private class TapState(
    val targets: List<Target>,
    val selectedTarget: Target?,
    val options: List<Lineup>,
    val picking: Boolean,
    val onMapTap: (WorldPos) -> Unit,
    val onTargetTap: (Target) -> Unit,
    val onOptionTap: (Lineup) -> Unit,
)

private fun DrawScope.drawGrenadeIcon(tm: TextMeasurer, g: Grenade, at: Offset, alpha: Float) {
    drawCircle(g.color.copy(alpha = alpha), 11.dp.toPx(), at)
    drawCircle(Color.Black.copy(alpha = 0.6f * alpha), 11.dp.toPx(), at, style = Stroke(1.dp.toPx()))
    drawCentredText(tm, g.letter, at, Color.Black.copy(alpha = alpha))
}

private fun DrawScope.drawCentredText(tm: TextMeasurer, text: String, at: Offset, color: Color) {
    val layout = tm.measure(text, TextStyle(color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold))
    drawText(layout, topLeft = at - Offset(layout.size.width / 2f, layout.size.height / 2f))
}

private fun DrawScope.drawGrid() {
    val lines = 8
    for (i in 1 until lines) {
        val f = i / lines.toFloat()
        drawLine(Color.White.copy(alpha = 0.06f), Offset(f * size.width, 0f), Offset(f * size.width, size.height))
        drawLine(Color.White.copy(alpha = 0.06f), Offset(0f, f * size.height), Offset(size.width, f * size.height))
    }
}
