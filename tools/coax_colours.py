"""Paints the dyed coax textures: the maintainer's coaxial_wire.png with a tracer stripe in each dye colour, one pixel
along every side of the jacket, so it shows from any angle. Writes textures/block/coaxial_wire_<colour>.png. Also the
dyed power wire textures (2026-10-27): the whole of wire.png tinted to the dye, keeping its shading, since a 1 px cable
has no room for a stripe. Writes textures/block/wire_<colour>.png; the wire items are 3D and need no icon. And the data
wire (2026-10-30): wire.png in light grey with darker flecks along it, so it tells apart from light grey power wire.
Writes textures/block/data_wire.png. And the dyed data wire (2026-10-31): the same flecks on a jacket tinted to the dye.
Writes textures/block/data_wire_<colour>.png.

    pip install pillow
    python tools/coax_colours.py

Then run tools/wire_assets.py for the models and blockstates (it reads COLOURS from here).
"""
import json
from pathlib import Path

from PIL import Image

ASSETS = Path(__file__).resolve().parent.parent / "src/main/resources/assets/radiocraft"
TEXTURES = ASSETS / "textures/block"
ITEMS = ASSETS / "textures/item"

# Minecraft's dye colours (DyeColor), in its order.
COLOURS = {
    "white": (249, 255, 254), "orange": (249, 128, 29), "magenta": (199, 78, 189), "light_blue": (58, 179, 218),
    "yellow": (254, 216, 61), "lime": (128, 199, 31), "pink": (243, 139, 170), "gray": (71, 79, 82),
    "light_gray": (157, 157, 151), "cyan": (22, 156, 156), "purple": (137, 50, 184), "blue": (60, 68, 170),
    "brown": (131, 84, 50), "green": (94, 124, 22), "red": (176, 46, 38), "black": (29, 29, 33),
}
# The data wire's jacket: lighter than the light grey dye, a little cool.
DATA_WIRE = (222, 224, 226)
# The jacket's four sides are 7 x 2 strips at rows 0, 2, 4 and 6; the stripe runs along the second row of each.
STRIPE_ROWS = (1, 3, 5, 7)
LENGTH = 7


def stripe(base, colour):
    out = base.copy()
    r, g, b = colour
    for y in STRIPE_ROWS:
        for x in range(LENGTH):
            shade = 0.85 if x % 3 == 0 else 1.0  # a little texture along it
            out.putpixel((x, y), (int(r * shade), int(g * shade), int(b * shade), 255))
    return out


def icon_stripe(icon, colour):
    """The coil's lighter turns take the dye colour; the connector (bottom right) stays as it is."""
    out = icon.copy()
    r, g, b = colour
    for y in range(icon.height):
        for x in range(icon.width):
            pr, pg, pb, a = icon.getpixel((x, y))
            light = (pr + pg + pb) / 3
            if a == 0 or light < 60 or (x >= 16 and y >= 22):
                continue
            k = min(1.0, light / 160)
            out.putpixel((x, y), (int(r * k), int(g * k), int(b * k), a))
    return out


def tinted(base, colour):
    """Every opaque pixel takes the dye colour at the pixel's own brightness (the darkest jacket shade is the dye's half tone)."""
    out = base.copy()
    r, g, b = colour
    for y in range(base.height):
        for x in range(base.width):
            pr, pg, pb, a = base.getpixel((x, y))
            if a == 0:
                continue
            k = 0.5 + 0.5 * min(1.0, ((pr + pg + pb) / 3) / 73)  # 73 is the jacket's highlight in wire.png.
            out.putpixel((x, y), (int(r * k), int(g * k), int(b * k), a))
    return out


def data_wire(wire, colour):
    """wire.png tinted to {@code colour} with darker flecks along it: twisted pairs showing through the jacket."""
    data = tinted(wire, colour)
    for y in STRIPE_ROWS:
        for x in range(LENGTH):
            if x % 3 == 1:
                r, g, b, a = data.getpixel((x, y))
                data.putpixel((x, y), (int(r * 0.6), int(g * 0.6), int(b * 0.65), a))
    return data


if __name__ == "__main__":
    wire = Image.open(TEXTURES / "wire.png").convert("RGBA")
    for name, colour in COLOURS.items():
        tinted(wire, colour).save(TEXTURES / f"wire_{name}.png")
    base = Image.open(TEXTURES / "coaxial_wire.png").convert("RGBA")
    for name, colour in COLOURS.items():
        stripe(base, colour).save(TEXTURES / f"coaxial_wire_{name}.png")
    icon = Image.open(ITEMS / "coax_wire_icon.png").convert("RGBA")
    for name, colour in COLOURS.items():
        icon_stripe(icon, colour).save(ITEMS / f"coax_wire_icon_{name}.png")
        model = {"parent": "minecraft:item/generated", "textures": {"layer0": f"radiocraft:item/coax_wire_icon_{name}"}}
        (ASSETS / "models/item" / f"coax_wire_{name}.json").write_text(json.dumps(model, indent=2) + "\n")
    data_wire(wire, DATA_WIRE).save(TEXTURES / "data_wire.png")
    for name, colour in COLOURS.items():
        data_wire(wire, colour).save(TEXTURES / f"data_wire_{name}.png")
    print("wrote", len(COLOURS), "dyed coax textures, icons and item models, and", len(COLOURS), "dyed wire textures")
