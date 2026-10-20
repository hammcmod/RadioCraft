# Redstone interference

Design notes agreed with the maintainer on 2026-10-20. The guiding principle, as everywhere in the mod: match how
radio actually works, simplified where it has to be.

Real-world basis: switching electronics (power supplies, motors, arcing contacts) radiate broadband noise. A nearby
antenna, and especially the feedline, picks it up and the operator hears it as a raised noise floor or a buzz. Steady
DC is silent. In the other direction, a transmitter's RF gets into unshielded electronics near the antenna and makes
them misbehave, worse when the antenna is badly matched because more power ends up on the feedline and in the shack.

## Receive side (implemented)

- **Trigger:** a redstone *change*, not a powered block. The server's neighbour-notify event fires for every block
  update; redstone signal sources (dust, repeaters, comparators, torches, levers, observers...) count as one full pop,
  blocks that are only driven by redstone (pistons, doors, note blocks, dispensers) as 0.3 of a pop, anything else as
  nothing. A block being placed or broken is not electrical.
- **Where:** within 4 blocks of the feedline (the radio block and its coax) or 8 blocks of the antenna (centre block and
  wire runs), fading linearly to nothing at the edge. The stronger of the two couplings wins.
- **How much:** one pop right on the wire adds 0.3 to the radio's interference level (0 to 1), decaying by a factor 0.8
  per tick, so a single flip is a short pop and a clock settles into a steady buzz. Band factor: 80m and 40m 1.0, 20m
  0.5, 10m and VHF 0.1.
- **What it does:** boosts the existing static. The client's static volume is `min(1, solar noise + interference)`
  times the antenna match factor. Above 0.25 a low hum fades in on top (a placeholder tone; a real buzz loop is on the
  asset list). No separate degradation of the received signal: when static masking (plan step 3) lands, the summed
  noise is what masks.
- **Only the receiving station** is affected. The transmitting station is unchanged.
- **Mitigation:** distance, and chokes once they exist. No shielded coax.
- **Config** (server, `Redstone Interference` section): enable/disable, strength multiplier, and an option to only
  affect stations whose SWR is at or above a threshold.
- **Performance:** each powered radio registers a `PickupZone` under the chunks its padded bounding box touches
  (rebuilt when its antenna changes and every 5 s). The event handler does one hash lookup per block update and
  returns when the chunk has no zone; only then does it classify the block and measure the distance to a few dozen
  points. Pops are summed per tick and sent to clients tracking the chunk as one small packet per active tick, and a
  quiet radio sends nothing.

Code: `common/radio/rfi/` (`RedstoneInterference`, `PickupZone`, `InterferenceLevel`), `CInterferencePacket`, the
static and hum sound instances, `RadioBlockEntity.tickInterference`. Tests: `RedstoneInterferenceGameTests`,
`PickupZoneTest`, `InterferenceLevelTest`. `/rcdebug radio <pos>` prints the current level and zone.

## Transmit side (implemented)

- **Trigger:** the station is transmitting: PTT held in SSB, or morse keyed within the last two ticks.
- **Reach:** redstone within 2 blocks of the antenna wire for QRP rigs, 6 for full-power rigs. With a bad SWR (at or
  above the config threshold, default 2.0) the RF is on the feedline and in the shack too: 10 blocks of the radio and
  5 of the coax. Vertical antennas (quarter wave vertical, J-pole, Slim Jim) only reach 2 blocks above or below their
  wire.
- **Dust** in reach reads an induced level for as long as the station transmits: 4, rising two per unit of SWR once
  the match is bad, capped at 15. Dust recomputes its power from its neighbours on every update, so a level written
  into its block state does not survive; `MixinRedStoneWireBlock` adds the induced level inside that calculation
  (one `@Inject` at the return of `RedStoneWireBlock.calculateTargetStrength`, `require = 0`). If a Minecraft update
  moves that method the injection is skipped with a warning, dust stops reacting and everything else keeps working.
- **Switches:** levers get pulled, buttons pressed and redstone torches flickered (two ticks) when the transmission
  starts. Switches mounted on a command block are never touched: RF must not fire an admin's command blocks. (The
  GameTest framework bolts exactly such a button onto every test plot; a transmitting test pressed it and restarted
  the test runner, which is how this rule was found.)
- **Predictable:** which components are hit comes from a hash of the station (radio and antenna position, SWR bucket,
  rig class) and the block position, compared against the config chance (default 0.35). The same door misbehaves
  every time you key up until the antenna, its match or the contraption changes.
- **Config:** `redstone_interference_transmit_enabled`, `redstone_interference_transmit_chance`; the master switch,
  the only-bad-SWR option and the SWR threshold are shared with the receive side.
- **Performance:** the scan happens once when a transmission starts, over the reachable box, reading only loaded
  chunks (an unloaded chunk would be loaded synchronously from inside a block entity tick) and doing distance maths
  only for redstone components. Dust levels live in `RfSignals`, whose fast path is a single volatile read.

Code: `TransmitInterference`, `RfSignals`, `mixin/MixinRedStoneWireBlock`, `RadioBlockEntity.tickInterference`.
Tests: `TransmitInterferenceGameTests` (dust level while keyed and release, lever and torch, bad SWR reach and level,
QRP reach), `TransmitInterferenceTest` (reach, vertical rule, roll spread, seed). `/rcdebug radio <pos>` lists what a
transmitting radio is inducing.
