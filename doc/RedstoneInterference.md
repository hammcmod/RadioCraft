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

## Transmit side (agreed, not built yet)

- Redstone near the antenna has a chance of being driven to a low signal level (about 4) when the station transmits:
  within 2 blocks for QRP rigs, 6 blocks for full-power rigs.
- With a bad SWR the reach grows to 10 blocks of the radio and 5 of the feedline, and the signal level rises with the
  SWR.
- Vertical antennas (the antenna type already knows its polarisation/shape) do not affect blocks more than 2 above or
  below them.
- Which blocks are hit must be **predictable**, not re-rolled every tick or every transmission. Proposal: decide the
  affected positions deterministically from the station (antenna position, SWR bucket, power level) whenever the
  network changes, so the same door opens every time you key up until you fix the antenna.
- Config: enable/disable, strength, only with bad SWR (same section as receive).
- Open implementation question: vanilla has no hook to inject a redstone signal into an arbitrary block. Options are a
  mixin on the level's signal lookup (cleanest, invisible to other mods) or directly toggling the few block types
  that can be set (levers, dust power, repeaters) for the duration of the transmission.
