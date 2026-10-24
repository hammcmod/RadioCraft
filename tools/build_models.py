"""Builds the in-game models from the maintainer's Blockbench exports, which stay as they are.

    python tools/build_models.py

Blockbench writes its export over the same file every time it saves, so nothing is edited in place: each entry in
BUILDS reads an export and writes the models the game uses under other names. Run it again after every re-export.

What it does on the way:
- Splits a two-block export (y 0 to 32) into a lower and an upper model at y = 16, and keeps the whole one for the item.
- Splits named lamps into their own full-bright models (the H19's screens), and gives named valves a soft glow.
- Turns a model drawn facing the wrong way (see rotate_model.py).
- Makes rotations legal for Minecraft 1.21.1, which takes one axis and -45, -22.5, 0, 22.5 or 45 degrees only.
  Blockbench's newer x/y/z form becomes axis and angle; a turn 180 degrees past a legal one is rewritten exactly;
  a slight tilt (5 degrees or less) is flattened, with a warning. Anything else stops the build: fix it in Blockbench.
"""
import copy
import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from rotate_model import turn_element, SIDES  # noqa: E402

ASSETS = Path(__file__).resolve().parent.parent / "src/main/resources/assets/radiocraft"
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
]

ITEM_DISPLAY = "item/j_pole_antenna.json"  # Hand and inventory sizes for a two-tall item.


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
    display = json.loads((MODELS / ITEM_DISPLAY).read_text(encoding="utf-8"))["display"]
    item = {"credit": export.get("credit", "Made with Blockbench"), "render_type": spec["render_type"]["upper"], "texture_size": export.get("texture_size", [16, 16]),
            "textures": dict(textures, particle=textures[first["lower"]]), "elements": item_elements, "display": display}
    write(spec["item"], item)
    print(f"  {spec['lower']}: {len(lower)} elements, {spec['upper']}: {len(upper)}, {spec['item']}")


def main():
    for spec in BUILDS:
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
        else:
            build_tall(spec, export, elements)


if __name__ == "__main__":
    main()
