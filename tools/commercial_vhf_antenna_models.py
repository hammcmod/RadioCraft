"""Builds the two commercial VHF antennas: the Ringo and the two-bay folded dipole.

    python tools/commercial_vhf_antenna_models.py

Writes under src/main/resources/assets/radiocraft (models/block, models/item, textures/block):

- ringo_antenna_lower    the lower block: base mount, mast, band clamp, and near the foot the Ringo's open horizontal
                         matching ring on its bracket, with the coax coming off it down the mast
- ringo_antenna_upper    the upper block: the thin white radiator and its cap
- folded_dipole_antenna_lower / _upper
                         a tower-mount two-bay folded dipole, drawn facing north: the mast pipe at the north edge (the
                         tower side), two standoff arms reaching south, each holding a tall narrow folded loop, coax
                         down the mast. Split from one 32-tall design into the two blocks.
- item models            the whole antenna, both halves
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from blockmodel import *  # noqa: E402,F403

ASSETS = Path(__file__).resolve().parent.parent / "src/main/resources/assets/radiocraft"
MODELS = ASSETS / "models/block"
ITEMS = ASSETS / "models/item"
TEXTURES = ASSETS / "textures/block"
SIZE = 32
CUTOUT = "minecraft:cutout"

FIBREGLASS = (226, 228, 222)
FIBREGLASS_DARK = (196, 198, 192)
ALUMINIUM = (186, 190, 194)
ALUMINIUM_LIGHT = (214, 218, 222)
ALUMINIUM_DARK = (140, 144, 150)
CLAMP = (84, 86, 90)
COAX = (24, 24, 26)
INSULATOR = (44, 44, 46)

ITEM_DISPLAY = {
    "thirdperson_righthand": {"rotation": [0, 0, 0], "translation": [0, 1.75, 0], "scale": [0.3, 0.3, 0.3]},
    "thirdperson_lefthand": {"rotation": [0, 0, 0], "translation": [0, 1.75, 0], "scale": [0.3, 0.3, 0.3]},
    "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 2, 0], "scale": [0.3, 0.3, 0.3]},
    "firstperson_lefthand": {"rotation": [0, 45, 0], "translation": [0, 2, 0], "scale": [0.3, 0.3, 0.3]},
    "gui": {"rotation": [30, 225, 0], "translation": [0, -4, 0], "scale": [0.42, 0.42, 0.42]},
    "ground": {"translation": [0, 2, 0], "scale": [0.25, 0.25, 0.25]},
    "fixed": {"translation": [0, -4, 0], "scale": [0.45, 0.45, 0.45]},
}


def item_model(builder, elements):
    m = builder.model(elements, CUTOUT)
    del m["parent"]
    m["display"] = ITEM_DISPLAY
    return m


def rot45(centre):
    return {"angle": 45.0, "axis": "y", "origin": list(centre)}


# ---------------------------------------------------------------- Ringo

RINGO = {
    "mast": [0, 0, 2, 16],
    "clamp": [2, 0, 4, 4],
    "cap": [2, 4, 4, 6],
    "ring": [4, 0, 8, 4],
    "coax": [8, 0, 9, 16],
}


def ringo():
    p = Painter(RINGO, SIZE, seed=11)
    p.fill("mast", FIBREGLASS, 4, edge(FIBREGLASS, FIBREGLASS_DARK))
    p.fill("clamp", CLAMP, 6, rim((60, 62, 66)))
    p.fill("cap", ALUMINIUM_DARK, 4)
    p.fill("ring", ALUMINIUM, 5, edge(ALUMINIUM_LIGHT, ALUMINIUM_DARK, vertical=False))
    p.fill("coax", COAX, 3)
    p.write(TEXTURES / "ringo_antenna.png")

    b = Builder(RINGO, "radiocraft:block/ringo_antenna", SIZE)
    y0, y1 = 4.0, 5.0  # the matching ring's height
    lower = [
        b.box("mount", [6, 0, 6], [10, 2, 10], "clamp"),
        b.box("mast", [7, 0, 7], [9, 16, 9], "mast"),
        b.box("band_clamp", [6.75, 13, 6.75], [9.25, 14.5, 9.25], "clamp"),
        b.box("ring_clamp", [6.5, 3.5, 6.5], [9.5, 5.5, 9.5], "clamp"),
        # The open ring: seven-eighths of an octagon round the mast, open to the east where the coax comes off.
        b.box("ring_north", [6, y0, 3], [10, y1, 4], "ring"),
        b.box("ring_south", [6, y0, 12], [10, y1, 13], "ring"),
        b.box("ring_west", [3, y0, 6], [4, y1, 10], "ring"),
        b.box("ring_nw", [3.25, y0, 4.25], [6.25, y1, 5.25], "ring", rotation=rot45((4.75, 4.5, 4.75))),
        b.box("ring_sw", [3.25, y0, 10.75], [6.25, y1, 11.75], "ring", rotation=rot45((4.75, 4.5, 11.25))),
        b.box("ring_ne", [9.75, y0, 4.25], [12.75, y1, 5.25], "ring", rotation=rot45((11.25, 4.5, 4.75))),
        b.box("ring_se", [9.75, y0, 10.75], [12.75, y1, 11.75], "ring", rotation=rot45((11.25, 4.5, 11.25))),
        b.box("ring_arm", [7.5, y0, 9.5], [8.5, y1, 12], "ring"),
        b.box("feed", [12, 3.5, 6.5], [13, 5.5, 7.5], "clamp"),
        b.box("coax_out", [9.5, 4.25, 6.75], [12, 4.75, 7.25], "coax"),
        b.box("coax_down", [9.25, 0, 6.75], [9.75, 4.75, 7.25], "coax"),
    ]
    upper = [
        b.box("joint", [7, 0, 7], [9, 1, 9], "clamp"),
        b.box("radiator", [7.25, 1, 7.25], [8.75, 14, 8.75], "mast"),
        b.box("cap", [7, 14, 7], [9, 15, 9], "cap"),
    ]
    save(MODELS, "ringo_antenna_lower", b.model(lower, CUTOUT))
    save(MODELS, "ringo_antenna_upper", b.model(upper, CUTOUT))
    whole = lower + [shift(el, 16) for el in upper]
    save(ITEMS, "ringo_antenna", item_model(b, whole))


# ---------------------------------------------------------------- folded dipole

DIPOLE = {
    "galv": [0, 0, 2, 16],
    "alu": [2, 0, 3, 16],
    "clamp": [3, 0, 5, 4],
    "coax": [5, 0, 6, 16],
    "insulator": [6, 0, 8, 4],
}


def folded_dipole():
    p = Painter(DIPOLE, SIZE, seed=12)
    p.fill("galv", GALV, 5, galvanized())
    p.fill("alu", ALUMINIUM, 4, edge(ALUMINIUM_LIGHT, ALUMINIUM_DARK))
    p.fill("clamp", CLAMP, 6, rim((60, 62, 66)))
    p.fill("coax", COAX, 3)
    p.fill("insulator", INSULATOR, 4)
    p.write(TEXTURES / "folded_dipole_antenna.png")

    b = Builder(DIPOLE, "radiocraft:block/folded_dipole_antenna", SIZE)
    specs = [dict(name="mast", frm=[7, 0, 1], to=[9, 32, 3], mat="galv"),
             dict(name="coax_mast", frm=[9, 0, 1.5], to=[10, 21, 2.5], mat="coax")]
    for bay, arm in (("lower", 4.0), ("upper", 20.0)):
        lo, hi = arm - 2.0, arm + 10.0  # the loop stands mostly above its arm
        specs += [
            dict(name=f"{bay}_clamp", frm=[6.5, arm - 0.5, 0.5], to=[9.5, arm + 1.5, 3.5], mat="clamp"),
            dict(name=f"{bay}_arm", frm=[7.5, arm, 3], to=[8.5, arm + 1, 11], mat="alu"),
            dict(name=f"{bay}_coax", frm=[7.6, arm - 0.6, 3], to=[8.4, arm, 10.5], mat="coax"),
            dict(name=f"{bay}_feed", frm=[7, arm - 0.5, 10.5], to=[9, arm + 1.5, 12.5], mat="insulator"),
            dict(name=f"{bay}_rod_near", frm=[7.5, lo, 11], to=[8.5, hi, 12], mat="alu"),
            dict(name=f"{bay}_rod_far", frm=[7.5, lo, 14], to=[8.5, hi, 15], mat="alu"),
            dict(name=f"{bay}_bottom", frm=[7.5, lo - 1, 11.5], to=[8.5, lo, 14.5], mat="alu"),
            dict(name=f"{bay}_top", frm=[7.5, hi, 11.5], to=[8.5, hi + 1, 14.5], mat="alu"),
        ]
    cells = split_cells(b, specs, {"lower": (0, 0, 0), "upper": (0, 16, 0)})
    save(MODELS, "folded_dipole_antenna_lower", b.model(cells["lower"], CUTOUT))
    save(MODELS, "folded_dipole_antenna_upper", b.model(cells["upper"], CUTOUT))
    whole = [b.box(s["name"], s["frm"], s["to"], s["mat"]) for s in specs]
    save(ITEMS, "folded_dipole_antenna", item_model(b, whole))


def shift(element, dy):
    import copy
    el = copy.deepcopy(element)
    el["from"][1] += dy
    el["to"][1] += dy
    if "rotation" in el:
        el["rotation"]["origin"][1] += dy
    return el


if __name__ == "__main__":
    ringo()
    folded_dipole()
