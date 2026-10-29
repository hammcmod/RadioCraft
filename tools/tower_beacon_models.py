"""Writes the tower beacon (a red obstruction light, after a 300 mm TWR beacon) models and texture: a column as thin
as the thin tower beam, grey base ring, ribbed red glass, a grey band with its latch, more red glass, and a grey cap.
Two models: `tower_beacon` (dark glass) and `tower_beacon_lit` (the glass full bright), switched by the `lit`
blockstate (TowerBeaconBlock).

    python tools/tower_beacon_models.py
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from blockmodel import Builder, Painter, FULL_BRIGHT, save  # noqa: E402

ROOT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/radiocraft"
MODELS = ROOT / "models/block"
TEXTURES = ROOT / "textures/block"

# Texture regions, in 16ths of the 16 px texture: [u0, v0, u1, v1].
REGIONS = {
    "metal": [0, 0, 4, 4],        # Grey cast aluminium: base ring, cap.
    "band": [0, 4, 4, 8],         # The middle band, a latch on it.
    "metal_top": [0, 8, 4, 12],   # Tops and bottoms of the metal parts.
    "glass": [4, 0, 8, 16],       # Ribbed red glass, unlit.
    "glass_lit": [8, 0, 12, 16],  # Ribbed red glass, lit.
    "glass_top": [12, 0, 16, 4],  # The glass seen end on (hidden between the metal parts, but filled).
}

METAL = (150, 152, 150)
METAL_DARK = (104, 106, 104)
METAL_LIGHT = (184, 186, 184)
LATCH = (40, 40, 42)


def paint():
    p = Painter(REGIONS, size=16, seed=300)
    p.fill("metal", METAL, jitter=5, fn=lambda x, y, w, h: METAL_LIGHT if y == 0 else METAL_DARK if y == h - 1 else None)
    p.fill("band", METAL, jitter=5, fn=lambda x, y, w, h: METAL_LIGHT if y == 0 else None)
    p.put("band", 1, 1, LATCH)
    p.put("band", 1, 2, LATCH)
    p.put("band", 2, 1, METAL_DARK)
    p.fill("metal_top", METAL, jitter=6)
    # Fresnel ribs: alternate rows of light and shade, darker at the edges of the face (a round lens seen side on).
    def glass(dark):
        def rib(x, y, w, h):
            edge = 1 if x in (0, w - 1) else 0
            if dark:
                base = (96, 12, 14) if y % 2 == 0 else (70, 6, 10)
            else:
                base = (255, 58, 48) if y % 2 == 0 else (214, 26, 26)
            return tuple(max(0, c - 30 * edge) for c in base)
        return rib
    p.fill("glass", (80, 10, 12), jitter=3, fn=glass(True))
    p.fill("glass_lit", (230, 40, 36), jitter=4, fn=glass(False))
    p.fill("glass_top", (120, 16, 18), jitter=3)
    p.write(TEXTURES / "tower_beacon.png")


def elements(lit):
    b = Builder(REGIONS, "radiocraft:block/tower_beacon", size=16)
    tops = {"up": "metal_top", "down": "metal_top"}
    glass = "glass_lit" if lit else "glass"
    extra = FULL_BRIGHT if lit else None
    return [
        b.box("base", [5.5, 0, 5.5], [10.5, 1.5, 10.5], "metal", tops),
        b.box("lower_glass", [6, 1.5, 6], [10, 6.5, 10], glass, {"up": "glass_top", "down": "glass_top"}, extra=extra),
        b.box("band", [5.5, 6.5, 5.5], [10.5, 7.5, 10.5], "band", tops),
        b.box("upper_glass", [6, 7.5, 6], [10, 12.5, 10], glass, {"up": "glass_top", "down": "glass_top"}, extra=extra),
        b.box("cap", [6.5, 12.5, 6.5], [9.5, 14, 9.5], "metal", tops),
    ]


def main():
    paint()
    b = Builder(REGIONS, "radiocraft:block/tower_beacon", size=16)
    save(MODELS, "tower_beacon", b.model(elements(False)))
    save(MODELS, "tower_beacon_lit", b.model(elements(True)))


if __name__ == "__main__":
    main()
