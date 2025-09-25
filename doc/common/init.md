# Init Package Overview

This document covers the registry and bootstrap classes under `src/main/java/com/arrl/radiocraft/common/init`.

These classes wire Radiocraft content into NeoForge/Minecraft registries, subscribe to lifecycle events, and expose helpers used across the mod.

## Package Contents

- `RadiocraftBlocks.java` — Registers all blocks. Groups power blocks (wire, waterproof wire, solar panel, large battery, charge controller), radio blocks (VHF base/receiver/repeater, HF radios and receivers, QRP radios, digital interface), antenna blocks (duplexer, tuner, connectors, baluns, specific antennas), plus utility blocks (solar weather station, microphone). Provides `simpleBlock(name, properties)` helper.
- `RadiocraftItems.java` — Registers items and block items. Includes crafting parts (radio crystal, speaker, hand microphone, HF circuit board, ferrite/coaxial cores), functional items (small battery, VHF handheld with `HandheldRadioState`, antenna wire), and `BlockItem` wrappers for registered blocks. Provides `simpleItem(...)` with optional tooltip and `simpleBlockItem(...)` helpers.
- `RadiocraftBlockEntities.java` — Registers `BlockEntityType`s for power blocks and radio hardware (HF variants, receivers, QRP, VHF base/receiver) and a shared antenna block entity type for multiple antenna blocks.
- `RadiocraftEntityTypes.java` — Registers custom `EntityType`s. Currently `AntennaWire` (misc, tiny bounding box, no velocity updates) for in‑world wire spans.
- `RadiocraftTabs.java` — Declares the Creative Mode tab. Populates development‑only entries when `RADIOCRAFT_DEV_ENV` is true, and always‑available release items/ingredients. Exposes `CREATIVE_TABS` register and `RADIOCRAFT_TAB` supplier.
- `RadiocraftCommands.java` — Subscribes to `RegisterCommandsEvent` and registers command trees from `CallsignCommands` and `SolarWeatherCommands`.
- `RadiocraftData.java` — Subscribes to data pack reload via `AddReloadListenerEvent` and adds `SolarEventReloadListener` under the key `solar_events`.
- `RadiocraftDataComponent.java` — Registers data components using NeoForge `DeferredRegister.DataComponents` for:
  - `energy` — `EnergyRecord` with persistent and network codecs.
  - `handheld_radio_state` — `HandheldRadioState` with persistent and network codecs.
- `RadiocraftSavedData.java` — On level load, initializes server‑side saved data singletons for player callsigns and block entity callsigns.
- `RadiocraftSoundEvents.java` — Registers fixed‑range `SoundEvent`s: `static` and `morse` (32f radius).
- `RadiocraftAntennaTypes.java` — Registers antenna simulation types into the API registry (`AntennaTypes`): HF (dipole, end‑fed, horizontal/vertical quad loops, quarter‑wave vertical) and VHF (J‑Pole, Slim Jim, Yagi).
- `RadiocraftTags.java` — Declares block tag keys used by gameplay systems: `antenna_blocks`, `antenna_wire_holders`, `coax_blocks`, `power_blocks`.
- `BENetworkTypes.java` — Wires block‑entity networks and objects into the BENetwork registry: networks (coaxial, power) and objects (default, solar panel, battery, radio, antenna, charge controller). Integrates with `AntennaNetworkManager` for antenna network IDs.

## Notes

- All registries use NeoForge `DeferredRegister` and should be attached to the mod event bus during common setup.
- Item/block parity: most blocks have a corresponding `BlockItem` registered in `RadiocraftItems` for creative access and placement.
- Development gating: `RadiocraftTabs` hides WIP content in release builds to keep the creative tab clean.
