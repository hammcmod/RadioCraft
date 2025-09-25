# Morse (CW) Buffering

This package implements a simple, tick-based buffering pipeline for Morse/CW (continuous wave) signal capture, transmission, and playback within RadioCraft. It models CW as a fixed-size boolean buffer where each entry represents one tick: `true` means carrier/on (tone present), `false` means off (silence).

## Classes

- `CWBuffer`
  - Immutable container for a single fixed-length slice of CW data.
  - Fields
    - `BUFFER_LENGTH` (int): Fixed buffer length (20 ticks).
    - `id` (int): Monotonic identifier used to order buffers in a stream.
    - `inputs` (boolean[]): Backing array of length `BUFFER_LENGTH`. If constructed with a shorter/longer array, contents are copied and padded/truncated to length 20.
  - Methods
    - `getId()`: Returns buffer identifier.
    - `getInputs()`: Returns the underlying boolean array (length 20).
    - `readNext()`: Returns the next boolean value and advances an internal read cursor.
    - `isFinished()`: True when the read cursor has consumed all entries.

- `CWSendBuffer`
  - Accumulates live input into fixed-size `CWBuffer` chunks for network transmission.
  - Context
    - Scoped to a world location via `dimension` (`ResourceKey<Level>`) and `pos` (`BlockPos`).
  - Operation
    - Maintains a `partialBuffer` (size 20), `currentIndex` write cursor, `currentId` sequence number, and `ticksSinceInput` inactivity timer.
    - `setShouldAccumulate()`: Mark the current tick as an “on” tick and reset inactivity timer. Call this when keying/receiving an input pulse during the current game tick.
    - `tick()`: On each game tick, appends the current tick’s state (`accumulateInput`) to `partialBuffer`, then clears `accumulateInput` and increments timers.
      - When `partialBuffer` fills, it is ready to be packaged as a `CWBuffer(id++, partialBuffer)` and sent; the call is currently commented out as a placeholder for packet wiring.
      - If inactivity exceeds `BUFFER_LENGTH` and the buffer is partially filled, logic is present (commented) to flush the partial buffer followed by an empty buffer (gap) to demarcate silence, then reset indices.
  - Notes
    - Networking calls are commented (`RadiocraftPackets.sendToServer(...)`), indicating integration points for actual packet dispatch.

- `CWReceiveBuffer`
  - Queues incoming `CWBuffer` chunks and provides tick-by-tick playback strengths.
  - Data
    - `playbackBuffer`: List of `PlaybackEntry(buffer, strength)`. `strength` is a float amplitude applied when the tick is “on”.
  - Methods
    - `addToBuffer(buffer, strength)`: Inserts into `playbackBuffer` sorted by ascending `buffer.id`, preserving stream order.
    - `getNextStrength()`: Reads the next boolean from the head buffer; if `true`, returns its `strength`, otherwise `0.0F`. Removes fully consumed buffers.
  - Record
    - `PlaybackEntry(CWBuffer buffer, float strength)`: Small value object used by the queue.

## Typical Flow

1. Input capture (e.g., key down) calls `CWSendBuffer.setShouldAccumulate()` during the tick.
2. Each tick, `CWSendBuffer.tick()` records that tick’s `on/off` into `partialBuffer` and, when full or idle for long enough, prepares a `CWBuffer` to send (packet integration needed).
3. On the receiving side, incoming `CWBuffer`s are added via `CWReceiveBuffer.addToBuffer(...)` with an associated `strength` (signal level).
4. Audio/render loop calls `CWReceiveBuffer.getNextStrength()` each tick to obtain the amplitude to play; returns `0.0F` when off or when the queue is empty.

## Integration Notes

- Buffer size is fixed at 20 ticks; timing and WPM mapping should account for this discretization.
- Ordering relies on strictly increasing `CWBuffer.id` from the sender.
- Silence segmentation: the commented logic in `CWSendBuffer` suggests sending an explicit empty buffer after inactivity to signal gaps between words/letters.
- Networking glue (`RadiocraftPackets` and a `CWBufferPacket`) is referenced but not implemented here; wire these to transmit `dimension`, `pos`, and the list of `CWBuffer`s.

