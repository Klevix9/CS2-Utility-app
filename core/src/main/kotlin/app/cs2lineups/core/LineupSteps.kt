package app.cs2lineups.core

/** One line of the throw instructions. */
data class ThrowStep(val title: String, val detail: String? = null)

object LineupSteps {
    /** Step-by-step throw instructions: position, aim, throw type, then notes. */
    fun of(l: Lineup, radar: Radar? = null): List<ThrowStep> {
        val steps = mutableListOf<ThrowStep>()
        val level = radar?.takeIf { it.hasLowerLevel }?.let { l.fromLevel(it) }
        val where = if (level == Level.LOWER) "${l.fromName} (lower level)" else l.fromName
        steps += ThrowStep("Go to $where", setposCommand(l))
        steps += ThrowStep(
            if (l.stance == Stance.CROUCH) "Crouch" else "Stand",
            "Aim: ${l.aim.description}" + (angles(l.aim)?.let { " ($it)" } ?: ""),
        )
        steps += ThrowStep(
            when (l.movement) {
                Movement.STILL -> "Stand still"
                Movement.JUMP -> "Jump throw"
                Movement.RUN -> "Run throw"
                Movement.WALK -> "Walk throw"
                Movement.RUN_JUMP -> "Run + jump throw"
            },
            when (l.click) {
                Click.LEFT -> "Left click (full throw)"
                Click.RIGHT -> "Right click (short lob)"
                Click.BOTH -> "Both buttons (medium)"
            },
        )
        l.notes?.takeIf { it.isNotBlank() }?.let { steps += ThrowStep("Tip", it) }
        return steps
    }

    /** Console command for a practice server, only when the exact spot and angles are recorded. */
    fun setposCommand(l: Lineup): String? {
        val z = l.fromZ ?: return null
        val pitch = l.aim.pitch ?: return null
        val yaw = l.aim.yaw ?: return null
        return "setpos ${fmt(l.from.x)} ${fmt(l.from.y)} ${fmt(z)};setang ${fmt(pitch)} ${fmt(yaw)} 0"
    }

    private fun angles(a: Aim): String? =
        if (a.pitch != null && a.yaw != null) "pitch ${fmt(a.pitch)}, yaw ${fmt(a.yaw)}" else null

    private fun fmt(v: Double): String = if (v == Math.rint(v)) v.toLong().toString() else "%.2f".format(java.util.Locale.ROOT, v)
}
