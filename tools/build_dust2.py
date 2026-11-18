"""Builds data/maps/de_dust2.json: an original schematic of Dust2 plus placeholder lineup spots.

Everything is authored in radar coordinates (u, v from 0 to 1, top-left origin,
north up) and converted to world units with the map's overview parameters.
Lineups recorded in-game (with getpos) should be added to the JSON directly
or merged with `--keep-recorded` (the default), which keeps non-placeholder
lineups already in the file.
"""
import json, os, sys

RADAR = {"image": "radars/de_dust2.png", "posX": -2476, "posY": 3239, "scale": 4.4, "sizePx": 1024}
SIZE = RADAR["scale"] * RADAR["sizePx"]

def w(u, v):
    return {"x": round(RADAR["posX"] + u * SIZE), "y": round(RADAR["posY"] - v * SIZE)}

def rect(u0, v0, u1, v1):
    return [(u0, v0), (u1, v0), (u1, v1), (u0, v1)]

AREAS = [
    ("spawn", rect(0.36, 0.80, 0.64, 0.96)),                     # T spawn
    ("floor", rect(0.42, 0.64, 0.52, 0.80)),                     # T ramp / top mid
    ("floor", rect(0.42, 0.29, 0.50, 0.66)),                     # mid
    ("floor", rect(0.58, 0.66, 0.74, 0.82)),                     # outside long
    ("floor", rect(0.64, 0.56, 0.74, 0.66)),                     # long doors
    ("floor", rect(0.74, 0.44, 0.94, 0.58)),                     # long / pit
    ("floor", rect(0.78, 0.14, 0.88, 0.44)),                     # long A
    ("site",  rect(0.60, 0.05, 0.89, 0.24)),                     # A site
    ("spawn", rect(0.46, 0.15, 0.60, 0.29)),                     # CT spawn
    ("floor", [(0.50, 0.24), (0.61, 0.24), (0.61, 0.44), (0.53, 0.44), (0.53, 0.30), (0.50, 0.30)]),  # short
    ("floor", rect(0.26, 0.20, 0.46, 0.30)),                     # CT mid to B
    ("site",  rect(0.05, 0.05, 0.31, 0.30)),                     # B site
    ("floor", rect(0.08, 0.30, 0.20, 0.56)),                     # upper tunnels
    ("floor", rect(0.20, 0.44, 0.42, 0.52)),                     # lower tunnels
    ("floor", [(0.08, 0.56), (0.22, 0.56), (0.22, 0.72), (0.37, 0.72), (0.37, 0.88), (0.08, 0.88)]),  # outside tunnels
]

# Named spots: callouts on the map and throw/landing positions for lineups.
SPOT = {
    "T spawn": (0.47, 0.89), "T spawn (right)": (0.58, 0.86), "Top mid": (0.46, 0.71),
    "Outside long": (0.66, 0.74), "Long doors": (0.69, 0.60), "Pit": (0.90, 0.51),
    "Long corner": (0.80, 0.51), "Long A": (0.83, 0.30), "Short bottom": (0.53, 0.42),
    "Catwalk": (0.57, 0.34), "Short stairs": (0.58, 0.26), "CT spawn": (0.53, 0.21),
    "A site": (0.74, 0.14), "Goose": (0.85, 0.09), "CT cross": (0.64, 0.21),
    "A default": (0.76, 0.16), "Mid doors": (0.46, 0.31), "CT mid": (0.36, 0.25),
    "Xbox": (0.47, 0.42), "Lower mid": (0.46, 0.56), "Upper tunnels": (0.14, 0.45),
    "Tunnel stairs": (0.15, 0.36), "Tunnel exit": (0.14, 0.33), "Lower tunnels": (0.30, 0.48),
    "Outside tunnels": (0.16, 0.72), "B site": (0.17, 0.17), "B platform": (0.27, 0.12),
    "B doors": (0.28, 0.25), "B window": (0.24, 0.07), "B car": (0.09, 0.10),
    "Back plat": (0.09, 0.21),
}
CALLOUTS = ["T spawn", "Top mid", "Mid doors", "Xbox", "Outside long", "Long doors", "Pit",
            "Long A", "A site", "Goose", "CT spawn", "Catwalk", "B site", "B doors",
            "Upper tunnels", "Lower tunnels", "Outside tunnels", "B car", "Lower mid"]

# grenade, side, throw from, lands at, movement, click
LINEUPS = [
    # T smokes
    ("smoke", "t", "T spawn", "Xbox", "jump", "left"),
    ("smoke", "t", "Top mid", "Xbox", "still", "left"),
    ("smoke", "t", "Outside long", "Xbox", "run_jump", "left"),
    ("smoke", "t", "Outside long", "CT cross", "jump", "left"),
    ("smoke", "t", "Long doors", "CT cross", "jump", "left"),
    ("smoke", "t", "Pit", "CT cross", "jump", "left"),
    ("smoke", "t", "T spawn", "Short stairs", "jump", "left"),
    ("smoke", "t", "Top mid", "Short stairs", "jump", "left"),
    ("smoke", "t", "Upper tunnels", "B doors", "still", "left"),
    ("smoke", "t", "Tunnel stairs", "B doors", "still", "left"),
    ("smoke", "t", "Outside tunnels", "B doors", "jump", "left"),
    ("smoke", "t", "Outside tunnels", "B window", "jump", "left"),
    ("smoke", "t", "Upper tunnels", "B window", "jump", "left"),
    ("smoke", "t", "Top mid", "Mid doors", "still", "left"),
    ("smoke", "t", "T spawn (right)", "Mid doors", "jump", "left"),
    # CT smokes
    ("smoke", "ct", "Long A", "Long doors", "jump", "left"),
    ("smoke", "ct", "A site", "Long doors", "jump", "left"),
    ("smoke", "ct", "Mid doors", "Top mid", "still", "left"),
    ("smoke", "ct", "CT spawn", "Top mid", "jump", "left"),
    ("smoke", "ct", "B site", "Tunnel exit", "still", "left"),
    ("smoke", "ct", "B platform", "Tunnel exit", "still", "both"),
    # T flashes
    ("flash", "t", "Long doors", "Long corner", "still", "right"),
    ("flash", "t", "Outside long", "Long corner", "jump", "left"),
    ("flash", "t", "Long A", "A site", "jump", "left"),
    ("flash", "t", "Catwalk", "A site", "still", "left"),
    ("flash", "t", "Upper tunnels", "B site", "still", "right"),
    ("flash", "t", "Tunnel stairs", "B site", "still", "both"),
    ("flash", "t", "Top mid", "Xbox", "still", "right"),
    # CT flashes
    ("flash", "ct", "B platform", "Tunnel stairs", "still", "left"),
    ("flash", "ct", "B site", "Tunnel stairs", "still", "right"),
    ("flash", "ct", "Long A", "Long doors", "still", "left"),
    ("flash", "ct", "CT spawn", "Catwalk", "still", "right"),
    # T molotovs
    ("molotov", "t", "Long A", "A default", "jump", "left"),
    ("molotov", "t", "Catwalk", "A default", "still", "left"),
    ("molotov", "t", "Long A", "Goose", "still", "left"),
    ("molotov", "t", "Upper tunnels", "B car", "jump", "left"),
    ("molotov", "t", "Tunnel stairs", "B site", "still", "left"),
    ("molotov", "t", "Upper tunnels", "Back plat", "jump", "left"),
    # CT molotovs
    ("molotov", "ct", "Long A", "Long doors", "jump", "left"),
    ("molotov", "ct", "A site", "Long doors", "jump", "left"),
    ("molotov", "ct", "B site", "Tunnel stairs", "still", "left"),
    ("molotov", "ct", "CT spawn", "Short bottom", "still", "left"),
    ("molotov", "ct", "Mid doors", "Top mid", "jump", "left"),
    # T HEs
    ("he", "t", "Upper tunnels", "B car", "jump", "left"),
    ("he", "t", "Long A", "A default", "still", "left"),
    ("he", "t", "Top mid", "Mid doors", "still", "left"),
    # CT HEs
    ("he", "ct", "Long A", "Long doors", "jump", "left"),
    ("he", "ct", "B platform", "Tunnel stairs", "still", "left"),
    ("he", "ct", "Mid doors", "Lower mid", "still", "left"),
]

NAMES = {"smoke": "smoke", "flash": "flash", "he": "HE", "molotov": "molotov"}

def slug(s):
    return "".join(c if c.isalnum() else "-" for c in s.lower()).strip("-").replace("--", "-")

def build(existing):
    lineups, seen = [], set()
    for g, side, frm, to, mv, click in LINEUPS:
        i = f"d2-{side}-{g}-{slug(to)}-from-{slug(frm)}"
        seen.add(i)
        lineups.append({
            "id": i,
            "title": f"{to} {NAMES[g]}",
            "grenade": g, "side": side,
            "from": w(*SPOT[frm]), "fromName": frm,
            "target": w(*SPOT[to]), "targetName": to,
            "stance": "stand", "movement": mv, "click": click,
            "aim": {"description": "Not recorded yet. Stand here on a practice server, find the aim point and save it with getpos."},
            "placeholder": True,
        })
    recorded = [l for l in existing if not l.get("placeholder")]
    # Drop placeholders that a recorded lineup replaces (or shares an id with).
    done = {l["id"] for l in recorded} | {l["replaces"] for l in recorded if l.get("replaces")}
    lineups = [l for l in lineups if l["id"] not in done] + recorded
    return {
        "id": "de_dust2",
        "displayName": "Dust II",
        "radar": RADAR,
        "layout": {
            "areas": [{"kind": k, "points": [w(u, v) for u, v in pts]} for k, pts in AREAS],
            "callouts": [{"name": n, "pos": w(*SPOT[n])} for n in CALLOUTS],
        },
        "lineups": lineups,
    }

if __name__ == "__main__":
    path = os.path.join(os.path.dirname(__file__), "..", "data", "maps", "de_dust2.json")
    existing = json.load(open(path))["lineups"] if os.path.exists(path) else []
    json.dump(build(existing), open(path, "w"), indent=2)
    print("wrote", path)
