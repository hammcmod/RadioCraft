"""Writes the wire junction's model and textures: a galvanised steel junction box half a block high, the full width so
power wire meets its sides. The top is the lid (a raised rim and four corner screws); each side shows the lid seam
and a knockout low down in the middle, where wire comes in.

    python tools/wire_junction_models.py
"""
import json
import random
from pathlib import Path
import sys

sys.path.insert(0, str(Path(__file__).resolve().parent))
from blockmodel import Painter  # noqa: E402

ROOT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/radiocraft"
MODELS = ROOT / "models/block"
TEXTURES = ROOT / "textures/block"

STEEL = (150, 154, 156)
STEEL_LIGHT = (186, 190, 192)
STEEL_DARK = (104, 108, 110)
SCREW = (78, 80, 82)
SCREW_SLOT = (52, 54, 56)
KNOCKOUT = (88, 92, 94)
KNOCKOUT_RIM = (122, 126, 128)


def spangle(p, region, base):
    """Galvanised steel: small flat patches of slightly different grey."""
    rnd = random.Random(7)

    def fn(x, y, w, h):
        return None
    p.fill(region, base, jitter=7, fn=fn)


def top():
    p = Painter({"all": [0, 0, 16, 16]}, size=16, seed=11)
    spangle(p, "all", STEEL)
    for i in range(16):
        for (x, y) in ((i, 0), (0, i)):
            p.put("all", x, y, STEEL_LIGHT)
        for (x, y) in ((i, 15), (15, i)):
            p.put("all", x, y, STEEL_DARK)
    for i in range(2, 14):  # The lid's inner pressed edge.
        for (x, y) in ((i, 2), (2, i)):
            p.put("all", x, y, STEEL_DARK)
        for (x, y) in ((i, 13), (13, i)):
            p.put("all", x, y, STEEL_LIGHT)
    for cx, cy in ((3, 3), (12, 3), (3, 12), (12, 12)):  # Corner screws, slotted.
        p.put("all", cx, cy, SCREW)
        p.put("all", cx - 1 if cx > 8 else cx + 1, cy, SCREW_SLOT)
    p.write(TEXTURES / "wire_junction_top.png")


def side():
    p = Painter({"all": [0, 0, 16, 16]}, size=16, seed=12)
    spangle(p, "all", STEEL)
    # Only the lower half shows (the box is 8 high): rows 8-15.
    for x in range(16):
        p.put("all", x, 8, STEEL_LIGHT)   # Lid edge.
        p.put("all", x, 9, STEEL_DARK)    # Lid seam.
        p.put("all", x, 15, STEEL_DARK)   # Bottom edge.
    for y in range(8, 16):
        p.put("all", 0, y, STEEL_LIGHT)
        p.put("all", 15, y, STEEL_DARK)
    for x, y in ((7, 12), (8, 12), (7, 13), (8, 13)):  # The knockout where wire comes in.
        p.put("all", x, y, KNOCKOUT)
    for x, y in ((6, 12), (6, 13), (9, 12), (9, 13), (7, 11), (8, 11), (7, 14), (8, 14)):
        p.put("all", x, y, KNOCKOUT_RIM)
    p.write(TEXTURES / "wire_junction_side.png")


def model():
    side_face = {"uv": [0, 8, 16, 16], "texture": "#side"}
    faces = {d: dict(side_face) for d in ("north", "south", "east", "west")}
    faces["up"] = {"uv": [0, 0, 16, 16], "texture": "#top"}
    faces["down"] = {"uv": [0, 0, 16, 16], "texture": "#top", "cullface": "down"}
    data = {"credit": "RadioCraft", "parent": "block/block",
            "textures": {"top": "radiocraft:block/wire_junction_top", "side": "radiocraft:block/wire_junction_side",
                         "particle": "radiocraft:block/wire_junction_side"},
            "elements": [{"name": "box", "from": [0, 0, 0], "to": [16, 8, 16], "faces": faces}]}
    path = MODELS / "wire_junction.json"
    path.write_text(json.dumps(data, indent="\t") + "\n", encoding="utf-8")
    print("wrote", path)


if __name__ == "__main__":
    top()
    side()
    model()
