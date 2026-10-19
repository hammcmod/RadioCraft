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

## Open bounties

### 1. Parrot controller

A small device that sits next to a radio and turns it into a simplex "parrot" repeater: whatever the radio receives
is re-transmitted after a short delay. Used for testing a station alone and later as a gameplay block.

- Size: about 10 wide x 16 tall x 3 deep (in pixels, 16 = one block), a thin wall- or desk-mounted panel.
- Lights: a couple of small indicator pixels showing it is linked to a radio (e.g. green when linked), and one red
  light for "transmitting" while it echoes. Each light wants an emissive layer and probably its own blockstate so
  it can switch (unlinked / linked / transmitting).
- Style: match the HF radios and charge controller (dark metal case, light front panel detail).
- States needed: `linked=false|true`, `transmitting=false|true`.

## Done

(none yet)
