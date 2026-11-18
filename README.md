# CS2 Utility

An Android app for CS2 grenade lineups. Pick a grenade, tap where you're standing on the radar, and see the lineups you can throw from there: where to aim, stance, movement and which click, plus an optional aim screenshot.

Dust2 only for now. The lineup data and logic are kept apart from the Android UI so a desktop companion can reuse them later.

## Install

Every push builds a debug APK. Open the latest run under **Actions → Build**, download the `cs2-utility-debug-apk` artifact, unzip it and install the `.apk` on your phone (allow installs from unknown sources).

To build yourself, open the folder in Android Studio and run the `app` configuration.

## Using it

1. Pick a grenade (Smoke, Flash, HE, Molotov) and optionally a side.
2. Tap your position on the map. Lineups thrown within about 500 units show as dashed lines to where they land, and in the list below.
3. Tap a lineup for the details.

**Radar image:** the app doesn't ship Valve's radar art. Use **⋮ → Set radar image…** and pick a square, north-up Dust2 radar image (for example a screenshot of the in-game overview, cropped to the map). Until then the map is a plain grid.

**Your own lineups:** tap **+ Add lineup**, tap the throw spot, tap the landing spot, then fill in the form. For an exact position, type `getpos` in the CS2 console while standing at the spot and aiming, then paste the output into the form. Your lineups are saved on the phone and can be edited or deleted.

## Bundled lineups

The bundled Dust2 lineups in `data/maps/de_dust2.json` are **placeholders** with approximate positions. Each one needs recording on a practice server before it's useful. Lineups must be recorded yourself, not copied from other lineup sites.

## Project layout

| Path | What it is |
| --- | --- |
| `core/` | Plain Kotlin: lineup model, JSON parsing and validation, world ↔ radar conversion, nearest-lineup search, `getpos` parser. No Android code, so a desktop app can use it. |
| `app/` | Android app (Jetpack Compose). |
| `data/maps/<map>.json` | Lineups for one map, plus its radar parameters. |
| `data/radars/`, `data/images/` | Optional bundled radar images and aim screenshots. |

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
  "placeholder": false
}
```

### Adding a map

Create `data/maps/<map>.json` with the map's radar parameters (`posX`, `posY`, `scale` from the map's overview file in the game) and its lineups. The tests in `core/` check every lineup sits inside the radar and that ids are unique.

## Development

```
./gradlew :core:test                 # works without the Android SDK: add -PcoreOnly
./gradlew :app:assembleDebug         # needs the Android SDK
```
