"""Writes the tower beam block models: three thicknesses of steel lattice beam that connect to any touching beam,
including the diagonal (kiddie-corner) ones.

    python tools/tower_beam_models.py [--preview DIR]

Each thickness is a hub-and-arms part set, so the mod can show any combination of touching neighbours:

- <beam>_core.json            a cube at the block centre, always drawn
- <beam>_arm_<direction>.json one arm per neighbour position (26: 6 faces, 12 edges, 8 corners), from the core to the
                              block boundary, where it meets the neighbour's matching arm end to end
- <beam>.json                 the beam straight up (core + up + down), for the inventory and a lone block

Directions are named up/down, north/south, east/west and joined in that order: `up`, `north_east`, `up_north_east`.
Face arms are plain boxes. Edge arms (two components, such as north_east or up_east) are boxes rotated 45 degrees
about the third axis at the block centre; they run 8*sqrt(2) units to the middle of the shared block edge and so poke
past the 16-unit boundary, which is legal (elements may reach -16..32). Corner arms (up_north_east and the like) lie
along a body diagonal that no single-axis rotation reaches, so they are stair-stepped: three cubes two units apart
along the diagonal, the last one overlapping the neighbour's first.

The parts are meant for a dynamic baked model (or a block entity's ModelData) that reads the 26 neighbours: 26
boolean blockstate properties would be 2^26 states, which Minecraft cannot register.

The texture is an opaque 16-pixel tile in the iron bars palette: light and shadow edge bars, a centre bar, and
staggered rails in the gaps between. It repeats every 8 pixels, so every face UV is the bars across (u) by the length
along the beam (v), rotated 90 degrees on faces whose length runs sideways.
"""
import argparse
import math
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from blockmodel import Painter, save, write_scene  # noqa: E402

ROOT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/radiocraft"
MODELS = ROOT / "models/block"
TEXTURES = ROOT / "textures/block"

SIZES = {"tower_beam_thick": 8, "tower_beam": 6, "tower_beam_thin": 4}
C = 8                           # the block centre
EDGE_LEN = 8 * math.sqrt(2)     # centre to the middle of a block edge
STEP = 2                        # the stair step of a corner arm

DIRS = [(x, y, z) for y in (1, 0, -1) for z in (-1, 0, 1) for x in (1, 0, -1) if (x, y, z) != (0, 0, 0)]
AXES = "xyz"

# iron bars palette
BAR_LIGHT = (171, 172, 171)
BAR_BRIGHT = (197, 197, 197)
BAR_MID = (156, 156, 156)
BAR_SHADOW = (114, 121, 110)
BAR_DARK = (104, 104, 104)
GAP = (64, 66, 62)


def dir_name(d):
    x, y, z = d
    parts = ("up" if y > 0 else "down" if y < 0 else "",
             "north" if z < 0 else "south" if z > 0 else "",
             "east" if x > 0 else "west" if x < 0 else "")
    return "_".join(p for p in parts if p)


def rotate(v, axis, angle):
    """Right-handed rotation of a vector about a model axis, as Minecraft and Blockbench apply element rotations."""
    s, c = math.sin(math.radians(angle)), math.cos(math.radians(angle))
    x, y, z = v
    if axis == "x":
        return (x, c * y - s * z, s * y + c * z)
    if axis == "y":
        return (c * x + s * z, y, -s * x + c * z)
    return (c * x - s * y, s * x + c * y, z)


def shifted(a, b):
    """Moves a UV range by multiples of the 8-pixel pattern period until it lies within 0..16."""
    while a < 0:
        a, b = a + 8, b + 8
    while b > 16 + 1e-6 and a - 8 >= 0:
        a, b = a - 8, b - 8
    assert 0 <= a and b <= 16 + 1e-6, (a, b)
    return round(a, 3), round(b, 3)


def beam_box(name, frm, to, t, length_axis, rotation=None):
    """A box of the beam whose faces show the bars across the cross-section and the rails along `length_axis`."""
    half = t / 2
    cross = [C - half, C - half, C + half, C + half]
    a, b = frm[AXES.index(length_axis)], to[AXES.index(length_axis)]
    v0, v1 = shifted(16 - b, 16 - a)
    normals = {"north": "z", "south": "z", "east": "x", "west": "x", "up": "y", "down": "y"}
    faces = {}
    for face, n in normals.items():
        if n == length_axis:                       # an end face: cross-section both ways
            faces[face] = {"uv": cross, "texture": "#0"}
        elif length_axis == "y" or (length_axis == "z" and n == "y"):
            faces[face] = {"uv": [C - half, v0, C + half, v1], "texture": "#0"}   # length runs down the face
        else:                                       # length runs across the face: turn the tile
            faces[face] = {"uv": [C - half, v0, C + half, v1], "texture": "#0", "rotation": 90}
    el = {"name": name, "from": list(frm), "to": list(to)}
    if rotation is not None:
        el["rotation"] = rotation
    el["faces"] = faces
    return el


def core(t):
    h = t / 2
    return [beam_box("core", [C - h] * 3, [C + h] * 3, t, "y")]


def arm(d, t):
    h = t / 2
    nz = [i for i in range(3) if d[i]]
    if len(nz) == 1:                                # a face arm: core face to block boundary
        i = nz[0]
        frm, to = [C - h] * 3, [C + h] * 3
        if d[i] > 0:
            frm[i], to[i] = C + h, 16
        else:
            frm[i], to[i] = 0, C - h
        return [beam_box("arm", frm, to, t, AXES[i])]
    if len(nz) == 2:                                # an edge arm: a box along one axis, turned 45 degrees
        target = tuple(v / math.sqrt(2) for v in d)
        axis = AXES[[i for i in range(3) if not d[i]][0]]
        for i in nz:
            for sign in (1, -1):
                e = [0, 0, 0]
                e[i] = sign
                for angle in (45, -45):
                    r = rotate(e, axis, angle)
                    if all(abs(r[k] - target[k]) < 1e-6 for k in range(3)):
                        frm, to = [C - h] * 3, [C + h] * 3
                        if sign > 0:
                            frm[i], to[i] = C, C + EDGE_LEN
                        else:
                            frm[i], to[i] = C - EDGE_LEN, C
                        frm, to = [round(v, 3) for v in frm], [round(v, 3) for v in to]
                        return [beam_box("arm", frm, to, t, AXES[i],
                                         {"angle": angle, "axis": axis, "origin": [C, C, C]})]
        raise AssertionError(d)
    boxes = []                                      # a corner arm: stair-stepped cubes along the body diagonal
    for k in (1, 2, 3):
        centre = [C + d[i] * STEP * k for i in range(3)]
        boxes.append(beam_box(f"arm_{k}", [c - h for c in centre], [c + h for c in centre], t, "y"))
    return boxes


def model(name, elements):
    return {"credit": "RadioCraft", "parent": "block/block",
            "textures": {"0": f"radiocraft:block/{name}", "particle": f"radiocraft:block/{name}"},
            "elements": elements}


def paint(name, t):
    """An 8-periodic tile: within the face's columns a light edge, a centre bar, a dark edge, rails in the gaps."""
    p = Painter({}, 16, seed=t)
    f0 = C - t // 2
    layouts = {8: ["L", "g1", "g1", "BL", "BD", "g2", "g2", "D"],
               6: ["L", "g1", "BL", "BD", "g2", "D"],
               4: ["L", "g1", "g2", "D"]}
    roles = {}
    for i, role in enumerate(layouts[t]):
        roles[(f0 + i) % 8] = role
    colours = {"L": BAR_LIGHT, "D": BAR_SHADOW, "BL": BAR_MID, "BD": BAR_DARK}
    for y in range(16):
        for x in range(16):
            role = roles.get(x % 8, "fill")
            if role in colours:
                c = colours[role]
                if role == "L" and p.rnd.random() < 0.2:
                    c = BAR_BRIGHT
            elif role == "g1" and y % 8 in (0, 1):
                c = BAR_MID if y % 8 == 0 else BAR_SHADOW
            elif role == "g2" and y % 8 in (4, 5):
                c = BAR_MID if y % 8 == 4 else BAR_SHADOW
            else:
                c = GAP
            j = p.rnd.randint(-4, 4)
            p.img[y][x] = tuple(max(0, min(255, v + j)) for v in c) + (255,)
    p.write(TEXTURES / f"{name}.png")


def parts(name, t):
    out = {f"{name}_core": core(t)}
    for d in DIRS:
        out[f"{name}_arm_{dir_name(d)}"] = arm(d, t)
    out[name] = core(t) + arm((0, 1, 0), t) + arm((0, -1, 0), t)
    return out


def cell(t, neighbours):
    """The elements of one block whose neighbours (direction tuples) are all beams."""
    els = core(t)
    for d in neighbours:
        for i, el in enumerate(arm(d, t)):
            el = dict(el)
            el["name"] = f"{dir_name(d)}_{i}"
            els.append(el)
    return els


def preview_items():
    """An urchin with all 26 arms and a 2x2x3 tower with every touching neighbour connected, per thickness."""
    items = []
    for row, (name, t) in enumerate(SIZES.items()):
        z = row * 56
        items.append(([name], cell(t, DIRS), (0, 0, z)))
        cells = {(x, y, zz) for x in (0, 1) for y in (0, 1, 2) for zz in (0, 1)}
        for (x, y, zz) in sorted(cells):
            touching = [d for d in DIRS if (x + d[0], y + d[1], zz + d[2]) in cells]
            items.append(([name], cell(t, touching), (56 + 16 * x, 16 * y, z + 16 * zz)))
    return items


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--preview", help="write a combined Blockbench preview under this directory")
    args = ap.parse_args()
    for name, t in SIZES.items():
        paint(name, t)
        for part, elements in parts(name, t).items():
            save(MODELS, part, model(name, elements))
    if args.preview:
        write_scene(args.preview, "radiocraft", TEXTURES, preview_items(), size=16)


if __name__ == "__main__":
    main()
