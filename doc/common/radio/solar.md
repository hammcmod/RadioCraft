# Solar Event System

This package implements a data‑driven "space weather" system that periodically sets a solar event per dimension. Solar events expose a noise factor that other radio systems can query to influence propagation, interference, or gameplay tuning.

## Files

- `src/main/java/com/arrl/radiocraft/common/radio/solar/SolarEvent.java`
  - Immutable definition of a solar event: `noise`, `minDuration`, `maxDuration`.
  - `getInstance()` creates a runtime `SolarEventInstance` with a randomized duration in `[minDuration, maxDuration]` using `Radiocraft.RANDOM`.
  - `SolarEventInstance` tracks `ticks`, exposes `isFinished()`, and advances with `tick()`.

- `src/main/java/com/arrl/radiocraft/common/radio/solar/SolarEventManager.java`
  - Global manager keyed by `ResourceKey<Level>` that holds the current `SolarEventInstance` per dimension.
  - NeoForge event subscribers:
    - `LevelTickEvent.Pre`: server‑side tick advances the current event, and when finished (or missing) selects a new one from data via `RadiocraftData.SOLAR_EVENTS.getWeightedRandom().getInstance()`. Logs dimension and noise for visibility.
    - `PlayerEvent.PlayerChangedDimensionEvent` and `PlayerEvent.PlayerLoggedInEvent`: hooks reserved for syncing noise to clients (packets commented out for now).
  - Public API: `setEvent(...)`, `getEvent(...)` overloads for `Level` and `ResourceKey<Level>`.

- `src/main/java/com/arrl/radiocraft/common/radio/solar/SolarEventReloadListener.java`
  - Extends `SimpleJsonResourceReloadListener` to load solar events from JSON under a provided resource directory (e.g., `radiocraft:solar_events`).
  - JSON schema per entry:
    - `noise` (float): multiplier or factor applied to radio systems.
    - `minDuration` (int), `maxDuration` (int): event lifetime in ticks.
    - `weight` (int): selection weight for random picking.
  - Caches events as `Map<ResourceLocation, Pair<SolarEvent,Integer>>` and maintains `totalWeight` for efficient weighted random selection.
  - `getWeightedRandom()` returns a random `SolarEvent` by weight; logs an error and returns a default `SolarEvent(0.3F, 1, 1)` if none loaded.

## Runtime Flow

- On server level tick, the manager ensures a current event exists for the dimension, ticks it each tick, and rolls a new event when the previous one finishes.
- Player join/dimension change hooks are in place for future client sync of the current noise value.

## Integration Notes

- Consumers should read `SolarEventManager.getEvent(level)` and then `getEvent().getNoise()` to factor solar noise into propagation or interference models.
- Ensure the `SolarEventReloadListener` is registered with the desired directory so data packs can define events. Example directory: `assets/radiocraft/solar_events`.
- Weights should sum to a reasonable total; extreme weights may starve other events.
- Durations are ticks; convert as needed for UI (20 ticks = 1 second).

## Example JSON

```json
{
  "noise": 0.6,
  "minDuration": 6000,
  "maxDuration": 12000,
  "weight": 3
}
```

This defines a moderately noisy event lasting 5–10 minutes at 20 TPS, selected with weight 3 relative to peers.
