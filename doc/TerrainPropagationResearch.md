# Research: terrain-aware propagation (line of sight and sky view)

Status: phases 1 and 2 implemented (2026-10-24) in `common/radio/terrain` (`TerrainAtlas`, `TerrainMath`,
`TerrainPropagation`, `ExpiringCache`); the atlas is saved with the world and the scaling note below is done (2026-10-24); the rest of phase 3 is open. Tests: `TerrainMathTest`, `TerrainBenchmarkTest` (timings,
printed), `TerrainGameTests` (a floating wall south of a station, and the cost in game). `/rcdebug terrain` shows the
map size and the cost so far; `/rcdebug terrain path <from> <to> <band>` what terrain does to one path.

## Measured cost (2026-10-24, the maintainer's dev machine)

| What | Cost |
|---|---|
| Line-of-sight loss, 100-500 block path, worked out | 5-10 us |
| Horizon profile, worked out | 18-32 us |
| Strength to one receiver in game (20m dipole, 50-250 blocks): terrain off / worked out every packet / cached | 0.23 / 1.5 / 0.24 us |
| Busy server (10 talking at once, 40 receivers, 50 frames a second), every packet | ~220 ms per second of talk (22% of a core) |
| The same, results kept 2 seconds (the default) | 5-8 ms per second of talk (under 1% of a core) |
| Atlas memory | ~165 bytes per chunk: 100,000 chunks ~16 MB |

The cache is what makes it cheap: a path or horizon is worked out at most once per `terrain_recalculate_seconds` per
pair of 4 block cells, so a talking station costs a map lookup per receiver per packet, and a walking handheld is
worked out again about once a second (as it changes cell).

## As built (differences from the plan below)

- A cell's height is the average of its 16 columns, not the highest, so tree trunks and posts barely count.
- Obstruction loss counts only past grazing (the knife-edge loss minus its 6 dB at v = 0), so open flat ground costs
  nothing compared with before. Losses are in power terms (dB / 10), like antenna gains.
- A path pays the worse of the knife edge and its cliffs, not both, and a cliff only counts when its top reaches the
  first Fresnel zone of the path (a mountain-top station does not care about cliffs far below it).
- Indoors is only for handhelds (no sky above the player's head, read on the server tick). A fixed antenna's balun is
  often inside the shack with the wires outside, so fixed antennas never count as indoors.
- Dimensions with a ceiling (the Nether) have no terrain at all. The GameTest server runs with terrain off except
  for its own tests (the test plots are hills to each other).

## The question

Two things terrain should do:
1. **Line of sight** (VHF everywhere, HF up close): a hill between two stations weakens or blocks the path, more on
   VHF than HF.
2. **Sky view** (HF skip): a skip signal leaves at an angle. A station deep in a valley, under a cliff or indoors
   loses the low-angle paths (10m and 20m DX) while high-angle 80m and 40m (NVIS) still get out.

It has to stay cheap for base stations (fixed) and for handhelds and future manpacks, which move every tick.

## Where the cost lands today

- Every voice frame (50 a second per talking station) is sent by `StaticAntenna#transmitAudioPacket` (or
  `PlayerRadio#transmitAudioPacket` for handhelds) to **every antenna on the band's network**, computing a strength
  for each. That runs on **Simple Voice Chat's thread**, not the server thread.
- So anything added per path must be (a) fast: microseconds, (b) thread-safe: no reading chunks or `Level` off the
  server thread, (c) unable to load chunks: a path of 500 blocks crosses chunks nobody has loaded.

## Options

| Option | How | Cost per path | Unloaded chunks | Thread | Verdict |
|---|---|---|---|---|---|
| A. Block ray march | `Level#clip` or stepping block by block | 50-200 us for 500 blocks, more in caves | Cannot (or force loads: very bad) | Server only | Too slow and too fragile |
| B. Live heightmap | `Level#getHeight(MOTION_BLOCKING_NO_LEAVES, x, z)` every 4 blocks | ~5 us | Cannot | Server only | Loaded chunks only, wrong thread |
| C. **Coarse atlas** | One height per 4 x 4 cell, copied from each chunk's heightmap when it loads | **~4 us** (spike) | Remembers every chunk loaded this session (and could persist) | Any (immutable arrays in a concurrent map) | **Recommended** |
| D. Worldgen estimate | `ChunkGenerator#getBaseHeight` for never-loaded cells | 20-100 us per column | Yes (natural terrain only) | Server only | Later, as a filler for C |

## Recommendation: a terrain atlas, knife-edge diffraction, horizon profiles

### 1. The terrain atlas
- Per dimension: `Long2ObjectOpenHashMap<short[16]>` (fastutil ships with Minecraft) behind a concurrent wrapper,
  or a `ConcurrentHashMap<Long, short[]>`: chunk key to the highest `MOTION_BLOCKING_NO_LEAVES` block of each 4 x 4
  cell (leaves do not block, water surfaces do).
- Filled on `ChunkEvent.Load` (and refreshed on `ChunkEvent.Unload`, which catches what players built while it was
  loaded). Each refresh swaps in a new array, so readers never see half an update.
- Cost: reading a loaded chunk's heightmap is 256 array reads; the spike's 10 us per chunk is mostly its synthetic
  sine terrain. Memory ~80 bytes per chunk with map overhead: 100,000 explored chunks is about 8 MB. A cap
  (forget the oldest beyond N chunks) bounds it.
- Unknown cells (never loaded this session) count as clear: the optimistic fallback costs nothing and is what the
  game does today. Phase 3 can persist the atlas (`SavedData`, region-like blobs) and fill unknown cells from worldgen.

### 2. Line-of-sight loss: knife-edge diffraction
- Walk the straight path in 4-block steps over the atlas (skip the first and last 8 blocks: the stations' own roof
  and mast). At each step the ground's clearance above the line between the two antennas gives the Fresnel-Kirchhoff
  parameter `v = h * sqrt(2 d / (lambda d1 d2))`. Take the worst `v` and the ITU-R P.526 loss
  `J(v) = 6.9 + 20 log10(sqrt((v - 0.1)^2 + 1) + v - 0.1)` dB (0 below v = -0.78), applied to the line-of-sight
  strength as a field factor `10^(-J/20)`.
- It brings the physics for free: wavelength is in `v`, so a 36 block ridge halfway along 500 blocks takes 2m down
  to under 10% while 80m keeps several times more (spike test `aHillBlocksVhfMoreThanHf`). Stations on hilltops
  clear it. One block is one metre, so real wavelengths work as they are.
- Single dominant edge first. If ridges in series need it later: Deygout (worst edge, then the worst on each side)
  costs about 3x and stays microseconds.
- Spike timing: **~4 us per 100-500 block path**, including two synthetic terrain evaluations the real thing does not
  do. At 50 frames a second and 10 receivers in range that is 2 ms a second of voice thread time without any cache.

### 3. Sky view for skip: horizon profiles
- Per station, the horizon elevation in 16 directions out to 64 blocks, from the atlas (spike: ~16 us). A skip
  signal towards bearing b leaving at takeoff angle t is cut by `horizon[b] - t` (full above, gone 10 degrees under
  in the spike; tune by ear).
- Takeoff angle per band (config): 10m about 5 degrees, 20m 10, 40m 30, 80m 60 (NVIS). So a valley station loses
  10m and 20m DX across the valley walls but works 80m fine, which is how it is in reality.
- Indoors: `Level#canSeeSky` at the antenna on the server thread (cheap) adds a fixed penalty, more for skip than for
  line of sight. Handhelds indoors: a fixed building loss on VHF (about 10 dB).

### 4. Fixed stations vs moving ones
- **Base stations and repeaters** (antenna blocks): compute the horizon profile when the antenna is recognised and
  when its chunk's atlas entry changes; keep it on the `StaticAntenna`. Path loss per pair is cheap enough to compute
  per frame, or cache per (pair, band) and drop it when either atlas cell along the path changes (rare).
- **Handhelds and manpacks** (`PlayerRadio`): quantise the position to its 4 x 4 cell and 4 blocks of height. Cache
  the horizon profile per quantised cell for a few seconds (players walk about 4 blocks a second, so it is recomputed
  every second or so while moving, ~16 us). Path loss computed per frame from the current position (4 us), or cached
  per (cell pair) for a second. Indoor flag updated on the server thread in the player's tick, read on the voice
  thread.
- Nothing ever reads chunks on the voice thread; everything it reads is immutable arrays swapped atomically.

### 5. HF ground wave over steep ground
Knife-edge alone barely touches 80m. Maintainer (2026-10-24): sharp inclines should hurt HF ground wave too. So
HF line of sight also takes a steepness loss: each step along the path where the atlas height changes by more than
`terrain_steep_rise` (default 8 blocks) between neighbouring 4-block cells costs a few dB, scaled by band (10m most,
80m least), capped per path. Gentle hills stay free on HF; cliffs and mountain walls do not.

### 6. Config
- `terrain_enabled` (server, default true): the master switch for low-performance servers. Off means no chunk
  hooks, no atlas memory and no terrain maths at all, exactly today's behaviour.
- `terrain_line_of_sight` (on/off), `terrain_sky_view` (on/off), `terrain_atlas_max_chunks`, `terrain_steep_rise`,
  the takeoff angles per band, the indoor losses.

### 7. How harsh
Maintainer (2026-10-24): per band and realistic, so no softening cap on the ITU-R loss. A ridge between two
handhelds on 2m can kill the path; 80m mostly shrugs off hills but not cliffs (section 5).

## Phases

1. Atlas (chunk load/unload hooks, concurrent storage, cap) + knife-edge line of sight for VHF and HF line of sight.
   GameTests: two stations either side of a placed stone ridge lose most of their 2m signal, keep more on 80m, and
   get it all back when the ridge is removed (after the chunk refresh).
2. Horizon profiles + skip clearance + indoor penalties, with the per-band takeoff angles. GameTests: a station in a
   built valley loses 20m skip across it, not along it, and keeps 80m.
3. Persistence and worldgen fill for never-loaded chunks; `/bandcalculate` and the antenna analyzer report terrain
   losses ("path blocked by terrain: -18 dB").

## Decisions (maintainer, 2026-10-24)

- Harshness: realistic, per band (section 7).
- HF ground wave: feels sharp inclines (section 5).
- A `terrain_enabled` server switch turns the whole system off (section 6).

- Memory cap (2026-10-24): yes, `terrain_atlas_max_chunks` default 100,000 (about 16 MB), oldest forgotten first.

## Scaling to many stations (2026-10-24)

The maintainer's question: players with several APRS stations, base stations and redstone-driven gear each.

- What costs is **transmissions per second x receivers each reaches**, not the number of stations. Voice is the big
  one (50 packets a second while someone talks); APRS beacons and digipeats are a packet every few seconds or
  minutes, and CW a few buffers a second, so those add almost nothing however many stations exist.
- Today every transmission works out a strength for **every antenna on the band's network**, even ones thousands of
  blocks past any range. With 30 players x 5 stations (150 antennas) and 10 talking: 75,000 strengths a second. The
  plain strength is ~0.25 us (about 19 ms a second); terrain worked out every packet would add ~5 us each (~375 ms a
  second, too much); with the 2 second cache it adds roughly the cost of a map lookup.
- Fixed station to fixed station: neither end moves, so the answer only changes when a chunk on the path changes.
  Keep it until then (each atlas chunk gets a version number; a kept path remembers the versions it read, or simply a
  per-dimension "terrain changed" counter for the chunks it crossed) instead of 2 seconds. That makes fixed pairs
  free after the first packet, loaded or not.
- Stations in unloaded chunks: their terrain cannot change while unloaded, so their kept paths never expire there.
- Moving stations (handhelds, manpacks) keep the 2 second cache; only pairs with a mover are worked out again.
- Before any of that, skip receivers out of any possible range (past the band's longest skip, and past line of sight
  on VHF) with one distance check, and later index each network's antennas by area so a transmission only visits
  nearby ones. That cuts the 75,000 a second down to the receivers that can actually hear.
