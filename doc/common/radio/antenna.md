# Antenna Package Overview

This directory contains the core server‑side logic for RadioCraft antennas: immutable packet types, antenna instances, antenna networks, and concrete antenna type implementations (HF and VHF). Code lives under `src/main/java/com/arrl/radiocraft/common/radio/antenna`.

## Key Concepts

- `IAntenna`: API for an antenna instance placed in the world.
- `IAntennaType<T>`: Describes matching and RF behavior for a shape/type; owns `T extends AntennaData`.
- Packets: `IAntennaPacket` carries a signal between antennas (voice or CW) with wavelength/frequency/strength.
- Networks: Antennas of the same band are grouped and exchange packets through an `AntennaNetwork`.

## Files

- `AntennaCWPacket.java`
  - Immutable CW (Morse) packet implementing `IAntennaPacket`.
  - Holds `ServerLevel`, `Collection<CWBuffer>` (the keyed audio buffers), `wavelength`, `frequency`, mutable `strength`, and `IAntenna source`.
  - Used for CW transmissions; receivers may scale `strength` before consuming buffers.

- `AntennaVoicePacket.java`
  - Voice (SSB/voicechat) packet implementing `IAntennaPacket`.
  - Wraps `de.maxhenkel.voicechat.api.ServerLevel` and exposes the backing `net.minecraft.server.level.ServerLevel` via `getLevel()`.
  - Contains `short[] rawAudio`, `wavelength`, `frequency`, mutable `strength`, `IAntenna source`, and `UUID sourcePlayer` of the talker.

- `AntennaData.java`
  - Base class for antenna state payloads. Extends `INBTSerializable<CompoundTag>` for save/load.
  - Concrete `...types.data.*` classes derive from this.

- `StaticAntenna.java`
  - Concrete, non‑moving antenna implementation of `IAntenna` and `INBTSerializable`.
  - Binds an `IAntennaType<T>` with its `T extends AntennaData`, the world `Level`, and an atomic `AntennaPos`.
  - Handles transmit/receive for:
    - Voice: builds `AntennaVoicePacket`, applies type transmit efficiency and SWR via `SWRHelper`, forwards to peers, then receivers apply `getReceiveEfficiency` and feed an `AntennaNetworkObject`.
    - CW: builds `AntennaCWPacket`, applies type transmit/receive efficiencies similarly.
  - Joins/leaves an `AntennaNetwork` via `setNetwork(...)`.
  - Persists only the `data` object to NBT; position is managed externally.

- `AntennaNetwork.java`
  - Thread‑safe `Set<IAntenna>` (synchronized) of antennas for a band.
  - Adds/removes antennas and exposes `allAntennas()` for iteration by transmitters.

- `networks/AntennaNetworkManager.java`
  - Registry of band networks keyed by `ResourceLocation`.
  - Pre‑creates `HF_ID` and `VHF_ID` networks using `new AntennaNetwork()`.
  - `getNetwork(id)` returns the network instance for the given band.

- `BERadioNetwork.java`
  - Concurrent map of block‑entity voice receivers (`BEVoiceReceiver`) by `BlockPos`.
  - Note: Methods warn not to call from the voice thread when mutating; used by the VOIP path.

## Antenna Types (`types/`)

Common helpers:
- `NonDirectionalAntennaType<T extends AntennaData>`
  - Provides base `getTransmitEfficiency` and `getReceiveEfficiency` using distance, band model (`BandUtils.getBaseStrength`), and day/night, scaled by type constants `receive`, `transmit`, `los`, `skip`.
- `DirectionalAntennaType<T extends AntennaData>`
  - Extends non‑directional behavior and multiplies by `getDirectionalEfficiency(...)` based on geometry/orientation.

Concrete HF types:
- `DipoleAntennaType`
  - Matches a 1:1 balun with exactly two horizontal arms in opposite directions (±15° tolerance) ending at their tips.
  - `SWR`: ideal when each arm ≈ quarter‑wave; penalty grows 0.5 per block of error per arm.
  - Data: `DipoleAntennaData(armLength1, armLength2)`.

- `EndFedAntennaType`
  - Matches a 1:1 balun with a single horizontal wire arm ending at its tip.
  - `SWR`: ideal near quarter‑wave; penalty 0.5 per block of error.
  - Data: `EndFedAntennaData(length)`.

- `HorizontalQuadLoopAntennaType`
  - Matches a square horizontal loop fed by a 2:1 balun; traverses wires to confirm a closed square on same Y.
  - `SWR`: ideal when side ≈ quarter‑wave; penalty 0.5 per block.
  - Data: `HorizontalQuadLoopAntennaData(sideLength)`.

- `QuarterWaveVerticalAntennaType`
  - Matches a 1:1 balun with no wires and a vertical stack of `ANTENNA_POLE` blocks above; height is pole count.
  - `SWR`: 1.0 only at exact quarter‑wave height, otherwise 10.0 (very poor).
  - Data: `QuarterWaveVerticalAntennaData(height)`.

- `VerticalQuadLoopAntennaType` (Directional)
  - Matches a vertical square loop fed by a 2:1 balun, aligned on X or Z and fed from the bottom side.
  - Directional gain: favors off‑broadside; scales efficiency based on whether path is perpendicular to loop plane.
  - `SWR`: as horizontal loop.
  - Data: `VerticalQuadLoopAntennaData(sideLength, xAxis)`.

- `WideBandReceiveAntennaType`
  - Structure: two perpendicular iron‑bar arms at Y+2 and Y+3 above a two‑pole mast.
  - Receive‑only: `getTransmitEfficiency` returns 0.
  - Receive scaling by wavelength: 2m=0.7, 10m=0.5, 20m=0.3, 40/80m=0.2.
  - Data: `EmptyAntennaData`.

VHF types (`types/vhf/`):
- `JPoleAntennaType`
  - Matches the `J_POLE_ANTENNA` block; distance slightly compressed (÷1.3) in path loss.
  - `SWR`: only ideal on 2m band.
  - Data: `EmptyAntennaData`.

- `SlimJimAntennaType`
  - Matches the `SLIM_JIM_ANTENNA` block; disabled in thunderstorms for both TX and RX.
  - `SWR`: only ideal on 2m band.
  - Data: `EmptyAntennaData`.

- `YagiAntennaType` (Directional)
  - Matches the `YAGI_ANTENNA` block; distance compressed (÷2.0) to model gain.
  - Directional pattern based on the block’s facing: strong forward lobe, severe side/rear attenuation.
  - `SWR`: only ideal on 2m band.
  - Data: `YagiAntennaData(facing)`.

## Data Classes (`types/data/`)

- `EmptyAntennaData`: Placeholder for types with no extra parameters.
- `DipoleAntennaData`: Stores the two arm lengths.
- `EndFedAntennaData`: Stores single arm length.
- `HorizontalQuadLoopAntennaData`: Stores side length.
- `QuarterWaveVerticalAntennaData`: Stores mast height.
- `VerticalQuadLoopAntennaData`: Stores side length and axis orientation flag.
- `YagiAntennaData`: Stores `Direction facing` for pattern calculations.

## Integration Notes

- Band selection and base path‑loss are delegated to `BandUtils.getBaseStrength(...)` using `wavelength`, LOS/skip multipliers, distance, and day/night.
- SWR effects are applied at transmit time in `StaticAntenna` using `SWRHelper.getEfficiencyMultiplier(getSWR(wavelength))`.
- `AntennaNetworkObject` bridges antenna receive events to block‑entity networks via `IBENetworks`.
- Concurrency: antenna sets and radio maps use synchronized or concurrent collections to be safe with the voice thread.

