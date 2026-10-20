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

## From the 2026-10-19 work (CW restored, transmission tests)

- [ ] CW by ear between two stations: tone starts and stops cleanly, no stuck tone, sidetone on the sending radio.
- [ ] `/rcdebug parrot <pos> <seconds>` on a radio with no controller still echoes voice and CW.
