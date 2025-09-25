# Voice Package Overview

This package integrates RadioCraft’s radio system with the Simple Voice Chat API, handling Opus encode/decode, microphone packet intake, in‑game audio channel playback, and player‑handheld radio behavior.

## Package Contents

- `src/main/java/com/arrl/radiocraft/common/radio/voice/EncodingManager.java`
  - Manages per‑player Opus encoder/decoder pairs used for converting between PCM and Opus frames.
  - `getOrCreate(UUID)` lazily creates and caches an `EncodingData` entry keyed by player UUID.
  - `close(UUID)` closes and removes a player’s encoder/decoder.
  - `EncodingData` holds an `OpusEncoder` and `OpusDecoder`, created via `RadiocraftVoicePlugin.API`; provides `reset()` to clear codec state and `close()` to release resources.

- `src/main/java/com/arrl/radiocraft/common/radio/voice/RadiocraftVoicePlugin.java`
  - Simple Voice Chat plugin entrypoint annotated with `@ForgeVoicechatPlugin` and implementing `VoicechatPlugin`.
  - Captures the `VoicechatServerApi` instance, registers event listeners, and routes decoded microphone audio into RadioCraft’s systems.
  - `onMicrophonePacket(MicrophonePacketEvent)` (server‑side):
    - Ensures `API` is initialized.
    - Decodes incoming Opus data to PCM using `EncodingManager` per sender UUID.
    - Feeds decoded audio to the player’s handheld radio (`PlayerRadioManager`), enabling radio transmission logic.
    - Iterates `VoiceTransmitters.LISTENERS` within broadcast range and forwards packets to eligible transmitters.
    - Resets codec state on silence frames (empty payloads) to avoid drift.
  - `onPlayerDisconnected` cleans up codec state for the player.
  - Contains TODO for a dedicated volume category for radios.

- `src/main/java/com/arrl/radiocraft/common/radio/voice/RadioManager.java`
  - Holds a per‑`Level` mapping to `BERadioNetwork`, creating a network on demand via `getNetwork(Level)`.
  - Provides `setNetwork(Level, BERadioNetwork)` to replace the network for a level.

### Handheld subpackage

- `src/main/java/com/arrl/radiocraft/common/radio/voice/handheld/HandheldVoiceReceiver.java`
  - Bridges received antenna audio to an entity‑bound `EntityAudioChannel` for playback via the Voice Chat API.
  - Lazily opens an `EntityAudioChannel` (one per receiver) and re‑encodes incoming PCM using `EncodingManager` before sending.
  - Applies gain based on packet signal strength prior to encoding.
  - `isReceiving` flag controls whether audio is forwarded; `setReceiving(boolean)` toggles it.

- `src/main/java/com/arrl/radiocraft/common/radio/voice/handheld/PlayerRadio.java`
  - Core container for a player’s VHF handheld radios; both an `IVoiceTransmitter`, `IVoiceReceiver`, and `IAntenna` implementation.
  - Tracks ephemeral per‑tick state for all handhelds a player carries (`SynchronousRadioState`), including:
    - Power/PTT state, frequency (kHz), item location (held/offhand/hotbar/backpack), and gain/mic gain.
    - Running sample stats to update a receive‑strength/power meter back into the item capability each tick.
  - Network integration:
    - Maintains membership in an `AntennaNetwork` (VHF) via `setNetwork` and `tick`.
    - `transmitAudioPacket` forwards player voice to other antennas on the same network, applying path loss via `BandUtils.getBaseStrength` and cloning PCM per recipient.
    - `receiveAudioPacket`/`receive` play incoming packets if any radio is powered and tuned to the packet frequency; applies strength, held‑state, and user gain, then sends over an entity audio channel.
  - Voice Chat channel usage:
    - `openChannel()` creates an `EntityAudioChannel` tied to the player’s UUID (workaround for a known bug), sets distance, and can set a category (TODO).
    - Re‑encodes to Opus using the sender’s UUID via `EncodingManager` before sending.
  - Lifecycle:
    - Weak reference to `Player` avoids forcing entity loads; `tick()` refreshes radio list, voice position/level, and antenna position each server tick.

- `src/main/java/com/arrl/radiocraft/common/radio/voice/handheld/PlayerRadioManager.java`
  - Global registry of `PlayerRadio` instances keyed by player UUID.
  - Subscribes to player login/logout/clone and server tick events:
    - On login: create and register a `PlayerRadio`.
    - On logout: detach player reference and remove the radio.
    - On clone (death): preserve/retarget the `PlayerRadio` to the new entity or recreate if missing.
    - On server tick: calls `tick()` on all `PlayerRadio`s to keep ephemeral state current.

## Data Flow Summary

1) Player speaks → Simple Voice Chat delivers `MicrophonePacketEvent` to `RadiocraftVoicePlugin`.
2) Plugin decodes Opus → PCM via `EncodingManager` and gives PCM to `PlayerRadio`.
3) `PlayerRadio` evaluates handheld state (power/PTT/frequency/gain) and, if transmitting, forwards PCM into the `AntennaNetwork` as `AntennaVoicePacket`s.
4) Receiving antennas (including other players’ radios) compute signal strength and call their receivers.
5) Receivers re‑gain PCM, re‑encode with `EncodingManager`, and send over per‑entity `AudioChannel` for in‑game playback.

## Notable Behaviors and TODOs

- Silence/empty frames reset codec state to keep Opus encoders/decoders healthy across gaps.
- `PlayerRadio` currently mixes receive gating by frequency and power; future refactor notes suggest per‑handheld receive handling and frequency support improvements.
- Volume category for handheld radios is scaffolded but disabled pending Voice Chat API behavior.

