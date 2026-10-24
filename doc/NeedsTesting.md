# Needs Testing

Things that are implemented and pass the automated tests but still need a human at a client (rendering, GUI, audio,
feel). Tick an item off once it has been played with, and note anything wrong in [todo.md](../todo.md) or fix it.
Add to this list whenever a change cannot be fully verified by `test` / `runGameTestServer`.

## Ionospheric Processor (added 2026-10-23)

- [ ] Placed and broken as one two-tall block; it faces the player when placed (check the front is the side you
  meant: the model is turned so its north side faces you).
- [ ] The three valves on top glow softly in the dark (light 10 on the glass and the filaments) and the glass is see
  through with the filaments visible inside. No odd sorting against water or stained glass behind it.
- [ ] Item in hand and inventory (borrows the J-pole's display settings).

## Ionosonde Antenna (added 2026-10-23)

- [ ] Placing it puts down both halves; breaking either half removes both and drops one item. The model sits right
  across the two blocks (the maintainer's export was split at y=16 into `ionosonde_lower` and `ionosonde_upper`), with
  both textures showing (no purple and black).
- [ ] The item looks right in the hand, the inventory and an item frame (it borrows the J-pole's display settings).
- [ ] On coax to a VHF base station the radio shows SWR 3; to an HF radio, SWR 8, and keying up wastes most of the
  power. Receiving on VHF is about as good as a J-pole; HF is faint.

## Radio Interface, H19 and Transfer Interface (added 2026-10-23)

The digital interface is now three blocks (dev tab): the **Radio Interface** touching a VHF radio, and the **H19
APRS Terminal** and **Digital Transfer Interface** touching it, or touching each other in a chain back to it.

- [ ] H19 model: renders facing the player, with the hitbox over the keyboard and case. Its screen is blank with the
  radio off, shows READY within half a second of switching a VHF radio on, CONTACT when a new station's beacon
  arrives, MSG when a message for this station arrives (MSG wins over CONTACT). The screen icons glow in the dark.
- [ ] Opening the H19 clears CONTACT and MSG back to READY. CONTACT also goes out by itself after 10 minutes
  (`aprs_contact_lamp_minutes`); MSG stays until opened.
- [ ] H19 screen: the old APRS and MSG tabs. The callsign field shows the Radio Interface's callsign (not editable
  here), the green lamp is lit while joined to a Radio Interface, and the digipeat lamp switches the Radio
  Interface's digipeating.
- [ ] Radio Interface screen: a plain panel with a callsign box (Enter saves; empty claims your own) and a TX ON / TX
  OFF button. With TX off, beacons, messages, files and digipeats don't go out; listening still works.
- [ ] Transfer Interface: the old FILES tab with the inventory; hoppers stock it and one underneath takes the copies.
  Its placeholder model is a stone box with a loom front and a cartography table top.
- [ ] JEI's item list stays clear of both scaled-up screens.
- [ ] A chain: radio, Radio Interface, Transfer Interface, H19 in a row works end to end; breaking the middle one
  greys the H19's green lamp and stops it sending.

## Maintainer's art: WEFAX receiver, static loop, 4:1 balun (added 2026-10-23)

- [ ] WEFAX receiver (dev tab): the model renders with its texture, faces the player when placed, turns with each
  facing, and the outline/collision box fits it (full base, printer body behind the front bar). It drops itself.
- [ ] Static loops with no hiccup every few seconds (new `static.ogg`), on HF and on a weak VHF signal.
- [ ] The 4:1 balun shows its own texture, different from the 2:1.

## Forge Energy and the power rebalance (added 2026-10-23)

With Create and Create: Crafts & Additions installed (NeoForge 1.21.1):
- [ ] An alternator next to a large battery charges it (Jade shows the charge rising); FE cables from other mods work too.
- [ ] Two small water wheels on one shaft (or a large wheel geared up to 8 RPM) into an alternator, feeding a large
  battery wired to a VHF base station: the battery holds steady with the radio transmitting most of the time.
- [ ] Delete old `radiocraft-common.toml` files first, or they keep the old (about 15x larger) numbers.
- [ ] A handheld's battery bar barely moves over a few minutes idle (it should last 12 in-game days).
- [ ] The desk charger fills a flat handheld in about 6 minutes.

## Solar weather station needs a radio (added 2026-10-23)

- [ ] On its own, right click prints "No signal: ..."; Jade shows the same line; a comparator reads 0.
- [ ] Put a VHF base station (or any radio) next to it with a J-pole on coax: the full report prints and the comparator
  lights up within half a second. Swap the J-pole for a dummy load: back to "No signal".
- [ ] The item tooltip mentions the radio.

## Block drops (added 2026-10-23)

Every block now has a loot table; before, breaking any RadioCraft block lost it. GameTests check the tables.

- [ ] In survival, mine a radio, a battery, a balun and the tuner with a pickaxe: each drops itself. By hand: nothing.
- [ ] Wire, waterproof wire and coax break instantly by hand and drop themselves.
- [ ] A J-pole broken from its top half drops one antenna, not two (creative drops none).
- [ ] The passthrough still gives back itself and the wall block; the desk charger still drops itself and its radio.

## Crystal qualities (added 2026-10-22)

Every crystal radio (10/20/40/80m, VHF base station, VHF handheld) is now three blocks/items: Premium (pure), plain
(good, the old id) and Cheap (poor). Textures are copies for now. The old Radio Crystal item is gone.

- [ ] Premium and Cheap radios place, render (floor and ceiling) and show their names and tooltip line; their GUIs open
  with their own (copied) GUI textures, including the Premium/Cheap VHF handheld screen.
- [ ] Raw crystals show the maintainer's pure/good/poor crystal textures (same for every band), encased crystals the old crystal texture; all are in the creative tab.
- [ ] Digging sand drops a crystal about 1 in 80 blocks, mostly poor.
- [ ] JEI shows the encased crystal, radio and crystal-swap recipes; the swap works in a crafting table.
- [ ] By ear: a Cheap HF radio slowly pulls a steady SSB voice up and down in pitch over a few minutes (drift up to
  1 kHz); a Premium radio's static is a touch quieter than the plain one's.
- [ ] Handhelds of every quality still charge in the desk charger and swap batteries.

## VHF repeater, duplexer, CTCSS and offsets (added 2026-10-21)

The repeater is now a working 2m repeater (`VHFRepeaterBlockEntity`). It needs a duplexer on its back, top or bottom,
with coax from the duplexer to the antenna. Handhelds and the VHF base station gained a CTCSS tone and a ±600 kHz
repeater offset. Every radio with nothing on its coax now works through its bare connector over a few blocks
(`bare_connector_range`, default 8). Run `./gradlew runData` first: the repeater screen's text is new.

- [ ] Repeater face: POWER toggles it; the RX/TX windows show input/output MHz; the four bottom windows show
  ON/OFF/PWR, DUP / NO DUP / NO ANT, RX (red TOT when timed out), TX (amber while transmitting).
- [ ] PRT RX / PRT TX open a panel over the face: type MHz, Enter or Done applies, Esc or Cancel leaves it without
  closing the repeater. "OUT -600 / +600" fill in the usual input. The RX/TX knobs step 20 kHz.
- [ ] CW ID: callsign (letters, digits, /) and "ID every" (off/5/10/15/30 min). After an over the repeater sends the
  courtesy beep, then the callsign in Morse tone if an ID is due.
- [ ] CTC RX / CTC TX: < > through the standard tones. With an access tone set, a handheld without that tone gets
  no repeat; with it, it does.
- [ ] MIC: with TX on, talking next to the repeater goes out on the output. TX off: it listens but does not repeat.
  SPEAKER: plays the input at the block.
- [ ] Knobs under the buttons: hang time (0-5 s) and timeout (off to 10 min), shown in a tooltip while turning.
  Talking longer than the timeout drops the repeat until you let go.
- [ ] Duplexer behind / on top of / under the repeater: coax wire draws a connection to it and the repeater works
  through it. A duplexer on the front or side does nothing (NO DUP).
- [ ] Antenna on the repeater but no duplexer, TX on: someone talking into it gives "psht ... psht ... psht" on
  the output instead of their voice. With TX off, receiving works normally.
- [ ] No antenna at all: the repeater (and any radio) works only within about 8 blocks.
- [ ] Handheld: ENTER from the frequency screen pages Tone, then Offset, then back; the arrows change the value; "-"
  / "+" and "T" show on the main screen when set. Handheld on 146.940 with -600 and the repeater's tone talks
  through a repeater on 146.340 in / 146.940 out.
- [ ] VHF base station: the right-hand display shows OFS and TONE; its three soft buttons (offset, tone down, tone
  up) and the two big knobs (tone, offset) change them.
- [ ] Simplex repeat controller (parrot): after it finishes echoing an over it sends a courtesy beep.
- [ ] The settings panel is a plain dark box (placeholder): say if it needs art.

## Off-frequency signals and the 2m tuning fix (added 2026-10-21)

The all-band on 144 MHz was stuck at 144.000: frequencies are floats, 144.001 MHz does not fit one exactly, and the
"not on a step" check reset every step. Steps are now counted from the band bottom. Off-frequency voice
(`Detuning`, `RadioVoiceProcessor`) is new: SSB is heard up to 10 kHz off, weaker, pitch-shifted (0.15 Hz per Hz off,
so 1.5 kHz at 10 kHz off) with the top or bottom of the voice cut, and splatter from 7 kHz out. FM is heard as usual
up to 2 kHz off (weaker). From 3 to 10 kHz off, or one VHF channel step (20 kHz) off, FM stays squelched but
crackles over a signal you are hearing on frequency.

- [ ] All-band on 2m: the frequency knob steps 144.001, 144.002 ... and the display follows; reload keeps it.
- [ ] SSB 1-2 kHz off (two 20m radios): voice is clear but a little higher or lower; tuning above on 20m sounds
  lower, on 40m/80m (LSB) higher.
- [ ] SSB 5-10 kHz off: "Donald Duck" voice, thin (bass or treble cut off), quieter; garbled splatter rises near
  10 kHz. 11 kHz off: nothing.
- [ ] FM (all-band FM, VHF base station, handhelds) 1-2 kHz off: sounds normal, a bit quieter. 3+ kHz off: silent,
  no squelch opening.
- [ ] FM splatter: listen to a station on frequency while a second one talks 5 kHz (HF all-band FM) or one 20 kHz
  step (handhelds/VHF) away: bursts of crackle over the wanted voice, gone when the second one stops.
- [ ] Tune the numbers if needed: constants at the top of `Detuning` (strengths, shift per Hz, splatter levels).

## Mode buttons and the all-band radio (added 2026-10-21)

Mode buttons now work like a radio's selector: the active mode is pressed in, the others are out, and pressing the
active one does nothing (the button art rows were swapped before, so "on" looked popped out).

- [ ] Every HF radio (10m, 20m, 40m, 80m, all-band): exactly one of SSB / CW (/ FM) is pressed in at all times, and it
  is the one the display shows. Clicking the pressed one changes nothing; clicking another swaps them instantly.
- [ ] All-band: the square button bottom-left switches it on and off (faint highlight on hover); the lamp by the light
  bulb comes on with power.
- [ ] All-band band knob: left click or scroll up turns it clockwise 3 > 7 > 14 > 28 > 144, right click or scroll down
  back; it stops at both ends. The knob art points at the label each time (sprites were lined up on a copy of the
  panel, so check them at real size), the frequency jumps to the bottom of the new band and the band survives a
  reload.
- [ ] All-band FM: pressed in alone, display shows "FM", voice works, and there is no background static (FM is
  squelched). SSB and CW still have static.
- [ ] All-band lamps under the brand plate: left green = a signal coming in, middle green = antenna matched (SWR 2 or
  better), right amber = transmitting.
- [ ] All-band on 144 (2m) in FM with a J-pole or slim jim on its coax: talks to a VHF base station and handhelds on the
  same frequency.
- [ ] MIC / SPK buttons and the two small knobs still do nothing (as on the other radios).

## Improvised antennas (added 2026-10-21)

Any wires on any balun now work: when no proper antenna type recognizes the build, it becomes an "Improvised" antenna
(`ImprovisedAntennaType`, -3 dBi) whose SWR grows with how far it is from ideal (arm lengths off a quarter wave, bent
or sloping arms, extra or branching arms, a balun of the wrong ratio). Clean builds still match their proper type.

- [ ] Rebuild the playtest dipole with one arm dropping a block: Jade shows "Antenna Type: Improvised", the two arms,
  "Shape off ideal: +0.2 SWR" and an SWR on 20m around 1.4; the radio works on it.
- [ ] Bend it, add a third wire, put it on a 9:1: the SWR climbs but it keeps working; a tuner brings it back.
- [ ] Right-clicking a balun shows "Antenna: Dipole" (or Improvised, ...) above the hotbar instead of a class name.

## Radio-sounding voice and static ducking (added 2026-10-21)

Received voice now goes through `RadioVoiceProcessor` on the server (band-pass 300-2700 Hz, AGC, soft overdrive) and
plays at `ReceiverMix.voiceLevel(S, N)`; the client's static is multiplied by `ReceiverMix.noiseDuck(S, N)`, so a strong
signal pushes the static down and a weak one stays buried. Skip signals fade in and out slowly (QSB), VHF heard off an
aurora gets a raspy buzz. Settings: "Receiver Audio" in `radiocraft-server.toml` (band edges, drive, VHF noise, fading
depth, an off switch). Cost measured at 35-55 microseconds per 20 ms frame per receiving radio.

- [ ] Voice sounds like a radio: thin, no deep bass or airy highs, steady loudness whether the talker whispers or
  shouts, a little crunch on peaks. Compare with `voice_processing_enabled = false`.
- [ ] Strong nearby station on a quiet band: the static drops away while they talk and comes back up within about half
  a second after they stop, smoothly (no clicks, no pumping on every word).
- [ ] Weak or distant station, or a blackout (`/rcdebug solar set blackout`): the voice is quieter and the static stays
  loud over it; hard to copy, as on a real rig.
- [ ] Skip contact (HF beyond line of sight): the voice swells and fades over several seconds, and the static rises as it
  fades.
- [ ] Handhelds (VHF): same band-pass and AGC; a handheld at the edge of range is quieter but not buried (no static on
  VHF). Off-hotbar handhelds are still half volume.
- [ ] Aurora VHF (see Aurora below): buzzy, raspy voice. Too much / too little?
- [ ] Tune by ear: `voice_band_low/high`, `voice_drive`, `skip_fading_depth`, `vhf_noise_floor`. Say which numbers
  sound right.

## Aurora (added 2026-10-21)

A blackout has a 30% chance (`aurora` in `solar_events/blackout.json`) of an aurora three days after it starts, lasting
a day. It only reaches stations with |Z| >= 10000 (`aurora_latitude` in the server config). There VHF skips 500-4000
blocks off the aurora, every HF band drops one condition step and HF static rises by `aurora_static`. Force one with
`/rcdebug solar aurora on` (`off` ends it), `/rcdebug solar` shows its state, `/tp @s ~ ~ 10500` to get under it, and
`/time set midnight`.

Graphics (procedural, `AuroraRenderer`, no textures):
- [ ] At night past Z 10000 (or -10000): green curtains low over the pole side of the sky (north at -Z, south at +Z),
  bright lower edge fading up through teal to violet, vertical rays drifting slowly, the curtains folding over time.
- [ ] It fades in over Z 9000-10000 (walk across), with dusk, and over a few seconds when it starts or ends. Gone by day,
  in rain, underwater and in the Nether/End.
- [ ] Looks right with shaders off at several render distances (2, 8, 16+ chunks), does not show through terrain or
  draw over the moon oddly, and does not hurt the frame rate.
- [ ] Say how it should look instead: colours, height, how much of the sky, how fast it moves. All are numbers in
  `AuroraRenderer#curtain`.

Radio:
- [ ] Two VHF stations (or handhelds) 2000 blocks apart, both past Z 10000: they hear each other only while the aurora
  is on. One of them below Z 10000: nothing.
- [ ] HF past Z 10000: the station's bands read one step worse than the same event further south; the static is louder.
- [ ] Weather station anywhere: "Aurora expected: storm on the way..." after a lucky blackout, "Aurora: active beyond
  Z +/-10000" while on (purple / green line). VHF reads "Aurora" for a station past the latitude, "Normal" elsewhere.

## Solar weather shapes HF skip (added 2026-10-21)

Each solar event now rates every HF band Good / Fair / Poor / Closed, separately by day and night
(`data/radiocraft/solar_events/*.json`, `bands.day` / `bands.night`). The rating scales the sky-wave (skip) signal:
Good 1.0, Fair 0.6, Poor 0.3, Closed 0.05. Line of sight is not touched. A weaker signal plays quieter against the same
static, so a poor band is hard to copy. Force an event with `/rcdebug solar set blackout` (`/rcdebug solar` shows
the band ratings), then check the station.

- [ ] Blackout by day: the station reads Closed on 10m, 20m, 40m and 80m. Two stations at skip distance (20m by day:
  2000-6000 blocks apart) hear each other only as a faint voice buried in the static.
- [ ] Blackout at night: 40m and 80m read Poor and still work, weak and noisy; 10m and 20m read Closed.
- [ ] Average and better: 20m, 40m and 80m read Good both day and night, as before; skip sounds like it did.
- [ ] Is the balance right? Closed at 0.05 might still be too audible (or Poor at 0.3 too easy) next to the static.
- [ ] Note: stations within line of sight (a few hundred blocks) are not affected by the band rating, only by the
  louder static.

## Radio static comes back after the sound engine drops it (added 2026-10-21)

A radio's static, morse and hum loops are now restarted within a second if Minecraft's sound engine drops them.

- [ ] Power an HF radio in SSB with a matched antenna and hear the static. Then each of these, and the static should
  come back within about a second: drag Voice/Speech to 0 and back; F3+T; switch the Windows output device (or plug
  in headphones); leave the area until the chunk unloads and walk back.
- [ ] No doubled static (louder than before) after any of the above.

## Simplex Repeat Controller ("parrot", added 2026-10-20)

Place one next to an HF radio with a working antenna, and a second station in earshot.

- [ ] Model looks right in world: body, top plate, bar and nubs sit where they do in the Blockbench file, for all four
  facings. The item in hand / inventory renders the same model.
- [ ] LEDs: dark red and dark green "off" pixels visible on the front face; green pixel glows fully bright while the
  radio receives, red while it sends the echo, both still visible in a dark room. No black square around the lit pixel
  (cutout rendering).
- [ ] Wire stub: a short coax-textured wire runs from the unit to the radio on the linked side, for every combination
  of block facing and link side, and never pokes through the LEDs. Placing or breaking the radio updates the wire.
- [ ] Name and tooltip: "Simplex Repeat Controller", tooltip text as written by the maintainer.
- [ ] Right click shows "Repeat delay: Ns" above the hotbar and cycles 1, 2, 3, 5, 10 s then back to 1. The chosen
  delay survives leaving and rejoining the world.
- [ ] By ear: speak into the far radio and hear yourself back after the delay; key CW and hear the morse back.
  Compare loudness with a direct contact at the same distance.
- [ ] Breaking the controller stops the echo. Breaking the radio and placing it back re-links.
- [ ] Recipe shows up in the crafting table / JEI (placeholder recipe, to be reviewed with all recipes).

## Redstone interference, receive side (added 2026-10-20)

Power up an HF radio in SSB with an antenna, stand where you can hear its static.

- [ ] Flip a lever or place a redstone block next to the radio or its coax: the static pops up for a quarter second.
  Do the same 3 blocks from a dipole arm: a smaller pop. 10 blocks away: nothing.
- [ ] Build a repeater clock next to the coax: the static stays louder and a low hum (the placeholder tone, pitched
  down) fades in. Break the clock: both die away within a second.
- [ ] Compare an 80m radio and a 20m radio with the same clock: 80m should be twice as affected, 10m barely.
- [ ] A redstone lamp or a door being toggled (not a signal source) should be a much smaller pop than a lever.
- [ ] Does the placeholder hum sound acceptable until a real buzz loop exists? Is the 0.25 threshold right, i.e. does
  a slow clock (1 s) stay hum-free while a fast one hums?
- [ ] Server config: `redstone_interference_enabled = false` silences it; `redstone_interference_only_bad_swr = true`
  silences a matched station but not one tuned off its antenna.

## Redstone interference, transmit side (added 2026-10-20)

Full-power HF rig with a matched dipole, redstone dust and a repeater or lamp within 6 blocks of the wire.

- [ ] Key up (PTT or CW): dust near the wire lights to level 4, the lamp or repeater fed by it reacts. Let go: it
  goes dark within a tick. Dust 8 blocks away does nothing.
- [ ] Which dust blocks react is the same every key-up. Shorten or lengthen a dipole arm (SWR changes): a different
  set, and with SWR 3 or worse the level is 8 or more and dust within 10 blocks of the radio or 5 of the coax joins in.
- [ ] A QRP radio with the same antenna only reaches 2 blocks.
- [ ] Levers and buttons within reach flip when you key up (about a third of them), torches blink off for two ticks.
  A button on a command block never does.
- [ ] A quarter wave vertical: dust 3 blocks above its base is unaffected, dust 3 blocks beside it is.
- [ ] Does 0.35 feel right as the fraction of components hit? Does level 4 read as "a glitch" rather than a working
  circuit? (Both are config values.)

## Comparator outputs (added 2026-10-20)

- [ ] Comparator against a radio, lamp or dust chain behind it: the output follows the strength of what the radio is
  receiving (a nearby strong station lights a long run, a weak one a short run) and goes dark about a second after
  the other station stops talking or keying. Works for SSB voice and CW, and on VHF base stations.
- [ ] Comparator against the Simplex Repeat Controller: reads 1, 2, 3, 5, 10 as the delay is cycled.
- [ ] Right clicking the controller makes a button click; is the volume and pitch right for a desk unit?
- [ ] Comparator against the Solar Weather Station: reads 1 to 6 (blackout to great) and changes when the event
  changes (`/rcdebug solar` shows the event; `/solarweather` too). A strongly powered chain with the station at the
  end makes a cheap conditions indicator.

## Solar weather station (added 2026-10-20)

- [ ] Right click: a chat line like "Solar conditions: Good (noise floor 20%). 10m band: Fair". The event name reads
  as a word, not a key (needs `./gradlew runData` first). Tooltip replaced the "not implemented" one.
- [ ] The block still renders and faces the right way (it became a block entity; existing placed ones keep working).

## Lightning static (added 2026-10-20)

Powered HF radio in SSB, a thunderstorm (`/weather thunder`) or `/summon lightning_bolt` a few blocks away.

- [ ] Each strike: a sharp crash in the static, gone within a second. Compare 80m and 20m radios: 80m twice as loud,
  10m barely. A strike 200 blocks away is still a small crash; beyond 512 blocks nothing.
- [ ] Does the crash read as lightning? The proposal says 40% for a second; the default is 1.2 (doubled on
  2026-10-21 at the maintainer's request, so nearby strikes saturate the static) on top of the solar noise floor,
  decaying with the same 0.8/tick as redstone pops. If it should rumble longer, say so.
- [ ] `lightning_static_enabled = false` silences it.

## Jade (added 2026-10-20)

- [ ] Radio: "Signal: n/15" appears while receiving, "Interference: n%" while redstone or lightning noise is up,
  "Simplex repeat: Ns delay" when a controller is linked.
- [ ] Simplex Repeat Controller: delay, "Radio: linked to the west" (or none), "State: Idle/Receiving/Repeating".
- [ ] Solar Weather Station: conditions with noise floor and level, then each HF band's Good/Fair/Poor/Closed.

## Antenna tuner (added 2026-10-20)

HF radio, coax, Antenna Tuner, coax, 1:1 balun with a dipole one or two blocks short per arm (SWR 2 to 3).

- [ ] With the tuner in the run the radio's static comes up to full (as with a matched dipole) and `/rcdebug radio`
  shows `swr=1.00 (tuner: antenna swr=3.00, power to antenna=94%)`. Break the tuner and bridge with coax: back to
  SWR 3 and quieter static.
- [ ] Tuner GUI: right click shows "Matched", "Antenna SWR: 3.0", "Radio sees: 1.0" in the panel right of the meter;
  the readout follows the radio being switched off ("Radio is off"), the antenna being broken ("No antenna
  recognized") and a 2+2 dipole on 40m ("Out of range: no match", SWR 10). Is the text placed and coloured
  acceptably on the texture? The dials still do nothing; say if the tuner should become manual.
- [ ] Jade on the coax lists "Antenna Tuner: at x, y, z" and still lists the antenna beyond the tuner.
- [ ] Tooltips: the tuner and the three baluns now have a sentence each instead of "Not Implemented".
- [ ] Redstone interference: with the tuner matching a SWR 3 dipole, the bad-SWR dust reach around the radio and
  coax goes away (the station counts as matched). Is that the right call, or should the coax after the tuner still
  count as badly matched?
- [ ] `antenna_tuner_enabled = false`: the readout says "Tuners disabled on this server" and the SWR is the raw one.
- [ ] Side effect of a bug fix (Testing.md audit item 13): breaking a radio, balun or tuner now really drops it from
  its coax network. Break a radio next to live coax and place it back: it should re-link and keep its settings; break
  a balun: the far station should stop hearing it at once (no ghost antenna).

## Inverted V and random wire (added 2026-10-20)

- [ ] Inverted V: 1:1 balun on a pole 3 high, two wires to poles on the ground 4 blocks out either side (each arm is
  5 along the wire). Right clicking the balun names `radiocraft:inverted_v`; Jade shows arms and drops; a 20m radio
  sees SWR 1. Raise one end above the balun: no longer recognized. Compare the strength heard at a far station with
  a flat 5+5 dipole: the V should be 10% weaker (25% on 40m).
- [ ] Random wire: Balun (9:1) (texture by the maintainer, 2026-10-21), one wire to a pole,
  a second wire on from that pole (12 blocks total, any shape). Right click names `radiocraft:random_wire`; Jade
  shows the length and "Needs an antenna tuner". A 20m radio without a tuner sees SWR 2.6, with a tuner 1.0; a
  wire shorter than 11 blocks on 20m is SWR 10. Fork the wire at the pole: not recognized.
- [ ] Both new types need `./gradlew runData` first (9:1 balun tags, name and tooltips, tuner readout text).

## Radio screen fixes (added 2026-10-21, from the first playtest)

- [ ] Frequency readout: every HF screen (10m, 20m, 40m, 80m, all band, receiver) and the VHF base station shows
  "14.000 MHz" (or the band's bottom) in its display window while powered, nothing while off. The box positions were
  measured off the textures, not seen in game: say where it sits wrong or is the wrong size (the 40m and receiver
  LCDs are tiny, so the text there is half size). Dial and buttons move it.
- [ ] A freshly placed radio comes up in SSB: static as soon as it is powered, no button press.
- [ ] PTT: hold it and the static (and any interference hum) stops for everyone near the radio, the readout turns
  orange-red and larger displays show "TX" top right. Let go: static back. In CW the readout follows your own key.
- [ ] Is the TX colour and marker enough feedback, or do you want proper TX/RX lights drawn on the panels?
- [ ] 80m: the readout now sits on the tan panel above the brown box, in dark red (maintainer's choice, 2026-10-21).
- [ ] VHF handheld: while entering a frequency the battery percentage moves up next to "Set Freq" instead of
  overprinting "MHz". The item is held upright now (plain item pose instead of the sword grip); first person too.
- [ ] Lightning: reported silent once, then fine after a client restart (maintainer, 2026-10-21; "audio and decay
  sounded good"). If it goes quiet again, note what happened just before (world reload, far travel, config change)
  and whether a lever next to the coax still pops, since both use the same client path.

## Solar weather station teletype (added 2026-10-21, maintainer request; sounds by the maintainer)

- [ ] Right click: the teletype starts at the block and the report prints in chat one line at a time over about
  3 seconds (header, Overall, Noise Floor, VHF, 10m, 20m, 40m, 80m), with the bell at the end. Does the spacing feel
  right against the sound? Two players clicking at once each get their own lines.
- [ ] Placing the station rings the bell once.
- [ ] Band words: VHF is always "Normal"; the HF bands read Good, Fair, Poor or Closed for the current event and time
  of day (see "Solar weather shapes HF skip" below). Say if the words should differ.
- [ ] `teletypefadeout.ogg` is registered but unused; say where you want it.
- [ ] Wire across a one-wide trench: coax on the trench floor climbs both walls and joins the wire on each side,
  no bridge needed (a bridge plus floor wire makes a real loop, which draws as a rectangle).
- [ ] Solar weather station outline and collision follow the model (body plus the paper roll at the back) for all
  four facings.
- [ ] Wire: at the foot of a trench or pit the run now goes up the wall on the side the wire above continues over,
  so it meets that wire head on instead of going up the nearest wall and across. Check the screenshot build.

## Solar panels join by touching (added 2026-10-21, maintainer request)

- [ ] A row of solar panels with a charge controller touching the end panel and a battery wired to the controller:
  the battery charges in daylight with no wire between the panels. Panels placed before or after the controller
  both work. A battery touching the controller with no wire does not charge (batteries need wire).
- [ ] Jade on a panel: "Making: N FE/tick" (0 with a reason at night or under a roof), the row's panel count and
  total, and whether a charge controller is connected.
- [ ] Known gap: breaking a panel in the middle of a row leaves the two halves on one network until something
  re-links (networks are not split when a device goes, only when wire does). Say if that matters.

## Wire Passthrough (added 2026-10-21, maintainer request; item model and texture by the maintainer)

Use the Wire Passthrough item on a plain solid block (stone, planks, bricks; not a device, wire or block with a
block entity). The block keeps its look but now conducts power wire and coax between all six sides.

- [ ] Click a wall block with the item: no visible change except the sound; wire or coax on either side now draws its
  stub into the wall face and the run connects through (`/rcdebug radio` sees the antenna, a battery feeds a radio).
- [ ] The converted block renders exactly like its neighbours in daylight and in the dark (it is lit from its
  brightest neighbour, since there is no light inside a wall). Check at a chunk border and with a glass or leaves
  block converted (transparent blocks are not allowed; the item should do nothing on them).
- [ ] Breaking it in survival drops the passthrough and the original block; creative drops nothing. Middle click
  gives the passthrough item.
- [ ] Jade on the coax lists what is beyond the wall. Jade on the passthrough itself shows the wall block's name
  (Jade reads the block state, which is the passthrough; say if it should show the mimicked block instead).
- [ ] Both kinds at once: a power wire and a coax into the same passthrough from different sides stay separate
  networks (the radio on the coax side does not end up on the battery's wire).

## Radios draw power (added 2026-10-21, maintainer request)

Block radios now need power (`radios_need_power`, server config, default on). A radio with no power, or on a
network that cannot keep up, is "browned out": no static, hears nothing, and its panel cycles frequency (1 s), "PWR"
(1 s), dark (1 s), then tries the power again.

- [ ] Radio with no battery at all: switch it on and watch the panel cycle; no static. `/rcdebug radio` says
  `switch=true powered=false (browned out ...)`.
- [ ] Solar panel -> charge controller -> large battery -> wire -> radio: the radio runs (draw 125 FE/tick listening,
  375 transmitting, in the common config). `/rcenergy blockinfo` on the battery (look at it and press tab for the
  coordinates) shows it draining; `/rcenergy fillblock 1500000` on it fills it. When it runs dry the radio browns
  out mid-session; once charged again it comes back at the next cycle.
- [ ] Is a 3 s cycle right, and should "PWR" be a different colour than the TX red? The power toggle stays "on"
  during a brown-out, since that is the switch position.
- [ ] `/rcenergy` positions now tab-complete to the block you are looking at (they were plain integers before).
  `/rcenergy infinite` is only for the desk charger; `fillblock` is the way to feed a battery or a radio directly.
- [ ] A world saved with a radio on comes back with the switch on; it runs only if the battery still has charge.
- [ ] Battery charge survives leaving and rejoining the world (it did not until 2026-10-21 midday: every relaunch
  emptied the batteries, which is what made a "charged" battery show PWR). Jade on a battery shows its own charge
  and the total over every battery on the same wire.
- [ ] With no battery on the wire at all the panel stays dark when switched on (no PWR cycle); PWR is only for a
  battery that cannot keep up.
- [ ] Display corners: the mode (SSB, CW, or FM on the VHF base station) top left, TX top right while sending, both
  in the same small size. Does the mode read well on each radio's display texture?
- [ ] "Going Ham" advancement: placing any block radio pops a toast with the icon of the 20m radio and a reminder
  that it needs power and an antenna; it gets its own RadioCraft tab in the advancements screen. Is the wording OK?

## 4:1 balun, off-centre-fed dipole, release tab (added 2026-10-21)

- [ ] New "Balun (4:1)" (`radiocraft:balun_four_to_one`) next to the 2:1 in the tab; its texture is the 2:1 art
  until the bounty is drawn. The 2:1 still feeds the quad loops (horizontal and vertical squares); try one.
- [ ] OCFD: 4:1 balun, arms 4 and 7 on the same level. Right click names `radiocraft:off_center_fed_dipole`; a 20m
  radio sees SWR 1, a 10m radio also 1, a 15m... there is no 15m radio, so check with `/rcdebug antenna`: 21 MHz
  is above 2. A 5 + 5 on the 4:1 is not recognized (that is a dipole, use the 1:1). A 40m OCFD is 7 + 14.
- [ ] Creative tab: the HF station items are in the main section without the blue "dev" watermark; the duplexer,
  repeater, digital interface, dish, microphone block and power blocks keep it (dev runs only).

## Tooltips (added 2026-10-21)

- [ ] The blanket "Not Implemented (Decorative Only)" line is gone from everything that works: the HF radios, QRP
  radios, both receivers, the VHF base station, the three wires, poles and connectors, the VHF antennas, solar panel,
  charge controller and large battery each have a sentence instead (needs `./gradlew runData`). Only the microphone
  block and the satellite dish still say not implemented. Read the sentences: anything wrong or missing?

## Digital interface (TNC): APRS (added 2026-10-21; now split, see "Radio Interface, H19 and Transfer Interface")

Two VHF base stations a few blocks apart (or with antennas), both on 144.390, each with a digital interface touching
it. `/rcdebug` is not needed; GameTests cover delivery, digipeating and the command, so this is about the screen and
the sound.

- [ ] Placing the interface sets it to your callsign (green lamp tooltip shows who set it). Clicking CALLSIGN opens
  an edit box over the map; ENTER saves, ESC cancels without closing the screen, typing 'e' does not close it, and
  saving it empty claims your own callsign again.
- [ ] The blue lamp beacons: the radio sends a short burst of packet tones (about half a second) that any radio on
  the frequency plays, then the other interface lists the station on the paper (callsign, distance, bearing) and
  draws its icon on the grey map, north up, with a hover tooltip (coordinates, comment, age, digipeaters). Is the
  burst too loud or too long? Do the tiny half-scale letters read OK on the paper and the fields?
- [ ] BCN (comment) and ICON (click to cycle: door, compass, minecart, boat, saddle, campfire, lightning rod, ender
  eye) show up in the far station's tooltip and map. The map zooms out to fit the furthest station.
- [ ] Beacon enable (on either tab) beacons right away and then every 10 minutes (server config
  `tnc_beacon_interval_seconds`); a redstone pulse into the interface beacons too. New-contact buzzer: a high note
  block blip the first time a station is heard.
- [ ] MSG tab: TO box and message box (100 characters), SEND MESSAGE. The far interface buzzes (low blip), the red
  lamp blinks on its screen, red dust flickers on top of the block, and a comparator reads 15 until someone opens the
  MSG tab. The message shows on both papers (yours as "> CALL: text"); long ones wrap and the paper scrolls.
- [ ] A message to ALL lands on every interface that hears it.
- [ ] Digipeating: the blue lamp under RPT on the APRS tab toggles it (lit = on, off by default). With it on, a
  packet heard goes back out a second later on every radio the interface touches; the red RPT lamp lights from
  hearing it until it has gone. Works with one radio (same frequency) or two (crosses over). Nothing past 8 hops.
- [ ] Dummy load (dev tab): touching a radio or on its coax, the radio shows SWR 1.0 on every band and reaches only
  about 10 blocks; an all-band radio switched from HF to 2m follows within half a second. Model and hitbox line up?
- [ ] Duplexer on top of the repeater: the coax legs run down the repeater's back and the duplexer turns to face it
  whichever way the repeater faces; take the repeater away and it goes back to the plain model.
- [ ] J-pole and duplexer: no more z-fighting (maintainer's model fixes).
- [ ] Aurora crackle: during an aurora (`/rcdebug` aurora), past the aurora latitude, in a snowy biome, faint pops
  every several seconds from a few blocks away (vanilla campfire crackle, 4 to 9% volume, high pitch). Should be
  near inaudible over rain, music or a radio's static. Nothing in a warm biome, or when the aurora ends.
  `/auroracrackle [pops]` (client command) plays one burst right now at the same volume and spread, anywhere.
- [ ] `/aprs beacon 144.390 W1AW Net tonight` and `/aprs broadcast all ALL hello` reach interfaces anywhere.
- [ ] Radios on ceilings: click the underside of a block with a radio. It hangs the right way up, its top against
  the block (a raised copy of its model; the repeater rises 4 px). Panel, display and lights read normally; the
  hitbox matches. Every radio, all four facings.
- [ ] Wires on ceilings: click the underside of a block with wire, coax or waterproof wire. It runs along the
  ceiling, joins hanging radios and devices, and carries on sideways when you extend it from a hanging wire (or
  where there is a ceiling but no floor). Runs down a wall hug it; at a beam hanging lower, the cable runs down
  the beam's face to a wire or device under it (the floor climb, upside down). Floor-to-ceiling columns line up on
  one edge. Check the pieces look right flipped (no gaps, textures not inside out) and hitboxes follow the cable.
- [ ] TNC screen size: it scales up to at most 80% of the window, in whole window pixels (crisp art). Try a few
  window sizes and GUI scales; clicks, slots, item dragging, scrolling and tooltips must all still line up.
- [ ] APRS map terrain: the grey screen shows the land around the interface like a vanilla map (shaded by slope,
  dimmed under the icons), north up, and zooms out with the stations heard. Past render distance it is dark.
  Does it repaint when you walk/build (every 3 s)? Any stutter opening the tab?
- [ ] RTTY (stations on an HF band too, say 14.080): whatever you type on the RTTY tab goes out as you type it
  (upper case only, no backspace: it is already on the air), and Enter or SEND ends the line. Both radios play the
  warbling RTTY tones, about 6 characters a second. The far interface prints "CALL: TEXT" with a blinking cursor,
  starts the teletype sound and rings the bell at the end of the line; yours prints its own copy. Does typing 'e'
  or a number key do anything it should not? Is the 0.6 scale text readable?
- [ ] RTTY paper: one sheet does 20 lines (top right of the paper shows lines left, PAPER OUT in red). With no
  paper nothing prints, but you can still send. Paper goes in by right clicking the interface with it, from a hopper
  on top or side, or the FILES tab's PAPER slot.
- [ ] RTTY at the edge of range prints with some characters garbled; a bit further and nothing at all.
- [ ] FILES: the tab shows a vanilla inventory panel under the interface (stand-in art) and the item slots: SEND,
  four RECEIVED, PAPER / BOOKS / MAPS. Other tabs hide them. Shift click sends paper, books and maps to their
  supply slots and written books, books and quills, paper and filled maps to SEND.
- [ ] Send a written book to the far callsign (TO), or to everyone (TO empty): the six LEDs fill up on both
  interfaces as it goes (a few seconds, plus half a second a page), the radios play packet tones, and the far one
  prints a "copy of" into RECEIVED using one of its blank books. The sender keeps the original. A copy of a copy is
  refused. The paper strip on the right logs TX/RX lines, NO BLANK BOOK, TRAY FULL.
- [ ] Send a filled map: SSTV tones (the classic "beep, bloop" start, then about 26 s of scanning) and the far
  side prints the same map on an empty map. Does it sound like SSTV? A hopper under the interface pulls the copies
  out of RECEIVED.
- [ ] Break the interface: its supplies, the SEND item and the received copies drop.

## Wires climb walls (added 2026-10-21)

Lay coax (or power wire) along the ground up to a one-block step with more wire on top of the step.

- [ ] The wire at the foot runs up the face of the step and meets the wire on top at its edge; no floating vertical
  piece, no gap at the top edge. Same for all four directions and for wire, waterproof wire and coax. Does the
  climb piece look right against the horizontal stub (no z-fighting at the bottom corner)?
- [ ] Order does not matter: foot first then top, or top first then foot, and the radio sees the antenna either way
  (`/rcdebug radio`). Break the step: the climb vanishes and the radio loses the antenna; put it back: it returns.
- [ ] A climb straight into a device on the step (battery on a ledge, balun on a post): the cable runs up the post,
  over its top edge and along the top to the middle of the balun's block, ending just under the balun's body. That
  last piece is drawn by the climbing wire outside its own block, so check it against the post's top texture and in
  the dark, and at chunk borders (x or z a multiple of 16).
- [ ] Balun one block above the post top, coax on the post under it (maintainer's build, 2026-10-21): the run comes
  up the post face, carries straight on up the same edge through the block on the post (no wall there, no jog) and
  turns in to the balun's middle only at the ceiling. Nothing extends to the middle at floor level any more. A
  second coax beside that one is a real branch and will draw a stub; leave it out.
- [ ] A wire directly above the foot wire wins: with coax at the foot, on the step and also floating above the foot,
  the foot does not climb (the column carries the run) but still hugs the step, and the piece above arrives at the
  step's face before turning onto the step. Removing the floating piece makes it climb; both look the same.
- [ ] A wire column straight up beside a post or wall (maintainer's screenshot, 2026-10-21): every piece hugs the
  post, the bottom piece turns from its floor run to the post, the top piece arrives on the post's face and turns
  towards its own connection. Where the post has a gap the run jogs back to the middle for that block.
- [ ] Waterproof wire now has a blockstate and renders like wire (it had none and showed the missing-model box).
- [ ] Coax has its own darker texture (maintainer, 2026-10-21, `coaxial_wire.png`) on every piece: floor, edge
  runs, corners, overhangs. Power wire and waterproof wire keep the light one.

## From the 2026-10-19 work (CW restored, transmission tests)

- [ ] CW by ear between two stations: tone starts and stops cleanly, no stuck tone, sidetone on the sending radio.
- [ ] `/rcdebug parrot <pos> <seconds>` on a radio with no controller still echoes voice and CW.
