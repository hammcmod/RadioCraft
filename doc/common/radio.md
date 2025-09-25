# Radio Subsystem Overview

This document outlines the structure and responsibilities of the radio subsystem under `src/main/java/com/arrl/radiocraft/common/radio`.

- For antenna-specific details, see `doc/common/radio/antenna.md`.
- For voice/VOIP specifics, see `doc/common/radio/voice.md`.

## Package Contents

- `Band.java` — Defines radio frequency bands used by gameplay systems.
- `BandUtils.java` — Utilities for band math, lookups, and validation.
- `BEVoiceReceiver.java` — Block entity implementation that can receive voice transmissions.
- `IVoiceReceiver.java` — Interface for components capable of receiving voice audio.
- `SWRHelper.java` — Standing Wave Ratio helpers for antenna efficiency and mismatch loss.
- `VoiceTransmitters.java` — Registry/helpers for voice transmitter sources.

### Subpackages

- `antenna/` — Antenna networks, packets, types, and data models.
  - See `doc/common/radio/antenna.md` for full documentation.
- `morse/` — Morse/CW buffering and processing utilities.
  - See `doc/common/radio/morse.md` for CW buffering, send/receive flow, and integration notes.
- `voice/` — Voice transport, encoding, and handheld radio integration.
  - See `doc/common/radio/voice.md` for full documentation.
 - `solar/` — Solar events and data reload listener.
  - See `doc/common/radio/solar.md` for full documentation.

## Notes

- The radio core coordinates band definitions, propagation helpers (e.g., SWR), and I/O contracts (`IVoiceReceiver`).
- Subpackages encapsulate domain concerns (antenna physics, morse I/O, solar modifiers, voice transport) to keep gameplay logic modular and data-driven.
