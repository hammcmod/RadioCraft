# RadioCraft alpha 1.9.2

A fix for 1.9.1, which could not start at all: the game stopped with `Failed to load mod file
META-INF/jarjar/sable-companion-common-1.21.1-1.6.0.jar`. The build step that fixed 1.9.0's JOrbis crash also unpacked
the bundled Sable Companion library, so the jar named a file it no longer had. The library is bundled intact again.
Sable stays optional; RadioCraft starts without it. Every release is now launched on a real NeoForge server and client
before it is published. Nothing else changed.

# RadioCraft alpha 1.9.1

A fix for 1.9.0, which could not start on a client ([issue #101](https://github.com/hammcmod/RadioCraft/issues/101)):
the game closed at once with `Module neoforge.coremods reads more than one module named jorbis`. The tape decoder's
JOrbis library was packed as a second copy of a module the Minecraft client already ships. It is now shaded into the
mod under its own package, so there is only one. Dedicated servers were not affected. Nothing else changed.

# RadioCraft alpha 1.9.0

Everything since the 1.2.0 release (commit 1476893, 2026-10-21). Minecraft 1.21.1, NeoForge. Needs Simple Voice
Chat; [Another Weather Mod](https://github.com/ITCMD/AnotherWeatherMod) 1.9.0 is optional and adds live storms to the
weather reports and WEFAX charts.

## Radios

- Each radio's frequency, mode and S-meter show on its model's screen (hidable in the client config), and the gain
  and mic gain dials work.
- The desk microphone works: hold to talk, sneak-click to lock, quieter with distance, with a timeout. Sneak-right-
  click a radio to take its hand mic on a cord; walk away and it snaps back.
- CW from the keyboard (Left Alt, rebindable) and from Chat Morse.
- Desk speakers, the wall speaker and the loudspeaker play the radio's audio; floor cables are drawn between radios
  and the accessories beside them.
- The HF manpack, worn on the back: a QRP whip, SSB and CW. The VHF handheld goes on the Curios belt with a talk key
  and drains its battery over days.
- FM receivers hiss under a weak signal and are silent otherwise.
- A bad SWR blows a radio's transmitter and poor-crystal radios can explode.
- Callsigns: choose your own real-format ham callsign, or let the game pick one, the first time you reach for a
  radio (`/callsign claim`). Repeaters get X#XX callsigns by distance from spawn. Operator moderation with
  `/radio moderateall` and `/radio moderate <player>`.

## Antennas and matching

- New antennas: delta loop, Moxon, the Hustler 5-BTV multiband vertical, the VHF Ringo, the 2-bay folded dipole
  (found at sites) and the ionosonde antenna.
- The antenna tuner is a real manual tuner with three dials and a tuning lamp. The new automatic tuner tunes itself
  on a band change and gives up with TUNE FAIL past its range.
- The duplexer's six cavity screws tune its TX and RX cavities; a badly tuned duplexer desenses its repeater. Coax
  behind a repeater runs up into its duplexer, and tuners join a radio by touching.
- Lightning: antennas draw strikes, lightning rods work as VHF antennas, and a strike blows the radios on an
  antenna, burns its coax and can light the batteries.
- `/bandcalculate <from> <to>` lists every band between two points, counting terrain losses.

## Propagation and solar weather

- Terrain-aware propagation: hills block line of sight, cliffs cost HF, valley walls cut low-angle skip, handhelds
  are weaker indoors. The terrain map is saved with the world and refreshed as chunks change.
- Sporadic E opens VHF for long range in good conditions, 10m has a long single-hop skip zone, and the aurora can
  appear at any Z.
- Propagation scaling for large servers: receivers out of any possible reach are skipped with one distance check,
  and fixed stations keep their terrain result until a chunk on the path changes.
- The solar weather station became the Teletype Machine: a 400-line roll that prints the solar report and RTTY at
  6 characters a second with real teletype sounds, collisions and per-line moderation. The Ionosonde and the
  Ionospheric Processor measure conditions for it.

## Power

- The large battery takes and gives Forge Energy, so Create water wheels and other mods' generators charge it; with
  Create installed, RadioCraft adds an alternator. Power is balanced around a large water wheel.
- The charge controller switch works; the repeater, the H19 terminal and the automatic tuner draw power.
- Power wire is now thin and comes in sixteen colours; coax is dyeable too. Wire bundles carry up to seven cables
  in one block, keeping their order round bends and up tower legs. The Wire Junction joins power wire of any colour.
- Hazards: a closed loop of power wire shorts and burns through, wet plain wire electrocutes, an open end in the
  rain sparks, and large batteries catch fire from flame, lava, a flint and steel or lightning. Four or more
  burning together explode. Server config `Battery Fires`.

## Repeaters, digital modes and stations

- The repeater sleeps after an idle interval and wakes on use. The parrot controller links a second radio with a
  sneak click.
- The TNC became three blocks on a station bus: the Radio Interface, the H19 APRS Terminal and the Radio Fax.
- APRS maps drawn like an old terminal (black land, contour lines, dithered water), callsigns in a pixel font, and
  framable 1x1, 2x2 and 3x3 map walls from the H19.
- The Radio Fax (replacing the Digital Transfer Interface) sends books, paper and maps over the air. The WEFAX
  receiver prints weather charts onto maps and holds the sheet in its tray.
- Weather: a weather API with text and APRS weather reports and WEFAX radar and forecast sheets. With Another
  Weather Mod, hurricanes blow antennas down and storm kinds, categories and tornado ratings are reported. The
  weather station links to H19s within 64 blocks and makes their beacons APRS WX beacons.
- Microwave links: microwave radios with unique IDs, dishes and the 2x2x4 horn, an RSSI and ALIGN page, signal
  grades, and radios on one power wire forming a data network so links route through sites.
- Direction finding: the DF-1 terminal with 3 to 8 antenna modules, and the handheld finder.
- Commercial gear: the commercial repeater rack, the commercial radio module (broken and working, found at sites)
  and the tapes it plays on the air.

## World

- Radio sites generate in normal worlds: a repeater site and a Long Lines microwave site on high ground, an
  ionospheric observatory, a weather observatory, a DF station and a buried bunker, each with working stations and
  loot. A third of the observatories and DF stations carry a commercial radio module that is on the air whether its
  chunk is loaded or not, so you can tune it in from far away before you find it.
- Tower beams in three thicknesses that join in 26 directions and bear on the blocks around them, tower footholds
  (and an old, breakable kind), and the Tower Beacon with its controller for a blinking red tower light.
- Crystals: raw 80/40/20/10m and VHF crystals in pure, good and poor quality turn up when digging; encased crystals
  set a radio's quality. A full recipe rework with recipes for every radio, antenna and tuner, the 5-BTV, the H19,
  the ionosonde, the processor and the WEFAX receiver, and block drops for everything.
- A Patchouli guide book covering radios, antennas, power, propagation, digital modes, repeaters, fire and safety
  and solar weather, given on first join (`/radio guide`). Every station it draws is powered and checked by a test.
- The finished power, repeater, APRS and station gear moved into the release creative tab, with the maintainer's
  models for the 5-BTV, ionosonde, processor, WEFAX receiver, desk mic, speakers, direction finder, weather
  station, interfaces, station furniture and tower beams.
- Sable support: radios, antennas and station gear work on Sable ships.

## Fixes

- A hang when a site with wires generated: wires and bundles no longer answer the light engine with their shape.
- Models that failed to load, J-pole and duplexer z-fighting, the duplexer floating above its repeater, the
  microwave radio model, loudspeaker ring frames and several textures.
- Microwave radios on a wire bundled with coax now power on; bundle strands no longer cross round bends; footholds
  stack with nothing behind them.
- Bundled Another Weather Mod fixes: a wind particle crash, Sodium cloud compatibility, rain direction, and clouds
  that did not build overhead as a storm moved in.
