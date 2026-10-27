"""Synthesizes the H19 terminal's whirr for assets/radiocraft/sounds/h19_whirr.ogg: the dull hum of its cooling fan
and power transformer, a three-second loop that repeats without a seam (every tone completes whole cycles in it and
the noise is filtered in a circle).

    pip install numpy soundfile
    python tools/h19_whirr.py

AprsTerminalScreen plays it, quietly, for as long as the screen is open. Mono.
"""
from pathlib import Path

import numpy as np
import soundfile as sf

RATE = 44100
SECONDS = 3.0
OUT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/radiocraft/sounds/h19_whirr.ogg"


def main():
    n = int(RATE * SECONDS)
    t = np.arange(n) / RATE
    rng = np.random.default_rng(19)
    freqs = np.fft.rfftfreq(n, 1 / RATE)
    # The fan: air rushing, a dull band low down, with a soft blade tone.
    spectrum = np.fft.rfft(rng.standard_normal(n))
    spectrum *= np.exp(-((freqs - 220) / 260) ** 2) + 0.25 * np.exp(-((freqs - 700) / 400) ** 2)
    air = np.fft.irfft(spectrum, n)
    air /= np.max(np.abs(air))
    blade = 0.25 * np.sin(2 * np.pi * 96 * t) + 0.08 * np.sin(2 * np.pi * 192 * t)  # 288 and 576 cycles a loop
    # The transformer: mains hum at 60 Hz and its harmonics.
    hum = 0.3 * np.sin(2 * np.pi * 60 * t) + 0.15 * np.sin(2 * np.pi * 120 * t) + 0.05 * np.sin(2 * np.pi * 180 * t)
    # The fan's slight unevenness, a slow wobble that also closes on the loop (once and twice per loop).
    wobble = 1 + 0.08 * np.sin(2 * np.pi * t / SECONDS) + 0.05 * np.sin(2 * np.pi * 2 * t / SECONDS + 1)
    wave = (air + blade) * wobble + hum
    wave *= 0.8 / np.max(np.abs(wave))
    OUT.parent.mkdir(parents=True, exist_ok=True)
    sf.write(OUT, wave.astype(np.float32), RATE, format="OGG", subtype="VORBIS")
    print("wrote", OUT)


if __name__ == "__main__":
    main()
