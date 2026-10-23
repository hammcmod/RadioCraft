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

### Textures: crystals and crystal-quality radios

Requested by the maintainer 2026-10-22 (they plan to draw these). All are placeholders copied from existing art:

- ~~Raw crystals~~ Done (2026-10-23, maintainer): `textures/item/crystal_pure.png`, `crystal_good.png` and
  `crystal_poor.png`, shared by every band's raw crystal of that quality.
- Encased crystals: `textures/item/encased_crystal_<band>_<quality>.png`, copies of the old radio crystal texture.
- ~~Insulation~~ Done (2026-10-23, maintainer): `textures/item/insulation.png`.
- Premium (`_pure`) and Cheap (`_poor`) radios: `textures/block/<radio>_<pure|poor>.png`,
  `textures/gui/<radio>_<pure|poor>.png` and `..._widgets.png` for hf_radio_10m/20m/40m/80m and vhf_base_station,
  and `textures/item/vhf_handheld_<pure|poor>.png` plus its GUI pair. All copies of the plain radio's art.

### Radio Interface and Digital Transfer Interface

From the TNC rework (2026-10-23, see [TncRework.md](TncRework.md)). The H19 APRS Terminal is done (maintainer).

- **Radio Interface** (`radio_interface`): still the old digital interface model. Wanted: a small modem box with
  PWR, DCD, TX, RX and RPT lamps. Its screen is a callsign box and a TX switch on a plain panel.
- **Digital Transfer Interface** (`transfer_interface`): a placeholder stone box. Wanted: a file and SSTV terminal
  with a paper feed. Its screen is the old FILES tab.
- Lamps go in as separate named elements, like the H19's MSG / CONTACT / READY: `python tools/split_lamps.py <model>
  <LAMP>...` splits a Blockbench export into the body and one full-bright model per lamp.

### Hustler 5-BTV vertical antenna

A model of the Hustler 5-BTV: a multiband HF trap vertical (80/40/30/20/15/10m), an aluminium tube with the
trap coils along it, standing on a ground mount. Requested by the maintainer 2026-10-21.

- Wanted: a 3D block model and texture, about 7.5 blocks tall (a multi-block or tall-rendered model; the base
  block is where the coax connects), with the traps visible as thicker coil sections up the tube.
- No antenna type for it yet; it will need one (multiband vertical) when the model lands.
