"""Builds the telephone's off-hook models, its handset item model and its blockstate from the maintainer's models.

    python tools/telephone_models.py

Reads (under assets/radiocraft/models/):
  block/telephone.json            the desk phone; its handset is the cubes named earpiece, mouthpiece and handle
  block/telephone_wallmount.json  the wall phone, same cube names. Drawn lying down or standing against a side: the
                                  side it touches (the one with the least room) is its back, and the blockstate turns
                                  it so the back goes against the wall
  item/phone_item.json            the handset, wherever its cubes sit in the block

Writes:
  block/telephone_off_hook.json, block/telephone_wall_off_hook.json   the same models without the handset cubes
  item/telephone_handset.json     the handset item: phone_item with display slots worked out round the handset's
                                  centre (hidden in the third person hand; "head" holds it by the ear, see
                                  client/render/PhoneEarLayer.java). Tune the slots in HAND/HEAD/... below.
  blockstates/telephone.json      desk and wall, on and off the hook, four facings each

Run it again after re-exporting any of the three models.
"""
import json
import math
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/radiocraft"
MODELS = ROOT / "models"
HANDSET = {"earpiece", "mouthpiece", "handle"}
FACINGS = {"north": 0, "east": 90, "south": 180, "west": 270}

# Display slots for the handset item: (rotation [x, y, z] degrees, where its centre goes in pixels, scale). The
# handset's long axis is z, earpiece toward +z (south). In "head" +x is the player's right, -z their face, +y up.
SLOTS = {
    "gui": ([30, 225, 0], [0, 0, 0], 1.6),
    "ground": ([0, 0, 0], [0, 2, 0], 1.0),
    "fixed": ([0, 90, 0], [0, 0, 0], 1.6),
    "firstperson_righthand": ([-30, 0, 0], [0, 3, 0], 1.2),
    # By the ear: the top of the handset turned to the cheek, tilted earpiece up and back, mouthpiece by the mouth.
    "head": ([-50, 0, 90], [7.5, -1.5, -1.0], 1.5),
}


def load(path):
    return json.loads(path.read_text(encoding="utf-8"))


def save(path, data):
    path.write_text(json.dumps(data, indent="\t") + "\n", encoding="utf-8")
    print("wrote", path.relative_to(ROOT))


def handset_names(model):
    return {(e.get("name") or "").lower() for e in model.get("elements", [])} & HANDSET


def off_hook(model):
    out = dict(model)
    out["elements"] = [e for e in model["elements"] if (e.get("name") or "").lower() not in HANDSET]
    return out


def bounds(elements):
    lo = [min(min(e["from"][i], e["to"][i]) for e in elements) for i in range(3)]
    hi = [max(max(e["from"][i], e["to"][i]) for e in elements) for i in range(3)]
    return lo, hi


def wall_turn(model):
    """The blockstate (x, y offset) that puts the side the wall model touches against the wall, which is south of a
    north-facing phone. x 90 turns the bottom to the south (and the south side up)."""
    lo, hi = bounds(model["elements"])
    gaps = {"down": lo[1], "south": 16 - hi[2], "north": lo[2], "east": 16 - hi[0], "west": lo[0]}
    back = min(gaps, key=gaps.get)
    return {"down": (90, 0), "south": (0, 0), "west": (0, 90), "north": (0, 180), "east": (0, 270)}[back], back


def turned_box(lo, hi, x_turn):
    """A box after the blockstate's x turn (90: up goes north, down south, south up)."""
    if x_turn == 0:
        return lo, hi
    corners = [(x, y, z) for x in (lo[0], hi[0]) for y in (lo[1], hi[1]) for z in (lo[2], hi[2])]
    moved = [(x, z, 16 - y) for x, y, z in corners]
    return [min(c[i] for c in moved) for i in range(3)], [max(c[i] for c in moved) for i in range(3)]


def rotate(v, rx, ry, rz):
    """As Minecraft's ItemTransform: rotationXYZ, so z first, then y, then x."""
    x, y, z = v
    a = math.radians(rz)
    x, y = x * math.cos(a) - y * math.sin(a), x * math.sin(a) + y * math.cos(a)
    a = math.radians(ry)
    x, z = x * math.cos(a) + z * math.sin(a), -x * math.sin(a) + z * math.cos(a)
    a = math.radians(rx)
    y, z = y * math.cos(a) - z * math.sin(a), y * math.sin(a) + z * math.cos(a)
    return [x, y, z]


def handset_display(item):
    cubes = [e for e in item["elements"] if (e.get("name") or "").lower() in HANDSET] or item["elements"]
    lo, hi = bounds(cubes)
    centre = [(lo[i] + hi[i]) / 2 - 8 for i in range(3)]  # From the middle of the block, in pixels.
    display = {
        "thirdperson_righthand": {"scale": [0, 0, 0]},  # Drawn by the ear instead (PhoneEarLayer).
        "thirdperson_lefthand": {"scale": [0, 0, 0]},
    }
    for slot, (rotation, target, scale) in SLOTS.items():
        moved = rotate([c * scale for c in centre], *rotation)
        translation = [round(target[i] - moved[i], 3) for i in range(3)]
        display[slot] = {"rotation": rotation, "translation": translation, "scale": [scale] * 3}
    return {"parent": "radiocraft:item/phone_item", "display": display}


def blockstate(wall_x, wall_y):
    variants = {}
    for facing, y in FACINGS.items():
        for wall in (False, True):
            for hook in (False, True):
                if wall:
                    model = "radiocraft:block/" + ("telephone_wall_off_hook" if hook else "telephone_wallmount")
                else:
                    model = "radiocraft:block/" + ("telephone_off_hook" if hook else "telephone")
                variant = {"model": model}
                if wall and wall_x:
                    variant["x"] = wall_x
                turn = (y + (wall_y if wall else 0)) % 360
                if turn:
                    variant["y"] = turn
                variants[f"facing={facing},off_hook={str(hook).lower()},wall={str(wall).lower()}"] = variant
    return {"variants": variants}


def main():
    desk = load(MODELS / "block/telephone.json")
    if HANDSET - handset_names(desk):
        raise SystemExit("block/telephone.json has no cube named " + ", ".join(sorted(HANDSET - handset_names(desk))))
    save(MODELS / "block/telephone_off_hook.json", off_hook(desk))

    wall = load(MODELS / "block/telephone_wallmount.json")
    if HANDSET - handset_names(wall):
        print("warning: telephone_wallmount.json lacks", ", ".join(sorted(HANDSET - handset_names(wall))), "- its off-hook model keeps them")
    save(MODELS / "block/telephone_wall_off_hook.json", off_hook(wall))
    (wall_x, wall_y), back = wall_turn(wall)
    print(f"wall model's back is its {back} side: blockstate x {wall_x}, y +{wall_y}")
    plate = [e for e in wall["elements"] if not e.get("name")]
    body = [e for e in wall["elements"] if e.get("name")]
    for label, part in (("body", body), ("backplate", plate)):
        if part:
            lo, hi = turned_box(*bounds(part), wall_x)
            print(f"  {label} facing north (TelephoneBlock WALL shape): {lo} to {hi}")

    save(MODELS / "item/telephone_handset.json", handset_display(load(MODELS / "item/phone_item.json")))
    save(ROOT / "blockstates/telephone.json", blockstate(wall_x, wall_y))


if __name__ == "__main__":
    main()
