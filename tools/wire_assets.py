"""Generates the wire part models (both textures), the dynamic block models and the three wire blockstates.

Model coordinate convention: every edge model is authored for the NORTH edge and rotated in the blockstate. An
edge run straddles the block face (z = -1..1): one pixel inside the wall it hugs (hidden) and one outside, so the
run in the block above a wall, which hugs the same face from the other side, lines up with it exactly. Pieces may
reach outside the 0..16 cube, which Minecraft renders fine.
"""
import json

import os
BASE = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "radiocraft") + os.sep
TEX = {"0": "radiocraft:block/wire", "particle": "radiocraft:block/wire"}


def faces(up=False, down=False, north=True, south=True):
    f = {
        "east": {"uv": [0, 2, 7, 4], "rotation": 270, "texture": "#0"},
        "west": {"uv": [0, 6, 7, 8], "rotation": 90, "texture": "#0"},
    }
    if north:
        f["north"] = {"uv": [0, 4, 7, 6], "rotation": 270, "texture": "#0"}
    if south:
        f["south"] = {"uv": [0, 0, 7, 2], "rotation": 90, "texture": "#0"}
    if up:
        f["up"] = {"uv": [7, 0, 9, 2], "rotation": 180, "texture": "#0"}
    if down:
        f["down"] = {"uv": [7, 0, 9, 2], "texture": "#0"}
    return f


def edge_run(y0, credit):
    mid = (y0 + 16) // 2
    return {
        "credit": credit,
        "parent": "block/block",
        "textures": TEX,
        "elements": [
            {"name": "wire", "from": [7, y0, -1], "to": [9, mid, 1], "faces": faces(down=y0 == 0)},
            {"name": "wire", "from": [7, mid, -1], "to": [9, 16, 1], "faces": faces(up=True)},
        ],
    }


def flat_run(z0, z1, y, credit):
    """A horizontal piece along z (north-south) at height y, 2 px thick, with the right texture rotation."""
    return {
        "credit": credit,
        "parent": "block/block",
        "textures": TEX,
        "elements": [
            {"name": "wire", "from": [7, y, z0], "to": [9, y + 2, z1], "faces": {
                "north": {"uv": [7, 0, 9, 2], "texture": "#0"},
                "south": {"uv": [7, 0, 9, 2], "texture": "#0"},
                "east": {"uv": [0, 2, 7, 4], "texture": "#0"},
                "west": {"uv": [0, 6, 7, 8], "texture": "#0"},
                "up": {"uv": [0, 0, 7, 2], "rotation": 90, "texture": "#0"},
                "down": {"uv": [0, 4, 7, 6], "rotation": 90, "texture": "#0"}}}
        ],
    }


models = {
    "wire_edge": edge_run(2, "Wire running up the north edge of the block, from the floor stub to the ceiling"),
    "wire_edge_full": edge_run(0, "Wire running up the north edge of the block, floor to ceiling (a run passing through)"),
    # Top of a climb up the north face ending at a device: on over the wall's top to the middle of the block on top of it.
    "wire_climb_overhang": flat_run(-7, 1, 16, "Climb up the north face ending at a device: the cable runs on over the wall's top to the device's middle"),
    # Run up the north edge ending at a device directly above: in along that block's floor to its middle.
    "wire_corner": {
        "credit": "Corner on the north edge: joins the top of a run coming up that edge to the neighbour's stub",
        "parent": "block/block",
        "textures": TEX,
        "elements": [{"name": "wire", "from": [7, 0, -1], "to": [9, 2, 1], "faces": {
            f: {"uv": [7, 0, 9, 2], "texture": "#0"} for f in ("north", "south", "east", "west", "up", "down")}}],
    },
    "wire_up_overhang": flat_run(-1, 7, 16, "Run up the north edge ending at a device above: the cable turns in to the device's middle"),
}
# Each wire kind has its own texture; the models are identical otherwise. "wire_*" models are the power wire's
# (the waterproof wire shares them); "coax_wire_*" models use the coax texture the maintainer drew (2026-10-21).
VARIANTS = {"wire": "radiocraft:block/wire", "coax_wire": "radiocraft:block/coaxial_wire"}
# Dyed coax (tools/coax_colours.py paints the textures): the same parts with a coloured stripe. Coax only joins coax of
# its own colour or plain coax (WireBlock#joins).
DYES = ["white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray", "cyan", "purple",
        "blue", "brown", "green", "red", "black"]
for dye in DYES:
    VARIANTS[f"coax_wire_{dye}"] = f"radiocraft:block/coaxial_wire_{dye}"
BASE_MODELS = ["middle", "horizontal", "vertical"]  # hand-made in Blockbench for the power wire, copied for coax


def retextured(model, texture):
    out = json.loads(json.dumps(model))
    out["textures"] = {k: texture for k in out.get("textures", {})}
    return out


def dump(model, name):
    json.dump(model, open(f"{BASE}models/block/{name}.json", "w", newline="\n"), indent="\t")


for prefix, texture in VARIANTS.items():
    for name, model in models.items():
        dump(retextured(model, texture), prefix + name[len("wire"):])
    if prefix != "wire":
        for base in BASE_MODELS:
            dump(retextured(json.load(open(f"{BASE}models/block/wire_{base}.json")), texture), f"{prefix}_{base}")

# The block model: one dynamic model per texture that picks the parts above in code (client/model/WireModel.java,
# rule in common/blocks/WireLook.java), and a blockstate with a single variant for every state.
for prefix in VARIANTS:
    json.dump({"loader": "radiocraft:wire", "prefix": f"radiocraft:block/{prefix}_"},
              open(f"{BASE}models/block/{prefix}_dynamic.json", "w", newline="\n"), indent="\t")
for name, prefix in [("wire", "wire"), ("waterproof_wire", "wire"), ("coax_wire", "coax_wire")] + [(f"coax_wire_{d}", f"coax_wire_{d}") for d in DYES]:
    json.dump({"variants": {"": {"model": f"radiocraft:block/{prefix}_dynamic"}}},
              open(f"{BASE}blockstates/{name}.json", "w", newline="\n"), indent=2)
print("part models:", ", ".join(models), "x", len(VARIANTS), "textures; dynamic block models and blockstates written")
