"""Generates the wire part models for every cable kind, the dynamic block models, the blockstates and the wire items.

Model coordinate convention: every edge model is authored for the NORTH edge and rotated in the blockstate. An
edge run straddles the block face (z = -w/2..w/2): half inside the wall it hugs (hidden) and half outside, so the
run in the block above a wall, which hugs the same face from the other side, lines up with it exactly. Pieces may
reach outside the 0..16 cube, which Minecraft renders fine.

Cable width: power wire (plain, waterproof and the sixteen dyed ones) is 1 px square, coax 2 px square (2026-10-27,
maintainer: thin hookup wire against fat RG-8, so the two kinds tell apart even when both are dyed, and seven 1 px
strands fit in a bundle with a pixel between them). The texture is the same 16 x 16 layout for both: the jacket's
four sides are 7 x 2 strips at rows 0, 2, 4 and 6 (a 1 px cable samples the second row of each, the dyed stripe row
of tools/coax_colours.py), the end cap is at (7, 0), the middle cube's faces fill x 12..16, y 0..6.
"""
import json
import os

BASE = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "radiocraft") + os.sep

DYES = ["white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray", "cyan", "purple",
        "blue", "brown", "green", "red", "black"]

# Model prefix -> (texture, cable width in pixels).
VARIANTS = {"wire": ("radiocraft:block/wire", 1), "coax_wire": ("radiocraft:block/coaxial_wire", 2),
            "data_wire": ("radiocraft:block/data_wire", 1)}
for dye in DYES:
    VARIANTS[f"wire_{dye}"] = (f"radiocraft:block/wire_{dye}", 1)
    VARIANTS[f"coax_wire_{dye}"] = (f"radiocraft:block/coaxial_wire_{dye}", 2)

# Blockstate name -> model prefix (the waterproof wire shares the plain wire's models).
BLOCKSTATES = {"wire": "wire", "waterproof_wire": "wire", "coax_wire": "coax_wire", "data_wire": "data_wire"}
for dye in DYES:
    BLOCKSTATES[f"wire_{dye}"] = f"wire_{dye}"
    BLOCKSTATES[f"coax_wire_{dye}"] = f"coax_wire_{dye}"


def tex(texture):
    return {"0": texture, "particle": texture}


def strip(row, w):
    """UV of a side strip at texture row pair `row` (0, 2, 4, 6): the whole 7 x 2 for a 2 px cable, its second row for 1 px."""
    return [0, row + (2 - w), 7, row + 2]


def cap(w):
    """UV of the end cap, w x w at (7, 0)."""
    return [7, 0, 7 + w, w]


def along_faces(w, up=False, down=False, north=True, south=True):
    """Faces of a piece running along z (north-south), authored for the north edge, turned for a vertical run."""
    f = {
        "east": {"uv": strip(2, w), "rotation": 270, "texture": "#0"},
        "west": {"uv": strip(6, w), "rotation": 90, "texture": "#0"},
    }
    if north:
        f["north"] = {"uv": strip(4, w), "rotation": 270, "texture": "#0"}
    if south:
        f["south"] = {"uv": strip(0, w), "rotation": 90, "texture": "#0"}
    if up:
        f["up"] = {"uv": cap(w), "rotation": 180, "texture": "#0"}
    if down:
        f["down"] = {"uv": cap(w), "texture": "#0"}
    return f


def flat_faces(w, north=True, south=True):
    """Faces of a flat piece lying along z on the floor."""
    f = {
        "east": {"uv": strip(2, w), "texture": "#0"},
        "west": {"uv": strip(6, w), "texture": "#0"},
        "up": {"uv": strip(0, w), "rotation": 90, "texture": "#0"},
        "down": {"uv": strip(4, w), "rotation": 90, "texture": "#0"},
    }
    if north:
        f["north"] = {"uv": cap(w), "texture": "#0"}
    if south:
        f["south"] = {"uv": cap(w), "texture": "#0"}
    return f


def box(x0, y0, z0, x1, y1, z1, faces, name="wire"):
    return {"name": name, "from": [x0, y0, z0], "to": [x1, y1, z1], "faces": faces}


def model(credit, texture, elements):
    return {"credit": credit, "parent": "block/block", "textures": tex(texture), "elements": elements}


def parts(texture, w):
    """Every part model for a cable `w` pixels square, keyed by part suffix (see WireLook.Part)."""
    lo, hi = 8 - w / 2, 8 + w / 2
    if w % 2 == 0:
        lo, hi = int(lo), int(hi)
    half = w / 2
    cube_uv = {  # The middle cube's six faces, from the texture's right-hand block (x 12..16).
        "north": [12, 4, 12 + w, 4 + w], "east": [14, 4, 14 + w, 4 + w], "south": [12, 2, 12 + w, 2 + w],
        "west": [14, 2, 14 + w, 2 + w], "up": [12, 0, 12 + w, w], "down": [14, 0, 14 + w, w],
    }
    middle_faces = {side: {"uv": uv, "texture": "#0"} for side, uv in cube_uv.items()}
    mid = (2 + 16) / 2 if w == 2 else 9
    return {
        "middle": model("The cube in the middle of the block that the stubs meet", texture, [box(lo, 0, lo, hi, w, hi, middle_faces)]),
        "horizontal": model("Stub from the north face to the middle", texture, [box(lo, 0, 0, hi, w, lo, flat_faces(w, south=False))]),
        "vertical": model("Run up the middle of the block from the middle cube to the ceiling", texture, [
            box(lo, w, lo, hi, mid, hi, along_faces(w)), box(lo, mid, lo, hi, 16, hi, along_faces(w, up=True))]),
        "edge": model("Wire running up the north edge of the block, from the floor stub to the ceiling", texture, [
            box(lo, w, -half, hi, (w + 16) / 2, half, along_faces(w)), box(lo, (w + 16) / 2, -half, hi, 16, half, along_faces(w, up=True))]),
        "edge_full": model("Wire running up the north edge of the block, floor to ceiling (a run passing through)", texture, [
            box(lo, 0, -half, hi, 8, half, along_faces(w, down=True)), box(lo, 8, -half, hi, 16, half, along_faces(w, up=True))]),
        "corner": model("Corner on the north edge: joins the top of a run coming up that edge to the neighbour's stub", texture, [
            box(lo, 0, -half, hi, w, half, {s: {"uv": cap(w), "texture": "#0"} for s in ("north", "south", "east", "west", "up", "down")})]),
        "climb_overhang": model("Climb up the north face ending at a device: the cable runs on over the wall's top to the device's middle", texture, [
            box(lo, 16, -7, hi, 16 + w, half, flat_faces(w))]),
        "up_overhang": model("Run up the north edge ending at a device above: the cable turns in to the device's middle", texture, [
            box(lo, 16, -half, hi, 16 + w, lo, flat_faces(w))]),
    }


def item_model(prefix, w):
    """The held/inventory item: a cross of four stubs round the middle, at the block's centre height, as the old hand-made wire.json."""
    lo, hi = 8 - w / 2, 8 + w / 2
    if w % 2 == 0:
        lo, hi = int(lo), int(hi)
    y0, y1 = 8 - w / 2, 8 + w / 2
    if w % 2 == 0:
        y0, y1 = int(y0), int(y1)
    cube_uv = {"north": [12, 4, 12 + w, 4 + w], "east": [14, 4, 14 + w, 4 + w], "south": [12, 2, 12 + w, 2 + w],
               "west": [14, 2, 14 + w, 2 + w], "up": [12, 0, 12 + w, w], "down": [14, 0, 14 + w, w]}
    elements = [box(lo, y0, lo, hi, y1, hi, {s: {"uv": uv, "texture": "#0"} for s, uv in cube_uv.items()})]
    along_z = lambda n, s: {"east": {"uv": strip(2, w), "texture": "#0"}, "west": {"uv": strip(6, w), "texture": "#0"},
                            "up": {"uv": strip(0, w), "rotation": 90, "texture": "#0"}, "down": {"uv": strip(4, w), "rotation": 90, "texture": "#0"},
                            **({"north": {"uv": cap(w), "texture": "#0"}} if n else {}), **({"south": {"uv": cap(w), "texture": "#0"}} if s else {})}
    along_x = lambda e, wst: {"north": {"uv": strip(6, w), "texture": "#0"}, "south": {"uv": strip(2, w), "texture": "#0"},
                              "up": {"uv": strip(0, w), "texture": "#0"}, "down": {"uv": strip(4, w), "texture": "#0"},
                              **({"east": {"uv": cap(w), "texture": "#0"}} if e else {}), **({"west": {"uv": cap(w), "texture": "#0"}} if wst else {})}
    elements.append(box(lo, y0, 0, hi, y1, lo, along_z(True, False)))
    elements.append(box(lo, y0, hi, hi, y1, 16, along_z(False, True)))
    elements.append(box(hi, y0, lo, 16, y1, hi, along_x(True, False)))
    elements.append(box(0, y0, lo, lo, y1, hi, along_x(False, True)))
    return {"parent": f"radiocraft:block/{prefix}_middle", "elements": elements}


def dump(obj, path, indent="\t"):
    with open(f"{BASE}{path}", "w", newline="\n") as f:
        json.dump(obj, f, indent=indent)


written = 0
for prefix, (texture, w) in VARIANTS.items():
    for suffix, m in parts(texture, w).items():
        dump(m, f"models/block/{prefix}_{suffix}.json")
        written += 1
    # The block model: one dynamic model per texture that picks the parts above in code (client/model/WireModel.java,
    # rule in common/blocks/WireLook.java).
    dump({"loader": "radiocraft:wire", "prefix": f"radiocraft:block/{prefix}_"}, f"models/block/{prefix}_dynamic.json")
    if w == 1:  # Wire items are the 3D cross; coax items are the maintainer's 2D icons (tools/coax_colours.py).
        dump(item_model(prefix, w), f"models/item/{prefix}.json", indent=2)

# A blockstate with a single variant for every state.
for name, prefix in BLOCKSTATES.items():
    dump({"variants": {"": {"model": f"radiocraft:block/{prefix}_dynamic"}}}, f"blockstates/{name}.json", indent=2)
dump({"parent": "radiocraft:item/wire"}, "models/item/waterproof_wire.json", indent=2)
# The wire bundle (several wires in one block, common/blocks/WireBundleBlock.java) draws every strand with its own
# kind's parts; its model only names the loader.
dump({"loader": "radiocraft:wire", "bundle": True}, "models/block/wire_bundle_dynamic.json")
dump({"variants": {"": {"model": "radiocraft:block/wire_bundle_dynamic"}}}, "blockstates/wire_bundle.json", indent=2)
print(f"{written} part models for {len(VARIANTS)} cable kinds; dynamic models, blockstates and wire items written")
