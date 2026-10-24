"""Turns block or item models about the vertical axis, for a model drawn facing the wrong way.

    python tools/rotate_model.py 180 block/ionospheric_processor_lower block/ionospheric_processor_upper item/ionospheric_processor

The angle is 90, 180 or 270, clockwise seen from above, the same as a blockstate's "y". Paths are under
assets/radiocraft/models/, without .json. Every element's corners, rotation and pivot are turned, the side faces
move round (north becomes east at 90), and the top and bottom textures turn with them. The model then needs no
rotation offset in its blockstate, and its item turns too.
"""
import json
import sys
from pathlib import Path

MODELS = Path(__file__).resolve().parent.parent / "src/main/resources/assets/radiocraft/models"
SIDES = ["north", "east", "south", "west"]


def turn_point(x, z, quarters, pivot=(8, 8)):
    px, pz = pivot
    for _ in range(quarters):  # A quarter turn clockwise from above about the pivot (the block's centre): north goes east.
        x, z = px + pz - z, pz - px + x
    return x, z


def turn_axis_angle(axis, angle, quarters):
    """A rotation about x or z after the model turns: x becomes z, z becomes -x, per quarter turn."""
    for _ in range(quarters):
        if axis == "x":
            axis = "z"
        elif axis == "z":
            axis, angle = "x", -angle
    return axis, angle


def turn_element(element, quarters, pivot=(8, 8)):
    fx, fy, fz = element["from"]
    tx, ty, tz = element["to"]
    ax, az = turn_point(fx, fz, quarters, pivot)
    bx, bz = turn_point(tx, tz, quarters, pivot)
    element["from"] = [min(ax, bx), fy, min(az, bz)]
    element["to"] = [max(ax, bx), ty, max(az, bz)]
    rotation = element.get("rotation")
    if rotation:
        if "origin" in rotation:
            ox, oy, oz = rotation["origin"]
            ox, oz = turn_point(ox, oz, quarters, pivot)
            rotation["origin"] = [ox, oy, oz]
        if "axis" in rotation and rotation["axis"] != "y":
            rotation["axis"], rotation["angle"] = turn_axis_angle(rotation["axis"], rotation["angle"], quarters)
    faces = element.get("faces", {})
    turned = {}
    for name, face in faces.items():
        if name in SIDES:
            turned[SIDES[(SIDES.index(name) + quarters) % 4]] = face
            if "cullface" in face and face["cullface"] in SIDES:
                face["cullface"] = SIDES[(SIDES.index(face["cullface"]) + quarters) % 4]
        else:
            # The top texture turns clockwise with the model; the bottom one, seen from below, the other way.
            step = 90 * quarters if name == "up" else -90 * quarters
            face["rotation"] = (face.get("rotation", 0) + step) % 360
            if face["rotation"] == 0:
                del face["rotation"]
            turned[name] = face
    element["faces"] = turned


def main(angle, paths):
    if angle not in (90, 180, 270):
        sys.exit("The angle must be 90, 180 or 270")
    for path in paths:
        file = MODELS / f"{path}.json"
        model = json.loads(file.read_text(encoding="utf-8"))
        for element in model.get("elements", []):
            turn_element(element, angle // 90)
        file.write_text(json.dumps(model, indent="\t") + "\n", encoding="utf-8")
        print(f"{path}: turned {angle} degrees")


if __name__ == "__main__":
    if len(sys.argv) < 3:
        sys.exit(__doc__)
    main(int(sys.argv[1]), sys.argv[2:])
