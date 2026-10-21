# Testing RadioCraft

Three layers, cheapest first. Use the cheapest one that can answer the question.

| Layer | Command | Time | Use it for |
|---|---|---|---|
| Unit tests | `./gradlew test` | ~20 s | Pure math: propagation, SWR, noise/static formulas, band tables, packet codecs |
| GameTests | `./gradlew runGameTestServer` | ~1 min | In-world behavior: structures, networks, block entities, save/load |
| Dev server + RCON | `tools/devserver.sh start`, then `python tools/rcon.py "<cmd>"` | ~20 s to start, instant after | Poking a live world, building scenarios, watching state change over time, server restarts |

Things none of these cover: rendering, GUIs, and audio. Check those in `./gradlew runClient`
(or `tools/deploy-prism.sh` for a Prism Launcher instance), and for audio, by ear.

## 1. Unit tests (`src/test/java`)

Plain JUnit 5. Minecraft and NeoForge classes are on the classpath (`addModdingDependenciesTo`), but the game is
**not** bootstrapped, so stick to code that does not touch registries, configs, or a `Level`. Classes like
`BandUtils`, `SWRHelper`, `Band#getDefaults`, the `*AntennaType#getSWR` methods, antenna data records and packet
stream codecs are fine.

- Name tests `<ClassUnderTest>Test`, in the same package as the class under test.
- Failures print in the Gradle output. Full reports: `build/reports/tests/test/index.html`.

## 2. GameTests (`src/gametest`)

The `gametest` source set is attached to the mod for dev runs only, so it never ships in the jar.
`runGameTestServer` starts a headless server, runs every `@GameTest` in the `radiocraft` namespace, and exits.
Gradle fails if a *required* test fails. To see results:

```bash
./gradlew runGameTestServer --console=plain > build/gametest.log 2>&1; echo EXIT=$?
grep -E "LogTestReporter|tests failed|tests passed|   - " build/gametest.log
```

Writing tests:

- Put them in a class annotated `@GameTestHolder(Radiocraft.MOD_ID)` and `@PrefixGameTestTemplate(false)`, with
  `template = GameTestUtil.EMPTY` (an empty 16x8x16 area, `src/gametest/resources/data/radiocraft/structure/empty16.nbt`).
- Positions are relative to the test area. `y = 1` sits on the floor.
- **Place wires with `GameTestUtil.placeWire`**, not `helper.setBlock`. `setBlock` uses the default state, which
  has no connected sides, while a player placing a wire gets its connections computed.
- Block entities load (and create their network objects) on the tick **after** placement. Anything you place in the
  same tick is linked up by then, so build in any order and use `succeedWhen` / `thenWaitUntil` for the assertion.
- Antenna wires: `GameTestUtil.wire(helper, from, to)`. Wires only survive between wire-holder blocks, so the helper
  puts an antenna pole at `to` if it is air. Antennas are re-evaluated `antenna_update_delay` seconds (default 5)
  after a change; the test server ticks as fast as it can, so this is a fraction of a second.
- Network objects: `GameTestUtil.networkObject(helper, pos, RadioNetworkObject.class)`.
- **Known bugs are encoded as `required = false` tests.** They show as "optional tests failed" without failing the
  run. When you fix one, it should start passing; then remove `required = false`.
- The test server runs ticks as fast as it can, so a 200-tick timeout takes well under a second.
- Comparators only compute their output when a neighbour tells them to. `ComparatorGameTests.placeComparator` puts
  one on a stone block; place it *before* the block it reads (placing that block updates it) or change the block's
  output afterwards, then read `ComparatorBlockEntity#getOutputSignal` with `thenWaitUntil`.
- Anything level-wide that other plots would notice (day time, weather, a lightning bolt, config values) needs its own
  `batch = "..."`: batches run one after another, plots in a batch run at the same time.

In a dev client you can also run them interactively: `/test runall`, `/test run radiocraft:<name>`.

## 3. Dev server + RCON

```bash
tools/devserver.sh start                  # builds, launches in the background, waits for RCON
python tools/rcon.py "rcdebug solar" "time set noon"
tools/devserver.sh log 40                 # recent non-DEBUG log lines
tools/devserver.sh restart                # after code changes, or to test what survives a restart
tools/devserver.sh stop
```

- The world is superflat, creative and peaceful, at `runs/server/devworld`. Delete that folder for a fresh world.
  The surface is at **y = -60**.
- **Build inside x/z -48..47.** A dedicated server stops ticking block entities 15 s after the last player leaves,
  which freezes radios, antennas and power. `devserver.sh start` force-loads the 6x6 chunks around the origin so
  that area keeps ticking with nobody online (`forceload query` to check).
- `online-mode=false`, so `./gradlew runClient` (user "Dev") can join `localhost` to look at what you built.
- RCON is on port 25575 with password `radiocraft-dev` (`RCON_PORT` / `RCON_PASSWORD` override both).
- **All commands in one `rcon.py` call run in the same tick.** Devices link to wires on the next tick, so inspect
  in a later call.
- To place a wire with `/setblock`, give its connections explicitly, e.g.
  `setblock 0 -60 3 radiocraft:coax_wire[north=true,south=true]`.
- Server configs (`radiocraft-server.toml`, band limits etc.) live in `runs/server/config/`. Delete the file to
  regenerate it with the current defaults.

### `/rcdebug` (permission level 2)

Read-only:

| Command | Shows |
|---|---|
| `rcdebug radio <pos>` | band, frequency, power, mode, PTT, gains, measured SWR, static factor, the power flag synced to clients, connected antennas with SWR |
| `rcdebug antenna <pos>` | recognized antenna type, its data, SWR at the centre of every band |
| `rcdebug network <pos>` | network object, energy, and every network it is on with its members |
| `rcdebug solar` | current solar event, noise, progress, time of day, weather, each HF band's condition |
| `rcdebug solar set <event>` | start a fresh spell of an event (`blackout`, `great`, ...) in this dimension |

Stand-ins for player actions (these change the world):

| Command | Does |
|---|---|
| `rcdebug wire <from> <to>` | strings an antenna wire, like the antenna wire item |
| `rcdebug power <pos> on\|off` | radio power switch |
| `rcdebug tune <pos> <MHz>` | sets frequency (clamped to the band) |
| `rcdebug mode <pos> ssb\|cw\|off` | mode buttons |
| `rcdebug transmit <pos>` | sends one 20 ms frame of tone from an SSB radio, as if its operator spoke (no PTT needed) |
| `rcdebug key <pos>` | keys one buffer of morse (dit, gap, dah) from a CW radio |
| `rcdebug parrot <pos> <seconds>\|off` | the radio echoes everything it receives after the delay (a simplex "parrot" repeater) |

Whatever another radio receives shows up as `lastSignal=<strength> receivedTicksAgo=<n>` in `rcdebug radio`. With a
client standing near the receiving radio you also hear it: voice through Simple Voice Chat, CW as the morse tone.

**Testing voice and CW alone.** Build two stations, make the far one a parrot (`rcdebug parrot 20 -60 0 2`), stand at
the near one, hold PTT and talk (or key morse in CW mode), let go. Two seconds later your transmission comes back from
the far station at the strength the propagation model gives that path, and you hear it at your own radio. The echo
carries the audio as the parrot heard it before its receiver scaled it, so what you hear has one path's loss, not two.
Move the parrot further away or detune its dipole to hear the signal weaken. The parrot must be on and in the same mode
as you (SSB echoes voice, CW echoes morse); the command's flag is not saved with the world.

The same thing as a block: place a **Simplex Repeat Controller** (the "parrot", `radiocraft:parrot_controller`) next to the far radio. It
links to the first radio on a horizontal side (a short wire is drawn towards it, `link=<side>` in the blockstate),
keeps that radio parroting with the controller's delay (2 s by default, right click cycles 1/2/3/5/10 s, saved with the
block), and its LEDs show what the radio is doing: green while it receives, red while it echoes. Breaking the controller
or the radio stops the parroting. `rcdebug parrot` still works on radios without a controller; on a radio with one, the
controller re-applies its own delay every tick.

Example: a 20m station with a dipole between two poles, built in one call, then inspected:

```bash
python tools/rcon.py "setblock 0 -60 0 radiocraft:hf_radio_20m" "setblock 0 -60 5 radiocraft:balun_one_to_one" \
  "setblock -5 -60 5 radiocraft:antenna_pole" "setblock 5 -60 5 radiocraft:antenna_pole" \
  "rcdebug wire 0 -60 5 -5 -60 5" "rcdebug wire 0 -60 5 5 -60 5" \
  "setblock 0 -60 1 radiocraft:coax_wire[north=true,south=true]" \
  "setblock 0 -60 2 radiocraft:coax_wire[north=true,south=true]" \
  "setblock 0 -60 3 radiocraft:coax_wire[north=true,south=true]" \
  "setblock 0 -60 4 radiocraft:coax_wire[north=true,south=true]" \
  "rcdebug power 0 -60 0 on" "rcdebug mode 0 -60 0 ssb" "rcdebug tune 0 -60 0 14.2"
sleep 6   # antenna update delay
python tools/rcon.py "rcdebug radio 0 -60 0"
```

Expected: `antennas=1`, `radiocraft:dipole swr=1.00`, `measuredSwr=1.00 staticFactor=1.000 syncedPowered=true`.
Wires strung to a block that is not a wire holder (air, dirt) are removed by the game after 3 s.

Build a second station the same way 20 blocks east (`x = 20`), then `rcdebug transmit 0 -60 0` and
`rcdebug radio 20 -60 0` shows `lastSignal=0.933` (1 - 20/300 for the 20m line of sight range of 300). Switch both to
`cw` and `rcdebug key` gives `0.956` (CW counts distance at 1/1.5).

## How the pieces fit (as of 2026-10)

- **Networks are in memory only.** `BENetworksCapability` is a per-level map that is never written to disk. Devices
  re-link themselves when their block entity loads: `INetworkObjectProvider#getNetworkObject` creates the object and
  calls `WireUtils.connectToWires`, which walks every wire run touching the block and merges whatever it finds onto
  one network. Placing a wire that touches two things (`WireBlock#onPlace`) merges from the wire's side; removing
  one splits. The walk normally stops at a device; a device that implements `IPassThroughNetworkObject` (the
  antenna tuner on coax via `ICoaxPassThrough`, solar panels on power) is walked through, so a radio and an antenna
  with a tuner between them share one coax network. Devices only link through wire, except where
  `BENetworkObject#linksDirectlyTo` says two touching devices join (solar panel to solar panel or to a charge
  controller, 2026-10-21). The `PassthroughBlock` is a `WireBlock` in both wire sets, so wire of either kind runs
  through it.
- **Radios persist** frequency, mode, gains, last measured SWR and the power flag in their block entity tag. On load
  the flag switches the fresh network object back on, so a radio left on stays on across restarts.
- **Power network objects tick** every server tick from `CommonEvents#onLevelTick` (only those in loaded chunks).
  Solar panel -> charge controller -> battery -> radio: batteries push into the radio's own small storage each
  tick and `RadioNetworkObject#resolvePower` consumes the receive or transmit cost from it. `wantsOn` is the
  player's switch, `isPowered` whether it runs; starved radios brown out and cycle `getDisplayPhase` (on / PWR /
  dark), retrying once a cycle. `radios_need_power = false` restores free power. The GameTest server runs with it
  false (`GameTestDefaults`, a `ServerStartedEvent` hook in the gametest source set that only fires for a
  `GameTestServer`), and `PowerGameTests` turns it on for its own batch.
- **Static volume** on the client is `NOISE_VOLUME * efficiency(SWR)`. `NOISE_VOLUME` comes from the solar event
  via `CNoisePacket` (sent on login, dimension change and whenever the event changes). A better match means louder
  band noise, which is the intended behaviour. Static is not yet ducked by a received signal.
- **Voice path** (server, voice thread): Simple Voice Chat mic packet -> `RadioBlockEntity#acceptVoicePacket` (only
  while `canTransmitVoice`: on, SSB, PTT down) -> `AntennaNetworkObject#transmitAudioPacket` ->
  `StaticAntenna#transmitAudioPacket`, which builds one `AntennaVoicePacket` per other antenna on the HF
  `AntennaNetwork` with strength `transmitEfficiency * efficiency(SWR)` -> receiving `StaticAntenna` scales by its
  receive gain -> `AntennaNetworkObject#receiveAudioPacket` (exactly one radio on the coax, frequency within 1 kHz)
  -> `RadioBlockEntity#receiveVoice`, which records the signal and plays it through `BEVoiceReceiver`. Strength
  inside line of sight is `1 - distance / losRange`; beyond that `BandUtils#getBaseStrength` uses the skip window.
- **CW path**: the client records key-down per tick in `CWSendBuffer` while the screen is open and sends
  `SCWBufferPacket`s (20 ticks each) -> `HFRadioBlockEntity#transmitMorse` (on, CW mode, screen open) ->
  `StaticAntenna#transmitCWPacket` (distance counted at 1/1.5, same SWR loss) -> `HFRadioBlockEntity#receiveCW` ->
  `CCWBufferPacket` to clients tracking the chunk -> `CWReceiveBuffer` -> `RadioMorseSoundInstance` volume per tick.
- **Last received signal**: `RadioBlockEntity#getLastSignalStrength/GameTime` remember what last arrived (voice or
  CW). This is what the transmission GameTests and `rcdebug radio` read; the static-masking work will send it to
  the client.
- **Antenna tuner**: `AntennaTunerBlockEntity` puts an `AntennaTunerNetworkObject` on the coax; `AntennaNetworkObject`
  tracks tuners like it tracks radios and answers `getRawSWR` (the antenna's own), `getSWR` (what the radio sees,
  1.0 within `antenna_tuner_max_swr`) and `getMatchEfficiency` (power reaching the antenna, `SWRHelper#tunedEfficiency`).
  `StaticAntenna` asks its network object for the match efficiency when transmitting, so the tuner changes both the
  radio's measured SWR and the strength others hear. `AntennaTunerGameTests` cover matched, out of range, removed and
  the heard strength with and without a tuner; `AntennaTunerTest` covers the maths. The tuner's screen reads
  `AntennaTunerBlockEntity#read` through menu data slots.
- **New antenna types** go in `RadiocraftAntennaTypes`. `AntennaTypes.match` tries every type in registry order, so
  match rules must be disjoint (the inverted V needs both ends below the balun, the dipole needs them level; the
  random wire needs the 9:1 balun). `AntennaGameTests` has a recognition test per type; `AntennaSwrTest` the SWR
  rule. A new feed-point block needs the three block tags in `RadiocraftBlockTagsProvider` (coax, antenna, wire
  holder) and therefore `./gradlew runData` before GameTests that connect it to coax can pass.
- **Wire links, wall hugging and climbs** (2026-10-21): the wire block state stores only the six connections, the
  hugged edge (`hug`), the climb (`climb` = none/one/both, both being a one-wide trench) and waterlogged, about 1,900
  states per wire block. Everything else about the look (the edge a run arrives on, how a run ends at a device) is
  worked out from the neighbours by `WireBlock#look`, and `WireLook#parts` picks the cable pieces; the hitbox and the
  client model (`client/model/WireModel`, a NeoForge geometry loader `radiocraft:wire` that reads the neighbours while
  the chunk is drawn) share that rule, and `WireLookTest` covers it. A look may only depend on blocks next to the
  wire, because the client only redraws neighbours of a change. History: the look used to live in the state as a
  generated multipart blockstate; at 57,000 to 100,000 states Minecraft kept about 6 GB of multipart condition objects
  and ran out of memory on world join. `WireBlock#getLinks` is still the one place that says what a wire is joined to,
  including the diagonal climb links; `WireClimbUpdater` refreshes wires around a changed block and `refresh` merges
  or splits networks when a climb comes or goes. `tools/wire_assets.py` writes the part models (both textures) and
  the single-variant blockstates; `tools/wirestate.py x y z` reads stored wire states off the dev server.

## Bug audit (2026-10)

Fixed, each with a GameTest or unit test that now runs as required:
- Battery charge was never saved: it lived only in the in-memory `BatteryNetworkObject`, so every reload emptied the
  battery. `LargeBatteryBlockEntity` now saves it and hands it to the fresh object (`PowerGameTests`, 2026-10-21).

1. Radio settings not saved - `radioSettingsSurviveSaveAndLoad`, `outOfBandFrequencyIsResetOnLoad`,
   `radioPowerStateSurvivesReload`.
2. Devices placed next to existing wire never connected - `radioFindsDipoleWhenPlacedOntoExistingCoax`,
   `radioFindsDipoleWhenAllPlacedAtOnce`.
3. Coax laid end to end never connected (and cutting coax never disconnected) -
   `radioFindsDipoleWhenCoaxLaidEndToEnd`, `radioLosesDipoleWhenCoaxIsCut`.
4. Power network objects never ticked - `solarPanelChargesBatteryInDaylight`, `solarPanelDoesNotChargeAtNight`.
5. Static factor stayed 0 on the dev server - not a mod bug: the idle server had stopped ticking block entities
   (see "Build inside x/z -48..47" above). `poweredRadioReportsStaticFactor` covers the real behaviour.
6. 40m band range was 7.3-9.0 MHz - now 7.0-7.3 MHz (`BandTest`). Existing worlds keep the old value in their
   `radiocraft-server.toml` until it is deleted or edited.
7. Static never played - `NOISE_VOLUME` is now set from `CNoisePacket` (`CNoisePacketTest` for the codec; the
   sound itself still needs a client and ears).
8. Antennas were unrecognized after a restart because wire entities load after the balun's block entity - the balun
   rechecks after the update delay and each wire asks for a recheck on its first tick.
9. CW did not work at all: the CW button never reached the server (no `SET_CW` action), the client never sent key
   input and the server never forwarded received morse (both packets had been lost in the NeoForge port) -
   `cwReachesRadioOnSameFrequency`, `cwIsNotHeardInSsbMode`, `cwNeedsPowerAndCwMode`, `CWBufferTest`,
   `CCWBufferPacketTest`.
10. A radio that was switched off (or never on) still received voice while in SSB mode -
    `unpoweredRadioHearsNothing`.
11. A broken balun's antenna stayed on the air (ghost receiver in the `AntennaNetwork`) - removed in
    `AntennaNetworkObject#discard`.
12. CW ignored antenna mismatch while voice did not - both now apply the SWR efficiency.
13. Breaking a network block (radio, balun, tuner) never told its networks: `AbstractNetworkBlock#onRemove` looked
    the object up through the block capability, which is keyed by the block at that position, and the block is
    already gone when `onRemove` runs. It now uses the level's `BENetworksCapability` directly, so the other members
    hear about the removal and the object is discarded - `removingTunerRestoresRawSwr`.

End to end, two stations talking (`TransmissionGameTests`): `voiceReachesRadioOnSameFrequency`,
`voiceWorksInBothDirections`, `radioOnAnotherFrequencyHearsNothing`, `radioInCwModeDoesNotHearVoice`,
`noAntennaMeansNoTransmission`. The parrot controller block (`ParrotControllerGameTests`): linking, delay follow,
echo timing and strength, LED states, release on removal. Redstone interference on receive
(`RedstoneInterferenceGameTests`, design in [RedstoneInterference.md](RedstoneInterference.md)): pickup zone
registration, pops by the feedline and antenna with distance fade, decay, a clock piling up, plain blocks and
unpowered radios staying silent. `/rcdebug radio <pos>` prints the live `interference=` level and the zone. Transmit
side (`TransmitInterferenceGameTests`, hit chance forced to 1): dust reads level 4 while keyed and clears after, a
lever flips and a torch flickers, a bad SWR reaches the shack with a higher level, a QRP rig reaches less. These prove
the dust mixin applied. Note for test authors: the framework puts a stone button on a command block at the north-west
corner of every plot; RF skips switches on command blocks, which is the only reason transmitting tests do not restart
the runner. Lightning crashes (`LightningStaticGameTests`, batch `lightning`): a near bolt pops the level and it dies
away, a far bolt is quieter, an unpowered radio hears nothing. Comparators (`ComparatorGameTests`,
`ParrotControllerGameTests.comparatorReadsDelayInSeconds`): the radio S-meter follows a received signal and falls back,
the solar weather station's level matches its event, the repeat controller reads its delay in seconds.

Still open:

- Radios do not consume power and cannot run out of it.
- The charge controller's on/off switch is ignored by its network object.
- Static is not masked by a received signal, and there is no squelch.
- VHF (FM) radios use the same static model as HF.
