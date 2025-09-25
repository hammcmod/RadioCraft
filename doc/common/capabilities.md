# Capabilities Overview

This document summarizes the capability implementations and registrations under `src/main/java/com/arrl/radiocraft/common/capabilities`.

Capabilities expose attachable behaviors/data for blocks, items, entities, and block entities using NeoForge’s capability system.

## Package Contents

- `RadiocraftCapabilities.java` — Declares and registers all capability entry points via `RegisterCapabilitiesEvent`:
  - Block capabilities: `IBENetworks` (block-entity network graph), `IAntennaNetworkCapability` (antenna networks), `IBlockEntityCallsignCapability` (callsign storage).
  - Item capability: `IVHFHandheldCapability` for the VHF handheld item, backed by data components.
  - Entity capabilities (player): `IAntennaWireHolderCapability` (tracks a held wire anchor), `IPlayerCallsignCapability` (server-saved callsigns).
  - Wiring: attaches block caps to select blocks (e.g., charge controller, VHF receiver, HF/all-band radios) and entity/item caps to players and the VHF handheld.
- `BENetworksCapability.java` — Implementation of `IBENetworks` managing:
  - Network objects mapped by `BlockPos`, with lifecycle (`tick`, `discard`) and NBT save/load stubs.
  - Network instances keyed by `UUID`, added/removed as graphs change.
  - Serialization to `CompoundTag` for networks and objects; deserialization placeholders note creation via a registry.
- `AntennaNetworkCapability.java` — Implementation of `IAntennaNetworkCapability` storing `AntennaNetwork` instances by `ResourceLocation` id, with getters/setters.
- `AntennaWireHolderCapability.java` — Implementation of `IAntennaWireHolderCapability` that keeps a per-player mapping to a `BlockPos` anchor for antenna wire placement. Provides `getHeldPos`/`setHeldPos` using the player UUID as key.
- `VHFHandheldCapability.java` — Implementation of `IVHFHandheldCapability` tied to an `ItemStack` representing the handheld radio:
  - Persists and updates state via the `HandheldRadioState` data component (`RadiocraftDataComponent.HANDHELD_RADIO_STATE_COMPONENT`).
  - Exposes power/PTT/frequency/gain/mic gain/receive strength accessors with clamping to valid VHF band limits via `Band.getBand(2)`.
  - Tracks an installed battery `ItemStack` (temporary placeholder; comment notes future data-component persistence).
- `BasicEnergyStorage.java` — Convenience wrapper around NeoForge `EnergyStorage` with multiple constructors, setters/getters, and NBT read/write helpers for simple energy containers.

## Notes

- Capability attachment points are limited to currently active/tested content; expand `RadiocraftCapabilities.registerCapabilities` as new blocks/entities ship.
- Handheld state updates use `ItemStack.update(...)` to ensure changes sync and persist; prefer this flow for any future item-bound state.
- `BENetworksCapability` deserialization is scaffolded; creation of networks/objects should route through the BENetwork registry when enabled.
