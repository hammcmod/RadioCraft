# RadioCraft alpha 1.10.0

Everything since 1.9.2, including 1.9.3's config fix (1.9.3 was never published). Minecraft 1.21.1, NeoForge. Needs
Simple Voice Chat; [Another Weather Mod](https://github.com/RadiocraftARRL/AnotherWeatherMod) 1.10.0 is optional and
adds live storms, tornado watches and day outlooks to the weather reports and WEFAX charts.

## Changes that affect existing worlds

- **Every telephone needs a Phone PABX now.** A microwave radio is no longer a phone exchange on its own: phones that
  were wired straight to a microwave radio say NO LINE until a powered PABX joins them (see Telephones below).
- **Plain wire only carries power.** Teletypes, faxes, WEFAX receivers, radio interfaces, the tape player, the
  Ionospheric Processor, wired speakers and microwave stations that do not touch their radio now link over the new
  data wire. Rewire them with data wire.

## Telephones

- **Phone PABX**, the exchange every phone needs: phones touching it or on its data wire call each other by
  extension. It needs power (`pabx_power_use`, 2 a tick) and its lamp shows green when it has it.
- Right click a PABX to set its **master number**, its **first extension** (its phones are numbered from it, 100 by
  default) and whether it is **behind another PABX**.
- PABXes on a microwave link (touching a radio, or on its data wire) are dialed by master number then extension: with
  master number 602, extension 100 is `602100`. A PABX behind another works like one of its lines, its number dialed
  before the extension, so you can build a whole phone network.
- The phone shows its full number and who is calling; the call timer now counts (it stuck at 0:00).
- Telephones have their own guide chapter, with a tip on Simple Voice Chat: with push-to-talk, hold its key while you
  speak on a phone; open mic never keys a radio.

## Wiring and antennas

- **Data wire**: a light grey wire for the station's data and audio lines (see above).
- **Antenna switch**: one radio to one of three antennas. Its input turns to the radio's coax by itself; a knob picks
  the output.
- **Receive-only antenna splitter**: every radio behind it hears the antenna, a little weaker; none can transmit
  through it. Recipe: four coax round an iron ingot.
- A radio on a dummy load plays only a faint hiss.

## Weather

- **WEFAX alarms**: the receiver gives a redstone signal, 1 for a message, 2 for a severe thunderstorm, 3 for a
  tornado watch and 4 for a hurricane (the strongest alert wins), and beeps.
- Tornado watches and today's and tomorrow's outlook from Another Weather Mod on the WEFAX and in weather reports.
- Lightning breaks the handhelds and manpacks a struck player carries (`lightning_breaks_carried_radios`), and a
  manpack hurts more (`lightning_manpack_extra_damage`).

## Sound

- Two stations on one frequency now mix: SSB adds them, FM captures the stronger and flips between them when they are
  close. Received levels glide instead of jumping (`voice_level_smoothing`).
- The redstone interference noise no longer goes off near Another Weather Mod's weather instruments in a storm (their
  readings are not switching), follows the radio's volume knob, and fades with distance like the static. The
  teletype and WEFAX chatter fade with distance too.

## Fixes and looks

- `radiocraft-server.toml` no longer corrects itself on every load, and edited band frequencies stay
  ([issue #102](https://github.com/RadiocraftARRL/RadioCraft/issues/102)).
- The radio fax's case uses its wood's planks.
- The HF manpack screen no longer overlaps; its battery reading sits at the right of the window.
- Breaking a WEFAX receiver (or a lit charge controller) shows its own particles, not the missing-texture ones.
- The antenna switch knob no longer flickers.
- Guide builds face their gear toward you, draw their antenna wires, and gain a microwave site.
