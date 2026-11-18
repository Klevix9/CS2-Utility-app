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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.cs2lineups.LineupRepository
import app.cs2lineups.core.Aim
import app.cs2lineups.core.Grenade
import app.cs2lineups.core.Level
import app.cs2lineups.core.Lineup
import app.cs2lineups.core.LineupFilter
import app.cs2lineups.core.LineupSteps
import app.cs2lineups.core.Radar
import app.cs2lineups.core.Target
import app.cs2lineups.core.Targets
import app.cs2lineups.core.LineupMatch
import app.cs2lineups.core.Side
import app.cs2lineups.core.WorldPos

private enum class Mode { BROWSE, PICK_FROM, PICK_TARGET }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LineupApp(repo: LineupRepository, maps: List<Pair<String, String>>, onSwitchMap: (String) -> Unit) {
    val map = repo.map
    var grenades by remember { mutableStateOf(setOf(Grenade.SMOKE)) }
    var side by remember { mutableStateOf<Side?>(null) }
    var query by remember { mutableStateOf("") }
    var favouritesOnly by remember { mutableStateOf(false) }
    var level by remember { mutableStateOf(Level.UPPER) }
    var me by remember { mutableStateOf<WorldPos?>(null) }
    var targetId by remember { mutableStateOf<String?>(null) }
    var selected by remember { mutableStateOf<Lineup?>(null) }
    var mode by remember { mutableStateOf(Mode.BROWSE) }
    var draft by remember { mutableStateOf(emptyList<WorldPos>()) }
    var editing by remember { mutableStateOf<Lineup?>(null) }
    var menuOpen by remember { mutableStateOf(false) }
    var mapMenuOpen by remember { mutableStateOf(false) }

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
            .onSuccess { toast(if (it == 0) "No ${map.displayName} lineups in that file" else "Imported $it lineup${if (it == 1) "" else "s"}") }
            .onFailure { toast("That file isn't a lineup export.") }
    }

    // Grenade type for a new lineup: the only selected one, else smoke.
    val grenade = grenades.singleOrNull() ?: Grenade.SMOKE
    val filter = LineupFilter(query = query, grenades = grenades, side = side, favouritesOnly = favouritesOnly)
    val visible = filter.apply(repo.allLineups, repo.favourites)
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
                    Box {
                        Text(
                            map.displayName + " ▾", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable { mapMenuOpen = true },
                        )
                        DropdownMenu(expanded = mapMenuOpen, onDismissRequest = { mapMenuOpen = false }) {
                            for ((id, name) in maps) DropdownMenuItem(
                                text = { Text(name) },
                                onClick = { mapMenuOpen = false; if (id != map.id) onSwitchMap(id) },
                            )
                        }
                    }
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
                            onClick = { repo.changeShowUnrecorded(!repo.showUnrecorded); targetId = null },
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Export my lineups…") },
                            enabled = repo.userLineups.isNotEmpty(),
                            onClick = { menuOpen = false; exporter.launch("${map.id.removePrefix("de_")}-lineups.json") },
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

            if (mode == Mode.BROWSE) {
                OutlinedTextField(
                    query, { query = it; targetId = null },
                    placeholder = { Text("Search lineups") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                )
                MultiChoiceChips(
                    Grenade.entries, grenades, { it.label },
                    { grenades = it; targetId = null },
                    modifier = Modifier.padding(horizontal = 12.dp),
                    dotColor = { it.color },
                )
                ChoiceChips(
                    listOf<Side?>(null, Side.T, Side.CT), side, { it?.label ?: "Both sides" },
                    { side = it; targetId = null },
                    modifier = Modifier.padding(horizontal = 12.dp),
                ) {
                    FilterChip(
                        selected = favouritesOnly,
                        onClick = { favouritesOnly = !favouritesOnly; targetId = null },
                        label = { Text("★ Favourites only") },
                    )
                }
            } else {
                ChoiceChips(
                    Grenade.entries, grenade, { it.label },
                    { grenades = setOf(it) },
                    modifier = Modifier.padding(horizontal = 12.dp),
                    dotColor = { it.color },
                )
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

            val hasLower = map.radar.hasLowerLevel
            val shownLevel = if (hasLower) level else Level.UPPER
            Box(Modifier.padding(top = 4.dp)) {
                RadarView(
                    map = map,
                    radarImage = if (shownLevel == Level.LOWER) repo.lowerRadarImage ?: repo.radarImage else repo.radarImage,
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
                    level = if (hasLower && mode == Mode.BROWSE) shownLevel else null,
                )
                if (hasLower) {
                    ChoiceChips(
                        listOf(Level.UPPER, Level.LOWER), shownLevel,
                        { if (it == Level.UPPER) "Upper" else "Lower" },
                        { level = it },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f), MaterialTheme.shapes.small)
                            .padding(horizontal = 6.dp),
                    )
                }
            }

            if (target != null) {
                ThrowOptions(target, options, me != null, onSelect = { selected = it }, onBack = { targetId = null }, modifier = Modifier.weight(1f))
            } else {
                TargetList(
                    g = grenades.singleOrNull()?.label?.lowercase() ?: "grenade",
                    filtered = !filter.copy(grenades = emptySet()).isDefault,
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
                radar = map.radar,
                favourite = lineup.id in repo.favourites,
                onToggleFavourite = { repo.toggleFavourite(lineup.id) },
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
                    if (grenades.isNotEmpty() && saved.grenade !in grenades) grenades = grenades + saved.grenade
                    targetId = Targets.group(filter.copy(grenades = grenades).apply(repo.allLineups, repo.favourites))
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
    g: String,
    filtered: Boolean,
    targets: List<Target>,
    me: WorldPos?,
    onSelect: (Target) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (targets.isEmpty()) {
        Hint(
            if (filtered) "No $g lineups match your search or filters."
            else "No $g lineups yet. Use + Add lineup to record one.",
            modifier,
        )
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
                    if (m.lineup.placeholder) Text("Unrecorded", style = MaterialTheme.typography.labelSmall, color = Palette.warn)
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
    radar: Radar,
    favourite: Boolean,
    onToggleFavourite: () -> Unit,
    mine: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onRecord: () -> Unit,
) {
    val image = remember(l.image) { l.image?.let(loadImage) }
    var confirmDelete by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    val setpos = LineupSteps.setposCommand(l)
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
                modifier = Modifier.padding(start = 8.dp).weight(1f),
            )
            TextButton(onClick = onToggleFavourite) {
                Text(if (favourite) "★" else "☆", fontSize = 22.sp, color = if (favourite) Palette.warn else MaterialTheme.colorScheme.onSurfaceVariant)
            }
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
        LineupSteps.of(l, radar).forEachIndexed { i, step ->
            val isSetpos = setpos != null && step.detail == setpos
            Step(i + 1, step.title, if (isSetpos) "Practice server: $setpos" else step.detail)
            if (isSetpos && setpos != null) {
                TextButton(
                    onClick = { clipboard.setText(AnnotatedString(setpos)) },
                    modifier = Modifier.padding(start = 32.dp),
                ) { Text("Copy command") }
            }
        }
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
