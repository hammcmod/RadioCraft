"""Builds the in-game models from the maintainer's Blockbench exports, which stay as they are.

    python tools/build_models.py

Blockbench writes its export over the same file every time it saves, so nothing is edited in place: each entry in
BUILDS reads an export and writes the models the game uses under other names. Run it again after every re-export.

What it does on the way:
- Splits a two-block export (y 0 to 32) into a lower and an upper model at y = 16, and keeps the whole one for the item.
- Cuts a model bigger than Minecraft allows into one model per block cell (the microwave horn, 2 x 4 x 2). These are
  read straight from the .bbmodel in doc/models, as no export can hold them: open it, edit, save, run this.
- Splits named lamps into their own full-bright models (the H19's screens), and gives named valves a soft glow.
- Turns a model drawn facing the wrong way (see rotate_model.py).
- Makes rotations legal for Minecraft 1.21.1, which takes one axis and -45, -22.5, 0, 22.5 or 45 degrees only.
  Blockbench's newer x/y/z form becomes axis and angle; a turn 180 degrees past a legal one is rewritten exactly;
  a slight tilt (5 degrees or less) is flattened, with a warning. Anything else stops the build: fix it in Blockbench.
"""
import copy
import itertools
import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from rotate_model import turn_element, SIDES  # noqa: E402

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src/main/resources/assets/radiocraft"
MODELS = ASSETS / "models"
LEGAL = (0.0, 22.5, -22.5, 45.0, -45.0)
FULL_BRIGHT = {"block_light": 15, "sky_light": 15, "ambient_occlusion": False}
VALVE_GLOW = {"block_light": 10, "sky_light": 10, "ambient_occlusion": False}

BUILDS = [
    {   # The H19 APRS terminal: a body and three screens, one lit at a time by the blockstate.
        "export": "block/aprs_h19.json",
        "body": "block/aprs_h19_body",
        "lamps": {"MSG": "block/aprs_h19_msg", "CONTACT": "block/aprs_h19_contact", "READY": "block/aprs_h19_ready"},
    },
    {   # The ionosonde antenna, two blocks tall.
        "export": "block/ionosonde.json",
        "lower": "block/ionosonde_lower", "upper": "block/ionosonde_upper", "item": "item/ionosonde",
        "textures": {"0": "radiocraft:block/ionosonde_bottom", "1": "radiocraft:block/ionosonde_top"},
        "render_type": {"lower": "minecraft:cutout", "upper": "minecraft:cutout"},
    },
    {   # The Hustler 5-BTV, seven blocks tall, one export per block; the item shows the base.
        "exports": [f"block/5_btv_antenna_{i}.json" for i in range(1, 8)],
        "segments": [f"block/5btv_antenna_segment_{i}" for i in range(1, 8)],
        "item": "item/5btv_antenna",
    },
    {   # The ionospheric processor, two blocks tall, turned to face the player, valves glowing through glass.
        "export": "block/ionospheric_solar_processor_bottom.json",
        "lower": "block/ionospheric_processor_lower", "upper": "block/ionospheric_processor_upper", "item": "item/ionospheric_processor",
        # Blockbench calls slot 0 "top" and slot 1 "bottom", but slot 0 is painted on the bottom half and the files are
        # named for the half they cover.
        "textures": {"0": "radiocraft:block/ionospheric_solar_processor_bottom", "1": "radiocraft:block/ionospheric_solar_processor_top"},
        "turn": 270,  # Turned twice, by the maintainer: 180 then 90 more clockwise.
        "glow": "TUBE",  # And the thin filaments inside the tubes.
        "render_type": {"lower": "minecraft:solid", "upper": "minecraft:translucent"},  # Only the valve glass is see-through.
    },
    {   # The tower foothold: one export with both rungs in its Breakable group, split into the whole foothold and the
        # broken one (the rung bent down), picked by the blockstate.
        "export": "block/tower_footholds.json",
        "variants": {"block/tower_foothold": "after_broken", "block/tower_foothold_broken": "before_broken"},
        "render_type": "minecraft:cutout",
    },
    {   # The AT&T Long Lines microwave horn, 2 by 2 blocks and 4 tall, the mouth facing north. One model per block,
        # microwave_horn_<x>_<y>_<z>; the whole horn only exists in the .bbmodel.
        "bbmodel": "doc/models/microwave_horn.bbmodel",
        "cells": "block/microwave_horn_{x}_{y}_{z}", "grid": (2, 4, 2),
        "textures": {"0": "radiocraft:block/microwave_horn"},
        "item": "item/microwave_horn",  # the whole horn at half size, which fits in one model
    },
]

# How a two-block-tall item sits in the inventory, hand and frames: vanilla's block views at half size, moved down so
# the model's middle (8 pixels higher than a block's) stays centred.
TALL_DISPLAY = {
    "gui": {"rotation": [30, 225, 0], "translation": [0, -2.5, 0], "scale": [0.3125, 0.3125, 0.3125]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 1, 0], "scale": [0.125, 0.125, 0.125]},
    "fixed": {"rotation": [0, 0, 0], "translation": [0, -4, 0], "scale": [0.25, 0.25, 0.25]},
    "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 1, 0], "scale": [0.1875, 0.1875, 0.1875]},
    "thirdperson_lefthand": {"rotation": [75, 45, 0], "translation": [0, 1, 0], "scale": [0.1875, 0.1875, 0.1875]},
    "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, -1, 0], "scale": [0.2, 0.2, 0.2]},
    "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, -1, 0], "scale": [0.2, 0.2, 0.2]},
}


def warn(text):
    print("  warning: " + text)


def legalize(element, where):
    """Puts an element's rotation in the form Minecraft 1.21.1 reads, or stops with what to fix in Blockbench."""
    rotation = element.get("rotation")
    if not rotation:
        return
    if "axis" not in rotation:  # Blockbench's newer {x, y, z} form.
        turned = {axis: rotation.get(axis, 0) for axis in "xyz" if rotation.get(axis, 0)}
        if len(turned) > 1:
            sys.exit(f"{where}: rotated on {', '.join(turned)} at once; Minecraft 1.21.1 takes one axis. Fix it in Blockbench.")
        axis, angle = next(iter(turned.items())) if turned else ("y", 0)
        rotation = {"angle": angle, "axis": axis, "origin": rotation.get("origin", [8, 8, 8])}
        element["rotation"] = rotation
    angle = float(rotation["angle"])
    if angle in LEGAL:
        return
    if rotation["axis"] == "y" and angle % 90 == 0:
        # Minecraft turns positive angles anticlockwise seen from above; turn_element turns clockwise.
        pivot = rotation["origin"]
        turn_element(element, int(-angle // 90) % 4, (pivot[0], pivot[2]))
        rotation["angle"] = 0.0
        return
    if rotation["axis"] == "y":
        for shift in (180.0, -180.0):
            if angle - shift in LEGAL:  # The same box turned 180 degrees about its pivot first.
                pivot = rotation["origin"]
                turn_element(element, 2, (pivot[0], pivot[2]))
                rotation["angle"] = angle - shift
                return
    if abs(angle) <= 5.0:
        warn(f"{where}: a {angle} degree tilt on {rotation['axis']} is flattened to 0 (Minecraft 1.21.1 cannot show it)")
        rotation["angle"] = 0.0
        return
    sys.exit(f"{where}: rotated {angle} degrees on {rotation['axis']}; Minecraft 1.21.1 takes only -45, -22.5, 0, 22.5 or 45. Fix it in Blockbench.")


def glows(element, name):
    if (element.get("name") or "").upper() == name:
        return True
    size = [element["to"][i] - element["from"][i] for i in range(3)]
    return size[0] <= 0.5 and size[2] <= 0.5  # A filament inside a valve.


def write(path, model):
    (MODELS / f"{path}.json").write_text(json.dumps(model, indent="\t") + "\n", encoding="utf-8")


def base(export, elements, **extra):
    model = {"credit": export.get("credit", "Made with Blockbench"), "parent": "block/block"}
    model.update(extra)
    model["texture_size"] = export.get("texture_size", [16, 16])
    model["textures"] = copy.deepcopy(export["textures"])
    model["elements"] = elements
    return model


def build_lamps(spec, export, elements):
    lamps = {name.upper(): [] for name in spec["lamps"]}
    body = []
    for element in elements:
        name = (element.get("name") or "").upper()
        (lamps[name] if name in lamps else body).append(element)
    for name, lit in lamps.items():
        if not lit:
            sys.exit(f"{spec['export']}: no element named {name}")
        for element in lit:
            element["faces"] = {"north": element["faces"]["north"]}  # The front is all that shows over the screen.
            element["neoforge_data"] = dict(FULL_BRIGHT)
        write(spec["lamps"][name], base(export, lit, render_type="minecraft:cutout", ambientocclusion=False))
    model = base(export, body)
    if "display" in export:
        model["display"] = export["display"]
    write(spec["body"], model)
    print(f"  {spec['body']}: {len(body)} elements; lamps {', '.join(lamps)}")


def build_tall(spec, export, elements):
    lower, upper = [], []
    for element in elements:
        if spec.get("glow") and element["from"][1] >= 16 and glows(element, spec["glow"]):
            element["neoforge_data"] = dict(VALVE_GLOW)
        (upper if element["from"][1] >= 15.5 else lower).append(element)
    textures = dict(spec["textures"])  # Texture slot to file, set here rather than trusting the export's names.
    used = {face["texture"].lstrip("#") for element in lower + upper for face in element["faces"].values()}
    if not used <= set(textures):
        sys.exit(f"{spec['export']}: texture slots {sorted(used - set(textures))} have no file in BUILDS")
    first = {half: next(iter(sorted({face["texture"].lstrip("#") for element in part for face in element["faces"].values()})), "0")
             for half, part in (("lower", lower), ("upper", upper))}
    item_elements = copy.deepcopy(lower + upper)
    for element in upper:
        element["from"][1] -= 16
        element["to"][1] -= 16
        if "origin" in element.get("rotation", {}):
            element["rotation"]["origin"][1] -= 16
    for half, part in (("lower", lower), ("upper", upper)):
        model = base(export, part, render_type=spec["render_type"][half])
        model["textures"] = dict(textures, particle=textures[first[half]])
        write(spec[half], model)
    display = copy.deepcopy(TALL_DISPLAY)
    item = {"credit": export.get("credit", "Made with Blockbench"), "render_type": spec["render_type"]["upper"], "texture_size": export.get("texture_size", [16, 16]),
            "textures": dict(textures, particle=textures[first["lower"]]), "elements": item_elements, "display": display}
    write(spec["item"], item)
    print(f"  {spec['lower']}: {len(lower)} elements, {spec['upper']}: {len(upper)}, {spec['item']}")


def build_variants(spec, export, elements):
    """One model per variant, each leaving out the elements with one name."""
    for out, left_out in spec["variants"].items():
        kept = [copy.deepcopy(e) for e in elements if (e.get("name") or "") != left_out]
        if len(kept) == len(elements):
            sys.exit(f"{spec['export']}: no element named {left_out}")
        write(out, base(export, kept, render_type=spec["render_type"]))
        print(f"  {out}: {len(kept)} elements (without {left_out})")


def build_stack(spec):
    for export_path, out in zip(spec["exports"], spec["segments"]):
        source = MODELS / export_path
        export = json.loads(source.read_text(encoding="utf-8"))
        elements = copy.deepcopy(export["elements"])
        for i, element in enumerate(elements):
            legalize(element, f"{export_path} element {i} ({element.get('name') or 'unnamed'})")
        model = base(export, elements, render_type="minecraft:cutout")
        write(out, model)
    write(spec["item"], {"parent": f"radiocraft:{spec['segments'][0]}"})
    print(f"  {len(spec['segments'])} segments, {spec['item']}")


# For each face: the axis its u runs along and the axis its v runs along, each with whether it starts from the box's
# high end (Minecraft's default UVs: north's u runs from east to west, every side's v from top to bottom).
FACE_AXES = {"north": ((0, True), (1, True)), "south": ((0, False), (1, True)),
             "east": ((2, True), (1, True)), "west": ((2, False), (1, True)),
             "up": ((0, False), (2, False)), "down": ((0, False), (2, True))}
# The axis each face lies across, and whether it is the box's high end there.
FACE_PLANE = {"west": (0, False), "east": (0, True), "down": (1, False), "up": (1, True), "north": (2, False), "south": (2, True)}


def from_bbmodel(path):
    """A Blockbench project's cubes as Minecraft elements (UVs in 0-16 units, rotations still in the x/y/z form)."""
    project = json.loads(path.read_text(encoding="utf-8"))
    resolution = project.get("resolution", {"width": 16, "height": 16})
    keys, scales = {}, {}
    for i, texture in enumerate(project.get("textures", [])):
        scale = (16 / texture.get("uv_width", resolution["width"]), 16 / texture.get("uv_height", resolution["height"]))
        for ref in (i, texture.get("uuid")):  # Faces name their texture by index or, in newer projects, by uuid.
            keys[ref], scales[ref] = str(texture.get("id", i)), scale
    elements = []
    for cube in project["elements"]:
        where = f"{path.name} element {cube.get('name') or 'unnamed'}"
        if cube.get("type", "cube") != "cube":
            warn(f"{where}: a {cube['type']}, not a cube; Minecraft cannot show it, so it is left out")
            continue
        if cube.get("export") is False:
            continue
        inflate = cube.get("inflate", 0)
        element = {"name": cube.get("name", ""), "from": [c - inflate for c in cube["from"]], "to": [c + inflate for c in cube["to"]]}
        turned = {axis: angle for axis, angle in zip("xyz", cube.get("rotation", [0, 0, 0])) if angle}
        if turned:
            element["rotation"] = dict(turned, origin=list(cube.get("origin", [8, 8, 8])))
        if cube.get("shade") is False:
            element["shade"] = False
        if cube.get("light_emission"):
            element["light_emission"] = cube["light_emission"]
        faces = {}
        for side, face in cube.get("faces", {}).items():
            ref = face.get("texture")
            if ref is None or ref is False:
                continue  # A face with no texture is left off, as Blockbench's export does.
            if ref not in keys:
                sys.exit(f"{where}: the {side} face uses a texture the project does not have")
            su, sv = scales[ref]
            u0, v0, u1, v1 = face["uv"]
            out = {"uv": [round(u0 * su, 4), round(v0 * sv, 4), round(u1 * su, 4), round(v1 * sv, 4)], "texture": "#" + keys[ref]}
            if face.get("rotation"):
                out["rotation"] = face["rotation"]
            if face.get("cullface"):
                out["cullface"] = face["cullface"]
            faces[side] = out
        element["faces"] = faces
        elements.append(element)
    return project, elements


def crop_uv(uv, rotation, s, t):
    """The part of a face's UV rectangle under the fractions s (along u) and t (along v) of the face, the face's own
    rotation taken into account (Minecraft hands each corner the UV of the next corner round per 90 degrees)."""
    (s0, s1), (t0, t1) = s, t
    for _ in range(rotation // 90 % 4):
        (s0, s1), (t0, t1) = (t0, t1), (1 - s1, 1 - s0)
    u0, v0, u1, v1 = uv
    return [round(u0 + (u1 - u0) * s0, 4), round(v0 + (v1 - v0) * t0, 4),
            round(u0 + (u1 - u0) * s1, 4), round(v0 + (v1 - v0) * t1, 4)]


def clip(element, low, high):
    """The part of an unrotated element between corners low and high, its faces cropped to match. A face where the
    element was cut is left off: it was inside the whole box and could never be seen."""
    frm, to = element["from"], element["to"]
    cut_from = [max(frm[i], low[i]) for i in range(3)]
    cut_to = [min(to[i], high[i]) for i in range(3)]

    def fractions(axis, from_high):
        size = to[axis] - frm[axis]
        if size == 0:
            return 0.0, 1.0
        a, b = ((to[axis] - c) / size if from_high else (c - frm[axis]) / size for c in (cut_from[axis], cut_to[axis]))
        return min(a, b), max(a, b)

    piece = copy.deepcopy(element)
    piece["from"], piece["to"] = cut_from, cut_to
    faces = {}
    for side, face in element["faces"].items():
        axis, high_end = FACE_PLANE[side]
        if (cut_to if high_end else cut_from)[axis] != (to if high_end else frm)[axis]:
            continue
        (ua, uh), (va, vh) = FACE_AXES[side]
        faces[side] = dict(face, uv=crop_uv(face["uv"], face.get("rotation", 0), fractions(ua, uh), fractions(va, vh)))
    piece["faces"] = faces
    return piece


def shift(element, offset):
    element["from"] = [element["from"][i] + offset[i] for i in range(3)]
    element["to"] = [element["to"][i] + offset[i] for i in range(3)]
    if "origin" in element.get("rotation", {}):
        element["rotation"]["origin"] = [element["rotation"]["origin"][i] + offset[i] for i in range(3)]
    return element


def build_cells(spec):
    """Cuts a .bbmodel bigger than one model may be into one model per block cell, each in its own 0-16 space.

    Unrotated cubes are cut at the block edges. A rotated cube cannot be, so it goes whole to the cell holding its
    middle, which works while it reaches no more than a block outside that cell (Minecraft's -16 to 32 limit).
    """
    source = ROOT / spec["bbmodel"]
    print(spec["bbmodel"])
    if not source.exists():
        warn("no .bbmodel yet; skipped")
        return
    project, elements = from_bbmodel(source)
    grid = spec["grid"]

    def cell_of(c, n):
        return min(max(int(c // 16), 0), n - 1)

    cells = {cell: [] for cell in itertools.product(*(range(n) for n in grid))}
    for element in elements:
        where = f"{spec['bbmodel']} element {element.get('name') or 'unnamed'}"
        legalize(element, where)
        if element.get("rotation", {}).get("angle", 0):
            middle = [(element["from"][i] + element["to"][i]) / 2 for i in range(3)]
            cell = tuple(cell_of(middle[i], n) for i, n in enumerate(grid))
            piece = shift(copy.deepcopy(element), [-16 * c for c in cell])
            if any(c < -16 or c > 32 for c in piece["from"] + piece["to"]):
                sys.exit(f"{where}: rotated, so it cannot be cut at the block edges, and it reaches more than a block "
                         f"outside the block it is in. Make it smaller or cut it in two in Blockbench.")
            cells[cell].append(piece)
            continue
        ranges = []
        for i, n in enumerate(grid):
            lo, hi = element["from"][i], element["to"][i]
            ranges.append([cell_of(lo, n)] if hi == lo else [c for c in range(n) if lo < 16 * c + 16 and hi > 16 * c])
        if not all(ranges):
            warn(f"{where}: outside the {' x '.join(map(str, grid))} blocks; left out")
        for cell in itertools.product(*ranges):
            low = [16 * c for c in cell]
            cells[cell].append(shift(clip(element, low, [c + 16 for c in low]), [-c for c in low]))
    textures = dict(spec["textures"])
    used = {face["texture"].lstrip("#") for part in cells.values() for element in part for face in element["faces"].values()}
    if not used <= set(textures):
        sys.exit(f"{spec['bbmodel']}: texture slots {sorted(used - set(textures))} have no file in BUILDS")
    resolution = project.get("resolution", {"width": 16, "height": 16})
    for (x, y, z), part in cells.items():
        write(spec["cells"].format(x=x, y=y, z=z),
              {"credit": "RadioCraft", "parent": "block/block", "texture_size": [resolution["width"], resolution["height"]],
               "textures": dict(textures, particle=next(iter(textures.values()))), "elements": part})
    print(f"  {len(cells)} cells, {sum(map(len, cells.values()))} elements from {len(elements)} cubes")
    if spec.get("item"):  # The whole model at half size: 2 x 4 x 2 blocks shrink to 1 x 2 x 1, inside the -16 to 32 limit.
        half = []
        for element in elements:
            element = copy.deepcopy(element)
            element["from"] = [round(v / 2, 4) for v in element["from"]]
            element["to"] = [round(v / 2, 4) for v in element["to"]]
            if "origin" in element.get("rotation", {}):
                element["rotation"]["origin"] = [round(v / 2, 4) for v in element["rotation"]["origin"]]
            half.append(element)
        write(spec["item"], {"credit": "RadioCraft", "texture_size": [resolution["width"], resolution["height"]],
                             "textures": dict(textures, particle=next(iter(textures.values()))), "elements": half,
                             "display": TALL_DISPLAY})
        print(f"  {spec['item']}: {len(half)} elements at half size")


def main():
    for spec in BUILDS:
        if "exports" in spec:
            print(spec["exports"][0] + " ...")
            build_stack(spec)
            continue
        if "bbmodel" in spec:
            build_cells(spec)
            continue
        source = MODELS / spec["export"]
        print(spec["export"])
        if not source.exists():
            warn("no export yet; skipped")
            continue
        export = json.loads(source.read_text(encoding="utf-8"))
        elements = copy.deepcopy(export["elements"])
        for i, element in enumerate(elements):
            legalize(element, f"{spec['export']} element {i} ({element.get('name') or 'unnamed'})")
            if spec.get("turn"):
                turn_element(element, spec["turn"] // 90)
        if "lamps" in spec:
            build_lamps(spec, export, elements)
        elif "variants" in spec:
            build_variants(spec, export, elements)
        else:
            build_tall(spec, export, elements)


if __name__ == "__main__":
    main()
