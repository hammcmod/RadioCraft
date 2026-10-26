"""Builds the single weather instruments for Another Weather Mod, in the same 1960s style as the weather station
(weather_station_model.py): copper and brass, cream enamel, grey-green hammertone, galvanized and cast iron.

    python tools/weather_instruments_model.py

Writes under E:/AnotherWeatherMod/src/main/resources/assets/anotherweathermod (models/block and textures/block):

- wind_vane            whole (item); wind_vane_base (post and letters); wind_vane_arrow_0..3 (the arrow at 0, 22.5,
                       45 and -22.5 degrees; with the blockstate's y rotation of 0/90/180/270 that gives 16 points,
                       frame 3 being 67.5 when the blockstate adds the next 90)
- barometer            whole; barometer_base (wall case and dial, back on the north face); barometer_needle_0..4
                       (stormy, rain, change, fair, very dry: the needle at -45, -22.5, 0, 22.5, 45 degrees)
- rain_gauge           whole (empty); rain_gauge_water_1..4 (the water column at four fill levels, translucent)
- solar_detector       a pyranometer: brass body, black sensor disc, glass dome on a levelled cast base
- tornado_siren        the whole two-block siren, y 0 to 32 (item and display only); tornado_siren_lower (pole and
                       control cabinet, the lower block); tornado_siren_upper_0..3 (pedestal and the round bell with
                       its nose cone, the upper block, at 0, 22.5, 45 and -22.5 degrees for a turning horn, as the
                       wind vane arrow)

- thermometer          whole; thermometer_base (wall plate, tube and bulb, back on the north face, translucent);
                       thermometer_column_0..4 (the red column at five heights)
- storm_protector      a full cube in the command block's manner: riveted slate with a brass frame, a shield and
                       lightning bolt on the sides and a storm spiral on the ends (two textures, cube_column)

With `--preview DIR` it also writes DIR/assets/anotherweathermod/models/block/anotherweathermod_preview.json, every
instrument in a row with its texture, for looking at them all at once in Blockbench (open it in a fresh Blockbench).
"""
import math
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from blockmodel import *  # noqa: E402,F403

# this script lives in both repositories: in Another Weather Mod it writes into its own tree, in RadioCraft across
ASSETS = next((p for p in (Path(__file__).resolve().parent.parent / "src/main/resources/assets/anotherweathermod",
                           Path("E:/AnotherWeatherMod/src/main/resources/assets/anotherweathermod")) if p.exists()))
MODELS = ASSETS / "models/block"
TEXTURES = ASSETS / "textures/block"
SIZE = 32
TRANSLUCENT = "minecraft:translucent"
GLASS = (196, 224, 228, 90)
GLASS_EDGE = (230, 244, 246, 150)
WATER = (56, 110, 170, 170)

# Four-pixel letters for the wind vane plates.
LETTERS = {
    "N": ("#..#", "##.#", "#.##", "#..#"),
    "E": ("###.", "#...", "##..", "###."),
    "S": (".###", "##..", "..##", "###."),
    "W": ("#..#", "#..#", "#.##", ".#.#"),
}
FRAMES = (0.0, 22.5, 45.0, -22.5)


def sides(region):
    return {f: region for f in ("north", "south", "east", "west")}


def y_rot(angle, origin):
    return {"angle": angle, "axis": "y", "origin": list(origin)}


def octagon(b, name, frm, to, mat, overrides=None, inset=0.1):
    """Two boxes, the second turned 45 degrees and a touch shorter, so a post or case reads as round."""
    cx, cz = (frm[0] + to[0]) / 2, (frm[2] + to[2]) / 2
    r = (to[0] - frm[0]) / 2 * 0.83
    return [b.box(name, frm, to, mat, overrides),
            b.box(name + "_turned", [cx - r, frm[1] + inset, cz - r], [cx + r, to[1] - inset, cz + r], mat,
                  rotation=y_rot(45.0, (cx, frm[1], cz)))]


# ---------------------------------------------------------------- wind vane

WV = {
    "post":   [0, 0, 2, 16],
    "clamp":  [2, 0, 6, 4],
    "arm":    [2, 4, 16, 5],
    "N":      [6, 0, 9, 3],
    "E":      [9, 0, 12, 3],
    "S":      [12, 0, 15, 3],
    "W":      [6, 5, 9, 8],
    "vane":   [9, 5, 15, 8],
    "arrow":  [2, 8, 6, 10],
    "rod":    [2, 10, 4, 16],
}


def paint_wind_vane():
    p = Painter(WV, SIZE, seed=1)
    p.fill("post", IRON, 5, edge(IRON_LIGHT, (48, 48, 44)))
    p.fill("clamp", IRON, 6, lambda x, y, w, h: IRON_LIGHT if (x, y) in ((1, 1), (6, 6)) else None)
    p.fill("arm", IRON, 5, edge(IRON_LIGHT, (48, 48, 44), vertical=False))
    for letter in LETTERS:
        p.fill(letter, BRASS, 8, rim(BRASS_DARK))
        p.glyph(letter, 1, 1, LETTERS[letter], (50, 40, 24))
    p.fill("vane", (160, 94, 56), 8, lambda x, y, w, h: VERDIGRIS if (x * 7 + y * 3) % 11 == 0 else None)
    p.fill("arrow", BRASS, 8, lambda x, y, w, h: BRASS_LIGHT if y == 0 else None)
    p.fill("rod", ROD, 5, lambda x, y, w, h: (120, 122, 118) if x == 0 else None)
    return p


def wind_vane_base(b):
    e = [b.box("post", [7, 0, 7], [9, 10, 9], "post"),
         b.box("base_clamp", [6.5, 0, 6.5], [9.5, 1.5, 9.5], "clamp"),
         b.box("collar", [6.75, 7.5, 6.75], [9.25, 8.5, 9.25], "clamp"),
         b.box("arm_ns", [7.75, 7.75, 2.25], [8.25, 8.25, 13.75], "arm"),
         b.box("arm_ew", [2.25, 7.75, 7.75], [13.75, 8.25, 8.25], "arm"),
         # letter plates stand across the arm ends, readable from both sides
         b.box("plate_n", [6.5, 6.5, 2], [9.5, 9.5, 2.25], "N"),
         b.box("plate_s", [6.5, 6.5, 13.75], [9.5, 9.5, 14], "S"),
         b.box("plate_e", [13.75, 6.5, 6.5], [14, 9.5, 9.5], "E"),
         b.box("plate_w", [2, 6.5, 6.5], [2.25, 9.5, 9.5], "W")]
    return e


def wind_vane_arrow(b, angle):
    rot = y_rot(angle, (8, 11, 8))
    return [b.box("pivot", [7.75, 10, 7.75], [8.25, 11.5, 8.25], "rod", rotation=rot),
            b.box("shaft", [7.75, 11, 2.5], [8.25, 11.5, 13], "vane", rotation=rot),
            b.box("fin", [7.875, 10.25, 10.5], [8.125, 13, 14], "vane", rotation=rot),
            b.box("head", [7.6, 10.75, 1.5], [8.4, 11.75, 3], "arrow", rotation=rot),
            b.box("tip", [7.85, 11, 1], [8.15, 11.5, 1.5], "arrow", rotation=rot)]


def build_wind_vane():
    b = Builder(WV, "anotherweathermod:block/wind_vane", SIZE)
    save(MODELS, "wind_vane", b.model(wind_vane_base(b) + wind_vane_arrow(b, 0.0)))
    save(MODELS, "wind_vane_base", b.model(wind_vane_base(b)))
    for i, a in enumerate(FRAMES):
        save(MODELS, f"wind_vane_arrow_{i}", b.model(wind_vane_arrow(b, a)))
    paint_wind_vane().write(TEXTURES / "wind_vane.png")


# ---------------------------------------------------------------- barometer

BM = {
    "dial":   [0, 0, 10, 10],    # painted round face in a square bezel
    "case":   [10, 0, 16, 6],    # brass case sides and back
    "ring":   [10, 6, 12, 8],    # hanging ring
    "needle": [12, 6, 14, 8],
}
# dial sectors from the left round the top to the right: stormy, rain, change, fair, very dry
SECTORS = ((40, 52, 110), (70, 110, 170), (150, 150, 140), (214, 182, 96), (200, 120, 50))


def paint_barometer():
    p = Painter(BM, SIZE, seed=2)

    def dial(x, y, w, h):
        cx, cy = (w - 1) / 2, (h - 1) / 2
        dx, dy = x - cx, cy - y
        r = math.hypot(dx, dy)
        if r > 9.6:
            return BRASS_DARK
        if r > 8.6:
            return BRASS_LIGHT if dx + dy < 0 else BRASS
        a = math.degrees(math.atan2(dy, dx))  # 0 east, 90 up, 180 west
        if 7.0 < r <= 8.6 and -10 <= a <= 190:
            return SECTORS[min(4, max(0, int((190 - a) / 40)))]
        if 5.8 < r <= 7.0 and -10 <= a <= 190 and int(a) % 20 < 6:
            return (60, 52, 40)
        if r < 1.2:
            return BRASS_DARK
        return (236, 228, 200)

    p.fill("dial", CREAM, 3, dial)
    p.fill("case", BRASS, 8, edge(BRASS_LIGHT, BRASS_DARK))
    p.fill("ring", BRASS_DARK, 6)
    p.fill("needle", (40, 40, 44), 4, lambda x, y, w, h: (90, 90, 96) if x == 0 else None)
    return p


def barometer_base(b):
    front = {"south": "dial"}
    e = [b.box("case", [3, 3, 0], [13, 13, 2], "case", front),
         b.box("case_turned", [3.85, 3.85, 0.25], [12.15, 12.15, 1.75], "case",
               rotation={"angle": 45.0, "axis": "z", "origin": [8, 8, 1]}),
         b.box("ring", [7.5, 13, 0.5], [8.5, 14, 1.5], "ring"),
         b.box("ring_top", [7.25, 14, 0.5], [8.75, 14.5, 1.5], "ring")]
    return e


def barometer_needle(b, angle):
    rot = {"angle": angle, "axis": "z", "origin": [8, 8, 2.25]}
    return [b.box("needle", [7.75, 7.5, 2], [8.25, 12.5, 2.5], "needle", rotation=rot),
            b.box("needle_tail", [7.8, 6, 2.05], [8.2, 7.5, 2.45], "needle", rotation=rot),
            b.box("boss", [7.4, 7.4, 2], [8.6, 8.6, 2.6], "ring")]


def build_barometer():
    b = Builder(BM, "anotherweathermod:block/barometer", SIZE)
    save(MODELS, "barometer", b.model(barometer_base(b) + barometer_needle(b, 0.0)))
    save(MODELS, "barometer_base", b.model(barometer_base(b)))
    for i, a in enumerate((-45.0, -22.5, 0.0, 22.5, 45.0)):
        save(MODELS, f"barometer_needle_{i}", b.model(barometer_needle(b, a)))
    paint_barometer().write(TEXTURES / "barometer.png")


# ---------------------------------------------------------------- rain gauge

RG = {
    "base":   [0, 0, 6, 6],      # cast iron foot
    "glass":  [6, 0, 10, 10],    # tube with marks on the left
    "collar": [10, 0, 16, 2],    # brass
    "funnel": [10, 2, 16, 6],    # copper, outside
    "mouth":  [0, 6, 6, 12],     # copper funnel mouth, dark inside
    "water":  [10, 6, 14, 10],
    "post":   [0, 12, 2, 16],
}


def paint_rain_gauge():
    p = Painter(RG, SIZE, seed=3)
    p.fill("base", IRON, 6, lambda x, y, w, h: IRON_LIGHT if (x, y) in ((2, 2), (9, 9)) else None)
    p.fill("glass", GLASS, 4, lambda x, y, w, h: GLASS_EDGE if x in (0, w - 1) else None)
    for y in range(1, 20, 2):
        p.put("glass", 1, y, (40, 40, 44, 210))
        if y % 4 == 1:
            p.put("glass", 2, y, (40, 40, 44, 210))
    p.fill("collar", BRASS, 8, edge(BRASS_LIGHT, BRASS_DARK, vertical=False))
    p.fill("funnel", COPPER, 8, edge(COPPER_LIGHT, COPPER_DARK))
    p.fill("mouth", (70, 36, 20), 3, rim(COPPER_LIGHT))
    p.fill("water", WATER, 6, lambda x, y, w, h: (120, 170, 220, 190) if y == 0 else None)
    p.fill("post", IRON, 5, edge(IRON_LIGHT, (48, 48, 44)))
    return p


def rain_gauge_base(b):
    e = [b.box("foot", [5, 0, 5], [11, 1, 11], "base"),
         b.box("tube", [6, 1, 6], [10, 10, 10], "glass"),
         b.box("collar", [5.5, 9.5, 5.5], [10.5, 10.75, 10.5], "collar"),
         b.box("throat", [6.5, 10.75, 6.5], [9.5, 11.5, 9.5], "funnel"),
         b.box("funnel", [5, 11.5, 5], [11, 13, 11], "funnel", {"up": "mouth"}),
         b.box("funnel_rim", [4.5, 13, 4.5], [11.5, 13.75, 11.5], "funnel", {"up": "mouth"})]
    e += octagon(b, "rim_round", [4.5, 13, 4.5], [11.5, 13.75, 11.5], "funnel", {"up": "mouth"})[1:]
    return e


def rain_gauge_water(b, level):
    top = 1.25 + 2.0 * level
    return [b.box("water", [6.25, 1.25, 6.25], [9.75, top, 9.75], "water")]


def build_rain_gauge():
    b = Builder(RG, "anotherweathermod:block/rain_gauge", SIZE)
    save(MODELS, "rain_gauge", b.model(rain_gauge_base(b), TRANSLUCENT))
    for level in range(1, 5):
        save(MODELS, f"rain_gauge_water_{level}", b.model(rain_gauge_water(b, level), TRANSLUCENT))
    paint_rain_gauge().write(TEXTURES / "rain_gauge.png")


# ---------------------------------------------------------------- solar strength detector (pyranometer)

SD = {
    "plate":  [0, 0, 8, 8],      # cast iron base
    "body":   [8, 0, 14, 4],     # brass body
    "disc":   [8, 4, 12, 8],     # black sensor disc under the dome
    "dome":   [0, 8, 6, 14],     # glass
    "screw":  [14, 0, 16, 2],
    "level":  [14, 2, 16, 4],    # bubble level
    "cable":  [12, 4, 14, 8],
    "post":   [6, 8, 8, 16],
}


def paint_solar_detector():
    p = Painter(SD, SIZE, seed=4)
    p.fill("plate", IRON, 6, rim((48, 48, 44)))
    p.fill("body", BRASS, 8, edge(BRASS_LIGHT, BRASS_DARK, vertical=False))
    p.fill("disc", BLACK, 4, rim((70, 70, 70)))
    p.fill("dome", GLASS, 4, lambda x, y, w, h: GLASS_EDGE if x + y < 3 else None)
    p.fill("screw", BRASS_DARK, 5)
    p.fill("level", (150, 150, 140), 4, lambda x, y, w, h: (120, 200, 90) if 1 <= x <= 2 and 1 <= y <= 2 else None)
    p.fill("cable", BAKELITE_DARK, 4)
    p.fill("post", GALV, 7, galvanized())
    return p


def solar_detector(b):
    e = [b.box("post", [7, 0, 7], [9, 2, 9], "post"),
         b.box("plate", [4, 2, 4], [12, 3, 12], "plate"),
         b.box("level", [10.25, 3, 10.25], [11.75, 3.75, 11.75], "level"),
         b.box("cable", [11, 1.5, 7.5], [12.5, 3, 8.5], "cable"),
         b.box("cable_down", [11.5, 0, 7.5], [12.5, 1.5, 8.5], "cable")]
    for i, (x, z) in enumerate(((4.25, 4.25), (11.25, 4.25), (4.25, 11.25), (11.25, 11.25))):
        e.append(b.box(f"screw_{i}", [x, 1.5, z], [x + 0.5, 3.25, z + 0.5], "screw"))
    e += octagon(b, "body", [5, 3, 5], [11, 5.5, 11], "body")
    e.append(b.box("disc", [6.5, 5.5, 6.5], [9.5, 5.75, 9.5], "disc"))
    e += octagon(b, "dome_0", [5.5, 5.5, 5.5], [10.5, 7, 10.5], "dome")
    e += octagon(b, "dome_1", [6, 7, 6], [10, 8.25, 10], "dome")
    e.append(b.box("dome_2", [7, 8.25, 7], [9, 9, 9], "dome"))
    return e


def build_solar_detector():
    b = Builder(SD, "anotherweathermod:block/solar_detector", SIZE)
    save(MODELS, "solar_detector", b.model(solar_detector(b), TRANSLUCENT))
    paint_solar_detector().write(TEXTURES / "solar_detector.png")


# ---------------------------------------------------------------- tornado siren (two blocks tall)
# A Federal Signal style siren: a big round bell with a nose cone in its mouth on a boxy cabinet, pale grey enamel.

TS = {
    "pole":    [0, 0, 2, 16],
    "strap":   [2, 0, 4, 8],      # iron mounting channel
    "cabinet": [4, 0, 10, 6],     # pale grey with louvres
    "plain":   [10, 0, 16, 6],    # pale grey
    "bell":    [4, 6, 10, 12],    # pale grey, lit from above
    "inner":   [10, 6, 16, 12],   # dark grey inside the bell
    "nose":    [4, 12, 8, 16],    # cream nose cone front with a red roundel
    "cone":    [8, 12, 10, 16],   # cone side
    "label":   [2, 8, 4, 9],      # red and blue maker's plate
    "rod":     [2, 10, 4, 16],
}
GREY = (198, 200, 196)
GREY_LIGHT = (226, 228, 224)
GREY_DARK = (150, 152, 150)
INNER = (96, 98, 96)


def paint_tornado_siren():
    p = Painter(TS, SIZE, seed=5)
    p.fill("pole", GALV, 7, galvanized())
    p.fill("strap", IRON, 6, lambda x, y, w, h: IRON_LIGHT if (x, y) in ((1, 3), (2, 12)) else None)
    p.fill("cabinet", GREY, 5, lambda x, y, w, h: GREY_DARK if 2 <= x <= 9 and 2 <= y <= 9 and y % 2 == 0 else None)
    p.fill("plain", GREY, 5, rim(GREY_DARK))
    p.fill("bell", GREY, 5, edge(GREY_LIGHT, GREY_DARK, vertical=False))
    p.fill("inner", INNER, 5, lambda x, y, w, h: (120, 122, 120) if (x + y) % 6 == 0 else None)
    p.fill("nose", CREAM, 4, lambda x, y, w, h: (178, 40, 40) if 2 <= x <= 5 and 2 <= y <= 5 else None)
    p.fill("cone", GREY, 5, edge(GREY_LIGHT, GREY_DARK, vertical=False))
    p.fill("label", (178, 40, 40), 5, lambda x, y, w, h: (40, 70, 150) if x >= 2 else None)
    p.fill("rod", ROD, 5, lambda x, y, w, h: (120, 122, 118) if x == 0 else None)
    return p


def ring(b, name, x0, y0, x1, y1, z0, z1, mat, inner, rotation=None):
    """A hollow chamfered-square tube along z out of eight axis-aligned boxes, so it can still turn on y."""
    c = (x1 - x0) / 6
    r = lambda n, frm, to, inside: b.box(f"{name}_{n}", frm, to, mat, {f: inner for f in inside}, rotation=rotation)
    return [r("top", [x0 + c, y1 - c, z0], [x1 - c, y1, z1], ("down",)),
            r("bottom", [x0 + c, y0, z0], [x1 - c, y0 + c, z1], ("up",)),
            r("west", [x0, y0 + c, z0], [x0 + c, y1 - c, z1], ("east",)),
            r("east", [x1 - c, y0 + c, z0], [x1, y1 - c, z1], ("west",)),
            r("nw", [x0 + c, y1 - 2 * c, z0], [x0 + 2 * c, y1 - c, z1], ("down", "east")),
            r("ne", [x1 - 2 * c, y1 - 2 * c, z0], [x1 - c, y1 - c, z1], ("down", "west")),
            r("sw", [x0 + c, y0 + c, z0], [x0 + 2 * c, y0 + 2 * c, z1], ("up", "east")),
            r("se", [x1 - 2 * c, y0 + c, z0], [x1 - c, y0 + 2 * c, z1], ("up", "west"))]


def tornado_siren_lower(b, dy=0):
    """The pole, mounting straps and the control cabinet: the lower block. dy lifts it for the whole model."""
    def y(v):
        return v + dy
    return [b.box("pole", [7, y(0), 7], [9, y(6), 9], "pole"),
            b.box("strap_w", [5.75, y(0.5), 6.75], [6.75, y(6.5), 9.25], "strap"),
            b.box("strap_e", [9.25, y(0.5), 6.75], [10.25, y(6.5), 9.25], "strap"),
            b.box("conduit", [6, y(0), 10], [7, y(6.5), 11], "rod"),
            b.box("cabinet", [3, y(6), 3], [13, y(14.5), 13], "cabinet", {"up": "plain", "down": "plain"}),
            b.box("label", [2.9, y(11), 6.5], [3.1, y(12), 8.5], "label"),
            b.box("lid", [2.5, y(14.5), 2.5], [13.5, y(15.25), 13.5], "plain"),
            b.box("rotator_base", [5.5, y(15.25), 5.5], [10.5, y(16), 10.5], "plain")]


def tornado_siren_upper(b, angle, dy=0):
    """The rotator pedestal and the bell: the upper block, turned `angle` about the pole."""
    def y(v):
        return v + dy
    rot = y_rot(angle, (8, y(8), 8))
    e = [b.box("pedestal", [5.5, y(0), 5.5], [10.5, y(2.5), 10.5], "plain"),
         b.box("riser", [6.5, y(2.5), 9], [9.5, y(6.5), 12.5], "plain", rotation=rot),
         b.box("throat", [6.5, y(6.5), 8.5], [9.5, y(9.5), 12], "bell", rotation=rot)]
    e += ring(b, "bell", 2, y(2), 14, y(14), 2.5, 5.5, "bell", "inner", rot)
    e += ring(b, "flare", 3.5, y(3.5), 12.5, y(12.5), 5.5, 7.5, "bell", "inner", rot)
    e += [b.box("back", [5, y(5), 7.5], [11, y(11), 8.5], "bell", {"north": "inner"}, rotation=rot),
          b.box("cone", [5.5, y(5.5), 4], [10.5, y(10.5), 7.5], "cone", {"north": "nose"}, rotation=rot),
          b.box("cone_tip", [6.75, y(6.75), 3.5], [9.25, y(9.25), 4], "cone", {"north": "nose"}, rotation=rot)]
    return e


def build_tornado_siren():
    b = Builder(TS, "anotherweathermod:block/tornado_siren", SIZE)
    save(MODELS, "tornado_siren", b.model(tornado_siren_lower(b) + tornado_siren_upper(b, 0.0, dy=16)))
    save(MODELS, "tornado_siren_lower", b.model(tornado_siren_lower(b)))
    for i, a in enumerate(FRAMES):
        save(MODELS, f"tornado_siren_upper_{i}", b.model(tornado_siren_upper(b, a)))
    paint_tornado_siren().write(TEXTURES / "tornado_siren.png")


# ---------------------------------------------------------------- thermometer

TH = {
    "plate":   [0, 0, 6, 14],    # cream enamel scale, back on the north face
    "back":    [10, 2, 12, 4],
    "glass":   [6, 0, 8, 12],
    "bulb":    [8, 0, 10, 2],
    "column":  [8, 2, 10, 4],
    "bracket": [10, 0, 12, 2],
}


def paint_thermometer():
    p = Painter(TH, SIZE, seed=6)

    def scale(x, y, w, h):
        if min(x, y, w - 1 - x, h - 1 - y) == 0:
            return CREAM_DIRTY
        if 2 <= y <= 24 and y % 2 == 0 and (x in (2, 3) if y % 4 == 0 else x == 3):
            return (50, 44, 36)
        if 2 <= y <= 6 and x in (8, 9):
            return (178, 40, 40)  # the hot end marked red
        return None

    p.fill("plate", CREAM, 3, scale)
    p.fill("back", CREAM, 4)
    p.fill("glass", GLASS, 4, lambda x, y, w, h: GLASS_EDGE if x == 0 else None)
    p.fill("bulb", (190, 40, 40), 6, lambda x, y, w, h: (230, 90, 90) if x + y < 2 else None)
    p.fill("column", (200, 44, 44), 5, lambda x, y, w, h: (236, 100, 100) if x == 0 else None)
    p.fill("bracket", BRASS, 8, edge(BRASS_LIGHT, BRASS_DARK, vertical=False))
    return p


def thermometer_base(b):
    return [b.box("plate", [5, 0.5, 0], [11, 15.5, 0.5], "back", {"south": "plate"}),
            b.box("bracket_top", [6, 14.5, 0.5], [10, 15.25, 1.25], "bracket"),
            b.box("bracket_bottom", [6, 0.75, 0.5], [10, 1.5, 1.25], "bracket"),
            b.box("tube", [7.25, 2.75, 0.5], [8.75, 14.25, 1.25], "glass"),
            b.box("bulb", [6.75, 1.25, 0.5], [9.25, 3, 1.5], "bulb")]


def thermometer_column(b, level):
    return [b.box("column", [7.5, 3, 0.65], [8.5, 4.5 + 2.4 * level, 1.1], "column")]


def build_thermometer():
    b = Builder(TH, "anotherweathermod:block/thermometer", SIZE)
    save(MODELS, "thermometer", b.model(thermometer_base(b) + thermometer_column(b, 2), TRANSLUCENT))
    save(MODELS, "thermometer_base", b.model(thermometer_base(b), TRANSLUCENT))
    for level in range(5):
        save(MODELS, f"thermometer_column_{level}", b.model(thermometer_column(b, level)))
    paint_thermometer().write(TEXTURES / "thermometer.png")


# ---------------------------------------------------------------- storm protector (a full cube, command block style)

SP = {"all": [0, 0, 16, 16]}
SLATE = (58, 66, 80)
SLATE_LIGHT = (74, 84, 100)
SHIELD = ("....########....",
          "...##########...",
          "...#........#...",
          "...#........#...",
          "...#........#...",
          "...#........#...",
          "...#........#...",
          "...#........#...",
          "...#........#...",
          "....#......#....",
          ".....#....#.....",
          "......#..#......",
          ".......##.......")
BOLT = (".....##",
        "....##.",
        "...##..",
        "..####.",
        "...##..",
        "..##...",
        ".##....",
        "##.....")


def frame(x, y, w, h):
    """The riveted brass frame round every face."""
    d = min(x, y, w - 1 - x, h - 1 - y)
    if d == 0:
        return BRASS_DARK
    if d == 1:
        return BRASS
    if d == 2:
        return (44, 50, 60)
    if (x in (3, w - 4)) and (y in (3, h - 4)):
        return BRASS_LIGHT
    return SLATE_LIGHT if (x * 3 + y * 5) % 7 == 0 else None


def paint_storm_protector_side():
    p = Painter(SP, SIZE, seed=7)
    p.fill("all", SLATE, 4, frame)
    p.glyph("all", 8, 9, SHIELD, BRASS)
    for y in range(11, 18):
        for x in range(12, 20):
            p.put("all", x, y, CREAM)
    for y in (18, 19, 20):
        for x in range(13 + (y - 18), 19 - (y - 18)):
            p.put("all", x, y, CREAM)
    p.glyph("all", 12, 11, BOLT, (178, 40, 40))
    return p


def paint_storm_protector_top():
    p = Painter(SP, SIZE, seed=8)
    p.fill("all", SLATE, 4, frame)
    cx = cy = 15.5
    for y in range(4, 28):
        for x in range(4, 28):
            r = math.hypot(x - cx, y - cy)
            if r > 11:
                continue
            a = math.atan2(y - cy, x - cx)
            if r < 1.8:
                p.put("all", x, y, BRASS_LIGHT)
            elif ((a * 3 + r * 0.9) % (2 * math.pi)) < 1.6 and r > 2.5:
                p.put("all", x, y, CREAM if r < 9 else BRASS)
    return p


def build_storm_protector():
    save(MODELS, "storm_protector", {"parent": "minecraft:block/cube_column",
                                     "textures": {"end": "anotherweathermod:block/storm_protector_top",
                                                  "side": "anotherweathermod:block/storm_protector_side"}})
    paint_storm_protector_side().write(TEXTURES / "storm_protector_side.png")
    paint_storm_protector_top().write(TEXTURES / "storm_protector_top.png")


def build_preview(root):
    """Every instrument in a row in one Blockbench-only model under `root` (see blockmodel.write_preview)."""
    cube = Builder({"all": [0, 0, 16, 16]}, "", SIZE)
    block = cube.box("cube", [0, 0, 0], [16, 16, 16], "all")
    for f in ("up", "down"):
        block["faces"][f]["texture"] = "#1"
    rows = [(["wind_vane"], wind_vane_base(b := Builder(WV, "", SIZE)) + wind_vane_arrow(b, 0.0)),
            (["barometer"], barometer_base(b := Builder(BM, "", SIZE)) + barometer_needle(b, 0.0)),
            (["rain_gauge"], rain_gauge_base(b := Builder(RG, "", SIZE)) + rain_gauge_water(b, 3)),
            (["solar_detector"], solar_detector(Builder(SD, "", SIZE))),
            (["thermometer"], thermometer_base(b := Builder(TH, "", SIZE)) + thermometer_column(b, 3)),
            (["tornado_siren"], tornado_siren_lower(b := Builder(TS, "", SIZE)) + tornado_siren_upper(b, 0.0, dy=16)),
            (["storm_protector_side", "storm_protector_top"], [block])]
    write_preview(root, "anotherweathermod", TEXTURES, rows, SIZE)


if __name__ == "__main__":
    build_wind_vane()
    build_barometer()
    build_rain_gauge()
    build_solar_detector()
    build_tornado_siren()
    build_thermometer()
    build_storm_protector()
    if len(sys.argv) > 2 and sys.argv[1] == "--preview":
        build_preview(sys.argv[2])
