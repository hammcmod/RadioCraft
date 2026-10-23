# TNC rework and solar weather: plan

Planning notes from 2026-10-23, updated with the maintainer's answers the same day. Steps 1 and 2 are built (see
"Status"); the rest is still a plan.

## Status (2026-10-23)

- **Built:** the station bus (`StationBus`), the TNC renamed the **Radio Interface** (`radio_interface`: callsign and
  TX switch, digipeating), the APRS Interface as the **H19 APRS Terminal** (`aprs_h19`, the maintainer's model; its
  screen shows READY, CONTACT or MSG) and the **Digital Transfer Interface** (`transfer_interface`, placeholder box).
  The H19 and Transfer screens are the old tabs until the maintainer redesigns them.
- **Changed from the plan below:** the Radio Interface's only UI is a callsign box and a TX on/off switch. Digipeating
  and the beacon settings are set from the H19, the way a real terminal configures its TNC. The H19's CONTACT light
  goes out when it is opened or after `aprs_contact_lamp_minutes`; MSG only when opened. There is no claim button: an
  empty callsign box claims your own.
- **Not yet:** RTTY (until the Teletype), steps 3 to 7, recipes for the H19 and Transfer Interface.

## Where things stand

`digital_interface` (the "TNC", dev-only tab) is one block that does everything: it links to every radio touching it,
holds the callsign, digipeats, and has four tabs (APRS, MSG, RTTY, FILES). `solar_weather_station` is a teletype-looking
block that prints the solar conditions and drives a comparator, as long as a radio with a real antenna is next to it.

Breaking worlds is fine for this rework (maintainer, 2026-10-23). A build went out, but no one really plays it.

## Blocks

| Block | Real-world idea | Connects to | Callsign | Does |
|---|---|---|---|---|
| **TNC** (now the Radio Interface) | The packet modem (KPC-3, TNC-2) | Radios it touches | Yes (MYCALL) | Turns APRS and file packets into tones and back; digipeats |
| **APRS Interface** (now the H19 APRS Terminal) | A terminal or tracker display | A TNC, directly or through other interfaces | Uses the TNC's | Beacons, the map, messages, unread lamp, buzzers, comparator |
| **Digital Transfer Interface** | A file and SSTV terminal | A TNC, directly or through other interfaces | Uses the TNC's | Sends and prints books, paper and maps |
| **Teletype** | A Model 15/28 teleprinter with its own terminal unit | Radios it touches, no TNC | See question 1 | RTTY typing and printing; SOLAR WX and WX buttons |
| **Ionospheric Processor** | The ionosonde's electronics rack | Coax to the Ionosonde Antenna; radios it touches | Yes | The only source of solar data; answers requests and sends bulletins |
| **Ionosonde Antenna** | The sounding antenna | Coax to the processor | — | A single block antenna; without it the processor knows nothing |

Today's `solar_weather_station` becomes the **Teletype** (its model already has the paper roll). The id can change,
since worlds may break.

### The station bus (APRS and Transfer interfaces)

- **Connecting:** an APRS or Transfer interface works if it touches a TNC, or touches another interface that does,
  through any number of hops. For example: radio, TNC, APRS interface, transfer interface, in a row.
- **Finding the TNC:** each interface searches outward through touching TNCs and interfaces, capped at about 16
  blocks. It searches again when a neighbouring block changes and remembers the result.
- **Two TNCs on one bus:** both transmit; packets come back once each, and the `seen` filter on each interface drops
  the duplicates. That's the same behaviour as a TNC touching two radios today, so a two-radio digipeater stays easy.
- **No TNC:** the interface still opens (log, map, typing) but can't transmit; a "NO TNC" lamp says why.
- **Callsign:** on the TNC. The interfaces show it read-only and send as it. Claiming moves to the TNC.

The Teletype and the Ionospheric Processor are not on the bus: each talks to the radios it touches, the way the TNC
does.

### Code split

- **TNC keeps:** `transmit`, the radio side of `receive`, digipeating, `seen`, the callsign and claiming.
  `RadioBlockEntity#getDigitalInterfaces` becomes "TNCs, teletypes and processors touching me".
- **Each terminal keeps its own packets:** APRS takes BEACON and MESSAGE, Transfer takes FILE and FILE_HEADER, and the
  Teletype and processor take RTTY and the solar packets.
- **Unchanged:** `TncPacket` and the on-air side (`AntennaDataPacket`, decoding, SNR, RTTY garbling).

## Solar weather

### Ionospheric Processor and Ionosonde Antenna

- **The antenna:** a new antenna type, one block, joined to the processor by coax like any other antenna. The processor
  only knows the solar weather while it has its ionosonde antenna on its coax. That replaces today's "radio with a real
  antenna next to the station" rule.
- **What the processor shows:** the comparator output (1 blackout to 6 great), a right click summary and Jade. The full
  printed report needs a Teletype.
- **Callsign:** it has its own, since it doesn't use a TNC. Bulletins and replies go out under it.
- **On the air:** a radio touching the processor lets it answer requests and send bulletins.

### Getting a report

- **Local:** a Teletype that shares a radio with the processor (both touching the same radio), or touches the processor
  directly, prints the report instantly with SOLAR WX. Nothing goes on the air.
- **Over the air:** a Teletype elsewhere presses SOLAR WX and sends a short request (a new `TncPacket.Kind`, QUERY,
  with a topic: SOLAR or WX). Any processor that hears it, on a powered radio, answers with an RTTY bulletin under its
  callsign. Everyone on that frequency sees the bulletin print.
- **No answer:** the Teletype times out after about 10 seconds and prints `NO ANSWER`. It can request once every 30
  seconds; a processor answers at most once every few seconds, and one answer serves every station listening.

### Bulletins

- **Timed bulletins:** off by default. A setting on the processor turns them on, every N minutes.
- **Blackout warning:** always on. Shortly before a blackout starts, the processor sends a warning bulletin, such as
  "SOLAR ALERT: RADIO BLACKOUT EXPECTED. HF CLOSING".
- **Code this needs:** today `SolarEventManager` picks the next event only at the moment the current one ends, so
  nothing can see a blackout coming. It needs to choose the next event a few minutes ahead and expose it, for example
  `getUpcoming(level)` with the event and its start time. The aurora already does this (`Aurora.isExpected`, the
  forecast windows). This lead time also lets the processor's own report say "blackout expected".

### WX button (weather)

- **When it shows:** WX is a separate button from SOLAR WX, shown only when a compatible weather mod is installed. It
  requests a weather report the same way (local, or QUERY WX over the air).
- **Where the data comes from:** each supported mod gets a small adapter behind one interface, for example
  `WeatherSource` with `isAvailable()` and `report(level, pos)`. The Teletype shows WX if any source is available.
- **Mods to look at:** Weather, Storms & Tornadoes (weather2, by Corosus, which has real storm fronts) and Serene
  Seasons (seasons and temperature). Both need checking for 1.21.1 NeoForge builds before choosing.
- **Vanilla is forecastable too:** the server knows how long until it rains or thunders (`ServerLevelData`'s
  rain and thunder timers). A vanilla adapter could print "Rain expected in about 4 minutes". It's worth deciding
  whether WX shows without a weather mod, using this (question 2).
- **Your own weather mod:** if no mod fits, the same `WeatherSource` interface is where a RadioCraft weather system
  would plug in.
- **WEFAX:** a WEFAX receiver later prints weather charts as maps, using the same source. It's on the todo list.

## Recipes (first drafts, to review like the others)

| Block | Draft | Idea |
|---|---|---|
| TNC | `IRI / #BP / III`: B HF circuit board, P comparator, R redstone | A modem: board, logic, iron case |
| APRS Interface | `IGI / MBC / III`: G glass pane (screen), M map, C compass, B HF circuit board | Map display |
| Digital Transfer Interface | `IGI / #BH / IKI`: H chest, K hopper | Storage and a feed path for supplies |
| Teletype | `ISI / BLB / PPP`: S paper, L lever, B buttons, P planks | Keyboard and paper; its built-in terminal unit makes it more than a terminal |
| Ionospheric Processor | `IGI / DBD / I#I`: G glass, D diamond, B all-band circuit board | The expensive part |
| Ionosonde Antenna | `L.L / LCL / .O.`: L lightning rod, C copper block, O coax | A sounding antenna |

The digital interface's current recipe (diamond, desk microphone) goes away with the old block.

## What you'll be designing

- **TNC:** a small box. Lamps for PWR, DCD (carrier heard), TX, RX and RPT (digipeat). A small screen with the
  callsign field, claim button and digipeat toggle.
- **APRS Interface:** today's APRS and MSG tabs: map, station list, beacon settings, the message terminal. Mostly
  existing art.
- **Digital Transfer Interface:** today's FILES tab: outbox, 4 received slots, book and map supplies, TO field, SEND,
  6 progress LEDs. The inventory panel art is already a bounty item.
- **Teletype:** a paper-roll display, typing line, paper slot, SOLAR WX button, WX button (hidden without a weather
  source), and a status line (NO RADIO, NO ANSWER, waiting). The block model can stay today's station model.
- **Ionospheric Processor:** a rack-style block. Its UI could be a callsign field, a "timed bulletins every N min"
  setting and a readout, or just Jade and a right click summary.
- **Ionosonde Antenna:** a single-block antenna model.

## Build order

1. **TNC and the station bus:** the bus search, the TNC block, the callsign and digipeat moved to it. GameTests:
   chains, no TNC, two TNCs, an interface broken out of the middle of a chain.
2. **APRS and Transfer interfaces:** each takes its share of today's block entity and screen. Port the TNC GameTests.
3. **Teletype:** RTTY straight through a radio, the paper and printing, and the old station's sound and bell.
4. **Ionosonde and processor:** the antenna type, the processor, the local SOLAR WX report and the comparator.
5. **Over the air:** the QUERY packet, answers, the solar event look-ahead, the blackout warning, timed bulletins.
6. **WX:** the `WeatherSource` interface, the vanilla adapter if wanted, then a first weather mod adapter.
7. **Recipes, tab placement and NeedsTesting.**

Each step can ship on its own.

## Decisions (maintainer, 2026-10-23)

- Four communication blocks: TNC, APRS Interface, Digital Transfer Interface, Teletype.
- The Teletype connects straight to a radio, like real RTTY with a terminal unit; no TNC.
- The callsign lives on the TNC and on the Ionospheric Processor.
- The observatory is two blocks: an Ionospheric Processor (radio-like) joined by coax to an Ionosonde Antenna (one
  block). A radio touching the processor lets other teletypes request and hear the solar weather.
- Timed bulletins are off by default; a blackout warning always goes out shortly before a blackout.
- Breaking worlds is fine for this rework.
- WX is separate from SOLAR WX and only shows with a compatible weather source. A WEFAX receiver comes later; a
  RadioCraft weather mod may be needed.

## More decisions (maintainer, 2026-10-23)

- The Teletype has its own callsign field.
- WX works in vanilla from the rain and thunder timers; weather mods add more when installed.
- The blackout warning goes out 3 minutes ahead, set by a server config.
- A local report only needs the Teletype on the same radio as the processor.
- Wording: automatic bulletins (timed and blackout warnings) are written as forecasts ("expected", "next few minutes");
  answers to a request describe current conditions.
