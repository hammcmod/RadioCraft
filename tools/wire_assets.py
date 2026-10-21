"""Generates the wire models that depend on each other and the three identical wire blockstates.

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

H = {"north": None, "east": 90, "south": 180, "west": 270}
SIDES = list(H)
ALL_EDGE = ["none"] + SIDES


def model(name, y=None):
    m = {"model": "radiocraft:block/" + name}
    if y:
        m["y"] = y
    return m


def alt(values):
    return "|".join(values)


def merge(a, b):
    """AND of two multipart condition objects, or None if they contradict (each value is a set of alternatives)."""
    out = dict(a)
    for k, v in b.items():
        if k in out:
            common = set(out[k].split("|")) & set(v.split("|"))
            if not common:
                return None
            out[k] = alt(sorted(common))
        else:
            out[k] = v
    return out


def cross(or_a, or_b):
    out = []
    for a in or_a:
        for b in or_b:
            m = merge(a, b)
            if m is not None and m not in out:
                out.append(m)
    return out


def when(items):
    return items[0] if len(items) == 1 else {"OR": items}


# The rendering rule (mirrored by WireBlock#buildShape). Cable already lies on the edges in S = {arrive, hug}. The
# "ports" the cable must reach are the connected neighbours (at their edges), the top of the block when the run up
# goes through the middle (up and no hug), and the bottom when the run from below comes up the middle (down and no
# arrival). The middle piece is drawn only when the cable has to pass through the middle: a port that is not on an
# edge in S, two different edges in S (the run crosses from one to the other), no edge cable at all, or a dead end
# (nothing but the edge cable: a nub so the block is visible).
middle_or = []
for d in SIDES:
    middle_or.append({d: "true", "arrive": alt([e for e in ALL_EDGE if e != d]), "hug": alt([e for e in ALL_EDGE if e != d])})
middle_or.append({"up": "true", "hug": "none"})
middle_or.append({"down": "true", "arrive": "none"})
for a in SIDES:
    for h in SIDES:
        if a != h:
            middle_or.append({"arrive": a, "hug": h})
middle_or.append({"arrive": "none", "hug": "none"})
noflags = {s: "false" for s in SIDES}
# ... a dead end being: no ports, and the cable does not pass straight through along an edge (it passes through when
# it arrives on an edge and goes on up or climbs).
dead = [dict(noflags, up="false", down="false"),
        dict(noflags, up="false", arrive=alt(SIDES)),
        dict(noflags, hug=alt(SIDES), down="false"),
        dict(noflags, hug=alt(SIDES), arrive=alt(SIDES))]
not_through = [{"arrive": "none"}, {"up": "false", "end": "none|up_device"}]
middle_or += cross(dead, not_through)

cases = [{"when": when(middle_or), "apply": model("wire_middle")}]
# A stub from the middle to an edge: when the middle is drawn and that edge has cable (in S) or a neighbour.
for e, y in H.items():
    triggers = [{e: "true"}, {"arrive": e}, {"hug": e}]
    cases.append({"when": when(cross(middle_or, triggers)), "apply": model("wire_horizontal", y)})
# A corner at an edge: the run from below arrives there and the cable leaves sideways on that same edge, nothing
# else to reach, so no middle: just the cube that joins the top of the run to the neighbour's stub.
for e, y in H.items():
    cond = {"arrive": e, "hug": "none", e: "true", "up": "false"}
    cond.update({s: "false" for s in SIDES if s != e})
    cases.append({"when": cond, "apply": model("wire_corner", y)})
# The run up: through the middle, or along an edge (full height when the run from below is on that edge too).
cases.append({"when": {"hug": "none", "up": "true"}, "apply": model("wire_vertical")})
for h, y in H.items():
    cases.append({"when": {"hug": h, "arrive": h}, "apply": model("wire_edge_full", y)})
    cases.append({"when": {"hug": h, "arrive": alt([e for e in ALL_EDGE if e != h])}, "apply": model("wire_edge", y)})
# The ends that draw their own last piece.
for h, y in H.items():
    cases.append({"when": {"hug": h, "end": "climb_device"}, "apply": model("wire_climb_overhang", y)})
    cases.append({"when": {"hug": h, "end": "up_device"}, "apply": model("wire_up_overhang", y)})

text = json.dumps({"multipart": cases}, indent=2) + "\n"
for name, prefix in [("wire", "wire"), ("waterproof_wire", "wire"), ("coax_wire", "coax_wire")]:
    open(f"{BASE}blockstates/{name}.json", "w", newline="\n").write(text.replace("radiocraft:block/wire_", f"radiocraft:block/{prefix}_"))
print("models:", ", ".join(models), "x", len(VARIANTS), "textures | blockstate cases:", len(cases), "| bytes:", len(text))
