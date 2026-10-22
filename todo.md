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
4. **Power.** ~~Radios draw from batteries and switch off when drained~~ Done (2026-10-21): block radios draw
   their receive/transmit cost per tick from the power network and brown out (panel cycles on / PWR / dark, no
   static) when starved; `radios_need_power` server config; `PowerGameTests`. Still open: the charge controller
   switch does something; VHF repeater power; the small battery / handheld drain.
5. **VHF (FM) static model.** Mostly silent except on very weak signals. After the HF static work settles.
6. **Update to Minecraft 26.3** and the matching NeoForge (maintainer, 2026-10-21), once the HF work above settles.
   Expect the usual port churn: GUI and render APIs, registries, mixin targets, and new Simple Voice Chat, Jade,
   TOP and GeckoLib versions.

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
- [ ] Add in SWR consequences (maintainer, 2026-10-21): high SWR, and any long transmission with no antenna, should
  damage the radio. For now a radio with no antenna just works at a few blocks' range.
- [ ] Dummy load block (maintainer, 2026-10-21): a coax load to transmit into safely, once SWR has consequences
- [ ] Implement duplexer tuning (maintainer, 2026-10-21): the six screws on the duplexer screen tune the RX and TX
  cavities; a badly tuned duplexer loses signal and lets the transmitter desense the receiver
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
- [x] VHF repeater (2026-10-21): input/output frequencies, CTCSS access and output tones, CW ID in Morse tone, hang
  time with courtesy beep, timeout timer, MIC/TX/SPEAKER, duplexer on the back/top/bottom (kerchunks without one),
  bare connector without an antenna. Handhelds and the base station got a tone and a ±600 kHz offset.
- [ ] Repeater: sleep/wake cycle (from the original plan) not done; the repeater is not in the release creative tab yet.
- [ ] Add multi-channel scan indicator to VHF receiver UI

### Digital & APRS Features
- [x] Digital interface (TNC), first slice (2026-10-21): packets ride the radio like voice (AFSK burst everyone on
  the frequency hears, then the packet decodes whole if on frequency and above the noise, `tnc_min_snr`); APRS
  beacons (button, redstone pulse, timer) with a map of heard stations, messages to a callsign or ALL with an unread
  light/buzzer/comparator, digipeating across two radios, `/aprs beacon` and `/aprs broadcast`. No printer block:
  paper/book/map supplies will live in the TNC (maintainer, 2026-10-21).
- [ ] TNC APRS map: real terrain under the heard stations; maintainer recalls a map mod considered as a dependency
- [x] TNC RTTY (2026-10-21): live typing at 45.45 baud, prints a line per sender, 20 lines per paper, garbles when
  weak; supplies by right click, hopper or the FILES slots
- [x] TNC FILES (2026-10-21): books, paper and maps (SSTV, from the map's picture) to a callsign or everyone; each
  receiver prints a copy on its own blank, the sender keeps the original; progress on the six LEDs
- [ ] TNC FILES: real inventory panel art (bounty); for now the vanilla chest's bottom half
- [ ] TNC single-radio digipeating (classic APRS, 8-hop cap, toggle); deferred by the maintainer 2026-10-21
- [ ] TNC: handheld/Curios side (APRS from a handheld radio), message acks and retries

### Antenna Expansions
- [ ] Implement Moxon antenna variants (VHF/10m/20m) and register antenna types
- [ ] Figure out how we'll allow building of HF yagis (maintainer, 2026-10-21)
- [ ] Hustler 5-BTV multiband vertical: antenna type once the model is drawn (bounty, 2026-10-21)
- [ ] Revisit delta loop construction to support triangular build flow and align analyzer messaging
- [ ] Add sag-varying visuals for long unsupported antenna wire runs
- [x] 4:1 balun and off-centre-fed dipole (2026-10-21, maintainer): new `balun_four_to_one` feeding an OCFD
  (short arm a third to two thirds of the long one, matched on the half wave and its 2nd and 4th harmonics, poor
  on the odd ones). The 2:1 stays for the quad loops. The 4:1 texture is a copy of the 2:1 art (bounty).
- [ ] Loop antennas (maintainer, 2026-10-21): the horizontal and vertical quad loops exist on the 2:1 balun but
  have never been played with; the proposal's delta loop (4:1, corner fed, each side a third of a wavelength) is
  not implemented. Play the quads, then decide on the delta loop and the loop gains.
- [x] Release creative tab (2026-10-21, maintainer): the HF station (radios, receivers, VHF base station, coax,
  wire holders, baluns, tuner, VHF antennas, parrot controller, weather station) moved out of the dev-only list
  and lost the dev watermark. Still dev-only: duplexer, repeater, digital interface, dish, microphone block, power.
- [x] Solar panels join by touching (2026-10-21, maintainer request): a row of panels and the charge controller at
  its end are one network without wire; batteries still need wire. Jade shows a panel's output and the row's.
  Open: a device removed from the middle of a touching row does not split the row (wire removal does).
- [x] Wire Passthrough (2026-10-21, maintainer request): an item that turns a plain solid block into a conductor
  for both wire kinds while keeping its look (`PassthroughBlock` + mimic block entity + renderer). Open: should
  Jade name the mimicked block; a 2-thick wall needs two of them.
- [x] Wires hug walls (2026-10-21, maintainer request): wire, waterproof wire and coax climb a solid block like
  redstone dust and link diagonally to the wire or device on top, instead of a floating vertical piece. Also gave the
  waterproof wire the blockstate it never had.

### World & Progression Additions
- [x] Solar weather station, minimal (2026-10-20): right click reports the current conditions, noise floor and whether
  10m is open (solar event JSONs carry an `openBands` list; good and great open 10m); a comparator reads 1 (blackout)
  to 6 (great); Jade shows the same. Nothing uses "10m open" for propagation yet (plan step 2).
- [ ] Solar weather station, rest of the proposal: printed paper report (consumes paper, hopper fed, redstone
  activated), best-band calculator between two coordinates
- [ ] Server setting to make the solar weather station require a powered VHF radio (maintainer, 2026-10-21)
- [ ] Improve wire spool storage UX (bulk crafting/collapse)
- [ ] Introduce random structures, auto-beacon radios, APRS Discord bridge, and external radio API hooks
- [ ] Review all recipes together (maintainer, 2026-10-20): align crafting costs (e.g., netherite-heavy radios) with the original spec or update the design doc to reflect lighter recipes; decide the simplex repeat controller recipe
