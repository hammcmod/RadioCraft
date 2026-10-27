"""Writes the GUI textures for the microwave radio, the telephone and the HF manpack.

    python tools/link_guis.py

- microwave_radio.png / _widgets.png (300 x 200): the radio's beige rack front as on the block: chrome rack handles,
  a dark CRT the screen class fills with green text, a red PWR lamp, three cream buttons (POWER, STATUS, SETUP), the
  FREQ knob and two chrome jacks, and a dark nameplate under the screen. The widgets texture has the lamp lit and each
  button pressed, at the same places.
- telephone.png / _widgets.png (180 x 240): the desk phone seen from above: the handset across the top, a green LCD,
  the EXT key beside it, the brown keypad deck with twelve keys, and CALL and HANG keys; widgets: each key pressed.
- hf_manpack.png / _widgets.png (280 x 176): the manpack's olive drab front panel in the PRC-25's manner: the
  frequency window, BAND, FREQ, AF and MIC knobs (the screen draws their pointers), the PWR and MODE switches, the PTT
  button, a TX lamp, and the handset hanging on its hook at the right. Widgets: the switches thrown, PTT pressed, the
  lamp lit, and the empty hook (handset taken).

The box constants below are the screens' hit boxes; the Java classes repeat them.
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from blockmodel import Painter  # noqa: E402

GUI = Path(__file__).resolve().parent.parent / "src/main/resources/assets/radiocraft/textures/gui"

FONT = {
    "A": ("###", "#.#", "###", "#.#", "#.#"), "B": ("##.", "#.#", "##.", "#.#", "##."), "C": ("###", "#..", "#..", "#..", "###"),
    "D": ("##.", "#.#", "#.#", "#.#", "##."), "E": ("###", "#..", "##.", "#..", "###"), "F": ("###", "#..", "##.", "#..", "#.."),
    "G": ("###", "#..", "#.#", "#.#", "###"), "H": ("#.#", "#.#", "###", "#.#", "#.#"), "I": ("###", ".#.", ".#.", ".#.", "###"),
    "K": ("#.#", "#.#", "##.", "#.#", "#.#"), "L": ("#..", "#..", "#..", "#..", "###"), "M": ("#.#", "###", "###", "#.#", "#.#"),
    "N": ("###", "#.#", "#.#", "#.#", "#.#"), "O": ("###", "#.#", "#.#", "#.#", "###"), "P": ("###", "#.#", "###", "#..", "#.."),
    "Q": ("###", "#.#", "#.#", "###", "..#"), "R": ("###", "#.#", "##.", "#.#", "#.#"), "S": ("###", "#..", "###", "..#", "###"),
    "T": ("###", ".#.", ".#.", ".#.", ".#."), "U": ("#.#", "#.#", "#.#", "#.#", "###"), "V": ("#.#", "#.#", "#.#", "#.#", ".#."),
    "W": ("#.#", "#.#", "#.#", "###", "#.#"), "X": ("#.#", "#.#", ".#.", "#.#", "#.#"), "Y": ("#.#", "#.#", ".#.", ".#.", ".#."),
    "Z": ("###", "..#", ".#.", "#..", "###"),
    "0": ("###", "#.#", "#.#", "#.#", "###"), "1": (".#.", "##.", ".#.", ".#.", "###"), "2": ("###", "..#", "###", "#..", "###"),
    "3": ("###", "..#", "###", "..#", "###"), "4": ("#.#", "#.#", "###", "..#", "..#"), "5": ("###", "#..", "###", "..#", "###"),
    "6": ("###", "#..", "###", "#.#", "###"), "7": ("###", "..#", "..#", "..#", "..#"), "8": ("###", "#.#", "###", "#.#", "###"),
    "9": ("###", "#.#", "###", "..#", "###"), "-": ("...", "...", "###", "...", "..."), "/": ("..#", "..#", ".#.", "#..", "#.."),
    "*": ("#.#", ".#.", "###", ".#.", "#.#"), "#": (".#.#.", "#####", ".#.#.", "#####", ".#.#."), ".": ("...", "...", "...", "...", ".#."),
    " ": ("...", "...", "...", "...", "..."),
}


class Canvas(Painter):
    def __init__(self, width, height, seed):
        super().__init__({}, width, seed, height=height)

    def rect(self, box, colour, jitter=0, fn=None):
        x0, y0, w, h = box
        for y in range(y0, y0 + h):
            for x in range(x0, x0 + w):
                c = (fn and fn(x - x0, y - y0, w, h)) or colour
                if c is None:
                    continue
                j = self.rnd.randint(-jitter, jitter) if jitter else 0
                self.img[y][x] = tuple(max(0, min(255, v + j)) for v in c[:3]) + (255,)

    def disc(self, cx, cy, r, colour, jitter=0, rim=None):
        for y in range(int(cy - r - 1), int(cy + r + 2)):
            for x in range(int(cx - r - 1), int(cx + r + 2)):
                d = ((x + 0.5 - cx) ** 2 + (y + 0.5 - cy) ** 2) ** 0.5
                if d <= r:
                    c = rim if rim and d > r - 1.2 else colour
                    j = self.rnd.randint(-jitter, jitter) if jitter else 0
                    self.img[y][x] = tuple(max(0, min(255, v + j)) for v in c[:3]) + (255,)

    def text(self, x, y, s, colour, scale=1):
        for ch in s:
            for dy, row in enumerate(FONT[ch]):
                for dx, bit in enumerate(row):
                    if bit == "#":
                        self.rect((x + dx * scale, y + dy * scale, scale, scale), colour)
            x += (len(FONT[ch][0]) + 1) * scale

    def label(self, box, s, colour, scale=1):
        """Text centred in a box."""
        w = sum(len(FONT[ch][0]) + 1 for ch in s) * scale - scale
        self.text(box[0] + (box[2] - w) // 2, box[1] + (box[3] - 5 * scale) // 2, s, colour, scale)


def bevel(light, dark, size=1):
    def fn(x, y, w, h):
        if y < size or x < size:
            return light
        if y >= h - size or x >= w - size:
            return dark
        return None
    return fn


# ------------------------------------------------------------------------------------------------- microwave radio

MW_W, MW_H = 300, 200
MW_CRT = (26, 14, 200, 144)
MW_LAMP = (248, 18, 12, 12)
MW_BUTTONS = [(234, 44, 40, 16), (234, 64, 40, 16), (234, 84, 40, 16)]   # POWER, STATUS, SETUP
MW_BUTTON_LABELS = ("POWER", "STATUS", "SETUP")
MW_KNOB = (240, 110, 28, 28)
MW_JACKS = [(238, 158, 10, 10), (258, 158, 10, 10)]
BEIGE = (196, 186, 164)
BEIGE_LIGHT = (218, 210, 190)
BEIGE_DARK = (150, 140, 120)
CHROME = (196, 198, 202)
CHROME_DARK = (120, 122, 128)
CREAM = (232, 226, 206)
INK = (52, 48, 40)


def mw_button(c, box, label, down):
    x0, y0, w, h = box
    c.rect(box, (96, 90, 78))
    face = (x0 + 1, y0 + 1 + (2 if down else 0), w - 2, h - 2 - (2 if down else 0))
    c.rect(face, CREAM if not down else (210, 202, 182), 2, bevel((248, 244, 232), (190, 182, 162)))
    c.label(face, label, INK)


def mw_lamp(c, box, lit):
    x, y, w, h = box
    c.rect(box, (70, 60, 52))
    c.disc(x + w / 2, y + h / 2, w / 2 - 1, (240, 60, 50) if lit else (130, 30, 28), 6, (255, 140, 120) if lit else (90, 22, 20))


def microwave():
    c = Canvas(MW_W, MW_H, 61)
    c.rect((0, 0, MW_W, MW_H), BEIGE, 3, bevel(BEIGE_LIGHT, BEIGE_DARK, 3))
    for x in (4, MW_W - 16):                                   # rack ears with their slots
        c.rect((x, 4, 12, MW_H - 8), BEIGE_DARK, 2, bevel(BEIGE, (120, 110, 92)))
        for y in (12, MW_H - 22):
            c.rect((x + 3, y, 6, 10), (60, 56, 50))
    for x in (8, MW_W - 14):                                   # chrome handles
        c.rect((x - 1, 30, 7, 140), CHROME, 4, bevel((236, 238, 240), CHROME_DARK))
    x, y, w, h = MW_CRT                                        # the screen in its bezel
    c.rect((x - 5, y - 5, w + 10, h + 10), (70, 66, 58), 2, bevel((44, 42, 38), (110, 104, 92), 2))
    c.rect(MW_CRT, (10, 16, 12), 2, lambda px, py, pw, ph: (24, 34, 28) if (px < 2 or py < 2) else (4, 8, 6) if (px >= pw - 2 or py >= ph - 2) else None)
    c.rect((26, 166, 200, 24), (54, 52, 48), 2, bevel((80, 78, 72), (30, 28, 26)))   # the nameplate
    c.label((26, 166, 200, 24), "MW-6   6 GHZ DIGITAL MICROWAVE RADIO", (220, 210, 180))
    c.label((234, 32, 40, 8), "PWR", INK)
    mw_lamp(c, MW_LAMP, False)
    for box, label in zip(MW_BUTTONS, MW_BUTTON_LABELS):
        mw_button(c, box, label, False)
    kx, ky, kw, kh = MW_KNOB                                   # the FREQ knob: skirt, grip, no pointer (drawn live)
    c.disc(kx + kw / 2, ky + kh / 2, kw / 2, (60, 58, 56), 3, (30, 30, 30))
    c.disc(kx + kw / 2, ky + kh / 2, kw / 2 - 4, (36, 36, 36), 4, (20, 20, 20))
    c.label((kx - 6, ky + kh + 2, kw + 12, 7), "FREQ", INK)
    for box, label in zip(MW_JACKS, ("TX", "RX")):
        jx, jy, jw, jh = box
        c.disc(jx + jw / 2, jy + jh / 2, jw / 2, CHROME, 4, CHROME_DARK)
        c.disc(jx + jw / 2, jy + jh / 2, 2, (20, 20, 20))
        c.label((jx - 2, jy + jh + 2, jw + 4, 7), label, INK)
    c.write(GUI / "microwave_radio.png")
    w = Canvas(MW_W, MW_H, 62)
    mw_lamp(w, MW_LAMP, True)
    for box, label in zip(MW_BUTTONS, MW_BUTTON_LABELS):
        mw_button(w, box, label, True)
    w.write(GUI / "microwave_radio_widgets.png")


# ------------------------------------------------------------------------------------------------- telephone

TEL_W, TEL_H = 180, 240
TEL_LCD = (16, 50, 120, 30)
TEL_EXT = (142, 54, 26, 22)
TEL_KEYS = [(24 + col * 46, 96 + row * 27, 36, 22) for row in range(4) for col in range(3)]
TEL_KEY_LABELS = ("1", "2", "3", "4", "5", "6", "7", "8", "9", "*", "0", "#")
TEL_CALL = (24, 206, 62, 22)
TEL_HANG = (94, 206, 62, 22)
TEL_CREAM = (230, 222, 196)
TEL_CREAM_DARK = (190, 180, 150)
TEL_BROWN = (96, 64, 40)


def tel_key(c, box, label, down, colour=TEL_CREAM, ink=INK):
    x0, y0, w, h = box
    c.rect(box, (60, 40, 26))
    face = (x0 + 1, y0 + 1 + (2 if down else 0), w - 2, h - 2 - (2 if down else 0))
    dark = tuple(max(0, v - 30) for v in colour)
    c.rect(face, colour if not down else dark, 2, bevel(tuple(min(255, v + 20) for v in colour), dark))
    c.label(face, label, ink, 2 if len(label) == 1 else 1)


def telephone():
    c = Canvas(TEL_W, TEL_H, 71)
    c.rect((0, 0, TEL_W, TEL_H), TEL_CREAM, 3, bevel((246, 240, 220), TEL_CREAM_DARK, 3))
    c.rect((8, 6, TEL_W - 16, 36), TEL_CREAM_DARK, 2, bevel((214, 206, 180), (150, 140, 112), 2))   # the handset
    for cx in (28, TEL_W - 28):
        c.disc(cx, 24, 14, (206, 198, 172), 3, (150, 140, 112))
        for dx, dy in ((-4, -4), (4, -4), (0, 0), (-4, 4), (4, 4)):
            c.disc(cx + dx, 24 + dy, 1.2, (90, 84, 70))
    x, y, w, h = TEL_LCD
    c.rect((x - 3, y - 3, w + 6, h + 6), (110, 100, 80), 2, bevel((80, 72, 56), (180, 170, 140)))
    c.rect(TEL_LCD, (150, 170, 120), 2)
    tel_key(c, TEL_EXT, "EXT", False)
    c.rect((14, 88, TEL_W - 28, 146), TEL_BROWN, 3, bevel((130, 92, 60), (64, 42, 26), 2))   # the keypad deck
    for box, label in zip(TEL_KEYS, TEL_KEY_LABELS):
        tel_key(c, box, label, False)
    tel_key(c, TEL_CALL, "CALL", False, (120, 170, 110))
    tel_key(c, TEL_HANG, "HANG", False, (190, 90, 80))
    c.write(GUI / "telephone.png")
    w = Canvas(TEL_W, TEL_H, 72)
    tel_key(w, TEL_EXT, "EXT", True)
    for box, label in zip(TEL_KEYS, TEL_KEY_LABELS):
        tel_key(w, box, label, True)
    tel_key(w, TEL_CALL, "CALL", True, (120, 170, 110))
    tel_key(w, TEL_HANG, "HANG", True, (190, 90, 80))
    w.write(GUI / "telephone_widgets.png")


# ------------------------------------------------------------------------------------------------- HF manpack

MP_W, MP_H = 280, 176
MP_WINDOW = (18, 14, 124, 36)          # the frequency window (the screen writes in it)
MP_BAND = (160, 14, 36, 36)            # knobs: x, y, w, h (round)
MP_FREQ = (30, 62, 52, 52)
MP_AF = (104, 76, 34, 34)
MP_MIC = (150, 76, 34, 34)
MP_PWR = (18, 136, 22, 28)             # toggle switches
MP_MODE = (58, 136, 22, 28)
MP_PTT = (98, 140, 44, 22)
MP_LAMP = (156, 146, 12, 12)
MP_HOOK = (214, 10, 22, 10)
MP_HANDSET = (204, 22, 52, 126)
OLIVE = (92, 100, 62)
OLIVE_LIGHT = (118, 126, 84)
OLIVE_DARK = (62, 68, 40)
STENCIL = (226, 222, 196)


def mp_knob(c, box, big=False):
    x, y, w, h = box
    cx, cy, r = x + w / 2, y + h / 2, w / 2
    c.disc(cx, cy, r, (40, 40, 36), 3, (20, 20, 18))
    c.disc(cx, cy, r - (6 if big else 4), (28, 28, 26), 4, (14, 14, 12))
    for k in range(12 if big else 8):                          # knurling
        import math
        a = k * 2 * math.pi / (12 if big else 8)
        c.disc(cx + math.cos(a) * (r - 2), cy + math.sin(a) * (r - 2), 1, (70, 70, 64))


def mp_switch(c, box, up):
    x, y, w, h = box
    c.rect(box, (40, 42, 30), 2, bevel((70, 74, 52), (24, 26, 18)))
    c.disc(x + w / 2, y + h / 2, w / 2 - 3, (150, 152, 150), 3, (90, 92, 90))
    lever_y = y + 2 if up else y + h // 2
    c.rect((x + w // 2 - 2, lever_y, 4, h // 2 - 2), (200, 202, 200), 4, bevel((236, 238, 236), (120, 122, 120)))


def mp_ptt(c, box, down):
    x0, y0, w, h = box
    c.rect(box, (30, 30, 26))
    face = (x0 + 1, y0 + 1 + (2 if down else 0), w - 2, h - 2 - (2 if down else 0))
    c.rect(face, (54, 54, 48) if not down else (40, 40, 36), 3, bevel((80, 80, 72), (24, 24, 20)))
    c.label(face, "PTT", STENCIL)


def mp_lamp(c, box, lit):
    x, y, w, h = box
    c.rect(box, (40, 36, 30))
    c.disc(x + w / 2, y + h / 2, w / 2 - 1, (240, 70, 50) if lit else (110, 30, 24), 6, (255, 150, 120) if lit else (80, 20, 16))


def mp_handset(c, box):
    x, y, w, h = box
    c.rect((x + 10, y, w - 20, h), (34, 34, 30), 3, bevel((60, 60, 54), (18, 18, 16)))      # the grip
    c.rect((x, y, w, 26), (40, 40, 36), 3, bevel((66, 66, 58), (20, 20, 18)))              # earpiece
    c.rect((x, y + h - 26, w, 26), (40, 40, 36), 3, bevel((66, 66, 58), (20, 20, 18)))      # mouthpiece
    for k in range(5):
        c.rect((x + 8 + k * 8, y + 8, 4, 10), (22, 22, 20))
        c.rect((x + 8 + k * 8, y + h - 18, 4, 10), (22, 22, 20))
    c.rect((x + w - 14, y + 46, 8, 22), (60, 60, 54), 2, bevel((90, 90, 80), (30, 30, 26)))   # the press-to-talk bar
    for k in range(10):                                           # the coiled cord down the side
        c.disc(x + 4, y + h + 2 + k * 2.6, 2.2, (24, 24, 22), 0, (50, 50, 46))


def manpack():
    c = Canvas(MP_W, MP_H, 81)
    c.rect((0, 0, MP_W, MP_H), OLIVE, 5, bevel(OLIVE_LIGHT, OLIVE_DARK, 3))
    for x, y in ((6, 6), (MP_W - 10, 6), (6, MP_H - 10), (MP_W - 10, MP_H - 10), (190, 6), (190, MP_H - 10)):
        c.disc(x + 2, y + 2, 2, (130, 136, 100), 0, (50, 54, 34))   # rivets
    c.rect((196, 0, 4, MP_H), OLIVE_DARK, 3)                         # the seam where the handset bay starts
    x, y, w, h = MP_WINDOW
    c.rect((x - 4, y - 4, w + 8, h + 8), (40, 42, 30), 2, bevel((30, 32, 22), (100, 106, 72), 2))
    c.rect(MP_WINDOW, (14, 14, 12), 2)
    c.text(x, y + h + 6, "MHZ", STENCIL)
    mp_knob(c, MP_BAND)
    c.label((MP_BAND[0] - 4, MP_BAND[1] + MP_BAND[3] + 3, MP_BAND[2] + 8, 7), "BAND", STENCIL)
    c.text(MP_BAND[0] - 10, MP_BAND[1] + 4, "10", STENCIL)
    c.text(MP_BAND[0] + MP_BAND[2] + 2, MP_BAND[1] + 4, "20", STENCIL)
    mp_knob(c, MP_FREQ, True)
    c.label((MP_FREQ[0], MP_FREQ[1] + MP_FREQ[3] + 3, MP_FREQ[2], 7), "FREQ", STENCIL)
    mp_knob(c, MP_AF)
    c.label((MP_AF[0] - 4, MP_AF[1] + MP_AF[3] + 3, MP_AF[2] + 8, 7), "AF", STENCIL)
    mp_knob(c, MP_MIC)
    c.label((MP_MIC[0] - 4, MP_MIC[1] + MP_MIC[3] + 3, MP_MIC[2] + 8, 7), "MIC", STENCIL)
    mp_switch(c, MP_PWR, False)
    c.label((MP_PWR[0] - 6, MP_PWR[1] - 9, MP_PWR[2] + 12, 7), "PWR", STENCIL)
    mp_switch(c, MP_MODE, False)
    c.label((MP_MODE[0] - 8, MP_MODE[1] - 9, MP_MODE[2] + 16, 7), "SSB/CW", STENCIL)
    mp_ptt(c, MP_PTT, False)
    mp_lamp(c, MP_LAMP, False)
    c.label((MP_LAMP[0] - 4, MP_LAMP[1] - 9, MP_LAMP[2] + 8, 7), "TX", STENCIL)
    c.text(18, 6, "AN/PRC-HF", STENCIL)
    hx, hy, hw, hh = MP_HOOK
    c.rect(MP_HOOK, (150, 152, 150), 3, bevel((210, 212, 210), (90, 92, 90)))
    mp_handset(c, MP_HANDSET)
    c.write(GUI / "hf_manpack.png")

    w = Canvas(MP_W, MP_H, 82)
    mp_switch(w, MP_PWR, True)
    mp_switch(w, MP_MODE, True)
    mp_ptt(w, MP_PTT, True)
    mp_lamp(w, MP_LAMP, True)
    # The empty hook bay, drawn over the handset when the mic is out.
    w.rect((MP_HANDSET[0] - 2, MP_HANDSET[1] - 2, MP_HANDSET[2] + 4, MP_HANDSET[3] + 30), OLIVE, 5)
    w.rect(MP_HOOK, (150, 152, 150), 3, bevel((210, 212, 210), (90, 92, 90)))
    w.label((MP_HANDSET[0], MP_HANDSET[1] + 50, MP_HANDSET[2], 7), "MIC OUT", STENCIL)
    w.write(GUI / "hf_manpack_widgets.png")


if __name__ == "__main__":
    microwave()
    telephone()
    manpack()
