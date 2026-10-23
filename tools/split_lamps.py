"""Splits a Blockbench block model export into a body and one full-bright model per lamp.

    python tools/split_lamps.py aprs_h19 MSG CONTACT READY

Reads models/block/<name>.json, takes out every element named after a lamp (case-insensitive), writes each lamp to
models/block/<name>_<lamp>.json with only its front (north) face, lit full bright and cutout, and writes the rest back
to <name>.json as the body. Re-export from Blockbench and run it again after changing the model; a model that has
already been split is left alone.
"""
import json
import sys
from pathlib import Path

MODELS = Path(__file__).resolve().parent.parent / "src/main/resources/assets/radiocraft/models/block"


def main(name, lamps):
    path = MODELS / f"{name}.json"
    model = json.loads(path.read_text(encoding="utf-8"))
    wanted = {lamp.lower() for lamp in lamps}
    body, lit = [], {lamp: [] for lamp in wanted}
    for element in model["elements"]:
        label = (element.get("name") or "").lower()
        (lit[label] if label in wanted else body).append(element)
    if not any(lit.values()):
        print(f"{name}: no lamp elements, already split")
        return
    missing = [lamp for lamp, elements in lit.items() if not elements]
    if missing:
        sys.exit(f"{name}: no element named {', '.join(missing)}")

    for lamp, elements in lit.items():
        for element in elements:
            element["faces"] = {"north": element["faces"]["north"]}
            element["neoforge_data"] = {"block_light": 15, "sky_light": 15, "ambient_occlusion": False}
        lamp_model = {
            "credit": model.get("credit", "Made with Blockbench"),
            "parent": "block/block",
            "render_type": "minecraft:cutout",
            "ambientocclusion": False,
            "texture_size": model["texture_size"],
            "textures": model["textures"],
            "elements": elements,
        }
        (MODELS / f"{name}_{lamp}.json").write_text(json.dumps(lamp_model, indent="\t") + "\n", encoding="utf-8")

    model["elements"] = body
    model.setdefault("parent", "block/block")
    model.pop("groups", None)  # Indices into the old element list; Minecraft ignores them anyway.
    path.write_text(json.dumps(model, indent="\t") + "\n", encoding="utf-8")
    print(f"{name}: body of {len(body)} elements, lamps {', '.join(sorted(lit))}")


if __name__ == "__main__":
    if len(sys.argv) < 3:
        sys.exit(__doc__)
    main(sys.argv[1], sys.argv[2:])
