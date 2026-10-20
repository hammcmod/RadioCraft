# Needs Testing

Things that are implemented and pass the automated tests but still need a human at a client (rendering, GUI, audio,
feel). Tick an item off once it has been played with, and note anything wrong in [todo.md](../todo.md) or fix it.
Add to this list whenever a change cannot be fully verified by `test` / `runGameTestServer`.

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

- [ ] Right click: a chat line like "Solar conditions: Good (noise floor 20%). 10m band: open". The event name reads
  as a word, not a key (needs `./gradlew runData` first). Tooltip replaced the "not implemented" one.
- [ ] The block still renders and faces the right way (it became a block entity; existing placed ones keep working).

## Lightning static (added 2026-10-20)

Powered HF radio in SSB, a thunderstorm (`/weather thunder`) or `/summon lightning_bolt` a few blocks away.

- [ ] Each strike: a sharp crash in the static, gone within a second. Compare 80m and 20m radios: 80m twice as loud,
  10m barely. A strike 200 blocks away is still a small crash; beyond 512 blocks nothing.
- [ ] Does the crash read as lightning? The proposal says 40% for a second; the default is 0.6 on top of the solar
  noise floor, decaying with the same 0.8/tick as redstone pops. If it should rumble longer, say so.
- [ ] `lightning_static_enabled = false` silences it.

## Jade (added 2026-10-20)

- [ ] Radio: "Signal: n/15" appears while receiving, "Interference: n%" while redstone or lightning noise is up,
  "Simplex repeat: Ns delay" when a controller is linked.
- [ ] Simplex Repeat Controller: delay, "Radio: linked to the west" (or none), "State: Idle/Receiving/Repeating".
- [ ] Solar Weather Station: conditions with noise floor and level, 10m open or closed.

## From the 2026-10-19 work (CW restored, transmission tests)

- [ ] CW by ear between two stations: tone starts and stops cleanly, no stuck tone, sidetone on the sending radio.
- [ ] `/rcdebug parrot <pos> <seconds>` on a radio with no controller still echoes voice and CW.
