package app.cs2lineups.ui

import androidx.compose.ui.graphics.Color
import app.cs2lineups.core.Click
import app.cs2lineups.core.Grenade
import app.cs2lineups.core.Movement
import app.cs2lineups.core.Side
import app.cs2lineups.core.Stance

val Grenade.label: String
    get() = when (this) {
        Grenade.SMOKE -> "Smoke"
        Grenade.FLASH -> "Flash"
        Grenade.HE -> "HE"
        Grenade.MOLOTOV -> "Molotov"
    }

/** One letter for the map icon. */
val Grenade.letter: String
    get() = when (this) {
        Grenade.SMOKE -> "S"
        Grenade.FLASH -> "F"
        Grenade.HE -> "H"
        Grenade.MOLOTOV -> "M"
    }

val Grenade.color: Color
    get() = when (this) {
        Grenade.SMOKE -> Color(0xFFB0BEC5)
        Grenade.FLASH -> Color(0xFFFFEB3B)
        Grenade.HE -> Color(0xFFEF5350)
        Grenade.MOLOTOV -> Color(0xFFFF9800)
    }

val Side.label: String
    get() = when (this) {
        Side.T -> "T"
        Side.CT -> "CT"
        Side.BOTH -> "T / CT"
    }

val Stance.label: String
    get() = when (this) {
        Stance.STAND -> "Standing"
        Stance.CROUCH -> "Crouching"
    }

val Movement.label: String
    get() = when (this) {
        Movement.STILL -> "Stand still"
        Movement.JUMP -> "Jump throw"
        Movement.RUN -> "Run throw"
        Movement.WALK -> "Walk throw"
        Movement.RUN_JUMP -> "Run + jump throw"
    }

val Click.label: String
    get() = when (this) {
        Click.LEFT -> "Left click (full throw)"
        Click.RIGHT -> "Right click (short lob)"
        Click.BOTH -> "Both buttons (medium)"
    }
