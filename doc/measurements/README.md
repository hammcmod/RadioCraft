# Measurement Artifacts

This directory captures raw instrument exports that feed in-game simulation data.
For antennas, NanoVNA Touchstone traces (`.s1p`) live under `antennas/` and are
converted into compact JSON during development.

## Updating Antenna Profiles

1. Export the sweep from the NanoVNA as an `.s1p` file using real/imaginary
   format (`# HZ S RI R 50`).
2. Drop the file into `doc/measurements/antennas/` for safekeeping.
3. Run the converter to regenerate the runtime JSON:

   ```bash
   python tools/antenna_converter.py doc/measurements/antennas/<input>.s1p \
     src/main/resources/data/radiocraft/antenna_profiles/<path>.json
   ```

   For the stock Rubber Ducky antenna this produces `rubber_ducky/vhf.json`
   and `rubber_ducky/uhf.json`.
4. Launch `./gradlew runData` if additional data packs depend on the refresh,
   then review and commit the resulting diff.

The JSON profiles are hot-reloadable via Minecraft's resource reload path, so
client and server instances pick up new sweeps without recompiling the mod.
