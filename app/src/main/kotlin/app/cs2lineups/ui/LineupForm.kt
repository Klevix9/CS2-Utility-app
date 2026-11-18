package app.cs2lineups.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import app.cs2lineups.core.Aim
import app.cs2lineups.core.Click
import app.cs2lineups.core.Getpos
import app.cs2lineups.core.Grenade
import app.cs2lineups.core.Lineup
import app.cs2lineups.core.Movement
import app.cs2lineups.core.Side
import app.cs2lineups.core.Stance

/** Form for adding or editing one of the player's own lineups. */
@Composable
fun LineupForm(
    initial: Lineup,
    importImage: (Uri) -> String?,
    loadImage: (String) -> ImageBitmap?,
    onSave: (Lineup) -> Unit,
    onCancel: () -> Unit,
) {
    var title by remember { mutableStateOf(initial.title) }
    var fromName by remember { mutableStateOf(initial.fromName) }
    var targetName by remember { mutableStateOf(initial.targetName) }
    var grenade by remember { mutableStateOf(initial.grenade) }
    var side by remember { mutableStateOf(initial.side) }
    var stance by remember { mutableStateOf(initial.stance) }
    var movement by remember { mutableStateOf(initial.movement) }
    var click by remember { mutableStateOf(initial.click) }
    var aim by remember { mutableStateOf(initial.aim.description) }
    var notes by remember { mutableStateOf(initial.notes.orEmpty()) }
    var getpos by remember { mutableStateOf("") }
    var image by remember { mutableStateOf(initial.image) }
    val preview = remember(image) { image?.let(loadImage) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) importImage(uri)?.let { image = it }
    }
    val parsed = Getpos.parse(getpos)

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            if (initial.title.isBlank()) "New lineup" else "Edit lineup",
            style = MaterialTheme.typography.headlineSmall,
        )
        OutlinedTextField(title, { title = it }, label = { Text("Name (e.g. Xbox smoke)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        ChoiceChips(Grenade.entries, grenade, { it.label }, { grenade = it }, dotColor = { it.color })
        ChoiceChips(listOf(Side.T, Side.CT, Side.BOTH), side, { it.label }, { side = it })
        OutlinedTextField(fromName, { fromName = it }, label = { Text("Throw from (e.g. T spawn)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(targetName, { targetName = it }, label = { Text("Lands at (e.g. Xbox)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Text("Stance", style = MaterialTheme.typography.labelMedium)
        ChoiceChips(Stance.entries, stance, { it.label }, { stance = it })
        Text("Movement", style = MaterialTheme.typography.labelMedium)
        ChoiceChips(Movement.entries, movement, { it.label }, { movement = it })
        Text("Throw", style = MaterialTheme.typography.labelMedium)
        ChoiceChips(Click.entries, click, { it.label.substringBefore(" (") }, { click = it })
        OutlinedTextField(
            aim, { aim = it },
            label = { Text("Where to aim") },
            placeholder = { Text("e.g. crosshair on the left tip of the antenna") },
            modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp),
        )
        OutlinedTextField(
            getpos, { getpos = it },
            label = { Text("Optional: paste getpos output") },
            supportingText = {
                Text(
                    when {
                        getpos.isBlank() -> "Type getpos in the CS2 console at the throw spot for an exact position and angle."
                        parsed != null -> "Using exact position and angle from getpos."
                        else -> "Couldn't read that. It should look like: setpos x y z;setang pitch yaw 0"
                    }
                )
            },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(notes, { notes = it }, label = { Text("Notes (optional)") }, modifier = Modifier.fillMaxWidth())
        preview?.let {
            Image(it, contentDescription = "Aim screenshot", contentScale = ContentScale.FillWidth, modifier = Modifier.fillMaxWidth())
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = {
                picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }) { Text(if (image == null) "Add aim screenshot" else "Change screenshot") }
            if (image != null) TextButton(onClick = { image = null }) { Text("Remove") }
        }
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                enabled = title.isNotBlank(),
                onClick = {
                    onSave(
                        initial.copy(
                            title = title.trim(),
                            grenade = grenade,
                            side = side,
                            from = parsed?.pos ?: initial.from,
                            fromZ = parsed?.z ?: initial.fromZ,
                            fromName = fromName.trim().ifEmpty { "Unnamed spot" },
                            targetName = targetName.trim().ifEmpty { "Unnamed target" },
                            stance = stance,
                            movement = movement,
                            click = click,
                            aim = Aim(
                                description = aim.trim().ifEmpty { "No aim description yet." },
                                pitch = parsed?.pitch ?: initial.aim.pitch,
                                yaw = parsed?.yaw ?: initial.aim.yaw,
                            ),
                            image = image,
                            notes = notes.trim().ifEmpty { null },
                            placeholder = false,
                        )
                    )
                },
            ) { Text("Save") }
            TextButton(onClick = onCancel) { Text("Cancel") }
        }
    }
}
