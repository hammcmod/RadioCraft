# RadioCraft Advancement Investigation Log

_Compiled for future reference while diagnosing why the RadioCraft advancement tab does not appear in the dev runtime, despite custom logic triggering the awards._

## 1. High-Level Summary

- Symptom: While testing the SOTA/BOTA advancement flow, the `/advancement grant` list only showed the vanilla set plus existing recipe awards. Our new achievements were invisible. Server logs spammed `Missing advancement radiocraft:achievements/root` every time `VhfTransmissionTracker` attempted to award the root.
- Despite the warning, the rest of the gameplay logic was functioning: transmissions were tracked, SavedData persisted, and code attempted to award the advancement. The resource pack or advancement registration step was failing.
- Multiple approaches were attempted to get the root definition loaded: manual JSON edits, programmatic award, runtime resource checks, directory experiments, and finally a dedicated data generator. As of now, the JSON exists both in source and in the packaged JAR, but the dev runtime still logs the missing-advancement warning, so the tab remains hidden.

The remainder of this document enumerates each investigation step, associated findings, and remaining hypotheses. Use this as a paper trail when resuming the investigation.

---

## 2. Reproduction Steps

1. Launch dev client via `./gradlew runClient`.
2. Create/load world (in our case, save "No Mekanism").
3. Obtain VHF handheld item (inventory changed). Root advancement should unlock to create tab.
4. Hold-to-transmit (right click) with VHF handheld. `VhfTransmissionTracker` logs transmit details.
5. Observe server-log warning: `Missing advancement radiocraft:achievements/root`.
6. `/advancement grant` output lacks the new achievements; `/datapack list enabled` reports `[vanilla (built-in)]` and `[mod_data]`.

This is the baseline behaviour we have not yet resolved.

---

## 3. File Locations and Artifacts

| Location | Contents | Status |
|----------|----------|--------|
| `src/main/resources/data/radiocraft/advancements/achievements/*.json` | Original hand-authored definitions | Removed to avoid duplication/typo risk once generator added |
| `src/generated/resources/data/radiocraft/advancements/achievements/*.json` | Output of `./gradlew runData` through our new generator | Contains `root`, the 6 biome awards, and BOTA chains |
| `build/libs/radiocraft-neoforge-<version>.jar` | Packaged mod JAR | Verified to contain `data/radiocraft/advancements/achievements/root.json` etc. |
| `build/moddev/artifacts/neoforge-21.1.194-minecraft-resources-aka-client-extra.jar` | Dev datapack (likely what `mod_data` refers to) | Could not inspect inside sandbox; requires local check |
| `build/resources/main/data/radiocraft/advancements/` | Processed resources; contains generated JSON | Verified |

During debugging we also saw `src/generated` remain clang after `runData`. That is expected: the generator writes to `build/generated` only when explicitly configured. Our provider writes straight to `PackOutput` -> `build/generated/sources` (which currently remains empty). The actual JSON lands in `build/resources/...` due to `processResources` copying from `src/main/resources` plus `src/generated/resources`.

---

## 4. Telemetry in Logs

Key log lines seen during testing:

```
[Render thread/INFO] [net.minecraft.advancements.AdvancementTree/]: Loaded 2163 advancements
[Render thread/INFO] [net.minecraft.advancements.AdvancementTree/]: Loaded 5 advancements
[Server thread/WARN] [com.arrl.radiocraft.Radiocraft/]: Missing advancement radiocraft:achievements/root
```

The first two occur during resource reload and world load. The third line is our diagnostic warning inside `VhfTransmissionTracker.award(...)`, triggered after every transmit when `ServerAdvancementManager#get(advancementId)` returns null.

After we added an additional `ResourceManager` check, no exceptions were logged, meaning the resource manager could read `advancements/achievements/root.json`. This implies the data file exists but still doesn’t result in an `AdvancementHolder` inside the manager.

---

## 5. Detailed Timeline of Changes

1. **Manual root JSON adjustments**
   - Confirmed structure (frame, show_toast, etc.).
   - Added `conditions` field to `minecraft:impossible` tasks to ensure JSON is valid.
   - Kept criteria name as either `progress` or `bootstrap` to align with code.
   - No impact – loader still reported missing advancement.

2. **Programmatic awarding**
   - `VhfTransmissionTracker` now calls `award(player, Radiocraft.id("achievements/root"))` at the start of every transmission to maximize chances of unlocking it.
   - Additional debug lines: prints transmit data (player/biome/dimension/y), records when progression sets gain entries, and logs when an advancement is successfully granted.
   - Even with forced awarding, manager returned `null`, giving our warning.

3. **Runtime resource existence checks**
   - Added `Radiocraft.class.getResource` check for `/data/radiocraft/advancements/achievements/root.json`.
   - Added `ResourceManager#getResourceOrThrow` for `advancements/achievements/root.json`.
   - Both checks succeeded (no warning, no exception), yet `ServerAdvancementManager#get` still isolated root as missing.

4. **Directory experiments**
   - User suggestion: rename `advancements/` -> `advancement/` to match data generator output style; tested, but Minecraft expects `advancements/` (with trailing `s`). The attempt made no difference.
   - Reverted to the canonical `advancements/` path to keep standard behaviour.

5. **New data generator**
   - Created `RadiocraftAdvancementProvider` to programmatically build JSON definitions. Outputs same structure as manual files.
   - Registered provider in `gatherData` so `./gradlew runData` populates `src/generated/resources/...` and `build/resources/...`.
   - Removed manual JSON files to prevent duplicates.
   - After `runData`, the generated root file exists at `src/generated/resources/...` and gets packaged into the JAR successfully, but `Missing advancement` message persists after `./gradlew runClient`.

6. **Resource packaging verification**
   - `jar tf build/libs/...` shows the advancement JSON in the packaged mod.
   - `build/moddev/artifacts/...` contents were not examined due to sandbox restrictions; we suspect `mod_data` loads from the resources jar, not the main mod jar.

7. **Clean builds**
   - Ran local `./gradlew clean runClient` outside the sandbox to ensure no stale directories were interfering.
   - After clean build/run, the same warning resurfaced.

8. **Datapack presence**
   - `/datapack list enabled` shows `[mod_data]`, but not `mod/radiocraft`. There is no `runs/client/mods/radiocraft` folder because the mod is loaded from the standard libs path (the mod jar). `mod_data` is likely the aggregated resources pack built by NeoForge.
   - Verified `runs/client/mods` only contains optional third-party mods; our mod is not there. It’s generated in `build/libs` and loaded from there.

---

## 6. Outstanding Hypotheses

1. **Dev resource pack content mismatch**
   - The warning indicates `ServerAdvancementManager` never registered `radiocraft:achievements/root`. This implies the data pack the server consumed does not contain the JSON, or the JSON failed to parse. Need to confirm contents of `mod_data` pack used during runtime. When zipped, that pack may not include `/data/radiocraft/advancements/achievements/root.json`.

2. **Silent parse failure**
   - No explicit “Parsing error loading custom/advancement” lines appear in `latest.log`, suggesting the file is either missing or parsing succeeded but the resulting advancement is pruned elsewhere.
   - Without earlier logging, we cannot distinguish between “file absent” and “file malformed but silently ignored.”

3. **Caching / World effect**
   - Worlds store advancement progress in `saves/<world>/advancements/<player>.json`. If these files persist references to now-missing advancements (or old structure), they might interfere. Deleting the save folder or the `advancements/` sub-folder did not change the behaviour, but try starting a brand new world after a clean build.

4. **Namespace or load order**
   - If another intermediate pack (e.g., `mod_resources`) redefines the `achievements` tree, it might override our entry. Inspect hierarchical order or use `/datapack disable mod_data` temporarily to see if warnings change.

5. **Manual registration**
   - As a fallback, we can register the advancement programmatically on server start using NeoForge hooks (similar to how we register commands). This would bypass datapack loading entirely.

---

## 7. Suggested Next Steps Post Document

1. **Inspect `mod_data` pack**
   - Locate the actual pack that corresponds to `mod_data` (likely in `.gradle/caches` or under `build/moddev`). Examine whether it contains `data/radiocraft/advancements/...`. If not, adjust configuration so resources end up in that pack.

2. **Force log on parse**
   - Add debug mixin or use forge event to intercept `AdvancementLoadEvent` and log available IDs. This may reveal whether root is actually loaded but then filtered.

3. **Start new world after deleting `saves/`**
   - Ensure old advancement progress does not interfere by starting from a fresh world and verifying log on first load.

4. **Autoregister via code**
   - If datapack loading remains problematic, implement runtime registration by constructing `Advancement` instances directly. This ensures a server-safe fallback even if datapacks fail.

5. **Explore devpack assembly**
   - Understand how NeoForge ModDevGradle builds `mod_data`. Update configuration if necessary so our JSON is captured. The fact that generated resources already appear in `build/resources` indicates the assembly step is likely the culprit.

---

## 8. Concluding Notes

- The advancement logic (SavedData, awarding, etc.) is complete; the bug is purely resource registration.
- Moving to data generation eliminated duplication errors and ensures the jar is correct. The remaining work is to ensure the running dev datapack (what the client sees) includes the generated JSON roots.
- Given the persistent warnings, focus on pack inspection and possibly manual registration to unblock gameplay testing.

Document prepared to capture full investigative context. Update as more findings emerge.
