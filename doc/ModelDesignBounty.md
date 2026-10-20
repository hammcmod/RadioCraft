# Model design bounty

Blocks and items that need a proper model and texture from someone who draws. Code can ship with a placeholder
(a plain box with flat colours), but these are what players will look at, so they deserve the same hand-drawn style
as the existing radios and antennas.

## How the existing assets are made

- Block models are Blockbench exports (`"credit": "Made with Blockbench"`, `"parent": "block/block"`) in
  `src/main/resources/assets/radiocraft/models/block/`, with one texture atlas per block in
  `textures/block/` (32x32 for small things like the baluns, 64x64 for radios and the charge controller) and
  `texture_size` set to match. `charge_controller.json` is a good small example, `hf_radio_20m.json` a large one.
- Lit parts use a separate emissive texture named `<block>_e.png` (see `all_band_radio_e.png`) or a glow mask
  (`desk_charger_glowmask.png`), so lights stay bright in the dark.
- Blocks with an on/off look have two models (`charge_controller.json`, `charge_controller_on.json`) switched by
  blockstate.
- Deliverables for a bounty: the `.bbmodel` source, the exported block model JSON, the texture PNG(s) including any
  emissive layer, and if the block has states, one model per state. Item models can usually reuse the block model.

## How a model gets into the game

What was done for the parrot controller, as the pattern for the next one:

- Export the block model JSON from Blockbench, then split it by state: one model with the body, and one tiny model
  per light holding only the lit face (`parrot_controller_rx.json` / `_tx.json`). The light models get
  `"render_type": "minecraft:cutout"` (transparent pixels would render black otherwise) and
  `"neoforge_data": {"block_light": 15, "sky_light": 15, "ambient_occlusion": false}` on the element so the LED is
  full bright. No `_e` texture or glow mask is needed for a plain JSON block; those are for the GeckoLib models.
- A multipart blockstate stacks them: body rotated by `facing`, each light when its property is true, and any
  connection stubs (the parrot's wire towards its radio) by their own property.
- Textures are lower-case files under `textures/block/`; the `.bbmodel` source goes in `doc/models/`.

## Open bounties

### Sound: redstone interference buzz

Not a model, but the same kind of ask. `RadioInterferenceHumSoundInstance` plays when redstone keeps switching near a
station (a clock next to the coax). It currently reuses `morse.ogg` pitched down to a hum as a placeholder.

- Wanted: `rfi_buzz.ogg`, a 1 to 2 second seamless loop of a mains-style buzz (50/60 Hz fundamental with harmonics,
  a little crackle), mono OGG Vorbis like the existing `static.ogg`.
- To wire it in: drop the file in `assets/radiocraft/sounds/`, add an `rfi_buzz` entry to `sounds.json`, register it
  in `RadiocraftSoundEvents` next to `STATIC`, and point the hum sound instance at it (one line, pitch back to 1.0).

## Done

### 1. Parrot controller

A small desk unit (12 x 3 x 10 px) next to a radio that turns it into a simplex "parrot" repeater. Modelled and
textured by the maintainer, 2026-10-20: `models/block/parrot_controller*.json`, `textures/block/parrot_controller*.png`,
source in `doc/models/parrot_controller.bbmodel`. Green LED = the radio is receiving, red LED = it is echoing; the
wire stub towards the linked radio reuses the coax wire texture.
