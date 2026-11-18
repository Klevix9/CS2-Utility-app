# CS2 Utility

An Android app for CS2 grenade lineups. Pick a grenade, tap where you're standing on the radar, and see the lineups you can throw from there: where to aim, stance, movement and which click, plus an optional aim screenshot.

Maps: Dust II, Mirage, Inferno, Nuke, Ancient, Anubis and Cache (the CS2 Premier pool, which FACEIT's pool also contains). Switch maps by tapping the map name. Every map ships with Valve's CS2 radar image. Only Dust II has placeholder lineups so far; the others start empty, so record your own lineups. The lineup data and logic are kept apart from the Android UI so a desktop companion can reuse them later.

## Install

Tagged versions (`v1.0.0` …) are published on the Releases page with a signed APK; see [docs/RELEASING.md](docs/RELEASING.md). Every push also builds a debug APK. Open the latest run under **Actions → Build**, download the `cs2-utility-debug-apk` artifact, unzip it and install the `.apk` on your phone (allow installs from unknown sources).

To build yourself, open the folder in Android Studio and run the `app` configuration.

## Using it

1. Pick a grenade (Smoke, Flash, HE, Molotov) and optionally a side. Every spot that grenade can land on gets an icon.
2. Tap the map where you are. Landing spots you can't reach from near there fade out.
3. Tap the spot you want. Its throw positions appear as numbered markers and in a list, closest first.
4. Tap a throw position for step-by-step instructions: where to stand, where to aim, how to throw.

**The map** is Valve's CS2 radar image from `data/radars/<id>.png` (© Valve, sourced from the [MurkyYT/cs2-map-icons](https://github.com/MurkyYT/cs2-map-icons) mirror) when present; otherwise the app falls back to an original, simplified drawing (Dust II only). To use a different radar, pick a square, north-up image with **⋮ → Use my own radar image…**; it overrides both.

**Recording lineups:** bundled lineups start out as *unrecorded* placeholders: the spot is approximate and there's no aim point. On a practice server, stand at the spot, line up the throw, type `getpos` in the console, then open the lineup in the app and tap **Record this lineup**. Paste the `getpos` output, describe the aim point and attach a screenshot. You can also record a brand-new one with **+ Add lineup**. Hide the placeholders with **⋮ → Show unrecorded lineups**.

**Sharing:** **⋮ → Export my lineups…** saves your lineups to a file. Teammates load it with **Import lineups…**.

## Adding recorded lineups to the app

Exported files use the same lineup format as `data/maps/de_dust2.json`. Copy the recorded lineups into that file's `lineups` list (screenshots go in `data/images/`), then run:

```
python3 tools/build_dust2.py
```

It rebuilds the drawn map and placeholder list and drops every placeholder that a recorded lineup replaces.

## Project layout

| Path | What it is |
| --- | --- |
| `core/` | Plain Kotlin: lineup model, JSON parsing and validation, world ↔ radar conversion, nearest-lineup search, `getpos` parser. No Android code, so a desktop app can use it. |
| `app/` | Android app (Jetpack Compose). |
| `data/maps/<map>.json` | Lineups for one map, plus its radar parameters. |
| `data/radars/<id>.png` | Valve's CS2 radar images (© Valve, from the MurkyYT/cs2-map-icons mirror). |
| `data/images/` | Optional aim screenshots. |
| `tools/build_dust2.py` | Generates the drawn Dust II map and the placeholder lineup spots. |
| `docs/` | Release steps, privacy policy and store listing. |

## Lineup format

Positions are CS2 world units, exactly as `getpos` prints them (`setpos x y z;setang pitch yaw 0`).

```json
{
  "id": "d2-t-xbox-smoke-from-tspawn",
  "title": "Xbox smoke",
  "grenade": "smoke",            // smoke | flash | he | molotov
  "side": "t",                   // t | ct | both
  "from": { "x": -350, "y": -820 },
  "fromName": "T spawn",
  "fromZ": -126.0,               // optional
  "target": { "x": -380, "y": 1350 },
  "targetName": "Xbox (mid)",
  "stance": "stand",             // stand | crouch
  "movement": "jump",            // still | jump | run | walk | run_jump
  "click": "left",               // left | right | both
  "aim": { "description": "…", "pitch": -12.5, "yaw": 88.1 },
  "image": "images/xbox.jpg",    // optional, relative to data/
  "notes": "optional",
  "placeholder": false,
  "replaces": "d2-t-smoke-xbox-from-t-spawn"   // optional: the placeholder this records
}
```

### Adding a map

Create `data/maps/<map>.json` with the map's radar parameters (`posX`, `posY`, `scale` from the map's overview file in the game) and its lineups. The tests in `core/` check every lineup sits inside the radar and that ids are unique.

## Development

```
./gradlew :core:test                 # works without the Android SDK: add -PcoreOnly
./gradlew :app:assembleDebug         # needs the Android SDK
```
