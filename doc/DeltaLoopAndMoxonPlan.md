# Plan: delta loop and Moxon antennas

Status: plan only (2026-10-24). Todo items: "Implement Moxon antenna variants (VHF/10m/20m)" and the delta loop
under "Loop antennas". No models are needed for the wire versions; the VHF Moxon as a block would need one (bounty).

## What the proposal says

| Antenna | Build | Performance |
|---|---|---|
| Delta loop | Triangle, each side 1/3 wavelength, coax at a corner, at least 4 m above ground, 4:1 balun | 10% of local (line of sight) range, 50% better long range (skip) |
| 10m Moxon | Rectangle of antenna wire, 3 x 1, horizontal | 50% more range in one direction, 15% in the others |
| 20m Moxon | Rectangle of antenna wire, 7 x 3, horizontal | same |
| VHF Moxon | Craftable block, stands vertically, 1 m tall | same |

The proposal is background, not a spec (AGENTS.md): the numbers below adapt it to what the code already does.

## How antennas are recognised today

- `AntennaTypes.match(level, balunPos)` tries every registered `IAntennaType` in order; the Improvised type is the
  fallback (`registerFallbackType`). Each type's `match` walks `AntennaWire.getWires(level, pos)` from the balun.
- `HorizontalQuadLoopAntennaType` already walks a closed loop (balun, three corners, back to the balun) and checks
  it is a square. The delta loop is the same walk with three corners and a triangle check.
- Reach comes from `NonDirectionalAntennaType(id, rxGainDbi, txGainDbi, los, skip)`: `los` and `skip` scale the
  band's line-of-sight and skip strength. Directional antennas extend `DirectionalAntennaType` and implement
  `getDirectionalEfficiency(data, from, to)`, as the Yagi does.
- SWR: every block off the resonant length adds 0.5, as for the dipole and quads.

## Delta loop

**Build:** a 4:1 balun with exactly two wires, going to two corner holders (poles or connectors), joined by a third
wire: a closed triangle of three wires, fed at one corner. Any orientation: flat, upright (the usual way, apex up or
apex down) or sloping.

**Match rules (`DeltaLoopAntennaType#match`):**
1. Balun is `BALUN_FOUR_TO_ONE` with exactly 2 wires (the OCFD also uses the 4:1, but with 2 *open* arms; the closed
   loop check tells them apart, so try the delta loop before the OCFD).
2. Follow wire 1 to corner A; A has exactly 2 wires; the other one goes to corner B; B's other wire comes back to the
   balun. Anything else (a branch, a fourth corner) is not a delta loop (it falls through to Improvised).
3. Sides: s1 = balun-A, s2 = A-B, s3 = B-balun, measured as straight-line block distances (wires sag, but the
   matcher uses end points, like the other types).
4. Height: the lowest of the three points is at least `delta_loop_min_height` (default 4) blocks above the ground
   below it (`level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z)`). Too low still matches, with an SWR
   penalty (+1.0) and the "too low" note in Jade, rather than not matching: low loops do work, badly.

**SWR:** the perimeter wants to be one wavelength (each side a third). SWR = 1 + 0.5 per block the perimeter is off
the wavelength (rounded), + 0.25 per block the longest side exceeds the shortest (lopsided triangles match worse).
Wavelength in blocks: 80m 80, 40m 41, 20m 21, 10m 10 (1 block = 1 m, as `BandUtils`).

**Reach:** `los` 0.1, `skip` 1.5, gain 1 dBi both ways (proposal: 10% local, 50% better long range). That makes it
the DX loop, the opposite trade-off to the 5-BTV's mix.

**Data:** `DeltaLoopAntennaData(perimeter, maxSide, minSide, tooLow)` with NBT like the quad loop data.

## Moxon (wire, 10m and 20m)

A Moxon is a two-element beam bent into a rectangle: the driven element along the front, fed in the middle, with its
ends folded back; the reflector along the back with its ends folded forward; the folded tails almost meet, leaving a
small gap.

**Build in blocks:**
```
  R---------R        back: reflector (wire between two holders), tails come forward
  |         |
  r         r        r = reflector tail ends (holders), d = driven tail ends; gap of 1 block between r and d
  d         d
  |         |
  D----B----D        front: driven element, 1:1 balun B in the middle, wires to the corners D, tails back
```
- Driven element: 1:1 balun with two wires to front corners D; each D has one more wire going back to a tail end d.
- Reflector: a separate wire run, holder r - corner R - corner R - holder r, not connected to the balun (it is a
  parasitic element: the matcher looks for it at the expected place instead of walking to it).
- The tail ends d and r sit on the same line, one block apart (the gap).
- Width (D to D) and depth (front to back) from the proposal: 20m 7 x 3, 10m 3 x 1. With a 1 block gap a 10m Moxon
  has no room for tails, so the 10m version is "two parallel wires 1 block apart": driven 3 wide on the balun,
  reflector 3 wide behind it. That is really a two-element Yagi, which is fine to call a 10m Moxon in game.

**Match rules (`MoxonAntennaType#match`):**
1. Balun is 1:1 with exactly 2 wires, both horizontal and opposite (like a dipole), each ending at a corner D.
2. Each D has exactly one more wire, horizontal and at a right angle, to a tail end d, both tails on the same side.
3. Mirror the shape across the gap: look for wires at r (d moved one block further back) going to R corners and a
   wire R-R. Use `AntennaWire.getWires(level, rPos)` for each expected holder.
4. If no reflector is found, it is not a Moxon: the balun's build falls through to the dipole check (bent arms) and
   then Improvised, which is right: without the reflector it is a bent dipole.
5. The beam points from the reflector to the driven element (front). Store the facing in the data.

Try the Moxon before the dipole, since a Moxon's driven element starts like a dipole.

**SWR:** width and depth for the band: 20m wants 7 x 3, 10m 3 x 1 (per proposal; scale by wavelength for others
later). SWR = 1 + 0.5 per block off in width + 0.5 per block off in depth. Only 20m and 10m sizes for now; any other
size is matched but at the SWR that gives.

**Pattern (`getDirectionalEfficiency`):** angle between the facing and the bearing to the other station, in the
horizontal plane. Forward (within 45 degrees): 1.5. Sides fall linearly to 0.15 at 90 degrees. Behind: 0.15 (a good
front-to-back ratio is the point of a Moxon). Same both ways (transmit and receive).

**Data:** `MoxonAntennaData(width, depth, facing)`.

## VHF Moxon (block)

Not in this round: it needs a model. When the art exists it is a two-tall block like the Yagi (`DoubleVHFAntennaBlock`,
facing from placement) with the Moxon pattern above and SWR 1 on 2m. Add it to the model bounty when we get there.

## Work list

1. `DeltaLoopAntennaType` + data + registration (before OCFD and the Improvised fallback). Unit tests for the
   geometry (closed triangle, perimeter, lopsidedness) using `BlockPos` only, and GameTests: a 20m delta loop at
   height 5 matches with SWR 1; one at height 1 matches with the low penalty; a 4:1 with two open arms is still an
   OCFD.
2. `MoxonAntennaType` + data + registration (before the dipole). GameTests: a 20m Moxon matches facing the right way;
   removing the reflector turns it into Improvised; a station in front hears it 10x louder than one behind
   (1.5 / 0.15).
3. Jade (`AntennaInfoProvider`) and the balun right-click name the new types; the antenna analyzer reports them.
4. Patchouli: entries with a build page each (balun, holders and connectors as the multiblock, wire runs in the text).
5. `doc/NeedsTesting.md`: build each in game, check the wire runs are easy to string.

## Open questions for the maintainer

- Delta loop below 4 m: penalty (proposed) or no match?
- 10m Moxon at 3 x 1 has no room for the folded tails: accept the two-wire version, or make it 3 x 2?
- Should a Moxon's beam also cut line-of-sight range behind it, or only skip? (Proposed: both, same pattern.)
