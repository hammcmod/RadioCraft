# HF Block Radio MVP

## Summary
The minimum viable HF block radio feature is a playable push-to-talk loop: two powered block radios, each connected to one valid antenna network, can tune to the same frequency, transmit microphone audio through Simple Voice Chat, and play received audio from the destination radio block.

For this milestone, the microphone and speaker are treated as built into the radio block. Detached microphone, speaker, and wiring blocks are future work.

## MVP Behavior
- Radio GUI controls must send authoritative server updates for power, SSB mode, PTT state, and frequency stepping.
- Pressing GUI PTT makes Simple Voice Chat emit microphone packets and marks the server-side radio as transmitting.
- Releasing GUI PTT, closing the GUI, or server menu removal clears server-side PTT.
- Frequency tuning is server-owned and step-based. Clients may update optimistically, but the server clamps to the radio band.
- A receiving block radio accepts voice when it is powered, in SSB receive mode, connected to exactly one antenna, and tuned within 1 kHz of the transmitted frequency.
- Received audio is played through the existing locational `BEVoiceReceiver` channel at the radio block position.

## Implementation Notes
- Use one serverbound block-radio control payload for power, SSB, PTT, and frequency-step actions.
- Keep using `VoiceTransmitters` to collect nearby player microphone audio for block radios.
- Keep using the existing antenna network route: radio -> attached antenna -> HF antenna network -> receiving antenna -> attached radio.
- Treat multiple antennas attached to one radio as an invalid overdraw/fail state for MVP.
- Keep existing SWR and antenna efficiency math, but do not add solar weather, tuner effects, fire/destruction, or full skip modeling for this milestone.

## Test Plan
- Run `./gradlew test`.
- Run `./gradlew build`.
- Manual dev-world scenario:
  - Place two same-band HF radios.
  - Connect each radio to exactly one valid matching HF antenna.
  - Power both radios and enable SSB.
  - Tune both radios to the same frequency.
  - Hold PTT on radio A and confirm audio plays at radio B.
  - Release PTT and confirm transmission stops.
  - Move radio B one frequency step away and confirm audio no longer passes.
  - Close the GUI while holding PTT and confirm transmission stops.
  - Attach multiple antennas to one radio and confirm it does not transmit successfully.

## Deferred
- Separate microphone and speaker blocks connected by wire.
- CW packet UI/network restoration.
- Antenna tuner and duplexer runtime effects.
- Solar event noise and day/night HF range changes.
- SWR fire, destruction, and other overdraw consequences.
