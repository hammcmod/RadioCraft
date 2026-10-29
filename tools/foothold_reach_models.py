"""Writes the tower foothold models that hang on a tower beam: the maintainer's whole and broken footholds, moved back
4, 5 or 6 pixels (the `mount` blockstate property, see TowerFootholdBlock.Mount) so the rail sits against the beam's steel
instead of floating at the block edge. Re-run after editing block/tower_foothold.json or tower_foothold_broken.json.

    python tools/foothold_reach_models.py
"""
import copy
import json
from pathlib import Path

MODELS = Path(__file__).resolve().parent.parent / "src/main/resources/assets/radiocraft/models/block"
SOURCES = ["tower_foothold", "tower_foothold_broken"]
REACHES = [4, 5, 6]   # 8 minus half a beam's width: thick 8 px, normal 6, thin 4
STANDOFF = 0.01       # The rail stops this far short of the steel: flush, its flat back z-fights with the beam's face


def shifted(model, reach):
    """The model with every element moved `reach` pixels (less STANDOFF) towards +z, the side it hangs on (facing north)."""
    out = copy.deepcopy(model)
    reach = reach - STANDOFF
    for el in out.get("elements", []):
        el["from"][2] = round(el["from"][2] + reach, 4)
        el["to"][2] = round(el["to"][2] + reach, 4)
        if "rotation" in el and "origin" in el["rotation"]:
            el["rotation"]["origin"][2] = round(el["rotation"]["origin"][2] + reach, 4)
    return out


def main():
    for name in SOURCES:
        model = json.loads((MODELS / f"{name}.json").read_text(encoding="utf-8"))
        for reach in REACHES:
            path = MODELS / f"{name}_reach_{reach}.json"
            path.write_text(json.dumps(shifted(model, reach), indent="\t") + "\n", encoding="utf-8")
            print("wrote", path)


if __name__ == "__main__":
    main()
