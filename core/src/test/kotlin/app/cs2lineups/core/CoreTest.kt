package app.cs2lineups.core

import java.io.File
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CoreTest {
    private val dataDir = File(System.getProperty("dataDir"))
    private val dust2 = MapParser.parse(File(dataDir, "maps/de_dust2.json").readText())

    @Test
    fun `projection round-trips`() {
        val proj = RadarProjection(dust2.radar)
        val p = WorldPos(123.0, -456.0)
        val (u, v) = proj.toRadar(p)
        val back = proj.toWorld(u, v)
        assertTrue(abs(back.x - p.x) < 1e-6 && abs(back.y - p.y) < 1e-6)
    }

    @Test
    fun `radar corners match overview params`() {
        val proj = RadarProjection(dust2.radar)
        assertEquals(WorldPos(-2476.0, 3239.0), proj.toWorld(0.0, 0.0))
    }

    @Test
    fun `finder filters by grenade and sorts by distance`() {
        val tapped = WorldPos(-1950.0, 1280.0) // upper tunnels
        val smokes = LineupFinder.near(dust2, tapped, Grenade.SMOKE)
        assertTrue(smokes.isNotEmpty())
        assertTrue(smokes.all { it.lineup.grenade == Grenade.SMOKE })
        assertEquals(smokes.sortedBy { it.distance }, smokes)
    }

    @Test
    fun `finder respects max distance`() {
        val far = LineupFinder.near(dust2, WorldPos(2000.0, -1200.0), maxDistance = 100.0)
        assertTrue(far.isEmpty())
    }

    @Test
    fun `json round-trips`() {
        assertEquals(dust2, MapParser.parse(MapParser.encode(dust2)))
    }
}

class GetposTest {
    @Test
    fun `parses getpos output`() {
        val r = Getpos.parse("setpos -1950.031250 1280.500000 -32.000000;setang -12.540000 88.100006 0.000000")!!
        assertEquals(WorldPos(-1950.03125, 1280.5), r.pos)
        assertEquals(-12.54, r.pitch)
        assertEquals(88.100006, r.yaw)
    }

    @Test
    fun `rejects other text`() {
        assertEquals(null, Getpos.parse("hello"))
    }
}

class TargetsTest {
    private fun lineup(id: String, g: Grenade, from: WorldPos, to: WorldPos, name: String = id) = Lineup(
        id = id, title = id, grenade = g, from = from, fromName = "x",
        target = to, targetName = name, aim = Aim("a"),
    )

    @Test
    fun `groups nearby landing spots of the same grenade`() {
        val ls = listOf(
            lineup("a", Grenade.SMOKE, WorldPos(0.0, 0.0), WorldPos(1000.0, 1000.0), "B doors"),
            lineup("b", Grenade.SMOKE, WorldPos(500.0, 0.0), WorldPos(1100.0, 1050.0), "B doors"),
            lineup("c", Grenade.FLASH, WorldPos(0.0, 0.0), WorldPos(1000.0, 1000.0)),
            lineup("d", Grenade.SMOKE, WorldPos(0.0, 0.0), WorldPos(3000.0, 3000.0)),
        )
        val ts = Targets.group(ls)
        assertEquals(3, ts.size)
        val doors = ts.single { it.name == "B doors" }
        assertEquals(listOf("a", "b"), doors.lineups.map { it.id })
    }

    @Test
    fun `throw options are sorted by distance from me`() {
        val t = Targets.group(listOf(
            lineup("far", Grenade.SMOKE, WorldPos(2000.0, 0.0), WorldPos(0.0, 1000.0)),
            lineup("near", Grenade.SMOKE, WorldPos(100.0, 0.0), WorldPos(0.0, 1000.0)),
        )).single()
        assertEquals(listOf("near", "far"), Targets.throwOptions(t, WorldPos(0.0, 0.0)).map { it.lineup.id })
        assertTrue(Targets.reachable(t, WorldPos(0.0, 0.0)))
        assertTrue(!Targets.reachable(t, WorldPos(-5000.0, 0.0)))
    }
}
