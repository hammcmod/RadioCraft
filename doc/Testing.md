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
| `rcdebug solar` | current solar event, noise, progress, time of day, weather |

Stand-ins for player actions (these change the world):

| Command | Does |
|---|---|
| `rcdebug wire <from> <to>` | strings an antenna wire, like the antenna wire item |
| `rcdebug power <pos> on\|off` | radio power switch |
| `rcdebug tune <pos> <MHz>` | sets frequency (clamped to the band) |
| `rcdebug mode <pos> ssb\|cw\|off` | mode buttons |

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

## How the pieces fit (as of 2026-10)

- **Networks are in memory only.** `BENetworksCapability` is a per-level map that is never written to disk. Devices
  re-link themselves when their block entity loads: `INetworkObjectProvider#getNetworkObject` creates the object and
  calls `WireUtils.connectToWires`, which walks every wire run touching the block and merges whatever it finds onto
  one network. Placing a wire that touches two things (`WireBlock#onPlace`) merges from the wire's side; removing
  one splits.
- **Radios persist** frequency, mode, gains, last measured SWR and the power flag in their block entity tag. On load
  the flag switches the fresh network object back on, so a radio left on stays on across restarts.
- **Power network objects tick** every server tick from `CommonEvents#onLevelTick` (only those in loaded chunks).
  Solar panel -> charge controller -> battery works; radios do not yet draw power (`RadioNetworkObject#tick` is an
  MVP stub and `canPowerOn` is always true).
- **Static volume** on the client is `NOISE_VOLUME * efficiency(SWR)`. `NOISE_VOLUME` comes from the solar event
  via `CNoisePacket` (sent on login, dimension change and whenever the event changes). A better match means louder
  band noise, which is the intended behaviour. Static is not yet ducked by a received signal.

## Bug audit (2026-10)

Fixed, each with a GameTest or unit test that now runs as required:

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

Still open:

- Radios do not consume power and cannot run out of it.
- The charge controller's on/off switch is ignored by its network object.
- Static is not masked by a received signal, and there is no squelch.
- VHF (FM) radios use the same static model as HF.
