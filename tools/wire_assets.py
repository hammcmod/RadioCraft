"""Generates the wire models that depend on each other and the three identical wire blockstates.

Model coordinate convention: every edge model is authored for the NORTH edge (z = 0..2) and rotated in the
blockstate. Pieces may reach outside the 0..16 cube (the overhangs), which Minecraft renders fine.
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
            {"name": "wire", "from": [7, y0, 0], "to": [9, mid, 2], "faces": faces(down=y0 == 0)},
            {"name": "wire", "from": [7, mid, 0], "to": [9, 16, 2], "faces": faces(up=True)},
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
    "wire_climb_overhang": flat_run(-7, 0, 16, "Climb up the north face ending at a device: the cable runs on over the wall's top to the device's middle"),
    # Run up the north edge ending at a device directly above: in along that block's floor to its middle.
    "wire_up_overhang": flat_run(0, 7, 16, "Run up the north edge ending at a device above: the cable turns in to the device's middle"),
}
for name, model in models.items():
    json.dump(model, open(f"{BASE}models/block/{name}.json", "w", newline="\n"), indent="\t")

H = {"north": None, "east": 90, "south": 180, "west": 270}
SIDES = list(H)


def model(name, y=None):
    m = {"model": "radiocraft:block/" + name}
    if y:
        m["y"] = y
    return m


def others(h):
    return "|".join(["none"] + [s for s in SIDES if s != h])


cases = []
# A. middle piece: whenever the block is not just an edge run passing straight through
middle_or = [{d: "true"} for d in SIDES] + [{"hug": "none"}, {"arrive": "none"}]
for h in SIDES:
    middle_or.append({"hug": h, "arrive": "|".join(s for s in SIDES if s != h)})
cases.append({"when": {"OR": middle_or}, "apply": model("wire_middle")})
# B. horizontal stubs to connected neighbours
for d, y in H.items():
    cases.append({"when": {d: "true"}, "apply": model("wire_horizontal", y)})
# C. stub from the middle to the hugged edge, when the middle is drawn, the arrival is not on that edge already
#    and no neighbour stub goes that way
for h, y in H.items():
    cases.append({"when": {"hug": h, h: "false", "arrive": others(h)}, "apply": model("wire_horizontal", y)})
# D. stub from the arrival edge to the middle, when the middle is drawn (a branch, or the run does not go on up
#    that edge) and no neighbour stub goes that way
for h, y in H.items():
    or_list = [{"arrive": h, h: "false", d: "true"} for d in SIDES if d != h]
    or_list.append({"arrive": h, h: "false", "hug": others(h)})
    cases.append({"when": {"OR": or_list}, "apply": model("wire_horizontal", y)})
# E. vertical up the middle
cases.append({"when": {"hug": "none", "up": "true"}, "apply": model("wire_vertical")})
# F. vertical up an edge: full height when the run from below is on the same edge, else from the stub level
for h, y in H.items():
    cases.append({"when": {"hug": h, "arrive": h}, "apply": model("wire_edge_full", y)})
    cases.append({"when": {"hug": h, "arrive": others(h)}, "apply": model("wire_edge", y)})
# G. the ends that draw their own last piece
for h, y in H.items():
    cases.append({"when": {"hug": h, "end": "climb_device"}, "apply": model("wire_climb_overhang", y)})
    cases.append({"when": {"hug": h, "end": "up_device"}, "apply": model("wire_up_overhang", y)})

text = json.dumps({"multipart": cases}, indent=2) + "\n"
for name in ["wire", "coax_wire", "waterproof_wire"]:
    open(f"{BASE}blockstates/{name}.json", "w", newline="\n").write(text)
print("models:", ", ".join(models), "| blockstate cases:", len(cases))
