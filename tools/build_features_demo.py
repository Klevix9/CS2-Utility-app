"""Builds demo/features-demo.html: a browser mock-up of the new features using the real map data and radars.
Run from the repo root: python3 tools/build_features_demo.py"""
import base64, json, pathlib

root = pathlib.Path(__file__).resolve().parent.parent
b64 = lambda p: "data:image/png;base64," + base64.b64encode((root / "data" / p).read_bytes()).decode()
maps = {}
for mid in ("de_dust2", "de_nuke"):
    m = json.loads((root / "data/maps" / f"{mid}.json").read_text())
    m["radar"]["imageData"] = b64(m["radar"]["image"])
    if m["radar"].get("lowerImage"):
        m["radar"]["lowerImageData"] = b64(m["radar"]["lowerImage"])
    maps[mid] = m

# Demo-only samples so the Nuke level toggle and the step view have something to show.
# They are NOT in data/maps and their coordinates are illustrative, not verified lineups.
maps["de_nuke"]["lineups"] = [
    {"id": "demo-up", "title": "DEMO sample (upper level)", "grenade": "smoke", "side": "t",
     "from": {"x": -1200, "y": -700}, "fromZ": -300, "fromName": "Demo spot A", "target": {"x": -300, "y": -500},
     "targetName": "Demo target A", "stance": "stand", "movement": "jump", "click": "left",
     "aim": {"description": "Demo aim text", "pitch": -30.5, "yaw": 12.25}, "notes": "Demo only: not a real lineup.", "demo": True},
    {"id": "demo-low", "title": "DEMO sample (lower level)", "grenade": "flash", "side": "ct",
     "from": {"x": -400, "y": -800}, "fromZ": -700, "fromName": "Demo spot B", "target": {"x": 200, "y": -800},
     "targetName": "Demo target B", "stance": "crouch", "movement": "still", "click": "right",
     "aim": {"description": "Demo aim text"}, "demo": True},
]
html = (root / "tools/features_demo_template.html").read_text().replace("__MAPS__", json.dumps(maps))
(root / "demo/features-demo.html").write_text(html)
print("wrote demo/features-demo.html", len(html) // 1024, "KB")
