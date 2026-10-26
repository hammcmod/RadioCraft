"""Paints the WEFAX receiver's screen textures, in the manner of the teletype's (teletype_machine.png and its paper
sheet), as a 2D drawing of the wefax_receiver block model with a little more detail.

    python tools/wefax_gui.py

Writes under assets/radiocraft/textures/gui:

- wefax_receiver.png (256x256): the machine. Blit rows TOP..256 at the screen's origin, as TeletypeScreen does.
- wefax_receiver_paper.png (256x256): the sprites the screen moves about:
    PAPER      the chart sheet, white, the mod draws the chart on it; it slides up out of the slot behind the drum
    BUTTONS    WX REPT, SOLR WX and AUTO, pressed (the machine texture has them up)
    LEVELS     the paper supply window, empty through full, five steps
    LAMP_ON    the lit power lamp

Every box below is {x, y, width, height} in texture pixels, so the screen can copy them.
"""
import random
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from blockmodel import Painter  # noqa: E402

GUI = Path(__file__).resolve().parent.parent / "src/main/resources/assets/radiocraft/textures/gui"
SIZE = 256
TOP = 84                      # the machine starts this far down the texture; the screen is 256 x (256 - TOP)
                              # and the paper rises into the space above the slot

# ---- the machine's layout in wefax_receiver.png (texture pixels)
HOUSING = (16, 146, 224, 64)   # the rear housing holding the drum; the paper slot is along its top
SLOT = (60, 140, 136, 7)       # the paper comes up out of here, behind the drum
DRUM = (40, 152, 176, 28)      # the chart drum
BASE = (8, 210, 240, 40)       # the plinth with the controls
BUTTONS = [(28, 220, 44, 18), (80, 220, 44, 18), (132, 220, 44, 18)]   # WX REPT, SOLR WX, AUTO, up
LEVEL = (196, 216, 20, 28)     # the paper supply window
LAMP = (226, 222, 8, 8)        # the power lamp

# ---- sprites in wefax_receiver_paper.png
PAPER = (0, 0, 136, 136)
BUTTONS_DOWN = [(0, 144, 44, 18), (48, 144, 44, 18), (96, 144, 44, 18)]
LEVELS = [(0 + 24 * i, 168, 20, 28) for i in range(5)]
LAMP_ON = (144, 144, 8, 8)

TAN = (205, 186, 150)
TAN_LIGHT = (224, 208, 174)
TAN_DARK = (168, 148, 112)
EDGE = (112, 94, 64)
GREY = (142, 142, 140)
GREY_LIGHT = (196, 196, 192)
GREY_DARK = (84, 84, 82)
SLOT_DARK = (40, 36, 30)
CREAM = (236, 230, 212)
INK = (52, 48, 40)
PAPER_WHITE = (246, 244, 236)
RED = (160, 30, 30)
RED_LIT = (240, 70, 60)

FONT = {  # 3 x 5
    "A": ("###", "#.#", "###", "#.#", "#.#"), "E": ("###", "#..", "##.", "#..", "###"),
    "L": ("#..", "#..", "#..", "#..", "###"), "O": ("###", "#.#", "#.#", "#.#", "###"),
    "P": ("###", "#.#", "###", "#..", "#.."), "R": ("###", "#.#", "##.", "#.#", "#.#"),
    "S": ("###", "#..", "###", "..#", "###"), "T": ("###", ".#.", ".#.", ".#.", ".#."),
    "U": ("#.#", "#.#", "#.#", "#.#", "###"), "W": ("#.#", "#.#", "#.#", "###", "#.#"),
    "X": ("#.#", "#.#", ".#.", "#.#", "#.#"), " ": ("...", "...", "...", "...", "..."),
}
LABELS = ("WX REPT", "SOLR WX", "AUTO")


class Canvas(Painter):
    def __init__(self, seed):
        super().__init__({}, SIZE, seed)

    def rect(self, box, colour, jitter=0, fn=None):
        x0, y0, w, h = box
        for y in range(y0, y0 + h):
            for x in range(x0, x0 + w):
                c = (fn and fn(x - x0, y - y0, w, h)) or colour
                if c is None:
                    continue
                j = self.rnd.randint(-jitter, jitter) if jitter else 0
                self.img[y][x] = tuple(max(0, min(255, v + j)) for v in c[:3]) + (c[3] if len(c) == 4 else 255,)

    def text(self, x, y, s, colour):
        for ch in s:
            for dy, row in enumerate(FONT[ch]):
                for dx, bit in enumerate(row):
                    if bit == "#":
                        self.img[y + dy][x + dx] = colour + (255,)
            x += 4


def bevel(light, dark, size=1):
    def fn(x, y, w, h):
        if y < size or x < size:
            return light
        if y >= h - size or x >= w - size:
            return dark
        return None
    return fn


def button(c, box, label, down):
    x0, y0, w, h = box
    c.rect(box, EDGE)
    face = (x0 + 1, y0 + 1 + (2 if down else 0), w - 2, h - 2 - (2 if down else 0))
    c.rect(face, CREAM if not down else (214, 206, 186), 2, bevel((248, 244, 232), (190, 182, 162)))
    tw = len(label) * 4 - 1
    c.text(face[0] + (face[2] - tw) // 2, face[1] + (face[3] - 5) // 2, label, INK)


def level_window(c, box, level):
    x0, y0, w, h = box
    c.rect(box, EDGE)
    c.rect((x0 + 1, y0 + 1, w - 2, h - 2), SLOT_DARK)
    sheets = level * 5              # five sheets a step, each a line of white with a shadow
    for i in range(sheets):
        y = y0 + h - 3 - i
        c.rect((x0 + 3, y, w - 6, 1), PAPER_WHITE if i % 2 == 0 else (200, 196, 186))


def paint_machine():
    c = Canvas(1)
    # the plinth, lit from above
    c.rect(BASE, TAN, 3, bevel(TAN_LIGHT, TAN_DARK, 2))
    c.rect((BASE[0], BASE[1] + BASE[3] - 6, BASE[2], 6), TAN_DARK, 3)
    c.rect((BASE[0] + 2, BASE[1] + BASE[3] - 2, BASE[2] - 4, 2), EDGE)
    # the rear housing: its top face seen from above, a darker right side, and the paper slot along the top
    c.rect(HOUSING, TAN, 3, bevel(TAN_LIGHT, TAN_DARK, 2))
    c.rect((HOUSING[0] + 2, HOUSING[1], HOUSING[2] - 4, 10), TAN_LIGHT, 3)
    c.rect((HOUSING[0] + 2, HOUSING[1] + 10, HOUSING[2] - 4, 1), TAN_DARK)
    c.rect((HOUSING[0] + HOUSING[2] - 8, HOUSING[1] + 10, 6, HOUSING[3] - 12), TAN_DARK, 3)
    c.rect(SLOT, SLOT_DARK, 2, bevel(EDGE, (20, 18, 14)))
    c.rect((SLOT[0] - 4, SLOT[1] - 2, SLOT[2] + 8, 2), GREY_DARK)   # the paper guide bar
    # the drum: a cylinder shaded round, in a cradle, on two end bearings
    x0, y0, w, h = DRUM
    c.rect((x0 - 4, y0 + h - 6, w + 8, 10), TAN_DARK, 3, bevel(EDGE, TAN))
    def drum(x, y, w, h):
        t = y / (h - 1)
        base = ((214, 214, 210) if t < 0.12 else GREY_LIGHT if t < 0.3 else GREY if t < 0.62 else
                (112, 112, 110) if t < 0.85 else GREY_DARK)
        if x < 3 or x >= w - 3:
            return EDGE if y in (0, h - 1) else GREY_DARK
        if y in (0, h - 1):
            return EDGE
        if x % 24 == 12 and 0.3 <= t < 0.62:
            return (152, 152, 150)   # faint rings
        return base
    c.rect(DRUM, GREY, 2, drum)
    c.rect((x0 - 8, y0 + 4, 8, h - 8), TAN_DARK, 2, bevel(TAN, EDGE))
    c.rect((x0 + w, y0 + 4, 8, h - 8), TAN_DARK, 2, bevel(TAN, EDGE))
    # the stylus bar across the drum
    c.rect((x0 + 6, y0 + h - 10, w - 12, 3), GREY_DARK, 2, bevel((120, 120, 118), (50, 50, 48)))
    # a nameplate
    c.rect((HOUSING[0] + 10, HOUSING[1] + 40, 42, 9), (170, 150, 90), 3, bevel((200, 180, 110), (120, 100, 50)))
    c.text(HOUSING[0] + 13, HOUSING[1] + 42, "WX", (70, 56, 30))
    # the control bar: a darker strip holding the buttons, the paper window and the lamp
    c.rect((BASE[0] + 12, BASE[1] + 6, BASE[2] - 24, 28), TAN_DARK, 2, bevel(EDGE, TAN_LIGHT))
    for box, label in zip(BUTTONS, LABELS):
        button(c, box, label, False)
    level_window(c, LEVEL, 0)
    c.rect(LAMP, RED, 4, bevel((190, 60, 60), (100, 20, 20)))
    c.write(GUI / "wefax_receiver.png")


def paint_paper():
    c = Canvas(2)
    x0, y0, w, h = PAPER
    def sheet(x, y, w, h):
        if y >= h - 3 and (x * 7 + y * 3) % 5 < 2:
            return (0, 0, 0, 0)                        # a torn bottom edge
        if x in (0, w - 1):
            return (214, 210, 198)
        if y == 0:
            return (226, 222, 210)
        return None
    c.rect(PAPER, PAPER_WHITE, 2, sheet)
    for box, label in zip(BUTTONS_DOWN, LABELS):
        button(c, box, label, True)
    for i, box in enumerate(LEVELS):
        level_window(c, box, i)
    c.rect(LAMP_ON, RED_LIT, 4, bevel((255, 130, 120), (200, 40, 40)))
    c.write(GUI / "wefax_receiver_paper.png")


if __name__ == "__main__":
    paint_machine()
    paint_paper()
