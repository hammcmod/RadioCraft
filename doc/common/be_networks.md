# Block-Entity Networks (be_networks)

This package contains helpers and network-object implementations that let block entities participate in in-world networks (power and coax). It complements the public API under `com.arrl.radiocraft.api.benetworks` by providing concrete game logic for RadioCraft blocks.

Key concepts referenced below:
- `BENetworkObject`/`PowerNetworkObject` (API): A node that can join one or more `BENetwork`s and react to membership changes.
- `BENetwork`/`PowerBENetwork` (API): A network grouping of `BENetworkObject`s, e.g., a power bus or coax run.
- Wire traversal: Wires are `WireBlock`s that connect adjacent nodes; utilities here discover and manage connected components.

Files in this package

- `ICoaxNetworkObject.java`
  - Marker interface indicating the object attaches to coax networks (e.g., radios, antennas). Used to filter connectivity via wire utilities and registries.

- `WireUtils.java`
  - Utilities to traverse wire graphs and manage `BENetwork` membership around a wire change.
  - `mergeNetworks(...)`: Starting at a wire, finds all connected `BENetworkObject`s matching a predicate and merges their networks into one, creating a fallback network if none exist.
  - `splitNetworks(...)`: From a wire position, discovers connections on each side and splits them into separate networks, replacing the old network(s) with new ones per side.
  - `tryConnect(...)`: Attempts to connect a `BENetworkObject` at a position to any adjacent wire’s network, creating and registering a network as needed.
  - `getConnections(...)` / `getFirstConnection(...)`: DFS over wires to find attached network objects and the side (`Direction`) they connect on.

Network objects (`network_objects`)

- `AntennaNetworkObject.java`
  - Type id: `radiocraft:antenna`. Implements `ICoaxNetworkObject` and extends `BENetworkObject`.
  - Holds a single `StaticAntenna<?>` plus a synchronized list of connected `RadioNetworkObject`s discovered via coax networks.
  - Routes received audio/CW packets to exactly one attached radio tuned to the correct frequency; if more than one radio is attached, calls `overdraw(...)` to notify radios of an invalid multi-load state.
  - Persists antenna type/data and the antenna-network id used by `AntennaNetworkManager`.
  - Reacts to network membership changes to keep its `radios` list in sync for the coax network type.
  - Provides `getSWR(int wavelength)` derived from the underlying `StaticAntenna` (returns high SWR when multiple radios are attached).

- `RadioNetworkObject.java`
  - Type id: `radiocraft:radio`. Implements `ICoaxNetworkObject` and extends `PowerNetworkObject`.
  - Tracks power state (`isPowered`), transmit state, and per-tick power use for receive/transmit.
  - `tick(...)`: Consumes energy each tick while powered, with higher cost when transmitting.
  - Maintains a synchronized list of connected `AntennaNetworkObject`s by observing membership of the coax network.
  - Exposes `canPowerOn()` via a simulated power draw to check if the radio can start.

- `RepeaterNetworkObject.java`
  - Extends `RadioNetworkObject` for repeaters. Contains documentation noting it handles receive/repeat while the BE may be unloaded; specific repeating logic lives at the network-object level rather than the BE.

- `BatteryNetworkObject.java`
  - Type id: `radiocraft:battery`. Extends `PowerNetworkObject`.
  - Acts as an indirect consumer only; on `tick(...)` it pushes energy outward to attached `PowerBENetwork`s up to its extract rate and stored energy.

- `ChargeControllerNetworkObject.java`
  - Type id: `radiocraft:charge_controller`. Extends `PowerNetworkObject`.
  - Direct consumer on the power bus with enable flag and last-tick power metrics. On `tick(...)` pushes energy to connected networks prioritizing direct-consume targets.
  - Persists `isEnabled` and reports `getLastPowerTick()` for UI.

- `SolarPanelNetworkObject.java`
  - Type id: `radiocraft:solar_panel`. Extends `PowerNetworkObject`.
  - Producer-only node. On daytime ticks with sky access, generates power scaled by rain and pushes into attached `PowerBENetwork`s; records `lastPowerTick` for UI.
  - Persists `canSeeSky` state for resume.

Typical flow

- When a wire is placed/removed, `WireUtils.mergeNetworks` or `WireUtils.splitNetworks` updates connected components so all attached network objects share the correct `BENetwork` instance(s).
- Power objects (`SolarPanelNetworkObject`, `ChargeControllerNetworkObject`, `BatteryNetworkObject`, `RadioNetworkObject`) implement `tick(...)` to generate/consume/forward energy via `PowerBENetwork.pushPower(...)` or their own storage.
- Coax objects (`RadioNetworkObject`, `AntennaNetworkObject`) listen for membership changes on the coax network, maintaining cross-references for routing radio/antenna traffic.

Related APIs

- See `src/main/java/com/arrl/radiocraft/api/benetworks` for abstract network and node types used here.
- See `src/main/java/com/arrl/radiocraft/common/blocks/WireBlock.java` for how wire connectivity is defined and queried by `WireUtils`.

How BENetworks Work (Deeper Dive)

- Network identity and type
  - Each `BENetwork` has a UUID and a type id (e.g., `BENetwork.COAXIAL_TYPE`, power type). The type controls which objects are meaningful members and how transfers happen (e.g., power vs. signal).
  - Network instances live in the per-level `IBENetworks` capability, which owns lifecycle (add/remove) and lookup for objects and networks.

- Network objects and sides
  - A `BENetworkObject` can connect on multiple sides (`Direction`) and even to multiple network types simultaneously (e.g., a `RadioNetworkObject` connects to power and coax).
  - Each object stores side→network mappings and exposes `getNetwork(Direction)` and `setNetwork(Direction, BENetwork)` used by `WireUtils` when wiring changes.

- Membership callbacks
  - `BENetworkObject` receives events so objects can react without scanning neighbors:
    - `onNetworkAdd(BENetwork)`: Invoked after the object is added to a network; objects usually enumerate `network.getNetworkObjects()` to build local caches (e.g., radios collecting antennas).
    - `onNetworkRemove(BENetwork)`: Invoked before/after detaching to clear caches.
    - `onNetworkUpdateAdd/Remove(BENetwork, BENetworkObject)`: Fired when other objects join/leave the same network; used to keep cross-references synchronized.

- Forming networks via wires
  - Wires are the graph edges. `WireUtils.getConnections(...)` performs a DFS flood through `WireBlock` connections and collects reachable `BENetworkObject`s that match a predicate (e.g., “is `ICoaxNetworkObject`”). The result maps each found object to the `Direction` it faces back toward the wire.
  - `mergeNetworks(...)` inspects all found objects, collects their existing networks (if any), merges them into a single `BENetwork.merge(...)`, and attaches any previously un-networked objects to the merged instance. When no networks exist, it uses `fallbackSupplier` to create and register a new one in `IBENetworks`.
  - `splitNetworks(...)` is used when removing or isolating a wire. It computes the set of reachable objects per side of the broken wire and, for each side, instantiates a fresh network (via `BENetworkRegistry.createNetwork(type, UUID, level)`), reattaching those objects and updating the capability registry.

- Power flow specifics
  - `PowerNetworkObject` exposes an internal energy storage with `maxReceive`/`maxExtract` and helper `tryConsumeEnergy(amount, simulate)`.
  - Power is pushed “outward” across the network each tick by producers/buffers:
    - `SolarPanelNetworkObject.tick` computes generation and calls `PowerBENetwork.pushPower(amount, allowIndirect, directOnly, simulate)` to distribute to consumers.
    - `BatteryNetworkObject.tick` and `ChargeControllerNetworkObject.tick` push up to their extract limit, tracking what moved this tick for UI.
    - `RadioNetworkObject.tick` consumes from its attached power network using `tryConsumeEnergy` based on whether it is receiving or transmitting.

- Coax/signal flow specifics
  - Objects that should connect to coax implement `ICoaxNetworkObject`. This is used as a predicate in `WireUtils` and by registries to segregate coax networks from power networks even when wires are adjacent.
  - `RadioNetworkObject` and `AntennaNetworkObject` mirror each other’s membership callbacks to maintain synchronized, thread-safe lists (`antennas` in radio, `radios` in antenna).
  - Antenna objects use `AntennaNetworkManager` and `AntennaNetwork` (separate from block-entity networks) to handle RF propagation; the BE network is the physical coax run linking radios and antennas.

- Persistence and save/load
  - Every network object implements `save(CompoundTag)`/`load(IBENetworks, CompoundTag)` to persist custom fields in addition to base network data. Examples: radio power/transmit state, charge controller `isEnabled`, solar panel `canSeeSky`, antenna type and serialized antenna data.
  - During load, objects restore local state and re-associate with the correct `BENetwork`s via the capability.

- Typical wire lifecycle
  - Place wire: `mergeNetworks` runs; objects on both sides end up in the same network; membership callbacks hydrate caches (e.g., radios discover antennas).
  - Break wire: `splitNetworks` runs; per-side subgraphs are assigned fresh networks; membership callbacks prune caches so signal/power no longer cross the break.

