"""Writes the speaker block models: desk speakers in every wood, a large cabinet speaker, a wall speaker, a premium
desk speaker in three colourways, and a vintage PA horn loudspeaker.

    python tools/speaker_models.py [--preview DIR --planks DIR]

- desk_speaker_<wood>       a 6x5x4 wooden cabinet with a cloth grille, a knob and a terminal strip on the back, for
                            oak, spruce, birch, jungle, acacia, dark_oak, mangrove, cherry, bamboo, crimson and
                            warped. The cabinet faces use the vanilla plank textures (minecraft:block/<wood>_planks)
                            so there is one grille texture (desk_speaker.png) and a model per wood.
- large_speaker             a full block in the National NC-style: grey speckled cabinet, round grille cloth with a
                            diamond weave, three chrome bars and a red diamond badge, chrome feet
                            (large_speaker.png for the front and trim, large_speaker_side.png for the cabinet)
- wall_speaker              16 wide and tall, 3 deep, back against the south face (the vanilla wall-block way, so
                            facing=north shows it as drawn); a silver frame round marbled grey cloth with a round
                            badge, and a terminal strip on the back for a wire in the block behind
                            (wall_speaker.png front, wall_speaker_side.png edges and back)
- premium_speaker_black / _silver / _gold
                            an 8x6x6 metal desk speaker with a slotted grille, bezel strips, a volume knob and four
                            feet; one geometry, a texture per colourway
- loudspeaker               a PA reflex horn: a blue-grey stepped bell with a cream rim pointing north, an octagonal
                            driver behind, on a U-bracket and wooden base

`--preview DIR` writes DIR/assets/radiocraft/models/block/radiocraft_preview.json with everything in a row; the
plank textures are read from `--planks DIR` (a folder of <wood>_planks.png pulled out of the client jar).
"""
import argparse
import math
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from blockmodel import *  # noqa: E402,F403

ROOT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/radiocraft"
MODELS = ROOT / "models/block"
TEXTURES = ROOT / "textures/block"

WOODS = ["oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry", "bamboo", "crimson", "warped"]

# Files the maintainer has since edited by hand (Blockbench, image editor): the script no longer writes these. The
# loudspeaker's bell steps were added to the shipped file by hand too, from horn_steps() below.
HAND_EDITED = {"loudspeaker.json", "wall_speaker.json", "wall_speaker.png"}

CLOTH = (176, 150, 104)
CLOTH_LIGHT = (214, 192, 140)
CLOTH_DARK = (120, 98, 62)
GREY = (122, 124, 126)
GREY_LIGHT = (156, 158, 160)
GREY_DARK = (84, 86, 88)
CHROME = (206, 210, 214)
CHROME_DARK = (140, 144, 150)
HORN = (72, 84, 98)
HORN_LIGHT = (98, 112, 128)
HORN_DARK = (48, 56, 66)
WOOD = (120, 84, 48)


def speckle(dark, light, density=0.18):
    """A hammertone / wrinkle-finish shader: random dark and light flecks."""
    def fn(x, y, w, h):
        v = (x * 7919 + y * 104729 + x * y * 31) % 100 / 100
        return dark if v < density else light if v > 1 - density / 2 else None
    return fn


def weave(light, dark, period=4):
    """A cloth shader: a diamond weave."""
    def fn(x, y, w, h):
        d = (x + y) % period, (x - y) % period
        return light if d[0] == 0 else dark if d[1] == 0 else None
    return fn


def disc(p, region, cx, cy, r, inner_fn, rim=None, rim_width=0):
    """Paints a circle inside a region: `inner_fn(x, y)` gives the colour, `rim` the ring round it."""
    x0, y0, x1, y1 = p.px(region)
    for y in range(y0, y1):
        for x in range(x0, x1):
            dx, dy = x - x0 - cx + 0.5, y - y0 - cy + 0.5
            d = math.hypot(dx, dy)
            if d < r - rim_width:
                c = inner_fn(x - x0, y - y0)
            elif d < r and rim:
                c = rim
            else:
                continue
            if c:
                j = p.rnd.randint(-5, 5)
                p.img[y][x] = tuple(max(0, min(255, v + j)) for v in c) + (255,)


# ---------------------------------------------------------------- desk speaker, one model per wood

DESK = {
    "wood": ("wood", 0, 0, 16, 16),
    "cloth": [0, 0, 4, 4],
    "terminal": [4, 0, 6, 1],
    "knob": [4, 1, 5, 2],
}


def paint_desk():
    p = Painter(DESK, 32, seed=3)
    p.fill("cloth", CLOTH, 6, weave(CLOTH_LIGHT, CLOTH_DARK))
    p.fill("terminal", BAKELITE, 4, lambda x, y, w, h: BRASS if x in (1, 2) and y == 1 else None)
    p.fill("knob", BAKELITE_DARK, 4, lambda x, y, w, h: (120, 112, 100) if x == 1 and y == 0 else None)
    p.write(TEXTURES / "desk_speaker.png")


def desk_elements(b):
    return [
        b.box("cabinet", [5, 0, 6], [11, 5, 10], "wood"),
        b.box("grille", [5.5, 0.5, 5.75], [9.5, 4.5, 6], "cloth"),
        b.box("knob", [10, 2, 5.5], [10.5, 2.5, 6], "knob"),
        b.box("terminal", [7, 1, 10], [9, 2, 10.5], "terminal"),
    ]


def desk_builder(wood):
    return Builder(DESK, {"0": "radiocraft:block/desk_speaker", "wood": f"minecraft:block/{wood}_planks"}, 32)


# ---------------------------------------------------------------- large speaker

LARGE = {
    "front": [0, 0, 14, 15],
    "bar": ("0", 14, 0, 15, 11),
    "badge": ("0", 14, 11, 16, 13),
    "chrome": ("0", 14, 13, 16, 15),
    "hammertone": ("1", 0, 0, 14, 15),
    "bottom": ("1", 14, 0, 16, 2),
}


def paint_large():
    p = Painter({k: v for k, v in LARGE.items() if v[0] != "1"}, 64, seed=4)
    p.fill("front", GREY, 4, speckle(GREY_DARK, GREY_LIGHT))
    cloth = weave(CLOTH_LIGHT, CLOTH_DARK, 6)
    disc(p, "front", 28, 30, 25, lambda x, y: cloth(x, y, 0, 0) or CLOTH, rim=(40, 40, 42), rim_width=2)
    p.fill("bar", CHROME, 5, edge((236, 238, 240), CHROME_DARK))
    p.fill("badge", (176, 32, 32), 6, lambda x, y, w, h: (230, 220, 200) if 2 <= x < w - 2 and 2 <= y < h - 2 else None)
    p.fill("chrome", CHROME, 5, lambda x, y, w, h: CHROME_DARK if y % 2 else None)
    p.write(TEXTURES / "large_speaker.png")
    q = Painter({k: v for k, v in LARGE.items() if v[0] == "1"}, 32, seed=5)
    q.fill("hammertone", GREY, 4, speckle(GREY_DARK, GREY_LIGHT))
    q.fill("bottom", GREY_DARK, 4)
    q.write(TEXTURES / "large_speaker_side.png")


def large_elements(b):
    els = [b.box("cabinet", [1, 1, 1], [15, 16, 15], "hammertone", {"north": "front", "down": "bottom"})]
    for i, (x, z) in enumerate(((1, 1), (11, 1), (1, 11), (11, 11))):
        els.append(b.box(f"foot_{i}", [x, 0, z], [x + 4, 1, z + 4], "chrome"))
    for i, x in enumerate((5.5, 7.5, 9.5)):
        els.append(b.box(f"bar_{i}", [x, 3, 0.5], [x + 1, 14, 1], "bar"))
    els.append(b.box("badge", [7, 7.5, 0.25], [9, 9.5, 0.5], "badge",
                     rotation={"angle": 45, "axis": "z", "origin": [8, 8.5, 0.5]}))
    return els


def large_builder():
    return Builder(LARGE, {"0": "radiocraft:block/large_speaker", "1": "radiocraft:block/large_speaker_side"}, 64)


# ---------------------------------------------------------------- wall speaker

WALL = {
    "front": [0, 0, 16, 16],
    "edge": ("1", 0, 0, 16, 3),
    "edge_v": ("1", 0, 3, 3, 16),
    "back": ("1", 3, 3, 16, 16),
    "terminal": ("1", 3, 3, 7, 5),
}


def paint_wall():
    p = Painter({"front": WALL["front"]}, 64, seed=6)
    cloth = (118, 116, 110)
    p.fill("front", cloth, 5, lambda x, y, w, h: (104, 102, 96) if (x + y) % 3 == 0 else None)
    x0, y0, x1, y1 = p.px("front")
    for _ in range(26):                                  # the marbled threads
        x, y = p.rnd.randint(6, 57), p.rnd.randint(6, 57)
        dx, dy = p.rnd.choice((-1, 0, 1)), p.rnd.choice((-1, 0, 1))
        for _ in range(p.rnd.randint(8, 24)):
            if 5 <= x < 59 and 5 <= y < 59:
                p.img[y0 + y][x0 + x] = (204, 198, 182, 255)
            if p.rnd.random() < 0.3:
                dx, dy = p.rnd.choice((-1, 0, 1)), p.rnd.choice((-1, 0, 1))
            x, y = x + dx, y + dy
    for y in range(64):                                   # the silver frame, bevelled
        for x in range(64):
            d = min(x, y, 63 - x, 63 - y)
            if d < 4:
                c = CHROME if d == 0 or (d < 2 and y < 32) else CHROME_DARK if d == 3 else (184, 188, 192)
                p.img[y0 + y][x0 + x] = tuple(max(0, min(255, v + p.rnd.randint(-4, 4))) for v in c) + (255,)
    disc(p, "front", 52, 52, 5, lambda x, y: (30, 30, 32) if math.hypot(x - 52 + 0.5, y - 52 + 0.5) > 1.5 else (220, 214, 196),
         rim=CHROME, rim_width=1)
    p.write(TEXTURES / "wall_speaker.png")
    q = Painter({k: v for k, v in WALL.items() if k != "front"}, 32, seed=7)
    q.fill("edge", CHROME, 5, edge((236, 238, 240), CHROME_DARK, vertical=False))
    q.fill("edge_v", CHROME, 5, edge((236, 238, 240), CHROME_DARK))
    q.fill("back", (70, 66, 60), 4, speckle((54, 50, 46), (86, 82, 76), 0.1))
    q.fill("terminal", BAKELITE, 4, lambda x, y, w, h: BRASS if x in (2, 5) and y in (1, 2) else None)
    q.write(TEXTURES / "wall_speaker_side.png")


def wall_elements(b):
    return [
        b.box("body", [0, 0, 13], [16, 16, 16], "back", {"north": "front", "up": "edge", "down": "edge",
                                                         "east": "edge_v", "west": "edge_v"}),
        b.box("terminal", [6, 1, 12.5], [10, 3, 13], "terminal"),
    ]


def wall_builder():
    return Builder(WALL, {"0": "radiocraft:block/wall_speaker", "1": "radiocraft:block/wall_speaker_side"}, 64)


# ---------------------------------------------------------------- premium desk speaker, three colourways

PREMIUM = {
    "front": [0, 0, 8, 6],
    "side": [8, 0, 14, 6],
    "top": [0, 6, 8, 12],
    "back": [8, 6, 16, 12],
    "knob": [14, 0, 16, 2],
    "foot": [14, 2, 16, 3],
    "bezel": [0, 12, 8, 13],
}

COLOURWAYS = {
    "black": ((34, 34, 36), (62, 62, 64), (16, 16, 18), (186, 188, 192)),
    "silver": ((178, 180, 184), (210, 212, 216), (126, 128, 132), (52, 52, 54)),
    "gold": ((198, 162, 76), (228, 198, 112), (150, 116, 46), (44, 38, 30)),
}


def paint_premium(name, palette):
    body, light, dark, trim = palette
    p = Painter(PREMIUM, 32, seed=8)
    brushed = lambda x, y, w, h: light if (x * 3 + y) % 7 == 0 else dark if (x * 5 + y) % 11 == 0 else None
    p.fill("side", body, 3, brushed)
    p.fill("top", body, 3, brushed)
    p.fill("back", dark, 3, lambda x, y, w, h: BRASS if 6 <= x < 10 and y in (8, 9) else None)
    p.fill("front", body, 3, lambda x, y, w, h: (dark if 2 <= x < w - 2 and 2 <= y < h - 2 and y % 2 == 0 else
                                                 trim if (x == 1 or x == w - 2) and 1 <= y < h - 1 else None))
    p.fill("bezel", body, 3, edge(light, dark, vertical=False))
    p.fill("knob", trim, 4, lambda x, y, w, h: body if x == 2 and y < 2 else None)
    p.fill("foot", (28, 26, 24), 3)
    p.write(TEXTURES / f"{name}.png")


def premium_elements(b):
    els = [
        b.box("cabinet", [4, 0.5, 5], [12, 6.5, 11], "side", {"north": "front", "up": "top", "south": "back", "down": "back"}),
        b.box("bezel_top", [4, 5.5, 4.75], [12, 6.5, 5], "bezel"),
        b.box("bezel_bottom", [4, 0.5, 4.75], [12, 1.5, 5], "bezel"),
        b.box("knob", [10.5, 1.5, 4.5], [11.5, 2.5, 5], "knob"),
    ]
    for i, (x, z) in enumerate(((4.5, 5.5), (11, 5.5), (4.5, 10), (11, 10))):
        els.append(b.box(f"foot_{i}", [x, 0, z], [x + 0.5, 0.5, z + 0.5], "foot"))
    return els


def premium_builder(name):
    return Builder(PREMIUM, f"radiocraft:block/{name}", 32)


# ---------------------------------------------------------------- loudspeaker horn

HORNREG = {
    "horn": [0, 0, 8, 8],
    "horn_inner": [8, 0, 16, 8],
    "rim": [0, 8, 8, 9],
    "bracket": [8, 8, 10, 15],
    "base": [10, 8, 16, 14],
    "driver": [0, 9, 6, 15],
}

HC, HY = 8, 9           # the horn axis: x and y


def paint_horn():
    p = Painter(HORNREG, 32, seed=9)
    p.fill("horn", HORN, 4, lambda x, y, w, h: HORN_LIGHT if y < 2 else None)
    p.fill("horn_inner", HORN_DARK, 4)
    p.fill("rim", (222, 214, 196), 4)
    p.fill("bracket", (58, 62, 68), 4, rim((78, 82, 88)))
    p.fill("base", WOOD, 6, lambda x, y, w, h: (98, 66, 36) if y % 4 == 0 else None)
    p.fill("driver", HORN, 4, lambda x, y, w, h: HORN_DARK if x % 3 == 0 else HORN_LIGHT if y < 2 else None)
    p.write(TEXTURES / "loudspeaker.png")


def ring(b, name, z0, z1, half, thick, mat, inner):
    """Four plates making a hollow square ring round the horn axis between z0 and z1."""
    x0, x1, y0, y1 = HC - half, HC + half, HY - half, HY + half
    return [
        b.box(f"{name}_top", [x0, y1 - thick, z0], [x1, y1, z1], mat, {"down": inner}),
        b.box(f"{name}_bottom", [x0, y0, z0], [x1, y0 + thick, z1], mat, {"up": inner}),
        b.box(f"{name}_west", [x0, y0 + thick, z0], [x0 + thick, y1 - thick, z1], mat, {"east": inner}),
        b.box(f"{name}_east", [x1 - thick, y0 + thick, z0], [x1, y1 - thick, z1], mat, {"west": inner}),
    ]


def horn_elements(b):
    els = [
        b.box("base", [3, 0, 8], [13, 1, 15], "base"),
        b.box("bracket_west", [4, 1, 10], [5, 8, 12], "bracket"),
        b.box("bracket_east", [11, 1, 10], [12, 8, 12], "bracket"),
        b.box("bracket_bar", [4, 7, 10], [12, 8, 12], "bracket"),
        b.box("driver", [5, 6, 11], [11, 12, 15], "driver"),
        b.box("driver_turned", [5, 6, 11], [11, 12, 15], "driver",
              rotation={"angle": 45, "axis": "z", "origin": [HC, HY, 13]}),
        b.box("throat", [6.5, 7.5, 8], [9.5, 10.5, 11], "horn"),
    ]
    for i, (z0, z1, half) in enumerate(BELL):
        els += ring(b, f"bell_{i}", z0, z1, half, 0.5, "horn", "horn_inner")
    els += ring(b, "rim", 0, 0.5, 7, 0.5, "rim", "rim")
    return els + horn_steps(b)


BELL = ((6, 8, 2.5), (4, 6, 3.5), (2, 4, 5), (0.5, 2, 7))   # each ring of the bell: z0, z1, half width


def horn_steps(b):
    """The step between each ring and the next, so the bell is closed: a frame half a unit thick at the front of
    the smaller ring, out to the bigger ring's outline, and a plate closing the throat into the first ring."""
    t = 0.5
    z0, z1, half = BELL[0]
    x0, x1, y0, y1 = HC - half, HC + half, HY - half, HY + half
    els = [b.box("step_throat", [x0, y0, z1], [x1, y1, z1 + t], "horn", {"north": "horn_inner"})]
    for i in range(len(BELL) - 1):
        z = BELL[i][0]                       # the front of the smaller ring, where the bigger one begins
        a, c = BELL[i][2], BELL[i + 1][2]    # the inner and outer half widths of the frame
        els += [
            b.box(f"step_{i}_top", [HC - c, HY + a, z], [HC + c, HY + c, z + t], "horn"),
            b.box(f"step_{i}_bottom", [HC - c, HY - c, z], [HC + c, HY - a, z + t], "horn"),
            b.box(f"step_{i}_west", [HC - c, HY - a, z], [HC - a, HY + a, z + t], "horn"),
            b.box(f"step_{i}_east", [HC + a, HY - a, z], [HC + c, HY + a, z + t], "horn"),
        ]
    return els


def horn_builder():
    return Builder(HORNREG, "radiocraft:block/loudspeaker", 32)


# ----------------------------------------------------------------

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--preview", help="write a Blockbench preview under this directory")
    ap.add_argument("--planks", help="a folder of vanilla <wood>_planks.png for the preview")
    args = ap.parse_args()

    paint_desk()
    paint_large()
    if "wall_speaker.png" not in HAND_EDITED:
        paint_wall()
    for name, palette in COLOURWAYS.items():
        paint_premium(f"premium_speaker_{name}", palette)
    paint_horn()

    items = []
    for i, wood in enumerate(WOODS):
        b = desk_builder(wood)
        m = b.model(desk_elements(b))
        m["textures"]["particle"] = f"minecraft:block/{wood}_planks"
        save(MODELS, f"desk_speaker_{wood}", m)
        items.append((m["textures"], desk_elements(b), (12 * i, 0, -32)))
    builders = [("large_speaker", large_builder(), large_elements), ("wall_speaker", wall_builder(), wall_elements)]
    builders += [(f"premium_speaker_{n}", premium_builder(f"premium_speaker_{n}"), premium_elements) for n in COLOURWAYS]
    builders.append(("loudspeaker", horn_builder(), horn_elements))
    for i, (name, b, fn) in enumerate(builders):
        m = b.model(fn(b))
        if f"{name}.json" in HAND_EDITED:
            print(f"kept {name}.json (edited by hand)")
        else:
            save(MODELS, name, m)
        items.append((m["textures"], fn(b), (24 * i, 0, 0)))
    if args.preview:
        write_scene(args.preview, "radiocraft", TEXTURES, items, size=64,
                    sources={"minecraft": Path(args.planks)} if args.planks else None)


if __name__ == "__main__":
    main()
