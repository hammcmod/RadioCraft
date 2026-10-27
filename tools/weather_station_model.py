"""Builds the weather station block models and texture (1960s look: copper cups, copper vane, cream enamel shield,
hammertone transmitter box, galvanized mast).

    python tools/weather_station_model.py

Writes under assets/radiocraft:
- models/block/weather_station.json        the whole station, cups still (item and static use)
- models/block/weather_station_base.json   everything but the cups and the vane (what the block draws)
- models/block/weather_station_vane.json   the wind vane, arrow pointing north, pivoting at x 13, z 8
- models/block/weather_station_cups.json   the cup wheel, turning about x 8, z 8
                                           (WeatherStationRenderer turns both smoothly with the wind)
- textures/block/weather_station.png       32x32

The matching single instruments are in weather_instruments_model.py.
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from blockmodel import *  # noqa: E402,F403

ASSETS = Path(__file__).resolve().parent.parent / "src/main/resources/assets/radiocraft"
SIZE = 32

# Texture regions in UV units: [u0, v0, u1, v1].
R = {
    "mast":    [0, 0, 2, 16],    # galvanized steel, vertical
    "plate":   [2, 0, 8, 6],     # cream enamel shield plate tops
    "edge":    [2, 6, 8, 7],     # shield plate rims
    "arm":     [2, 7, 16, 8],    # galvanized, horizontal
    "core":    [8, 0, 10, 6],    # bakelite sensor housing
    "cup":     [10, 0, 12, 2],   # copper cup outside
    "cup_in":  [12, 0, 14, 2],   # copper cup mouth
    "hub":     [14, 0, 16, 2],   # hammertone hub
    "vane":    [10, 2, 16, 4],   # copper sheet vane
    "arrow":   [10, 4, 12, 6],   # brass arrowhead
    "box":     [2, 8, 8, 12],    # hammertone transmitter box
    "front":   [8, 8, 11, 11.5], # transmitter box front: nameplate and dial
    "clamp":   [8, 12, 12, 16],  # cast iron
    "rod":     [12, 12, 14, 16], # dark steel spacer rods
}


def paint():
    p = Painter(R, SIZE, seed=1962)
    p.fill("mast", GALV, 7, galvanized())
    p.fill("arm", GALV, 7, galvanized(vertical=False))
    p.fill("plate", CREAM, 5, rim(CREAM_DIRTY))
    p.fill("edge", (200, 191, 162), 4, lambda x, y, w, h: (160, 150, 124) if y == h - 1 else None)
    p.fill("core", BAKELITE, 4, lambda x, y, w, h: BAKELITE_DARK if y % 3 == 1 else None)
    p.fill("cup", COPPER, 8, lambda x, y, w, h: COPPER_LIGHT if x + y < 2 else COPPER_DARK if x + y > 5 else None)
    p.fill("cup_in", (70, 36, 20), 3, rim((190, 120, 72)))
    p.fill("hub", (92, 104, 90), 8)
    p.fill("vane", (160, 94, 56), 8, lambda x, y, w, h: VERDIGRIS if (x * 7 + y * 3) % 11 == 0 else None)
    p.fill("arrow", BRASS, 8, lambda x, y, w, h: BRASS_LIGHT if y == 0 else None)
    p.fill("box", HAMMERTONE, 10, hammertone)
    p.fill("front", HAMMERTONE, 8, rim((80, 92, 78)))
    # brass nameplate (rows 1-2) and a round dial window (rows 4-5)
    for x in range(1, 5):
        p.put("front", x, 1, (196, 162, 78))
        p.put("front", x, 2, (168, 136, 60))
    p.put("front", 2, 2, (60, 48, 30))
    p.put("front", 3, 2, (60, 48, 30))
    for x, y in ((2, 4), (3, 4), (2, 5), (3, 5)):
        p.put("front", x, y, (230, 222, 196))
    p.put("front", 3, 4, (30, 30, 30))  # needle
    p.put("front", 1, 4, (150, 150, 140))
    p.put("front", 4, 5, (150, 150, 140))  # chrome bezel glints
    p.fill("clamp", IRON, 6, lambda x, y, w, h: IRON_LIGHT if (x, y) in ((1, 1), (6, 6)) else None)
    p.fill("rod", ROD, 5, lambda x, y, w, h: (120, 122, 118) if x == 0 else None)
    return p


B = Builder(R, "radiocraft:block/weather_station", SIZE)
SIDES = {f: "edge" for f in ("north", "south", "east", "west")}


def base_elements():
    e = []
    # mast and clamps
    e.append(B.box("mast", [7, 0, 7], [9, 13, 9], "mast"))
    e.append(B.box("base_clamp", [6.5, 0, 6.5], [9.5, 1.5, 9.5], "clamp"))
    e.append(B.box("arm_clamp", [6.75, 6.75, 6.75], [9.25, 8.25, 9.25], "clamp"))
    e.append(B.box("crossarm", [1.5, 7, 7.5], [14.5, 8, 8.5], "arm"))
    # radiation shield on the west end: cap, plates, sensor, spacer rods
    e.append(B.box("shield_cap", [0.5, 6.25, 5], [6.5, 7, 11], "plate", SIDES))
    for i, y in enumerate((4.75, 3.25, 1.75)):
        e.append(B.box(f"shield_plate_{i}", [1, y, 5.5], [6, y + 0.75, 10.5], "plate", SIDES))
    e.append(B.box("sensor", [2.5, 1.25, 7], [4.5, 6.25, 9], "core"))
    for i, (x, z) in enumerate(((1.25, 5.75), (5.25, 5.75), (1.25, 9.75), (5.25, 9.75))):
        e.append(B.box(f"spacer_{i}", [x, 1.75, z], [x + 0.5, 6.25, z + 0.5], "rod"))
    # transmitter box on the mast, front facing east
    e.append(B.box("transmitter", [9, 2.5, 6.5], [12, 6, 9.5], "box", {"east": "front"}))
    e.append(B.box("conduit", [9.75, 6, 7.75], [10.25, 7, 8.25], "rod"))
    # the wind vane's pivot post on the east end (the vane itself turns: vane_elements)
    e.append(B.box("vane_pivot", [12.75, 8, 7.75], [13.25, 9.25, 8.25], "rod"))
    # hub (round, so it does not need to turn)
    e.append(B.box("hub", [7, 13, 7], [9, 14.75, 9], "hub"))
    e.append(B.box("hub_cap", [7.25, 14.75, 7.25], [8.75, 15.5, 8.75], "hub"))
    return e


def vane_elements():
    """The wind vane, arrow pointing north, about its pivot at x 13, z 8."""
    e = []
    e.append(B.box("vane_shaft", [12.75, 9.25, 3.5], [13.25, 9.75, 13], "vane"))
    e.append(B.box("vane_fin", [12.875, 8.5, 11], [13.125, 12, 14], "vane"))
    e.append(B.box("vane_head", [12.6, 9, 2.5], [13.4, 10, 4], "arrow"))
    e.append(B.box("vane_tip", [12.85, 9.25, 2], [13.15, 9.75, 2.5], "arrow"))
    return e


def cup_elements(angle):
    rot = {"angle": angle, "axis": "y", "origin": [8, 14, 8]}
    e = [B.box("cup_arm_x", [2.5, 13.75, 7.75], [13.5, 14.25, 8.25], "arm", rotation=rot),
         B.box("cup_arm_z", [7.75, 13.75, 2.5], [8.25, 14.25, 13.5], "arm", rotation=rot)]
    # cup centres at the arm ends; each mouth faces the same way round the wheel
    cups = (("east", (13.5, 8), "south"), ("north", (8, 2.5), "east"),
            ("west", (2.5, 8), "north"), ("south", (8, 13.5), "west"))
    for name, (cx, cz), mouth in cups:
        e.append(B.box(f"cup_{name}", [cx - 1.25, 12.75, cz - 1.25], [cx + 1.25, 15.25, cz + 1.25], "cup",
                       {mouth: "cup_in"}, rotation=rot))
    return e


if __name__ == "__main__":
    models = ASSETS / "models/block"
    save(models, "weather_station", B.model(base_elements() + vane_elements() + cup_elements(0.0)))
    save(models, "weather_station_base", B.model(base_elements()))
    save(models, "weather_station_vane", B.model(vane_elements()))
    save(models, "weather_station_cups", B.model(cup_elements(0.0)))
    paint().write(ASSETS / "textures/block/weather_station.png")
