package app.cs2lineups.core

/** Console commands for practising lineups on a local (cheats-enabled) CS2 server. */
object PracticeConfig {
    val commands: List<Pair<String, String>> = listOf(
        "sv_cheats 1" to "Allow practice commands",
        "sv_infinite_ammo 1" to "Unlimited grenades",
        "sv_grenade_trajectory_prac_pipreview 1" to "Show a landing-spot preview while aiming",
        "sv_grenade_trajectory_prac_trailtime 5" to "Keep the grenade's path on screen for 5 seconds",
        "mp_roundtime_defuse 60" to "Long rounds so there is no rush",
        "mp_freezetime 0" to "Skip freeze time",
        "mp_warmup_end" to "End warmup",
    )

    /** All commands as one pasteable console block. */
    fun script(): String = commands.joinToString("\n") { it.first }
}
