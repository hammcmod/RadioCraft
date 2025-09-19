# Development Checklist

## Phase 1 – Core Gameplay Readiness

### Radios & Networking
- [x] Core HF radios (10m/20m/40m/80m) and their recipes are registered
- [ ] Core HF radios need working SWR/propagation hooks to be usable
- [x] HF receiver, QRP radios, and the all-band radio shapeless combinations exist
- [x] VHF Handhelds have functional radios
### Antennas & Signal Model
- [x] Dipole, end-fed, quarter-wave vertical, loop, wide-band receive, and VHF Yagi/J-pole/Slim Jim antennas are implemented
- [ ] Antenna tuner and duplexer blocks need runtime effects (matching, losses, repeater protection) to unblock repeater work
- [ ] Antenna analyzer implementation needs to be implemented
- [ ] Antennas need to have an implementation
- [ ] Hook SWR consequences (fire/destruction) into `RadioBlockEntity` once analyzer feedback exists
- [ ] Integrate solar event noise and day/night skip changes so HF radios actually gain/lose range based on `SolarEventManager`
- [ ] Implement HF static/grounding modifiers and lightning spikes so propagation feedback reaches the player (depends on the power/static hooks in Radios section)

### Events, Commands & Config
- [x] Callsign capability with `/callsign` tools implemented
- [ ] Implement administrative callsign commands (e.g., `/callsign set`) with permission checks
- [ ] Extend server config to cover callsign digit pools, HF static squelch, modern/vintage toggles, and “talk everywhere” overrides promised in the proposal
- [ ] Add `/bandcalculate` helper to complement antenna tuning once analyzer exists (depends on antenna data persistence)

## Phase 2 – Power Systems
### Power & Components
- [ ] Make the solar panel and batteries actually use FE/other energy sources
- [x] Wire block recipe implemented
- [x] Waterproof wire crafting implemented
- [x] Radio crystal/speaker/microphone/HF circuit board recipes exist, albeit with lighter-cost ingredients than the proposal (iron/planks/redstone torch instead of diamond/iron)
- [ ] Finish small alkaline battery energy handling — `SmallBatteryItem` still shows “not implemented” and never tracks charge; required before handheld drain logic can land
- [ ] Implement handheld battery drain and Curios slot behavior in `VHFHandheldItem#inventoryTick` (blocked by the battery energy component above)
- [ ] Large battery behavior (stacking capacity, discharge, explosion-on-fire) remains unimplemented; tooltips warn “not implemented” and power draw from radios currently ignores battery levels
- [x] Solar panel recipe uses glass + a daylight detector rather than kelp/iron
- [ ] Wire–water electrocution rules and battery ignition on short circuits are still missing from `WireBlock` and related power logic

### Radios & Networking
- [ ] Ensure radio power draw, on/off state, and static playback tie into battery/solar networks via `RadioNetworkObject` once large-battery work is complete

## Phase 3 – Extended Systems & Content

### VHF/UHF Channel and Repeater Features
- [ ] VHF repeater remains decorative—no block entity, callsign prompt, duplexer validation, or sleep/wake cycle (depends on duplexer functionality from Antennas section)
- [ ] VHF receiver UI lacks the multi-channel scan indicator and still surfaces the “not implemented” tooltip (needs handheld battery work so receive states can display correctly)

### Digital & APRS Features
- [ ] Digital interface/TNC block remains a placeholder; implement data transfer, APRS messaging, and Curios integration after core radio power/SWR work is stable
- [ ] Add `/aprs beacon` and `/aprs broadcast` commands plus in-game item transfer once the digital interface is functional

### Antenna Expansions
- [ ] Implement Moxon antenna variants (VHF/10m/20m) and register corresponding antenna types after tuner/analyzer tooling is online
- [ ] Revisit delta loop construction to support the triangular build flow; ensure analyzer messaging reflects the new geometry
- [ ] Add sag-varying visuals for long unsupported antenna wire runs for immersion once functional tuning exists

### World & Progression Additions
- [ ] Upgrade the solar weather station from a tooltip placeholder to a working block (paper consumption, coordinate calculator, comparator output)—depends on solar events influencing gameplay
- [ ] Flesh out wire spool storage UX (bulk crafting/collapse) when power hazards are implemented
- [ ] Introduce random structures, auto-beacon radios, APRS Discord bridge, and external radio API hooks after the communication loop is fun and reliable
- [ ] Align crafting costs (e.g., netherite-heavy radios) with the original spec or update the design doc to reflect the lighter recipes currently shipped
