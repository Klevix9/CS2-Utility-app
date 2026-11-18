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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
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
import androidx.compose.ui.draw.clip
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

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    fun toast(msg: String) { scope.launch { snackbar.showSnackbar(msg) } }

    val radarPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) repo.setRadar(uri)
    }
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) runCatching { repo.exportTo(uri) }
            .onSuccess { toast("Exported $it lineup${if (it == 1) "" else "s"}") }
            .onFailure { toast("Couldn't save the file. Try another folder.") }
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) runCatching { repo.importFrom(uri) }
            .onSuccess { toast(if (it == 0) "No Dust II lineups in that file" else "Imported $it lineup${if (it == 1) "" else "s"}") }
            .onFailure { toast("That file isn't a lineup export.") }
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
      Box(Modifier.fillMaxSize()) {
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).safeDrawingPadding())
        Column(Modifier.safeDrawingPadding()) {
            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("CS2 UTILITY", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, letterSpacing = 1.5.sp)
                    Text(map.displayName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                }
                if (mode == Mode.BROWSE) {
                    TextButton(onClick = { mode = Mode.PICK_FROM; draft = emptyList(); targetId = null }) {
                        Text("+ Add lineup")
                    }
                }
                Box {
                    TextButton(onClick = { menuOpen = true }) { Text("⋮") }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Show unrecorded lineups") },
                            trailingIcon = { Checkbox(checked = repo.showUnrecorded, onCheckedChange = null) },
                            onClick = { repo.setShowUnrecorded(!repo.showUnrecorded); targetId = null },
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Export my lineups…") },
                            enabled = repo.userLineups.isNotEmpty(),
                            onClick = { menuOpen = false; exporter.launch("dust2-lineups.json") },
                        )
                        DropdownMenuItem(
                            text = { Text("Import lineups…") },
                            onClick = { menuOpen = false; importer.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Use my own radar image…") },
                            onClick = {
                                menuOpen = false
                                radarPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                        )
                        if (repo.hasCustomRadar) {
                            DropdownMenuItem(
                                text = { Text("Use drawn map again") },
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
                        .background(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.shapes.small)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        if (mode == Mode.PICK_FROM) "Step 1 of 2: tap where you throw from"
                        else "Step 2 of 2: tap where the grenade lands",
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
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
    }

    selected?.let { lineup ->
        ModalBottomSheet(onDismissRequest = { selected = null }) {
            LineupDetail(
                l = lineup,
                loadImage = repo::loadImage,
                mine = repo.isUserLineup(lineup),
                onEdit = { selected = null; editing = lineup },
                onDelete = { repo.delete(lineup); selected = null },
                onRecord = {
                    selected = null
                    editing = lineup.copy(
                        id = repo.newId(), aim = Aim(""), placeholder = false, replaces = lineup.id,
                    )
                },
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
                    if (m.lineup.placeholder) Text("Placeholder", style = MaterialTheme.typography.labelSmall, color = Palette.warn)
                    else if (m.lineup.id.startsWith("user-")) Text("Mine", style = MaterialTheme.typography.labelSmall, color = Palette.mine)
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
    onRecord: () -> Unit,
) {
    val image = remember(l.image) { l.image?.let(loadImage) }
    var confirmDelete by remember { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Dot(l.grenade.color)
            Text(
                "${l.grenade.label.uppercase()} · ${l.side.label} SIDE",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        Text(l.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        Text(
            "From ${l.fromName} to ${l.targetName}",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (l.placeholder) {
            Text(
                "Not recorded yet. The spot is approximate and there's no aim point. On a practice server, stand here, aim, and tap Record this lineup.",
                color = Palette.warn,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Palette.warn.copy(alpha = 0.10f), MaterialTheme.shapes.small)
                    .padding(10.dp),
            )
        }
        image?.let {
            Image(
                it, contentDescription = "Aim point", contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium),
            )
        }
        Step(1, "Go to ${l.fromName}", l.fromZ?.let { z ->
            if (l.aim.pitch != null && l.aim.yaw != null) "Practice server: setpos ${l.from.x} ${l.from.y} $z;setang ${l.aim.pitch} ${l.aim.yaw} 0" else null
        })
        Step(2, l.stance.label, "Aim: ${l.aim.description}")
        Step(3, l.movement.label, l.click.label)
        l.notes?.let { Field("Notes", it) }
        if (l.placeholder) {
            Button(onClick = onRecord) { Text("Record this lineup") }
        }
        if (mine) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onEdit) { Text("Edit") }
                if (confirmDelete) {
                    OutlinedButton(onClick = onDelete) { Text("Really delete?") }
                } else {
                    TextButton(onClick = { confirmDelete = true }) { Text("Delete") }
                }
            }
        }
    }
}

/** One numbered step of the throw instructions. */
@Composable
private fun Step(n: Int, title: String, detail: String?) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            Modifier.size(28.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text("$n", color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
        }
        Column(Modifier.padding(start = 12.dp).weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            detail?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
