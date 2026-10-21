# Needs Testing

Things that are implemented and pass the automated tests but still need a human at a client (rendering, GUI, audio,
feel). Tick an item off once it has been played with, and note anything wrong in [todo.md](../todo.md) or fix it.
Add to this list whenever a change cannot be fully verified by `test` / `runGameTestServer`.

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
