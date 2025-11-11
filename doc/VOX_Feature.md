# VOX (Voice-Operated Transmit) Feature - Implementation Report

## Overview

VOX (Voice-Operated Transmit) mode enables automatic voice transmission for VHF Handheld radios when the player speaks, eliminating the need to hold the PTT (Push-To-Talk) button. This feature provides hands-free operation while maintaining compatibility with traditional PTT usage.

---

## Implementation Details

### 1. Data Component Integration

**File:** `HandheldRadioState.java`

Added `vox` boolean field to the radio's persistent state:

```java
public record HandheldRadioState(
    float frequency,
    boolean powered,
    boolean pttDown,
    boolean vox,  // VOX enabled state
    float gain,
    float micGain,
    float receiveStrength
) {
    public static final Codec<HandheldRadioState> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.FLOAT.fieldOf("frequency").forGetter(HandheldRadioState::frequency),
            Codec.BOOL.fieldOf("powered").forGetter(HandheldRadioState::powered),
            Codec.BOOL.fieldOf("pttDown").forGetter(HandheldRadioState::pttDown),
            Codec.BOOL.fieldOf("vox").forGetter(HandheldRadioState::vox),
            Codec.FLOAT.fieldOf("gain").forGetter(HandheldRadioState::gain),
            Codec.FLOAT.fieldOf("micGain").forGetter(HandheldRadioState::micGain),
            Codec.FLOAT.fieldOf("receiveStrength").forGetter(HandheldRadioState::receiveStrength)
        ).apply(instance, HandheldRadioState::new)
    );
}
```

**Features:**
- Persistent across world save/load
- Default value: `false` (VOX disabled)
- Serialized via Codec system

---

### 2. Capability Interface Extension

**File:** `IVHFHandheldCapability.java`

Added VOX state getters and setters:

```java
public interface IVHFHandheldCapability extends IRadioCapability {
    boolean isVoxEnabled();
    void setVoxEnabled(boolean enabled);
}
```

**File:** `VHFHandheldCapability.java`

Implemented VOX methods:

```java
@Override
public boolean isVoxEnabled() {
    return state.vox();
}

@Override
public void setVoxEnabled(boolean enabled) {
    state = new HandheldRadioState(
        state.frequency(),
        state.powered(),
        state.pttDown(),
        enabled,  // Update VOX state
        state.gain(),
        state.micGain(),
        state.receiveStrength()
    );
    stack.set(RadiocraftDataComponents.VHF_HANDHELD_STATE, state);
}
```

---

### 3. Network Synchronization

**File:** `SHandheldRadioUpdatePacket.java`

Extended packet to sync VOX state between client and server:

```java
public record SHandheldRadioUpdatePacket(
    int slot,
    float frequency,
    boolean powered,
    boolean pttDown,
    boolean voxEnabled,  // VOX state
    float gain,
    float micGain
) implements CustomPacketPayload {
    
    public static final StreamCodec<RegistryFriendlyByteBuf, SHandheldRadioUpdatePacket> STREAM_CODEC = 
        StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SHandheldRadioUpdatePacket::slot,
            ByteBufCodecs.FLOAT, SHandheldRadioUpdatePacket::frequency,
            ByteBufCodecs.BOOL, SHandheldRadioUpdatePacket::powered,
            ByteBufCodecs.BOOL, SHandheldRadioUpdatePacket::pttDown,
            ByteBufCodecs.BOOL, SHandheldRadioUpdatePacket::voxEnabled,
            ByteBufCodecs.FLOAT, SHandheldRadioUpdatePacket::gain,
            ByteBufCodecs.FLOAT, SHandheldRadioUpdatePacket::micGain,
            SHandheldRadioUpdatePacket::new
        );
}
```

**Server-side handling:**

```java
public static void handleServer(SHandheldRadioUpdatePacket packet, IPayloadContext context) {
    context.enqueueWork(() -> {
        if (context.player() instanceof ServerPlayer player) {
            ItemStack stack = player.getInventory().getItem(packet.slot());
            IVHFHandheldCapability cap = stack.getCapability(RadiocraftCapabilities.VHF_HANDHELDS);
            
            if (cap != null) {
                cap.setFrequencyHertz(packet.frequency());
                cap.setPowered(packet.powered());
                cap.setPTTDown(packet.pttDown());
                cap.setVoxEnabled(packet.voxEnabled());  // Sync VOX state
                cap.setGain(packet.gain());
                cap.setMicGain(packet.micGain());
            }
        }
    });
}
```

---

### 4. Transmission Logic

**File:** `PlayerRadio.java`

Modified transmission condition to include VOX:

```java
@Override
public boolean isTransmitting() {
    return isPowered() && (isPTTDown() || isVoxEnabled());
}
```

**Behavior:**
- PTT down: Traditional manual transmission
- VOX enabled + powered: Automatic voice transmission
- Both can coexist: Either condition triggers transmission

---

### 5. GUI Implementation

**File:** `VHFHandheldScreen.java`

#### VOX Toggle Button

Added toggle button to the screen:

```java
addRenderableWidget(new ToggleButton(
    cap.isVoxEnabled(),           // Initial state
    leftPos + 39,                 // X position
    topPos + 6,                   // Y position
    20,                           // Width
    14,                           // Height
    152, 168,                     // Texture UV coordinates
    WIDGETS_TEXTURE,              // Texture resource
    256, 256,                     // Texture dimensions
    this::onToggleVox             // Callback
));
```

#### VOX Indicator on LCD

Display "VOX" text when mode is active:

```java
if (cap.isPowered()) {
    // Display frequency
    pGuiGraphics.drawString(this.font, 
        String.format("%03.3f MHz", cap.getFrequencyHertz() / 1000_000.0f), 
        leftPos + 80, topPos + 119, 0xFFFFFF);
    
    // Display VOX indicator
    if (cap.isVoxEnabled()) {
        pGuiGraphics.drawString(this.font, 
            "VOX", 
            leftPos + 80, topPos + 133, 0x00FF00);  // Green text
    }
}
```

#### TX LED Indicator

TX LED shows when radio is ready to transmit:

```java
TX_LED.setIsOn(cap.isPowered() && (cap.isPTTDown() || cap.isVoxEnabled()));
```

**Behavior:**
- On when PTT pressed (manual transmission)
- On when VOX enabled (ready for voice transmission)
- Off when radio is powered off

#### VOX State Management

```java
@Override
public void render(@NotNull GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
    updateCap();
    
    // Update VOX state for voice transmission
    RadiocraftClientValues.SCREEN_VOX_ENABLED = cap.isPowered() && cap.isVoxEnabled();
    
    // ... rest of render logic
}
```

#### Callback Implementation

```java
protected void onToggleVox(ToggleButton button) {
    cap.setVoxEnabled(button.isToggled);
    updateServer();
}
```

#### Screen Close Cleanup

```java
@Override
public void onClose() {
    super.onClose();
    RadiocraftClientValues.SCREEN_PTT_PRESSED = false;
    RadiocraftClientValues.SCREEN_VOICE_ENABLED = false;
    RadiocraftClientValues.SCREEN_VOX_ENABLED = false;  // Disable VOX
    RadiocraftClientValues.SCREEN_CW_ENABLED = false;
    
    if(cap.isPTTDown()) {
        cap.setPTTDown(false);
        updateServer();
    }
}
```

---

## GUI Layout

### Button Position
- **Location:** Top center of radio face
- **Coordinates:** (39, 6) relative to screen top-left
- **Size:** 20×14 pixels
- **Texture:** `vhf_handheld_widgets.png` at UV (152, 168)

### LCD Indicators
- **Frequency:** Line 1 at (80, 119)
- **VOX Text:** Line 2 at (80, 133) in green (0x00FF00)
- **Battery:** Line 2 at (140, 133) right-aligned

---

## Usage

### Enabling VOX Mode
1. Open VHF Handheld Radio GUI
2. Click VOX toggle button (top center)
3. Green "VOX" text appears on LCD
4. TX LED illuminates (ready to transmit)

### Voice Transmission
- **With VOX:** Speak into microphone (no button press needed)
- **With PTT:** Hold PTT button (traditional method)
- **Both:** Either method triggers transmission

### Disabling VOX Mode
1. Click VOX toggle button again
2. "VOX" text disappears from LCD
3. TX LED turns off
4. PTT button required for transmission

---

## Technical Notes

### State Persistence
- VOX state stored in `HandheldRadioState` data component
- Survives world save/load cycles
- Per-item setting (not global)

### Client-Server Sync
- Client changes synced via `SHandheldRadioUpdatePacket`
- Server authoritative for final state
- Real-time updates without lag

### Voice Detection
- Handled by existing voice transmission system
- `RadiocraftClientValues.SCREEN_VOX_ENABLED` flag
- Microphone input processed when flag is true

### Compatibility
- Works alongside PTT button
- No interference with existing features
- Battery drain same as PTT mode

---

## References

### Modified Files
- `HandheldRadioState.java` - Data component with VOX field
- `IVHFHandheldCapability.java` - Capability interface extension
- `VHFHandheldCapability.java` - Capability implementation
- `SHandheldRadioUpdatePacket.java` - Network packet with VOX sync
- `PlayerRadio.java` - Transmission logic update
- `VHFHandheldScreen.java` - GUI button and indicators

### Related Documentation
- `InitialProposal.md` - Original project specifications
- `todo.md` - Project roadmap
- `AGENTS.md` - Development guidelines

---

##  Credits

**Implemented by:** Nick11014 (Nick/Matheus Menezes)  
**GitHub:** https://github.com/Nick11014  
**Date:** November 4-11, 2025 
**Branch:** `feature/VOX-issue`
**Pull Request:** #46 - Desk Charger Block

**Project:** RadioCraft - Ham Radio Mod for Minecraft  
**Platform:** NeoForge 1.21.1