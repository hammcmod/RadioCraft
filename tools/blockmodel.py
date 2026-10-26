"""Shared helpers for the scripted block models (weather_station_model.py, weather_instruments_model.py).

A Painter builds a small PNG pixel by pixel from named regions given in UV units (0-16 across the texture, so one
unit is size/16 pixels). A Builder turns named boxes into Minecraft 1.21.1 model elements whose faces take their UVs
from those same regions, so the texture and the model never disagree about where a material lives.
"""
import json
import random
import struct
import zlib

# 1960s instrument palette
GALV = (146, 150, 148)        # galvanized steel
GALV_LIGHT = (172, 176, 174)
GALV_DARK = (112, 116, 115)
CREAM = (222, 214, 186)       # enamel
CREAM_DIRTY = (204, 195, 166)
COPPER = (176, 104, 60)
COPPER_LIGHT = (206, 140, 90)
COPPER_DARK = (130, 72, 40)
VERDIGRIS = (112, 150, 128)
BRASS = (184, 150, 66)
BRASS_LIGHT = (214, 182, 96)
BRASS_DARK = (150, 118, 48)
HAMMERTONE = (106, 120, 104)  # grey-green
HAMMERTONE_DARK = (92, 106, 90)
IRON = (66, 66, 60)
IRON_LIGHT = (120, 118, 110)
ROD = (90, 92, 88)
BAKELITE = (66, 46, 33)
BAKELITE_DARK = (40, 28, 20)
SAFETY_YELLOW = (206, 168, 52)  # faded civil-defence yellow
SAFETY_YELLOW_DARK = (168, 132, 36)
BLACK = (28, 28, 28)


def edge(light, dark, vertical=True):
    """A fill shader for sheet metal: a light edge on one side and a dark edge on the other."""
    if vertical:
        return lambda x, y, w, h: light if x == 0 else dark if x == w - 1 else None
    return lambda x, y, w, h: light if y == 0 else dark if y == h - 1 else None


def rim(colour):
    """A fill shader that draws a one-pixel border."""
    return lambda x, y, w, h: colour if min(x, y, w - 1 - x, h - 1 - y) == 0 else None


def galvanized(vertical=True):
    return edge(GALV_LIGHT, GALV_DARK, vertical)


def hammertone(x, y, w, h):
    return HAMMERTONE_DARK if (x * 5 + y * 3) % 4 == 0 else None


class Painter:
    """Paints one texture. With a multi-texture region table, pass only the regions that live on this texture."""

    def __init__(self, regions, size=32, seed=0):
        self.regions = regions
        self.size = size
        self.scale = size / 16
        self.rnd = random.Random(seed)
        self.img = [[(0, 0, 0, 0)] * size for _ in range(size)]

    def px(self, region):
        return tuple(int(round(v * self.scale)) for v in self.regions[region][-4:])

    def fill(self, region, base, jitter=6, fn=None, alpha=255):
        x0, y0, x1, y1 = self.px(region)
        for y in range(y0, y1):
            for x in range(x0, x1):
                c = base
                if fn:
                    c = fn(x - x0, y - y0, x1 - x0, y1 - y0) or base
                j = self.rnd.randint(-jitter, jitter)
                self.img[y][x] = tuple(max(0, min(255, v + j)) for v in c[:3]) + (c[3] if len(c) == 4 else alpha,)

    def put(self, region, x, y, c, alpha=255):
        x0, y0, _, _ = self.px(region)
        self.img[y0 + y][x0 + x] = tuple(c[:3]) + (c[3] if len(c) == 4 else alpha,)

    def glyph(self, region, x, y, rows, c):
        """Draws a bitmap given as strings of '#' and '.' at pixel offset x, y inside the region."""
        for dy, row in enumerate(rows):
            for dx, ch in enumerate(row):
                if ch == "#":
                    self.put(region, x + dx, y + dy, c)

    def write(self, path):
        raw = b"".join(b"\x00" + bytes(v for p in row for v in p) for row in self.img)

        def chunk(tag, data):
            return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)

        png = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", self.size, self.size, 8, 6, 0, 0, 0))
        png += chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b"")
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(png)
        print("wrote", path)


class Builder:
    """`regions` maps a material name to [u0, v0, u1, v1] on texture "#0", or to ("key", u0, v0, u1, v1) on another of
    the model's textures; `texture` is then a dict of key -> texture path."""

    def __init__(self, regions, texture, size=32, stretch=()):
        self.regions = regions
        self.texture = texture
        self.size = size
        self.stretch = set(stretch)  # regions a face always takes whole, however small the face (a sprite)

    def box(self, name, frm, to, mat, overrides=None, rotation=None, only=None, extra=None):
        """A cuboid whose faces all use region `mat`, except those named in `overrides` (face -> region).

        A face larger than its region takes the whole region; a face smaller takes the region's top-left corner.
        `only` keeps just the named faces (a lamp face on its own); `extra` adds fields such as neoforge_data.
        """
        dx, dy, dz = (to[i] - frm[i] for i in range(3))
        dims = {"north": (dx, dy), "south": (dx, dy), "east": (dz, dy), "west": (dz, dy),
                "up": (dx, dz), "down": (dx, dz)}
        faces = {}
        for face, (w, h) in dims.items():
            if only and face not in only:
                continue
            region = self.regions[(overrides or {}).get(face, mat)]
            key = "#0" if len(region) == 4 else f"#{region[0]}"
            u0, v0, u1, v1 = region[-4:]
            if (overrides or {}).get(face, mat) in self.stretch:
                uv = [u0, v0, u1, v1]
            else:
                uv = [u0, v0, round(u0 + min(w, u1 - u0), 3), round(v0 + min(h, v1 - v0), 3)]
            faces[face] = {"uv": uv, "texture": key}
        el = {"name": name, "from": list(frm), "to": list(to)}
        if rotation is not None:
            el["rotation"] = rotation
        el["faces"] = faces
        if extra:
            el.update(extra)
        return el

    def model(self, elements, render_type=None):
        textures = dict(self.texture) if isinstance(self.texture, dict) else {"0": self.texture}
        textures["particle"] = next(iter(textures.values()))
        m = {"credit": "RadioCraft", "parent": "block/block", "texture_size": [self.size, self.size],
             "textures": textures}
        if render_type:
            m["render_type"] = render_type
        m["elements"] = elements
        return m


FULL_BRIGHT = {"neoforge_data": {"block_light": 15, "sky_light": 15, "ambient_occlusion": False}}


def lamp(builder, name, frm, to, region, face="north"):
    """A full-bright lamp face on its own, for a cutout model switched in by blockstate (the parrot controller way)."""
    return builder.box(name, frm, to, region, only=(face,), extra=FULL_BRIGHT)


def write_preview(root, namespace, textures_dir, rows, size=32, spacing=20):
    """One Blockbench-only model with several models in a row, under a throwaway assets tree at `root`.

    Blockbench resolves namespaced textures from the assets folder above the model, and only on a fresh launch (a
    file handed to a running instance opens without its textures), so this gives one file to open in one go.
    `rows` is a list of (texture names, elements); the elements' "#k" references index that row's texture names.
    """
    import copy
    import shutil
    from pathlib import Path
    root = Path(root)
    (root / "assets" / namespace / "textures/block").mkdir(parents=True, exist_ok=True)
    textures, elements, k = {}, [], 0
    for i, (names, els) in enumerate(rows):
        keys = {}
        for n in names:
            shutil.copy(textures_dir / f"{n}.png", root / "assets" / namespace / "textures/block")
            keys[f"#{len(keys)}"] = f"#{k}"
            textures[str(k)] = f"{namespace}:block/{n}"
            k += 1
        for el in els:
            el = copy.deepcopy(el)
            el["name"] = f"{names[0]}/{el.get('name', '')}"
            el["from"][0] += spacing * i
            el["to"][0] += spacing * i
            if "rotation" in el:
                el["rotation"]["origin"][0] += spacing * i
            for face in el["faces"].values():
                face["texture"] = keys[face["texture"]]
            elements.append(el)
    textures["particle"] = textures["0"]
    save(root / "assets" / namespace / "models/block", f"{namespace}_preview",
         {"credit": "RadioCraft", "texture_size": [size, size], "textures": textures, "elements": elements})


def write_scene(root, namespace, textures_dir, items, size=32, name=None):
    """Like write_preview, but each item is (texture names, elements, (ox, oy, oz)) placed at its own offset, so
    multi-block assemblies can be laid out in three dimensions. Textures are shared between items by name."""
    import copy
    import shutil
    from pathlib import Path
    root = Path(root)
    (root / "assets" / namespace / "textures/block").mkdir(parents=True, exist_ok=True)
    textures, keys_by_name, elements = {}, {}, []
    for names, els, offset in items:
        keys = {}
        for n in names:
            if n not in keys_by_name:
                shutil.copy(textures_dir / f"{n}.png", root / "assets" / namespace / "textures/block")
                keys_by_name[n] = f"#{len(keys_by_name)}"
                textures[keys_by_name[n][1:]] = f"{namespace}:block/{n}"
            keys[f"#{len(keys)}"] = keys_by_name[n]
        for el in els:
            el = copy.deepcopy(el)
            el["name"] = f"{names[0]}/{el.get('name', '')}"
            for i in range(3):
                el["from"][i] += offset[i]
                el["to"][i] += offset[i]
                if "rotation" in el:
                    el["rotation"]["origin"][i] += offset[i]
            for face in el["faces"].values():
                face["texture"] = keys[face["texture"]]
            elements.append(el)
    textures["particle"] = textures["0"]
    save(root / "assets" / namespace / "models/block", name or f"{namespace}_preview",
         {"credit": "RadioCraft", "texture_size": [size, size], "textures": textures, "elements": elements})


def split_cells(builder, specs, cells):
    """Clips world-space boxes (dicts with name, frm, to, mat and optional overrides) to 16-unit cells.

    `cells` maps a cell name to its origin. Returns cell name -> elements in that cell's local space. Boxes are
    unrotated; a box crossing a cell edge becomes one piece per cell.
    """
    out = {}
    for cell, (ox, oy, oz) in cells.items():
        elements = []
        for spec in specs:
            frm = [max(spec["frm"][i], (ox, oy, oz)[i]) for i in range(3)]
            to = [min(spec["to"][i], (ox, oy, oz)[i] + 16) for i in range(3)]
            if all(to[i] > frm[i] for i in range(3)):
                local_from = [frm[i] - (ox, oy, oz)[i] for i in range(3)]
                local_to = [to[i] - (ox, oy, oz)[i] for i in range(3)]
                elements.append(builder.box(spec["name"], local_from, local_to, spec["mat"], spec.get("overrides")))
        out[cell] = elements
    return out


def save(models_dir, name, data):
    models_dir.mkdir(parents=True, exist_ok=True)
    path = models_dir / f"{name}.json"
    path.write_text(json.dumps(data, indent="\t") + "\n")
    print("wrote", path)
