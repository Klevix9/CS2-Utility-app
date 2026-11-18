package app.cs2lineups.core

/** Parsed output of the CS2 `getpos` console command. */
data class GetposResult(val pos: WorldPos, val z: Double, val pitch: Double, val yaw: Double)

object Getpos {
    private val number = """-?\d+(?:\.\d+)?"""
    private val pattern = Regex(
        """setpos(?:_exact)?\s+($number)\s+($number)\s+($number)\s*;?\s*setang(?:_exact)?\s+($number)\s+($number)""",
        RegexOption.IGNORE_CASE,
    )

    /**
     * Parses text like `setpos -1950.03 1280.5 -32.0;setang -12.5 88.1 0.0`.
     * Returns null when the text doesn't contain a getpos line.
     */
    fun parse(text: String): GetposResult? {
        val m = pattern.find(text) ?: return null
        val (x, y, z, pitch, yaw) = m.destructured
        return GetposResult(WorldPos(x.toDouble(), y.toDouble()), z.toDouble(), pitch.toDouble(), yaw.toDouble())
    }
}
