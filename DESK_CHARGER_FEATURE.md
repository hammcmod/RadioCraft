# Desk Charger Feature Documentation

## Overview

The Desk Charger is a block that allows players to charge VHF Handheld Radios in RadioCraft. It features a GeckoLib-based 3D model with an animated LED indicator that changes color based on the radio's charge level.

## Feature Summary

The Desk Charger was implemented across multiple commits in the `feature/desk-charger` branch between October 15-18, 2025. The feature includes:

- **Visual Model**: Custom GeckoLib 3D model with emissive LED
- **Energy System**: 1,000 FE internal buffer with configurable transfer rates
- **Smart LED Indicator**: Color-coded LED that reflects radio charge status
- **Interactive GUI**: Custom interface for inserting/removing radios
- **Player-Facing Placement**: Automatically orients toward the player
- **Full Rendering Support**: Proper display in-world, in-hand, and in inventory

---

## Implementation Details

### 1. Initial Model and Rendering (Commit: 59d38ea)

**GeckoLib Dependency Added**
- Added GeckoLib to `build.gradle.kts` for animated block entity support

**Core Components Created**
- `DeskChargerBlock`: BaseEntityBlock that holds the charger logic
- `DeskChargerBlockEntity`: BlockEntity for state management and energy storage
- `DeskChargerBlockRenderer`: GeckoLib renderer for the 3D model
- `desk_charger.geo.json`: GeckoLib geometry model with LED component

**Emissive LED**
- Created `desk_charger.png` base texture
- Created `desk_charger_glowmask.png` for LED emission
- Implemented `AutoGlowingGeoLayer` for fullbright LED rendering

**Registration**
- Registered block entity type in `RadiocraftBlockEntities`
- Registered block in `RadiocraftBlocks`
- Registered item in `RadiocraftItems`
- Added to creative tab in `RadiocraftTabs`
- Bound renderer in `ClientSetupEvents`

---

### 2. Blinking LED Animation (Commit: 42d14b6)

**Custom Glow Layer**
- Created `BlinkingAutoGlowingGeoLayer` to control LED visibility
- Used world game time to toggle glow on/off every 10 ticks
- Aligned with GeckoLib 4 API render method signature

**Renderer Update**
- Updated `DeskChargerBlockRenderer` to use `BlinkingAutoGlowingGeoLayer`
- LED now pulses periodically instead of being constantly lit

---

### 3. Color-Cycling LED (Commit: 17e2bf6)

**Dynamic Color Cycling**
- Refactored `BlinkingAutoGlowingGeoLayer` for color transitions
- Implemented 60-tick (3-second) cycle through red → yellow → green
- Added brief "off" state between pulses for visual clarity

**Rendering Improvements**
- Used `RenderType.eyes()` for proper emissive rendering that ignores world lighting
- Applied packed integer color tinting via correct `reRender` overload
- Added small Z-offset to `PoseStack` to prevent z-fighting with base model

---

### 4. Smooth Red-to-Green Transition (Commit: eac566b)

**Gradual Color Shift**
- Changed from discrete colors to smooth red → green gradient
- Extended cycle time to 60 seconds (1,200 ticks)
- Maintained blink behavior with black color during off-pulses

**Code Cleanup**
- Removed unused variables
- Converted all comments to English
- Removed trailing "2" suffix from variable names
- Ensured fullbright rendering in all lighting conditions

---

### 5. GUI Implementation (Commit: 0a7b212)

**Menu System**
- Created `DeskChargerMenu` with single slot at position (80, 35)
- Restricted slot to accept only `RadiocraftItems.VHF_HANDHELD`
- Implemented shift-click transfer support
- Registered `DESK_CHARGER` menu type in `RadiocraftMenuTypes`

**Screen Interface**
- Created `DeskChargerScreen` extending `AbstractContainerScreen`
- Designed `desk_charger_gui.png` texture
- Bound screen to menu type in `ClientSetupEvents`

**Block Interaction**
- Updated `DeskChargerBlock` to open GUI on right-click
- Updated `DeskChargerBlockEntity` to provide `ContainerLevelAccess`
- Implemented proper container data management

---

### 6. Energy System and LED Sync (Commit: aac78a8)

**Energy Buffer**
- Added 1,000 FE internal energy storage to `DeskChargerBlockEntity`
- Implemented proper energy capability registration
- Added `NeoForge IEnergyStorage` capability via `RadiocraftCapabilities`

**Transfer Logic**
- Implemented correct energy flow: simulate `radio.receiveEnergy()` before extracting from block entity
- Transfer rate: up to 100 FE per tick when radio is present
- Energy only flows when radio can accept charge

**LED Status Synchronization**
- LED color now reflects radio charge percentage:
  - **Red (0%)**: Empty battery
  - **Orange-Yellow (50%)**: Half charged
  - **Green (100%)**: Fully charged
- LED behavior changes based on charge state:
  - **Blinking**: Radio is charging
  - **Steady**: Radio is fully charged
- Client receives tick updates to keep LED synchronized

**Debug Commands**
- Added `/rcenergy fillblock <x> <y> <z>`: Fill desk charger's internal buffer
- Added `/rcenergy blockinfo <x> <y> <z>`: Display charger's energy status
- Added `/rcenergy transfer <x> <y> <z> <amount>`: Manually transfer energy to radio
- Added `/rcenergy infinite <x> <y> <z> [true|false]`: Toggle infinite energy mode

**Infinite Mode**
- Added persistent `infiniteEnergy` NBT field to `DeskChargerBlockEntity`
- When enabled, charger provides unlimited energy without draining internal buffer
- Useful for creative mode and testing

**Debug Logging**
- Added per-tick DEBUG-level logging for energy diagnostics:
  - Available energy in block entity
  - Energy transfer attempts and acceptance
  - Radio energy received and stored values

---

### 7. Radio Bone Visibility (Commit: 6337520)

**Dynamic Model Visibility**
- Implemented runtime bone visibility toggling in `DeskChargerBlockRenderer`
- Retrieves "Radio" bone from model animation processor
- Hides radio geometry when slot is empty
- Shows radio geometry immediately when VHF Handheld is inserted

**Implementation Details**
- Uses shared `DeskChargerModel` instance
- Applies visibility state on each render frame
- Wrapped in try-catch with debug logging for error handling
- No model changes required - purely runtime modification

---

### 8. Player-Facing Placement (Commit: 37fb052)

**Directional Block States**
- Added horizontal `FACING` property to `DeskChargerBlock`
- Registered property in `createBlockStateDefinition()`
- Implements all four horizontal directions (NORTH, SOUTH, EAST, WEST)

**Smart Placement**
- Implemented `getStateForPlacement()` to set initial facing
- Charger front automatically faces the player who placed it
- Uses player's horizontal facing direction for intuitive orientation

**Technical Details**
- Uses `BlockStateProperties.HORIZONTAL_FACING`
- Supports rotation for all horizontal directions
- No vertical placement supported (similar to other furniture blocks)

---

### 9. Localization and Tooltips (Commit: b4a2d60)

**User-Facing Text**
- Added block name translation: `"block.radiocraft.desk_charger": "Desk Charger"`
- Implemented tooltip in `DeskChargerBlock.appendHoverText()`:
  - Displays "Charges VHF Handheld Radios" in gray text
  - Uses `Component.translatable()` for localization support
  - Applied `ChatFormatting.GRAY` for visual consistency

**Generated Resources**
- Updated `src/generated/resources/assets/radiocraft/lang/en_us.json`
- Translation keys ready for community localization

---

### 10. Item Rendering (Commit: f1c3a3f)

**GeckoLib Item Rendering**
- Created `DeskChargerBlockItem` implementing `GeoItem` interface
- Implemented `DeskChargerItemRenderer` with `DefaultedItemGeoModel`
- Created `geo/item/desk_charger.geo.json` model for inventory/hand rendering
- Item model mirrors block model for visual consistency

**Block State and Model Files**
- Added `blockstates/desk_charger.json` with facing variants to suppress warnings
- Created `models/block/desk_charger.json` with `builtin/entity` parent
- Created `models/item/desk_charger.json` with display transforms:
  - Proper scaling and positioning for GUI, first-person, and third-person views
  - Uses `builtin/entity` parent for GeckoLib rendering

**Texture**
- Created `textures/item/desk_charger.png` for item icon
- Matches in-world appearance for consistency

**Registration Update**
- Modified `RadiocraftItems` to register `DeskChargerBlockItem` instead of simple `BlockItem`
- Ensures proper GeckoLib rendering pipeline activation

**Rendering Contexts**
- **In-World**: Uses `DeskChargerBlockRenderer` (block entity renderer)
- **In-Hand/Inventory/GUI**: Uses `DeskChargerItemRenderer` (item renderer)
- Both share same GeckoLib geometry for consistency

---

## Technical Architecture

### Block Entity Structure

```java
public class DeskChargerBlockEntity extends BlockEntity implements MenuProvider {
    // Single slot for VHF Handheld Radio
    private final ItemStackHandler inventory;
    
    // 1,000 FE internal buffer
    private final IEnergyStorage energyStorage;
    
    // Infinite energy mode (creative/testing)
    private boolean infiniteEnergy;
    
    // Per-tick energy transfer to radio (up to 100 FE/tick)
    public void tick(Level level, BlockPos pos, BlockState state);
    
    // GUI access
    public AbstractContainerMenu createMenu(...);
}
```

### Energy Flow

1. **External Sources** → Desk Charger internal buffer (1,000 FE capacity)
2. **Desk Charger** → VHF Handheld Radio (up to 100 FE/tick)
3. Radio capacity: 166,500 FE (configurable in `CommonConfig`)
4. Transfer only occurs when:
   - Radio is present in slot
   - Radio can accept energy (not full)
   - Charger has energy available (or infinite mode enabled)

### LED Color Mapping

```java
// LED color based on radio charge percentage
float chargePercent = radioEnergy / radioCapacity;

if (chargePercent >= 1.0f) {
    // Steady green - fully charged
    return 0x00FF00;
} else {
    // Blinking color transition - charging
    // Red (0%) → Yellow (50%) → Green (100%)
    int red = (int)(255 * (1.0f - chargePercent));
    int green = (int)(255 * chargePercent);
    return (red << 16) | (green << 8) | 0x00;
}
```

### Rendering Pipeline

**Block Entity (In-World)**
```
DeskChargerBlockRenderer
  ├─> Base model rendering (desk_charger.geo.json)
  ├─> BlinkingAutoGlowingGeoLayer (LED with dynamic color)
  └─> Dynamic bone visibility (Radio bone)
```

**Item (Inventory/Hand)**
```
DeskChargerItemRenderer
  ├─> Item model rendering (desk_charger.geo.json)
  └─> Display transforms for different contexts
```

---

## Admin Commands

### Energy Management

All commands require OP level 3 permission:

```
/rcenergy fillblock <x> <y> <z>
  - Fills the desk charger's internal buffer to maximum (1,000 FE)

/rcenergy blockinfo <x> <y> <z>
  - Displays current energy status:
    - Desk charger internal energy
    - Radio energy (if present)
    - Radio capacity
    - Transfer rate

/rcenergy transfer <x> <y> <z> <amount>
  - Manually transfers specified amount of energy to radio
  - Respects radio's maximum capacity

/rcenergy infinite <x> <y> <z> [true|false]
  - Toggles infinite energy mode
  - When true: unlimited energy without draining buffer
  - Persists across world save/load
```

---

## Testing and Validation

### Manual Testing Performed

1. **Model Rendering**: Verified 3D model displays correctly in-world
2. **LED Animation**: Confirmed blinking behavior and color transitions
3. **GUI Interaction**: Tested opening interface and slot restrictions
4. **Energy Transfer**: Validated charging mechanics and transfer rates
5. **LED Synchronization**: Verified LED reflects actual radio charge state
6. **Bone Visibility**: Confirmed radio appears/disappears correctly
7. **Placement**: Tested player-facing orientation on placement
8. **Item Rendering**: Verified appearance in inventory, hand, and GUI
9. **Commands**: Tested all admin commands for proper functionality

### Build Verification

- All commits compiled successfully with `./gradlew build`
- No lint warnings or errors introduced
- Data generation updated correctly with `./gradlew runData`
- Client testing performed with `./gradlew runClient`

---

## Configuration

### Energy Settings (CommonConfig.java)

```java
// VHF Handheld Radio capacity
public static final ForgeConfigSpec.IntValue VHF_ENERGY_CAPACITY;
default: 166500 FE

// Energy consumption rates
VHF_IDLE_DRAIN: ~0.58 FE/tick (12 days battery life)
VHF_RX_DRAIN: ~1.16 FE/tick (6 days battery life)
VHF_TX_DRAIN: ~3.47 FE/tick (2 days battery life)
```

### Desk Charger Constants

```java
// Defined in DeskChargerBlockEntity
ENERGY_CAPACITY: 1000 FE
TRANSFER_RATE: 100 FE/tick (maximum)
```

---

## Future Enhancements

Potential improvements identified during development:

1. **Multiple Slot Support**: Allow charging multiple radios simultaneously
2. **Crafting Recipe**: Add recipe for Desk Charger (currently creative-only)
3. **Hopper Integration**: Support automated item insertion/extraction
4. **Upgrade System**: Tiered chargers with faster transfer rates
5. **Wireless Charging**: Charge radios in nearby player inventories
6. **Solar Panel Integration**: Connect to solar power sources
7. **Network Integration**: Connect multiple chargers via power network
8. **Sound Effects**: Add audio feedback for charging state changes
9. **Particle Effects**: Visual particles when actively charging

---

## Dependencies

### Required Mods
- **NeoForge**: Minecraft modding platform
- **GeckoLib 4**: Animated model rendering

### Internal Dependencies
- `RadiocraftCapabilities`: Energy capability registration
- `RadiocraftItems.VHF_HANDHELD`: Target device for charging
- `CommonConfig`: Energy consumption/capacity values
- Data generation system for localization

---

## Related Systems

This feature integrates with:

1. **Energy System**: Uses NeoForge energy capabilities
2. **VHF Handheld Radio**: Primary device being charged
3. **Small Rechargeable Battery**: Can swap batteries with charged radios
4. **Creative Tabs**: Appears in RadioCraft creative menu
5. **GUI System**: Custom menu and screen implementation
6. **Data Generation**: Localization through datagen providers

---

## Contributors

- **Primary Implementation**: Nick11014 (Nick/Matheus Menezes)
- **Code Review**: Jared Dunbar (jrddunbr)
- **Branch**: `feature/desk-charger`
- **Implementation Period**: October 15-18, 2025

---

## Commit History Summary

Total commits: 12 (feature-specific commits on `feature/desk-charger`)

1. `59d38ea` - Initial model and emissive LED
2. `42d14b6` - Blinking LED animation
3. `17e2bf6` - Color-cycling LED (red/yellow/green)
4. `eac566b` - Smooth red-to-green transition
5. `0a7b212` - GUI implementation with VHF-only slot
6. `aac78a8` - Energy system and LED synchronization
7. `6337520` - Dynamic radio bone visibility
8. `37fb052` - Player-facing placement
9. `b4a2d60` - Localization and tooltips
10. `f1c3a3f` - Item rendering with GeckoLib

---

## Files Modified/Created

### New Files Created
- `DeskChargerBlock.java` - Main block class
- `DeskChargerBlockEntity.java` - Block entity with energy logic
- `DeskChargerBlockRenderer.java` - GeckoLib block renderer
- `BlinkingAutoGlowingGeoLayer.java` - Custom LED rendering layer
- `DeskChargerMenu.java` - Container menu for GUI
- `DeskChargerScreen.java` - Client-side GUI screen
- `DeskChargerBlockItem.java` - GeoItem implementation
- `DeskChargerItemRenderer.java` - Item renderer
- `desk_charger.geo.json` (block) - GeckoLib geometry
- `desk_charger.geo.json` (item) - Item geometry
- `desk_charger.json` (blockstates) - Block state definitions
- `desk_charger.json` (block model) - Block model file
- `desk_charger.json` (item model) - Item model file
- `desk_charger.png` - Base texture
- `desk_charger_glowmask.png` - Emissive LED texture
- `desk_charger_gui.png` - GUI background
- `desk_charger.png` (item) - Item icon

### Modified Files
- `build.gradle.kts` - Added GeckoLib dependency
- `ClientSetupEvents.java` - Registered renderer and screen
- `RadiocraftBlockEntities.java` - Registered block entity type
- `RadiocraftBlocks.java` - Registered block
- `RadiocraftItems.java` - Registered DeskChargerBlockItem
- `RadiocraftTabs.java` - Added to creative tab
- `RadiocraftMenuTypes.java` - Registered menu type
- `RadiocraftCapabilities.java` - Energy capability support
- `EnergyCommands.java` - Added desk charger commands
- `en_us.json` (generated) - Localization entries

---

## Known Limitations

1. **Creative Mode Battery Swap**: Battery swapping in creative mode uses client-side event handling due to Minecraft API limitations
2. **Single Radio Only**: Charger only supports one VHF Handheld at a time
3. **No Crafting Recipe**: Currently only obtainable via creative menu
4. **No Redstone Integration**: Cannot be controlled or monitored via redstone
5. **No Comparator Output**: Doesn't emit comparator signal based on charge state

---

## Code Quality Notes

- All code follows RadioCraft style guidelines (4-space indentation, Java 21)
- All comments written in English
- Proper error handling with try-catch blocks
- Debug logging available for troubleshooting
- No compiler warnings introduced
- Follows established patterns from other RadioCraft blocks
- Full Javadoc on public API surfaces

---

## Documentation Standards

This documentation follows RadioCraft's documentation standards:
- Present-tense commit message style
- Clear player-facing impact descriptions
- Technical implementation details included
- Testing procedures documented
- Future enhancement suggestions noted
- Related systems cross-referenced

---

## License

This feature is part of RadioCraft and follows the project's license terms.
See [LICENSE](LICENSE) file in repository root.

---

*Documentation generated: October 18, 2025*  
*Branch: feature/desk-charger*  
*Base: RadioCraft NeoForge mod for Minecraft*
