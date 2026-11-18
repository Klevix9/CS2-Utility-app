package app.cs2lineups.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.cs2lineups.LineupRepository
import app.cs2lineups.core.Aim
import app.cs2lineups.core.Grenade
import app.cs2lineups.core.Lineup
import app.cs2lineups.core.Target
import app.cs2lineups.core.Targets
import app.cs2lineups.core.LineupMatch
import app.cs2lineups.core.Side
import app.cs2lineups.core.WorldPos

private enum class Mode { BROWSE, PICK_FROM, PICK_TARGET }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LineupApp(repo: LineupRepository) {
    val map = repo.map
    var grenade by remember { mutableStateOf(Grenade.SMOKE) }
    var side by remember { mutableStateOf<Side?>(null) }
    var me by remember { mutableStateOf<WorldPos?>(null) }
    var targetId by remember { mutableStateOf<String?>(null) }
    var selected by remember { mutableStateOf<Lineup?>(null) }
    var mode by remember { mutableStateOf(Mode.BROWSE) }
    var draft by remember { mutableStateOf(emptyList<WorldPos>()) }
    var editing by remember { mutableStateOf<Lineup?>(null) }
    var menuOpen by remember { mutableStateOf(false) }

    val radarPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) repo.setRadar(uri)
    }

    val visible = repo.allLineups.filter {
        it.grenade == grenade && (side == null || it.side == Side.BOTH || it.side == side)
    }
    val targets = Targets.group(visible)
    val target = targets.firstOrNull { it.id == targetId }
    val options = target?.let { Targets.throwOptions(it, me) } ?: emptyList()
    val reachable: (Target) -> Boolean = { t -> me?.let { Targets.reachable(t, it) } ?: true }

    fun cancelAdd() { mode = Mode.BROWSE; draft = emptyList() }
    BackHandler(enabled = mode != Mode.BROWSE || target != null) {
        if (mode != Mode.BROWSE) cancelAdd() else targetId = null
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.safeDrawingPadding()) {
            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(map.displayName, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                if (mode == Mode.BROWSE) {
                    TextButton(onClick = { mode = Mode.PICK_FROM; draft = emptyList(); targetId = null }) {
                        Text("+ Add lineup")
                    }
                }
                Box {
                    TextButton(onClick = { menuOpen = true }) { Text("⋮") }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Set radar image…") },
                            onClick = {
                                menuOpen = false
                                radarPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                        )
                        if (repo.hasCustomRadar) {
                            DropdownMenuItem(
                                text = { Text("Remove radar image") },
                                onClick = { menuOpen = false; repo.clearRadar() },
                            )
                        }
                    }
                }
            }

            ChoiceChips(
                Grenade.entries, grenade, { it.label },
                { grenade = it; targetId = null },
                modifier = Modifier.padding(horizontal = 12.dp),
                dotColor = { it.color },
            )
            if (mode == Mode.BROWSE) {
                ChoiceChips(
                    listOf<Side?>(null, Side.T, Side.CT), side, { it?.label ?: "Both sides" },
                    { side = it; targetId = null },
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
            } else {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .background(Color(0xFF004D57), MaterialTheme.shapes.small)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        if (mode == Mode.PICK_FROM) "Step 1 of 2: tap where you throw from"
                        else "Step 2 of 2: tap where the grenade lands",
                        modifier = Modifier.weight(1f),
                        color = Color.White,
                    )
                    TextButton(onClick = ::cancelAdd) { Text("Cancel") }
                }
            }

            RadarView(
                map = map,
                radarImage = repo.radarImage,
                targets = if (mode == Mode.BROWSE) targets else emptyList(),
                reachable = reachable,
                selectedTarget = if (mode == Mode.BROWSE) target else null,
                options = if (mode == Mode.BROWSE) options.map { it.lineup } else emptyList(),
                me = if (mode == Mode.BROWSE) me else null,
                picking = mode != Mode.BROWSE,
                draft = draft,
                onMapTap = { p ->
                    when (mode) {
                        Mode.BROWSE -> { me = p; targetId = null }
                        Mode.PICK_FROM -> { draft = listOf(p); mode = Mode.PICK_TARGET }
                        Mode.PICK_TARGET -> {
                            editing = Lineup(
                                id = repo.newId(),
                                title = "",
                                grenade = grenade,
                                side = side ?: Side.T,
                                from = draft[0],
                                fromName = "",
                                target = p,
                                targetName = "",
                                aim = Aim(""),
                            )
                            draft = draft + p
                        }
                    }
                },
                onTargetTap = { targetId = it.id },
                onOptionTap = { selected = it },
                modifier = Modifier.padding(top = 4.dp),
            )

            if (target != null) {
                ThrowOptions(target, options, me != null, onSelect = { selected = it }, onBack = { targetId = null }, modifier = Modifier.weight(1f))
            } else {
                TargetList(
                    grenade = grenade,
                    targets = targets,
                    me = me,
                    onSelect = { targetId = it.id },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }

    selected?.let { lineup ->
        ModalBottomSheet(onDismissRequest = { selected = null }) {
            LineupDetail(
                l = lineup,
                loadImage = repo::loadImage,
                mine = repo.isUserLineup(lineup),
                onEdit = { selected = null; editing = lineup },
                onDelete = { repo.delete(lineup); selected = null },
            )
        }
    }

    editing?.let { lineup ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { editing = null; cancelAdd() },
            sheetState = sheetState,
        ) {
            LineupForm(
                initial = lineup,
                importImage = repo::importScreenshot,
                loadImage = repo::loadImage,
                onSave = { saved ->
                    repo.save(saved)
                    editing = null
                    cancelAdd()
                    grenade = saved.grenade
                    targetId = Targets.group(repo.allLineups.filter { it.grenade == saved.grenade })
                        .firstOrNull { t -> t.lineups.any { it.id == saved.id } }?.id
                    selected = saved
                },
                onCancel = { editing = null; cancelAdd() },
            )
        }
    }
}

/** Landing spots for the current grenade; reachable ones first when your position is set. */
@Composable
private fun TargetList(
    grenade: Grenade,
    targets: List<Target>,
    me: WorldPos?,
    onSelect: (Target) -> Unit,
    modifier: Modifier = Modifier,
) {
    val g = grenade.label.lowercase()
    if (targets.isEmpty()) {
        Hint("No $g lineups yet. Use + Add lineup to record one.", modifier)
        return
    }
    val rows = targets
        .map { t -> t to (me?.let { m -> t.lineups.minOf { it.from.distanceTo(m) } }) }
        .sortedBy { it.second ?: 0.0 }
    LazyColumn(modifier.fillMaxWidth()) {
        item {
            Text(
                if (me == null) "Tap a $g icon on the map for where you want it to land. Tap the map to mark where you are."
                else "Faded icons can't be reached from near you. Tap an icon to see where to throw from.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            )
        }
        items(rows, key = { it.first.id }) { (t, d) ->
            val reachable = me == null || Targets.reachable(t, me)
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(t) }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Dot(t.grenade.color.copy(alpha = if (reachable) 1f else 0.35f))
                Column(Modifier.padding(start = 12.dp).weight(1f)) {
                    Text(t.name, fontWeight = FontWeight.SemiBold, color = if (reachable) Color.Unspecified else MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "${t.lineups.size} throw spot${if (t.lineups.size == 1) "" else "s"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                d?.let {
                    Text("nearest ${it.toInt()} u", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            HorizontalDivider()
        }
    }
}

/** Where you can throw to reach one landing spot, numbered like the map markers. */
@Composable
private fun ThrowOptions(
    target: Target,
    options: List<LineupMatch>,
    hasMe: Boolean,
    onSelect: (Lineup) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier.fillMaxWidth()) {
        item {
            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("${target.grenade.label} ${target.name}", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Pick where to throw from",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = onBack) { Text("All spots") }
            }
        }
        itemsIndexed(options, key = { _, m -> m.lineup.id }) { i, m ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(m.lineup) }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(24.dp).background(Color.White, CircleShape),
                    contentAlignment = Alignment.Center,
                ) { Text("${i + 1}", color = Color.Black, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium) }
                Column(Modifier.padding(start = 12.dp).weight(1f)) {
                    Text(m.lineup.fromName, fontWeight = FontWeight.SemiBold)
                    Text(
                        "${m.lineup.stance.label} · ${m.lineup.movement.label} · ${m.lineup.click.label.substringBefore(" (")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    if (hasMe) Text("${m.distance.toInt()} u away", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (m.lineup.placeholder) Text("Placeholder", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFFB74D))
                    else if (m.lineup.id.startsWith("user-")) Text("Mine", style = MaterialTheme.typography.labelSmall, color = Color.Cyan)
                }
            }
            HorizontalDivider()
        }
    }
}

@Composable
private fun Hint(text: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.TopCenter) {
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun LineupDetail(
    l: Lineup,
    loadImage: (String) -> ImageBitmap?,
    mine: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val image = remember(l.image) { l.image?.let(loadImage) }
    var confirmDelete by remember { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp),
    ) {
        Text(l.title, style = MaterialTheme.typography.headlineSmall)
        Text("${l.grenade.label} · ${l.side.label} side", color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (l.placeholder) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Placeholder: position and aim still need recording on a practice server.",
                color = Color(0xFFFFB74D),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Spacer(Modifier.height(12.dp))
        image?.let {
            Image(it, contentDescription = "Aim point", contentScale = ContentScale.FillWidth, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
        }
        Field("Aim", l.aim.description)
        Field("How to throw", "${l.stance.label} · ${l.movement.label} · ${l.click.label}")
        Field("Stand at", l.fromName)
        Field("Lands at", l.targetName)
        l.notes?.let { Field("Notes", it) }
        if (l.fromZ != null && l.aim.pitch != null && l.aim.yaw != null) {
            Field("Practice server command", "setpos ${l.from.x} ${l.from.y} ${l.fromZ};setang ${l.aim.pitch} ${l.aim.yaw} 0")
        }
        if (mine) {
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onEdit) { Text("Edit") }
                if (confirmDelete) {
                    OutlinedButton(onClick = onDelete) { Text("Really delete?") }
                } else {
                    TextButton(onClick = { confirmDelete = true }) { Text("Delete") }
                }
                Spacer(Modifier.width(1.dp))
            }
        }
    }
}
