# Development Checklist

The 2026-10 bug audit (save/load, network linking, power ticking, static, 40m band) and what is still open are
listed in [doc/Testing.md](doc/Testing.md#bug-audit-2026-10); each fix has a test.

## Current plan (2026-10): HF radios fully working, with solar weather

In order. Each step gets GameTests where the server side can be checked; audio still needs clients and ears.

1. ~~**Prove two HF radios can talk.**~~ Done (2026-10-19): `TransmissionGameTests` run two 20m stations through
   the antenna network for voice and CW, checking strength, wrong frequency, wrong mode, power off and no antenna.
   CW works end to end again (`SCWBufferPacket` / `CCWBufferPacket`, `SET_CW`). `rcdebug transmit` / `rcdebug key`
   do the same on the dev server. Still to hear by ear in a client: morse tone playback and voice through SVC.
2. ~~**Make solar weather shape propagation**~~ Done (2026-10-21): each solar event rates every HF band by day and
   night and that scales skip; blackouts close HF; aurorae; the teletype prints the report. 10m skip added 2026-10-23.
3. ~~**Static masking and squelch.**~~ Done (2026-10-21): received strength ducks the static on the client; the
   voice is shaped like a radio's (see NeedsTesting "Radio-sounding voice and static ducking"). Still by ear.
4. **Power.** ~~Radios draw from batteries and switch off when drained~~ Done (2026-10-21): block radios draw
   their receive/transmit cost per tick from the power network and brown out (panel cycles on / PWR / dark, no
   static) when starved; `radios_need_power` server config; `PowerGameTests`. The repeater draws 4/17 and the
   handheld drains its battery over days (2026-10-23). The charge controller switch works (2026-10-24; controllers start switched on).
5. ~~**VHF (FM) static model.**~~ Done (2026-10-24): silent between signals and on a good one, a hiss under a weak one (`ReceiverMix.fmHiss`). Needs ears.
6. **Port to Minecraft 26.1** and the matching NeoForge, keeping a 1.21.1 branch running alongside it (maintainer,
   2026-10-23; was 26.3 on 2026-10-21), once most of the rest of this list is done.
   Expect the usual port churn: GUI and render APIs, registries, mixin targets, and new Simple Voice Chat, Jade,
   TOP and GeckoLib versions.

## Phase 1 – Core Gameplay Readiness

### Radios & Networking
- [x] Core HF radios (10m/20m/40m/80m) and their recipes are registered
- [x] Provide working SWR and propagation hooks so core HF radios are usable (2026-10: SWR per antenna type, skip, solar weather)
- [x] HF receiver, QRP radios, and the all-band radio shapeless combinations exist
- [x] VHF handhelds have functional radios
- [x] Hand mic on a cord (maintainer, 2026-10-23; done 2026-10-24): sneak-right-click a radio with an empty hand
  to take its hand mic (a "Hand Microphone" item that looks like the crafting one but is its own, uncraftable item),
  with a sagging cord drawn from the radio to your hand. Hold right click to talk. Walk more than 5 blocks away, put
  it away or log out and the cord snaps back; right click the radio with it to hang it up. Dropped, it vanishes. HF,
  QRP, all-band and VHF base stations; not receivers or the repeater.
- [x] Comparator S-meter (2026-10-20): a comparator against any radio reads the received signal strength 0-15, falling
  back to 0 a second after the signal ends. Jade shows the same reading, the interference level and the repeat delay.

### Antennas & Signal Model
- [x] Terrain-aware propagation (2026-10-24, [doc/TerrainPropagationResearch.md](doc/TerrainPropagationResearch.md)):
  terrain atlas from chunk loads, knife-edge line of sight, HF cliff loss, horizon profiles cutting low-angle skip,
  handhelds indoors, results kept `terrain_recalculate_seconds`, `terrain_enabled` master switch, `/rcdebug terrain`
- [x] Terrain map saved with the world (2026-10-24, `radiocraft_terrain.dat` per dimension)
- [x] `/bandcalculate` counts terrain losses (2026-10-24): both ends a few blocks above the mapped ground
- [ ] Terrain, rest of phase 3: fill never-loaded chunks from the world generator, show terrain losses in the
  antenna analyzer (once it exists)
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
- [x] Antenna tuner made manual (2026-10-23, maintainer): three dials (0-50) each have a reading that matches the
  antenna at the radio's frequency (seeded by antenna position, type, band and SWR, drifting across the band). The
  screen's lamp blinks red, faster as the SWR the radio sees nears 3, then yellow-green, then solid green at a
  match. `antenna_tuner_max_swr` default 6 -> 7; past it a tuner takes off (max - 1) at best (SWR 9 -> 3).
- [x] Automatic antenna tuner (2026-10-23, maintainer's model): on coax and power (`auto_tuner_power_use`), tunes
  itself on a new band or a 1 MHz move (SWR 3 for a second, relays clicking faster, then 1); past its range it clicks
  slowly for 6 s and gives up: TUNE FAIL on the radio's screen, FAIL on its own. Late-game recipe: a manual tuner,
  two boards, repeaters (relays), a comparator and a pure encased crystal of any band.
- [x] Duplexer runtime effect (2026-10-23): six cavity screws (TX on the left, RX on the right), each showing its
  frequency. Factory tuned: it follows its repeater until a player turns a screw. A screw off its signal loses power
  (half at 25 kHz) and shows the transmitter a mismatch; receive cavities weaken what the repeater hears.
- [x] Add in SWR consequences (maintainer, 2026-10-21; 2026-10-23: blown radios, poor-crystal explosions). A radio with
  no antenna is not damaged: it works through its bare connector at a few blocks' range.
- [x] Dummy load block (maintainer, 2026-10-21; done 2026-10-21, see the Digital section): a coax load to transmit into safely, once SWR has consequences
- [x] Implement duplexer tuning (maintainer, 2026-10-21; done 2026-10-23, see above), with desense: while the
  repeater transmits, its noise on the input (through the TX cavities) and its carrier (through the RX cavities) leak
  into its receiver. About 1% with a 600 kHz split and a tuned duplexer; a cavity turned onto the other frequency
  deafens it.
- [ ] Implement antenna analyzer UI and data capture
- [x] Implement shared antenna behavior hooks so antenna blocks drive propagation (antenna types set SWR, gain, reach)
- [x] Hook SWR consequences (fire/destruction) into `RadioBlockEntity` (2026-10-23: blown radios, see NeedsTesting)
- [x] Integrate solar event noise and day/night skip changes so HF radios gain or lose range based on `SolarEventManager` (2026-10-21)
- [x] Propagation scaling (2026-10-24): receivers beyond any possible reach are skipped with one distance check
  (`BandUtils.maxReach`), and terrain between two fixed stations is kept until a chunk on the path changes
- [ ] Index each network's antennas by area, if servers grow past a few hundred antennas per band

### Events, Commands & Config
- [x] Callsign capability with `/callsign` tools implemented
- [x] Implement administrative callsign commands (`/callsign set`, `reset`, `generate`: permission level 3)
- [ ] Extend server config to cover callsign digit pools, HF static squelch, modern/vintage toggles, and “talk everywhere” overrides promised in the proposal
- [x] Add `/bandcalculate` helper to complement antenna tuning (2026-10-23, see World & Progression)
- [x] Redstone interference, receive side (2026-10-20): redstone switching near the feedline/antenna boosts the
  radio's static, band and distance dependent, server configurable. Design in doc/RedstoneInterference.md.
- [x] Redstone interference, transmit side (2026-10-20): transmitting induces a level in nearby dust (mixin on the
  dust's power calculation) and flips levers, buttons and torches; reach by rig power and SWR, verticals only
  sideways, predictable positions, never on command blocks. Design in doc/RedstoneInterference.md.
- [ ] Redstone interference polish: a real buzz loop (asset bounty), chokes that cut feedline pickup, the
  interference term joining static masking once plan step 3 lands, and handheld radios (no block antenna) on transmit.

## Phase 2 – Power Systems

### Power & Components
- [x] Make the solar panel and batteries actually use FE/other energy sources (2026-10-23, see "Power from other mods")
- [x] Wire block recipe implemented
- [x] Waterproof wire crafting implemented
- [x] Radio crystal/speaker/microphone/HF circuit board recipes exist, albeit with lighter-cost ingredients than the proposal (iron/planks/redstone torch instead of diamond/iron)
- [x] Finish small alkaline battery energy handling so `SmallBatteryItem` tracks charge and removes the “not implemented” message
- [x] Implement handheld battery drain in `VHFHandheldItem#inventoryTick` (2026-10-23, spread over ticks)
- [x] Curios belt slot for the handheld (and the small battery), with a "talk on worn radio" key (2026-10-24)
- [x] Large batteries combine capacity when wired together, and radios brown out on an empty battery
- [x] Battery fires (2026-10-24, maintainer): fire or lava beside a large battery, a flint and steel, or lightning on
  power wire running to it sets it alight; it burns until consumed (`battery_burn_seconds`), setting fire around it and
  spreading to batteries it touches. Water holds the flames down (hiss, steam) but it keeps being consumed and flares
  up again if the water goes. A battery consumed in a group of 4+ explodes (power per battery, capped), setting the
  rest of the group alight; a quarter of the explosions start fires. Server config `Battery Fires`
- [ ] Battery self-discharge over time (proposal), if wanted
- [x] Solar panel recipe uses glass + a daylight detector rather than kelp/iron
- [x] Wire hazards (2026-10-24, maintainer), with power behind the wire: a wire joined back to itself (a closed loop
  of power wire) shorts, smokes, catches fire and burns through in 4 s, draining a quarter of the stored energy a
  second; plain wire touching water electrocutes anything in the water within 6 blocks (3 hearts a second) and drains
  the same; an open end of plain wire in the rain sparks (a transmitting 20m rig's draw) and zaps whatever touches it
  (2 hearts, six times the draw for a second). Waterproof wire only shorts. Server config `Electrical Hazards`
- [x] Lightning rods are very basic VHF antennas when coax runs to them (SWR 6, a quarter of a J-pole's range), and
  antennas draw lightning: a strike no lightning rod claimed lands on the highest antenna within 32 blocks a quarter of
  the time (`Antenna Lightning`)
- [x] A strike on an antenna (2026-10-24, maintainer) burns out the coax at the antenna (and 30% of the rest of the
  run), blows every radio on the run (SWR protection does not help), and 10% of the time sets the batteries those
  radios run on alight (`lightning_antenna_damage`, `lightning_coax_burn_chance`, `lightning_battery_ignite_chance`)
- [ ] Lightning arrestor (maintainer, 2026-10-24): an in-line coax block that stops a strike's surge (no coax burnt,
  no radio blown, no battery fire); needs a model (bounty) and a recipe

### Radios & Networking
- [x] Tie radio power draw, on/off state, and static playback into battery/solar networks via `RadioNetworkObject` (2026-10-21)
- [ ] Implement HF static/grounding modifiers so propagation feedback reaches the player
- [x] Lightning spikes (2026-10-20): a strike puts a crash of static on every powered radio in the dimension, fading
  with distance and by band, server configurable (`Lightning Static`). Design in doc/RedstoneInterference.md.

## Phase 3 – Extended Systems & Content

Models and textures we want drawn by someone else are listed in [doc/ModelDesignBounty.md](doc/ModelDesignBounty.md).

### VHF/UHF Channel and Repeater Features
- [ ] DTMF redstone controller (maintainer, 2026-10-24): a block beside a radio that listens for DTMF tone codes on
  its frequency and drives redstone outputs (remote switching, repeater control)
- [x] Simplex repeat controller ("parrot", id `parrot_controller`): links to the radio next to it and makes it echo
  what it receives after a delay, with receive/transmit LEDs and a wire drawn to the radio (2026-10-20; model and
  textures by the maintainer). Its recipe is a placeholder pending the recipe review below.
- [x] Parrot controller polish (2026-10-20): a button click when the delay changes, a comparator output of the delay
  in seconds, Jade lines (delay, linked side, state).
- [x] Parrot controller: sneak and right click to link the next radio round it (2026-10-24)
- [x] VHF repeater (2026-10-21): input/output frequencies, CTCSS access and output tones, CW ID in Morse tone, hang
  time with courtesy beep, timeout timer, MIC/TX/SPEAKER, duplexer on the back/top/bottom (kerchunks without one),
  bare connector without an antenna. Handhelds and the base station got a tone and a ±600 kHz offset.
- [x] Repeater sleep/wake (2026-10-24): woken by use, IDs every interval while awake, sleeps after a silent interval.
- [x] Fix the J-pole and duplexer models (maintainer fixed them, 2026-10-21): overlapping faces z-fight; nudge them apart by 0.01 px (maintainer, 2026-10-21)
- [x] Duplexer sits right on top of the repeater (done 2026-10-21 with the maintainer's model) (the repeater is 12 px tall, so a duplexer above it floats 4 px), and
  a clearer way to run coax to it: model the wire to where the antenna port is, or two coax ports on the back of the
  repeater (maintainer, 2026-10-21, undecided)
- [x] Radios and wires placed on the underside of a block hang from it (2026-10-21): radios the right way up (raised copies of their models made in datagen), wires as the floor logic upside down (hug, climb down beams)
- [ ] Add multi-channel scan indicator to VHF receiver UI

### Digital & APRS Features
- [ ] Rework the digital interface (TNC) into separate blocks (maintainer, 2026-10-22; plan in
  [doc/TncRework.md](doc/TncRework.md), with the solar weather teletype/observatory idea)
  - [x] Steps 1 and 2 (2026-10-23): the TNC is now the **Radio Interface** (callsign and TX switch only), joined by
    touch on the station bus to the **H19 APRS Terminal** (maps, messages, beacon and digipeat settings, the
    maintainer's model with READY / CONTACT / MSG screens) and the **Digital Transfer Interface** (files; placeholder
    box). The Radio Interface keeps the old recipe for now. Recipes (2026-10-23): the H19 is a CRT (tinted glass, or
    stained glass with `allow_stained_glass_for_h19_aprs`), a board and button keys; the Transfer Interface a board
    and a book.
  - [x] Step 3 (2026-10-23): the Teletype Machine (the solar weather station renamed) with RTTY through its radios,
    its own callsign and paper, and SOLAR WX printing the local report
  - [x] Ionosonde Antenna (2026-10-23, maintainer's model): two blocks tall like the J-pole, its own antenna type.
    On a radio it hears VHF well and HF weakly, and matches poorly: SWR 3 on VHF, 8 on HF. Dev tab; recipe (2026-10-23): lightning rods, antenna wire, copper, coax.
  - [x] Ionospheric Processor block (2026-10-23, maintainer's model): two blocks tall, its three valves glow softly
    (light 10) through translucent glass. Decorative so far, dev tab; recipe (2026-10-23): redstone torch valves,
    glass, two amethyst shards, a board and a clock
  - [ ] Steps 4 to 7: the processor reading the sky through its ionosonde, over-the-air requests and bulletins, WX,
    recipes
  - [ ] Redesigned UIs for the H19 and the Transfer Interface (maintainer); for now they are the old APRS/MSG and
    FILES tabs
  - [x] APRS maps (2026-10-23): right click an H19 with empty maps for a map (sneak: a 2 x 2 or 3 x 3 wall) of the
    stations heard, in an old-terminal style with pixel-font callsigns, repainted every 5 seconds
- [x] Digital interface (TNC), first slice (2026-10-21): packets ride the radio like voice (AFSK burst everyone on
  the frequency hears, then the packet decodes whole if on frequency and above the noise, `tnc_min_snr`); APRS
  beacons (button, redstone pulse, timer) with a map of heard stations, messages to a callsign or ALL with an unread
  light/buzzer/comparator, digipeating across two radios, `/aprs beacon` and `/aprs broadcast`. No printer block:
  paper/book/map supplies will live in the TNC (maintainer, 2026-10-21).
- [x] TNC APRS map terrain (2026-10-21): painted like a vanilla map from the chunks the client has loaded, zooming
  with the stations heard; beyond render distance it is dark
- [ ] TNC APRS map: terrain beyond render distance (optional Xaero's / JourneyMap integration for explored areas)
- [x] TNC screen scales up to 80% of the window in whole-pixel steps (2026-10-21)
- [x] TNC RTTY (2026-10-21): live typing at 45.45 baud, prints a line per sender, 20 lines per paper, garbles when
  weak; supplies by right click, hopper or the FILES slots
- [x] TNC FILES (2026-10-21): books, paper and maps (SSTV, from the map's picture) to a callsign or everyone; each
  receiver prints a copy on its own blank, the sender keeps the original; progress on the six LEDs
- [ ] Transfer Interface: real inventory panel art (bounty); for now the vanilla chest's bottom half
- [x] TNC digipeating (2026-10-21): blue lamp on the APRS tab toggles it (off by default), red RPT lamp while
  repeating; repeats on every radio it touches incl. the one it heard on, 8-hop cap (config), never a message for itself
- [x] Dummy load (2026-10-21, model by the maintainer): perfect SWR on every band, ~10 blocks, no skip; fits on a
  radio directly or on its coax and follows the radio's band (HF or VHF network)
- [x] Duplexer on top of the repeater shows its coax down the repeater's back (maintainer's `duplexer_on_repeater`)
- [ ] TNC: handheld/Curios side (APRS from a handheld radio), message acks and retries
- [ ] WX button on the teletype (maintainer, 2026-10-23): weather reports, shown only with a compatible weather mod
  (see [doc/TncRework.md](doc/TncRework.md)); we may end up writing our own weather mod
- [ ] WEFAX receiver model: its Rotary elements are turned -85, 80 and 42.5 degrees, which Minecraft 1.21.1 cannot
  load (it shows as missing). Re-export with Blockbench set to 1.21.1 and 22.5 degree steps (`ModelFilesTest` lists it)
- [ ] WEFAX receiver (maintainer, 2026-10-23): prints weather charts, after the WX work. The block and model are in
  (dev tab, decorative; recipe 2026-10-23: a board, paper drum and a clock); the UI comes once the rest of the WX work is done

### New bands and services (maintainer, 2026-10-23)
- [ ] Airband (VHF AM aircraft band)
- [ ] ISM band
- [ ] CB radio (11m)
- [ ] A Sputnik-style radio satellite (beeping beacon in orbit to listen for)

### Antenna Expansions
- [x] Moxon (HF wire, 1:1 balun; 20m 7 x 3, 10m 3 x 2) and delta loop (4:1 balun, closed triangle) (2026-10-24,
  [doc/DeltaLoopAndMoxonPlan.md](doc/DeltaLoopAndMoxonPlan.md)); the VHF Moxon block still needs a model
- [ ] Ringo antenna (maintainer, 2026-10-23)
- [ ] Industrial dipole antenna (maintainer, 2026-10-23)
- [ ] Figure out how we'll allow building of HF yagis (maintainer, 2026-10-21)
- [ ] Satellite dish: no recipe yet (maintainer, 2026-10-23); it stays dev-only and decorative until satellites
  exist, then gets a recipe with them.
- [x] Hustler 5-BTV multiband vertical (2026-10-23, maintainer's model): "5-BTV Multiband HF Antenna", seven blocks
  tall, placed and broken as one, coax to the base. SWR 1.3 on every HF band and VHF; on HF line of sight reaches
  half as far and skip a quarter further (the far edge of the skip window). Dev tab; late-game recipe (2026-10-23):
  two copper blocks of tubing, four ferrite traps, two lightning rods, coax
- [x] Delta loop (2026-10-24): a closed triangle on a 4:1 balun, upright or flat; analyzer messaging waits for the analyzer
- [ ] Add sag-varying visuals for long unsupported antenna wire runs
- [x] 4:1 balun and off-centre-fed dipole (2026-10-21, maintainer): new `balun_four_to_one` feeding an OCFD
  (short arm a third to two thirds of the long one, matched on the half wave and its 2nd and 4th harmonics, poor
  on the odd ones). The 2:1 stays for the quad loops. The 4:1 has its own texture (maintainer, 2026-10-23).
- [ ] Loop antennas (maintainer, 2026-10-21): the horizontal and vertical quad loops exist on the 2:1 balun but
  have never been played with; the proposal's delta loop (4:1, corner fed, each side a third of a wavelength) is
  not implemented. Play the quads, then decide on the delta loop and the loop gains.
- [x] Release creative tab (2026-10-21, maintainer): the HF station (radios, receivers, VHF base station, coax,
  wire holders, baluns, tuner, VHF antennas, parrot controller, weather station) moved out of the dev-only list
  and lost the dev watermark. Still dev-only: duplexer, repeater, radio interface and its terminals, dish, microphone block, power.
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
- [ ] In-game guide book like the Create mod's (Ponder-style scenes) explaining radios, antennas, power and the
  digital interface (maintainer, 2026-10-21)
  - [x] Patchouli guide book (2026-10-23): "Radiocraft Handbook", optional (only with Patchouli installed; dev runs
    have it). 8 categories, 34 entries with crafting pages and Visualize-able builds (stations, antennas, power,
    repeater site, APRS, teletype, sounding station). Book plus wire crafts it. Patchouli builds are inline block
    patterns, not NBT structures. `PatchouliBookTest` checks every link, recipe, item and block state in it.
  - [ ] Guide book: pictures (screenshots of the GUIs), the wide-band receive antenna, handheld APRS when it exists
  - [ ] PonderLib scenes, shipped as a datapack (maintainer, 2026-10-23)
- [ ] Cable trays (maintainer, 2026-10-21): one block carries several cables at once, colour coded; a coloured ring
  where a cable leaves the tray, at cable ends and where a cable meets a block, so you can tell the runs apart
- [x] Solar weather station, minimal (2026-10-20): right click reports the current conditions, noise floor and whether
  10m is open (solar event JSONs carry an `openBands` list; good and great open 10m); a comparator reads 1 (blackout)
  to 6 (great); Jade shows the same. Nothing uses "10m open" for propagation yet (plan step 2).
- [ ] Solar weather station, rest of the proposal: printed paper report (consumes paper, hopper fed, redstone
  activated), best-band calculator between two coordinates
- [x] `/bandcalculate <from> <to>` (2026-10-23, proposal admin command): lists each band between two x z points as
  line of sight, skip (with its condition), aurora or out of range, best first, from the band ranges in the server
  config and the current solar conditions. An estimate: antennas, power and noise are not counted.
- [x] 10m skip (2026-10-23): one long hop, 2500-8000 blocks by day and 2500-6000 at night, open only when the
  solar event rates 10m (great and good). Existing config files keep their zeros until the `10mm_*Skip*` values are
  set or the file is deleted.
- [x] The solar weather station needs a radio (2026-10-23, maintainer): one of any kind next to it with a real antenna
  on its coax (a dummy load does not count), else it reports "No signal" and its comparator reads 0. Server config
  `solar_weather_station_needs_radio` (default on) turns it off. The radio does not need to be powered.
- [ ] Improve wire spool storage UX (bulk crafting/collapse)
- [x] Crystals (2026-10-22): raw 80/40/20/10m and VHF crystals in pure, good and poor turn up when digging sand
  (`crystal_sand_chance`, quality weights in the server config); one iron ingot encases them. Each crystal radio
  comes as Premium (pure), plain (good) and Cheap (poor) blocks/items; crafting a radio with an encased crystal of
  another quality swaps it. QRP radios take only poor crystals and ignore them; the repeater takes only pure.
- [ ] Crystals from pressing sand with the Create mod's mechanical press (maintainer, 2026-10-22)
- [x] Insulation item texture (maintainer, 2026-10-23): `textures/item/insulation.png`
- [ ] Microphone element and speaker crafting components (maintainer, 2026-10-22): a microphone item (its own
  texture) used in the hand microphone and desk microphone recipes, and a speaker item used in radio recipes
  - [x] Microphone Element (2026-10-23, maintainer's texture): ` P `/`NCN` (paper, iron nuggets, coal or charcoal)
    makes 2; the hand microphone (in place of charcoal) and the desk microphone (in place of a hand mic) use it
  - [ ] Once the microphone item exists, the maintainer will switch the VHF handheld recipe to use it
- [ ] Desk speakers (maintainer, 2026-10-22): several kinds of desk speaker block that sit next to or on top of an
  HF radio and change its audio slightly (e.g. tone, passband, loudness)
- [ ] More speakers (maintainer, 2026-10-23)
- [ ] More radio models and brands (maintainer, 2026-10-23)
- [ ] Structures (maintainer, 2026-10-23): the worldgen plumbing to place the mod's own structures
  - [ ] AT&T Long Lines site (maintainer, 2026-10-23)
  - [ ] Prepper bunker (maintainer, 2026-10-23)
- [ ] Add something radio-related to villages (maintainer, 2026-10-23)
- [x] Tower foothold (2026-10-24): `tower_foothold` (crafted: 3 from iron + stick, never breaks) and
  `old_tower_foothold` (structures and creative): climbed like a ladder, stood on from above; an old one gives way
  `tower_foothold_break_chance` (0.05%) of the times a player climbs onto or steps on it, with a sped-up anvil clang,
  showing the maintainer's bent rung; a broken one cannot be climbed or stood on and drops nothing
- [x] Guide book on first join (2026-10-24, `guide_on_first_join`), and `/radiocraft guide` or `/radio guide`
- [ ] Industrial radio block (maintainer, 2026-10-23; needs a model): broken and working versions, found in
  structures, not usable by players; breaking it drops a random rare radio component (the all-band circuit board and
  others to come)
- [ ] Large microwave dish (maintainer, 2026-10-23; needs a model)
- [ ] Microwave radio (maintainer, 2026-10-23; needs a model): long-range, reliable point-to-point links, paired with
  the large microwave dish
- [ ] Manpack radio (maintainer, 2026-10-23): a radio worn on the back
  - [ ] [Traveler's Backpack](https://www.curseforge.com/minecraft/mc-mods/travelers-backpack) version
  - [ ] [Sophisticated Backpacks](https://modrinth.com/mod/sophisticated-backpacks) version
- [x] Power from other mods (2026-10-23, maintainer): the large battery takes and gives Forge Energy, so Create:
  Crafts & Additions' [Alternator](https://create-mod-addon.fandom.com/wiki/Alternator) or any other mod's generator
  can charge the power network. Power rebalanced around that: one large Create water wheel geared up to 8 RPM, or
  two small wheels on one shaft (512 SU either way), driving one alternator makes about 11 FE/t, which just keeps up
  with a VHF base station transmitting 80% of the time (2 FE/t listening, 13 transmitting). HF radios 8/26, QRP 2/13,
  repeater 4/17, solar panel 9, large battery 260000 (one Minecraft day of that base station), small battery 60000.
  The handheld's drain is now spread over ticks, so its battery really lasts the 2/6/12 days. Existing config files
  keep the old numbers until deleted. (A single small wheel tops out at about 5 FE/t, geared down to 4 RPM.)
- [ ] Look for other mods that would be easy to integrate with (maintainer, 2026-10-23)
- [ ] Introduce random structures, auto-beacon radios, APRS Discord bridge, and external radio API hooks
- [ ] Review all recipes together (maintainer, 2026-10-20): align crafting costs (e.g., netherite-heavy radios) with the original spec or update the design doc to reflect lighter recipes; decide the simplex repeat controller recipe
