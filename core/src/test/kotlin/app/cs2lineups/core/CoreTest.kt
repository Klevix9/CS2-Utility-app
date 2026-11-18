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
    fun `every bundled map parses and the radar centre is inside it`() {
        val files = File(dataDir, "maps").listFiles { f -> f.extension == "json" }!!
        assertEquals(7, files.size)
        for (f in files) {
            val m = MapParser.parse(f.readText())
            assertEquals(f.nameWithoutExtension, m.id)
            val (u, v) = RadarProjection(m.radar).toRadar(RadarProjection(m.radar).toWorld(0.5, 0.5))
            assertTrue(abs(u - 0.5) < 1e-9 && abs(v - 0.5) < 1e-9)
        }
    }

    @Test
    fun `bundled radar images are square at sizePx`() {
        for (f in File(dataDir, "maps").listFiles { f -> f.extension == "json" }!!) {
            val r = MapParser.parse(f.readText()).radar
            val img = File(dataDir, r.image).takeIf { it.exists() } ?: continue
            val bi = javax.imageio.ImageIO.read(img)!!
            assertEquals(r.sizePx to r.sizePx, bi.width to bi.height, img.name)
        }
    }

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

class ExchangeTest {
    private val l = Lineup(
        id = "user-1", title = "t", grenade = Grenade.FLASH, from = WorldPos(1.0, 2.0), fromName = "a",
        target = WorldPos(3.0, 4.0), targetName = "b", aim = Aim("aim"),
    )

    @Test
    fun `round trips an export`() {
        assertEquals(listOf(l), LineupExchange.decode(LineupExchange.encode("de_dust2", listOf(l))))
    }

    @Test
    fun `reads a bare list and a map file`() {
        val dataDir = java.io.File(System.getProperty("dataDir"))
        val mapText = java.io.File(dataDir, "maps/de_dust2.json").readText()
        assertTrue(LineupExchange.decode(mapText).isNotEmpty())
        assertEquals(1, LineupExchange.decode("[" + LineupExchange.encode("x", listOf(l)).substringAfter("[").substringBeforeLast("]") + "]").size)
    }

    @Test
    fun `rejects junk`() {
        kotlin.test.assertFailsWith<IllegalArgumentException> { LineupExchange.decode("hello") }
    }
}

class FilterTest {
    private fun l(id: String, g: Grenade, side: Side, title: String, placeholder: Boolean = false, z: Double? = null) = Lineup(
        id = id, title = title, grenade = g, side = side, from = WorldPos(0.0, 0.0), fromName = "Spot $id", fromZ = z,
        target = WorldPos(1.0, 1.0), targetName = "B Site", aim = Aim("corner"), placeholder = placeholder,
    )
    private val all = listOf(
        l("a", Grenade.SMOKE, Side.T, "B doors smoke"),
        l("b", Grenade.FLASH, Side.CT, "Short flash"),
        l("c", Grenade.SMOKE, Side.BOTH, "Window smoke", placeholder = true),
    )

    @Test fun `default filter keeps everything`() {
        assertTrue(LineupFilter().isDefault)
        assertEquals(3, LineupFilter().apply(all).size)
    }

    @Test fun `filters by grenade and side, both-side lineups always match`() {
        assertEquals(listOf("a", "c"), LineupFilter(grenades = setOf(Grenade.SMOKE)).apply(all).map { it.id })
        assertEquals(listOf("b", "c"), LineupFilter(side = Side.CT).apply(all).map { it.id })
    }

    @Test fun `query needs every word and ignores case`() {
        assertEquals(listOf("a"), LineupFilter(query = "DOORS smoke").apply(all).map { it.id })
        assertEquals(3, LineupFilter(query = "b site").apply(all).size)
        assertTrue(LineupFilter(query = "nothing").apply(all).isEmpty())
    }

    @Test fun `favourites and unrecorded`() {
        assertEquals(listOf("b"), LineupFilter(favouritesOnly = true).apply(all, setOf("b")).map { it.id })
        assertEquals(listOf("a", "b"), LineupFilter(includeUnrecorded = false).apply(all).map { it.id })
    }

    @Test fun `favourites toggle, encode, decode and prune`() {
        val f = Favourites.toggle(emptySet(), "x")
        assertEquals(emptySet(), Favourites.toggle(f, "x"))
        assertEquals(setOf("a", "b"), Favourites.decode(Favourites.encode(setOf("b", "a"))))
        assertEquals(emptySet(), Favourites.decode(null))
        assertEquals(setOf("a"), Favourites.prune(setOf("a", "gone"), all))
    }

    @Test fun `steps include setpos only when fully recorded`() {
        val plain = l("a", Grenade.SMOKE, Side.T, "t")
        assertEquals(null, LineupSteps.setposCommand(plain))
        assertEquals(3, LineupSteps.of(plain).size)
        val rec = plain.copy(fromZ = -32.0, aim = Aim("corner", pitch = -12.5, yaw = 88.0), notes = "Jump early")
        assertEquals("setpos 0 0 -32;setang -12.50 88 0", LineupSteps.setposCommand(rec))
        assertEquals(4, LineupSteps.of(rec).size)
    }

    @Test fun `nuke has a lower level split by height`() {
        val dataDir = java.io.File(System.getProperty("dataDir"))
        val nuke = MapParser.parse(java.io.File(dataDir, "maps/de_nuke.json").readText())
        assertTrue(nuke.radar.hasLowerLevel)
        assertTrue(java.io.File(dataDir, nuke.radar.lowerImage!!).exists())
        assertEquals(Level.LOWER, l("a", Grenade.SMOKE, Side.T, "t", z = -600.0).fromLevel(nuke.radar))
        assertEquals(Level.UPPER, l("a", Grenade.SMOKE, Side.T, "t", z = -100.0).fromLevel(nuke.radar))
        assertEquals(Level.UPPER, l("a", Grenade.SMOKE, Side.T, "t").fromLevel(nuke.radar))
    }
}

class PracticeConfigTest {
    @Test fun `script lists every command once`() {
        val lines = PracticeConfig.script().lines()
        assertEquals(PracticeConfig.commands.size, lines.size)
        assertEquals(lines.size, lines.toSet().size)
        assertTrue(lines.first() == "sv_cheats 1")
    }
}
