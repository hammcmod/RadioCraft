# Development Checklist

The 2026-10 bug audit (save/load, network linking, power ticking, static, 40m band) and what is still open are
listed in [doc/Testing.md](doc/Testing.md#bug-audit-2026-10); each fix has a test.

## Current plan (2026-10): HF radios fully working, with solar weather

In order. Each step gets GameTests where the server side can be checked; audio still needs clients and ears.

1. ~~**Prove two HF radios can talk.**~~ Done (2026-10-19): `TransmissionGameTests` run two 20m stations through
   the antenna network for voice and CW, checking strength, wrong frequency, wrong mode, power off and no antenna.
   CW works end to end again (`SCWBufferPacket` / `CCWBufferPacket`, `SET_CW`). `rcdebug transmit` / `rcdebug key`
   do the same on the dev server. Still to hear by ear in a client: morse tone playback and voice through SVC.
2. **Make solar weather shape propagation**, not just the static level: skip distance and strength per band, day vs
   night, blackouts killing HF. Open questions for the maintainer: which bands each event helps or hurts and by how
   much; per-dimension (as now) or global events; whether the solar weather station block should show the event now.
3. **Static masking and squelch.** Send received strength with the audio; the client ducks the static sound so a
   signal masks static and static wins as the signal weakens. Verify by ear.
4. **Power.** Radios draw from batteries and switch off when drained; the charge controller switch does something.
5. **VHF (FM) static model.** Mostly silent except on very weak signals. After the HF static work settles.

## Phase 1 – Core Gameplay Readiness

### Radios & Networking
- [x] Core HF radios (10m/20m/40m/80m) and their recipes are registered
- [ ] Provide working SWR and propagation hooks so core HF radios are usable
- [x] HF receiver, QRP radios, and the all-band radio shapeless combinations exist
- [x] VHF handhelds have functional radios
- [x] Comparator S-meter (2026-10-20): a comparator against any radio reads the received signal strength 0-15, falling
  back to 0 a second after the signal ends. Jade shows the same reading, the interference level and the repeat delay.

### Antennas & Signal Model
- [x] Dipole, end-fed, quarter-wave vertical, loop, wide-band receive, and VHF Yagi/J-pole/Slim Jim antennas are implemented
- [x] Antenna tuner runtime effect (2026-10-20): a tuner anywhere on the coax between a radio and its antenna is an
  automatic tuner. Within `antenna_tuner_max_swr` (default 6) the radio sees SWR 1 (static, interference and the
  radio's SWR all follow) and transmit keeps `1 - mismatchLoss * antenna_tuner_loss` (default 0.25, so a SWR 3
  antenna goes from 75% to 94% power); beyond it the tuner does nothing. The tuner GUI shows antenna SWR, what the
  radio sees and the state; the dials are still decorative. Overdraw (two radios or two antennas) is not tunable.
  Deviation from the proposal: the tuner may sit anywhere on the run, not only radio-side of the balun.
- [x] Inverted V dipole (2026-10-20): 1:1 balun up high, two opposite arms sloping down to lower ends (apex 90° or
  wider, ends may differ in height). Resonant arm length is 5% under a quarter wave, measured along the wire; SWR
  rises 0.5 per block like the dipole; transmit is 0.9 of a dipole's, 0.75 on 40m (proposal numbers).
- [x] Random wire (2026-10-20): new 9:1 balun block (placeholder texture, see the bounty) with one wire run of any
  shape, bending at poles/connectors, no forks or loops. Usable on any band where the run is at least a half wave;
  SWR is 2 + 0.5 per block off a half-wave multiple, so it always needs the tuner; gain -3 dBi both ways. Deviation
  from the proposal: fed straight from the 9:1 balun (a 1:1 balun in series would count as a second antenna).
- [ ] Duplexer runtime effect (repeater protection, losses) to unblock repeater work
- [ ] Implement antenna analyzer UI and data capture
- [ ] Implement shared antenna behavior hooks so antenna blocks drive propagation
- [ ] Hook SWR consequences (fire/destruction) into `RadioBlockEntity`
- [ ] Integrate solar event noise and day/night skip changes so HF radios gain or lose range based on `SolarEventManager`

### Events, Commands & Config
- [x] Callsign capability with `/callsign` tools implemented
- [ ] Implement administrative callsign commands (e.g., `/callsign set`) with permission checks
- [ ] Extend server config to cover callsign digit pools, HF static squelch, modern/vintage toggles, and “talk everywhere” overrides promised in the proposal
- [ ] Add `/bandcalculate` helper to complement antenna tuning
- [x] Redstone interference, receive side (2026-10-20): redstone switching near the feedline/antenna boosts the
  radio's static, band and distance dependent, server configurable. Design in doc/RedstoneInterference.md.
- [x] Redstone interference, transmit side (2026-10-20): transmitting induces a level in nearby dust (mixin on the
  dust's power calculation) and flips levers, buttons and torches; reach by rig power and SWR, verticals only
  sideways, predictable positions, never on command blocks. Design in doc/RedstoneInterference.md.
- [ ] Redstone interference polish: a real buzz loop (asset bounty), chokes that cut feedline pickup, the
  interference term joining static masking once plan step 3 lands, and handheld radios (no block antenna) on transmit.

## Phase 2 – Power Systems

### Power & Components
- [ ] Make the solar panel and batteries actually use FE/other energy sources
- [x] Wire block recipe implemented
- [x] Waterproof wire crafting implemented
- [x] Radio crystal/speaker/microphone/HF circuit board recipes exist, albeit with lighter-cost ingredients than the proposal (iron/planks/redstone torch instead of diamond/iron)
- [ ] Finish small alkaline battery energy handling so `SmallBatteryItem` tracks charge and removes the “not implemented” message
- [ ] Implement handheld battery drain and Curios slot behavior in `VHFHandheldItem#inventoryTick`
- [ ] Implement large battery behavior (stacking capacity, discharge, explosion-on-fire) and update tooltips accordingly; power draw from radios should respect battery levels
- [x] Solar panel recipe uses glass + a daylight detector rather than kelp/iron
- [ ] Implement wire–water electrocution rules and battery ignition on short circuits in `WireBlock` and related power logic

### Radios & Networking
- [ ] Tie radio power draw, on/off state, and static playback into battery/solar networks via `RadioNetworkObject`
- [ ] Implement HF static/grounding modifiers so propagation feedback reaches the player
- [x] Lightning spikes (2026-10-20): a strike puts a crash of static on every powered radio in the dimension, fading
  with distance and by band, server configurable (`Lightning Static`). Design in doc/RedstoneInterference.md.

## Phase 3 – Extended Systems & Content

Models and textures we want drawn by someone else are listed in [doc/ModelDesignBounty.md](doc/ModelDesignBounty.md).

### VHF/UHF Channel and Repeater Features
- [x] Simplex repeat controller ("parrot", id `parrot_controller`): links to the radio next to it and makes it echo
  what it receives after a delay, with receive/transmit LEDs and a wire drawn to the radio (2026-10-20; model and
  textures by the maintainer). Its recipe is a placeholder pending the recipe review below.
- [x] Parrot controller polish (2026-10-20): a button click when the delay changes, a comparator output of the delay
  in seconds, Jade lines (delay, linked side, state).
- [ ] Parrot controller: a way to pick the linked radio when two are adjacent (currently the first side found,
  north-east-south-west)
- [ ] Implement VHF repeater block entity, callsign prompt, duplexer validation, and sleep/wake cycle
- [ ] Add multi-channel scan indicator to VHF receiver UI

### Digital & APRS Features
- [ ] Implement digital interface/TNC block with data transfer, APRS messaging, and Curios integration
- [ ] Add `/aprs beacon` and `/aprs broadcast` commands plus in-game item transfer

### Antenna Expansions
- [ ] Implement Moxon antenna variants (VHF/10m/20m) and register antenna types
- [ ] Revisit delta loop construction to support triangular build flow and align analyzer messaging
- [ ] Add sag-varying visuals for long unsupported antenna wire runs

### World & Progression Additions
- [x] Solar weather station, minimal (2026-10-20): right click reports the current conditions, noise floor and whether
  10m is open (solar event JSONs carry an `openBands` list; good and great open 10m); a comparator reads 1 (blackout)
  to 6 (great); Jade shows the same. Nothing uses "10m open" for propagation yet (plan step 2).
- [ ] Solar weather station, rest of the proposal: printed paper report (consumes paper, hopper fed, redstone
  activated), best-band calculator between two coordinates
- [ ] Improve wire spool storage UX (bulk crafting/collapse)
- [ ] Introduce random structures, auto-beacon radios, APRS Discord bridge, and external radio API hooks
- [ ] Review all recipes together (maintainer, 2026-10-20): align crafting costs (e.g., netherite-heavy radios) with the original spec or update the design doc to reflect lighter recipes; decide the simplex repeat controller recipe
