# Alpha 1.9 release checklist (written 2026-10-28)
**Done 2026-10-29: tagged 1.9.0 and released without the playtests (items 2 and 3 stay in NeedsTesting.md).**

State when written: all 321 GameTests and the unit tests pass; nothing since commit 554e5e37 (Sable) is committed.

1. **Publish Another Weather Mod** (E:\AnotherWeatherMod): commit the cloud fix (`CloudRenderer.java`, new
   `CloudRendererTest`), build, upload. RadioCraft does not need it to run (compat only runs when it is installed).
   - Optional: list AWM in RadioCraft's `neoforge.mods.toml` as an *optional* dependency with a minimum version, so an
     old AWM is refused cleanly. (Asked the maintainer; no answer yet.)
2. **Playtest the radio sites**: the "Radio sites and modules on the air" section at the top of
   [NeedsTesting.md](NeedsTesting.md). Worlds in `runs/client/saves`:
   - **Radio Sites Worldgen** (normal world, generated sites): DF station + 2m module 230, 69, 197; ionospheric
     observatory + 20m module 704, 66, 608; weather observatory 1040, 74, 560; repeater site 40m numbers module
     -64, 141, 1312; Long Lines 128, 1792 and buried site 656, -400 (generate on arrival).
   - **Radio Sites 2** (flat showcase): the row at z -45 (Long Lines x -45, repeater -20, ionospheric 0, weather 18,
     DF 33), the buried site at -20, -61, -20, four more Long Lines at z -80 (4, 2, 2, 2 horns).
3. **Retest the two items reset from `[!]`** in NeedsTesting: the Ionospheric Processor terminal and the HF manpack
   (fixes were noted 2026-10-27).
4. **Version tag**: the jar version comes from the latest git tag (now `1.2.0`, builds read `1.2.0-dev.92`). Decide the
   tag for alpha 1.9; tag after committing.
5. **Commit** everything (stage by folder), then tag and build.
6. **Website wiki**: rebuilt from the guide (`python tools/build_wiki.py E:/Radiocraft` in Radiocraft-Website), five
   pages changed; commit and push that repo.

Notes:
- Mountain sites (repeater, Long Lines) need ground at y 130+, so they are rare: about 20 repeater sites and 1-3 Long
  Lines per 8 x 8 km in the test seed. Lower `min_y` or loosen `max_slope` in their structure JSON if too rare.
- The dev server may still hold the `sitesworld` (normal world) setup; `tools/devserver.sh start` without the
  `DEVSERVER_*` variables goes back to the flat `devworld`.
