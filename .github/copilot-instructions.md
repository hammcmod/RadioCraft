# RadioCraft - AI Coding Agent Instructions

## Project Overview
**RadioCraft** is a Minecraft 1.21.1 mod built on NeoForge that brings functioning amateur (ham) radios into Minecraft, funded by the ARRL. The mod simulates realistic radio communication including HF/VHF radios, antenna systems, propagation physics, power management, and voice transmission through integration with Simple Voice Chat.

## Core Architecture

### Radio Communication System
The mod follows a **network-based transmission model** where:
- **RadioBlockEntity** acts as the base for all radio types (HF radios: 10m/20m/40m/80m, VHF handhelds/base stations)
- **AntennaNetwork** manages communication between antennas on shared frequencies
- **BENetwork** (Block Entity Network) handles coaxial cable connections between radios and antennas
- Voice transmission uses Simple Voice Chat API with custom encoding via **RadiocraftVoicePlugin**

**Key Files:**
- `common/blockentities/radio/RadioBlockEntity.java` - Base radio implementation
- `common/radio/voice/RadiocraftVoicePlugin.java` - VoIP integration point
- `common/radio/antenna/AntennaNetwork.java` - Antenna communication hub
- `common/be_networks/network_objects/RadioNetworkObject.java` - Radio power/antenna management

### Voice Integration (The Core Innovation)
The mod integrates with Simple Voice Chat **without modifying SVC** using a sophisticated plugin system:

1. **Voice Capture** (`RadiocraftVoicePlugin.java`):
   - Intercepts `MicrophonePacketEvent` from SVC
   - Decodes Opus audio using `EncodingManager` (maintains per-player decoders)
   - Routes audio to nearby radio microphones (auditory range ~50 blocks)
   - Routes audio through player's handheld radio if equipped

2. **Transmission Flow**:
   ```
   Player speaks → SVC captures audio → Plugin decodes
                → PlayerRadio (if equipped) transmits via antenna network
                → Nearby radio mics capture → Radio transmits via antenna
                → Antenna propagates based on frequency/wavelength/SWR
                → Receiving antennas → Receiving radios → SVC plays audio
   ```

3. **Thread Safety**:
   - `VoiceTransmitters.LISTENERS` is synchronized for VoIP thread access
   - `AntennaNetwork.antennas` uses `Collections.synchronizedSet()`
   - `micPos` in RadioBlockEntity uses `AtomicReference<BlockPos>`

**Integration Flow:**
```
Voice → RadiocraftVoicePlugin.onMicrophonePacket()
     → PlayerRadio.acceptVoicePacket() or RadioBlockEntity microphone detection
     → RadioNetworkObject checks power/PTT state
     → AntennaNetworkObject.transmitAudioPacket()
     → StaticAntenna calculates propagation strength
     → AntennaNetwork routes packets to all antennas
     → BEVoiceReceiver.receive() → SVC channel playback
```

### Power and Energy System
Unlike Astrea-Core's centralized network, RadioCraft uses a **distributed network-object pattern**:
- **PowerNetworkObject** base class for all power-consuming blocks
- **PowerBENetwork** connects devices via wire blocks (similar to Redstone)
- **ChargeControllerNetworkObject** acts as power router (battery → network)
- **SolarPanelNetworkObject** generates power based on sky visibility
- **RadioNetworkObject** consumes power differently for transmit vs. receive

**Power Flow:**
```
Solar Panel → generates power (125 FE/tick, reduced by rain)
           → pushPower() to all PowerBENetwork members
           → ChargeController receives and stores in battery
           → ChargeController distributes to radios when enabled
           → Radio consumes based on mode (receive = low, transmit = high)
```

**Energy Consumption Patterns:**
- VHF Base Station: Half of HF radio consumption
- QRP Radios: 1/5th of standard HF consumption
- Transmit: ~4x receive power draw
- CW (Morse): 25% more efficient range than voice

### Antenna Physics System
Antennas are modeled with **type-specific resonance and propagation**:

1. **Antenna Types** (`common/radio/antenna/types/`):
   - Each type extends `NonDirectionalAntennaType` or `DirectionalAntennaType`
   - Types define SWR calculation, efficiency multipliers, and resonance formulas
   - Examples: `DipoleAntennaType`, `YagiAntennaType`, `QuarterWaveVerticalAntennaType`

2. **SWR (Standing Wave Ratio)**:
   - Calculated by comparing antenna length to wavelength
   - SWR > 3.0 can cause radio damage/fire
   - `SWRHelper.getEfficiencyMultiplier()` reduces transmission strength
   - Antenna tuners allow off-resonance operation with efficiency loss

3. **Propagation Calculation** (in `StaticAntenna.transmitAudioPacket()`):
   - Line-of-sight for VHF bands (exponential with height)
   - Skip/ionospheric propagation for HF bands (day/night cycles)
   - Directional antennas apply angular efficiency multipliers
   - Distance attenuation: 10% volume loss per step beyond full-strength range

**Antenna Construction:**
- Wire-based antennas built block-by-block in world
- `AntennaNetworkObject` stores antenna data and type
- `StaticAntenna<T>` caches transmit/receive calculations
- Connected to radios via coaxial cable (`BENetwork.COAXIAL_TYPE`)

## Critical Developer Patterns

### 1. Network Object Lifecycle
Every block entity that participates in networks must implement `INetworkObjectProvider`:
```java
@Override
public BENetworkObject createNetworkObject() {
    return new RadioNetworkObject(level, worldPosition, transmitUse, receiveUse);
}

@Override
public void onLoad() {
    super.onLoad();
    if(!level.isClientSide())
        getNetworkObject(level, worldPosition); // Force initialization
}
```
Network objects are cached per-chunk and persist across chunk unloads via `IBENetworks` capability.

### 2. Voice Transmission Safety
Always use **synchronized blocks** when accessing antenna networks from VoIP thread:
```java
List<IAntenna> listeningMicsCopy;
synchronized (listeningMics) {
    listeningMicsCopy = List.copyOf(listeningMics); // Copy to avoid locking game thread
}
```
Never call Minecraft API methods directly from `onMicrophonePacket()` - use `Level.getServer().execute()` for main thread work.

### 3. Radio State Synchronization
Radios have **multiple state layers**:
- `RadioNetworkObject.isPowered` - Server-side power state
- `RadioBlockEntity.wasPowered` - Client-side cached state (for rendering)
- `RadioBlockEntity.ssbEnabled` - Voice mode enabled
- `RadioBlockEntity.isPTTDown` - Push-to-talk button state

Always call `updateBlock()` after state changes to sync client:
```java
public void toggle() {
    if(getNetworkObject(level, worldPosition) instanceof RadioNetworkObject networkObject) {
        networkObject.isPowered = !networkObject.isPowered;
        updateIsReceiving(); // Updates voice channel subscriptions
        updateBlock(); // Syncs to client via ClientboundBlockEntityDataPacket
    }
}
```

### 4. Frequency and Band Management
Frequencies are stored in **kHz** (e.g., 14250 for 14.250 MHz):
```java
Band band = Band.getBand(wavelength); // wavelength = 20 for 20m band
int min = band.minFrequency(); // Returns minimum kHz for band
frequency = Mth.clamp(frequency + step * stepCount, min, max);
```
Use `RadiocraftServerConfig.HF_FREQUENCY_STEP` for tuning increments (default 1 kHz).

### 5. Menu and UI Synchronization
Radios use `ContainerData` for client-server data sync:
```java
public ContainerData getDataSlots() {
    RadioNetworkObject networkObject = (RadioNetworkObject)IBENetworks.getObject(level, worldPosition);
    return new ContainerData() {
        @Override
        public int get(int index) {
            return networkObject.isPowered ? 1 : 0; // Sync power state to UI
        }
        @Override
        public void set(int index, int value) {} // Use packets for client→server
    };
}
```
**Never** set network object state from client - always use custom packets.

### 6. Git-Based Versioning
The build system uses **git tags** for semantic versioning:
```kotlin
// build.gradle.kts determines version from git describe
version = getVersionFromGit() // e.g., "1.0.0-dev.5+g1234567" for 5 commits past v1.0.0
```
Clean tagged commits use the tag directly (e.g., `v1.2.3` → `1.2.3`). Never hardcode version strings.

## Build & Development

### Running the Mod
```powershell
# Build the project
.\gradlew.bat build

# Run client (with dev environment flag)
.\gradlew.bat runClient  # Sets RADIOCRAFT_DEV_ENV=true

# Run second client (for multiplayer testing)
.\gradlew.bat runClient2  # Uses --username Dev2

# Run server (headless)
.\gradlew.bat runServer --nogui

# Run data generators
.\gradlew.bat runData  # Outputs to src/generated/resources

# Print current git-derived version
.\gradlew.bat printVersion
```

### Data Generation System
RadioCraft uses NeoForge's data generation system to automatically create JSON files for recipes, blockstates, translations, and tags. **Always run `.\gradlew.bat runData` after modifying datagen classes** to regenerate assets.

#### Data Provider Classes (`src/main/java/com/arrl/radiocraft/datagen/`)

1. **RadiocraftBlockstateProvider** - Generates blockstate and model JSONs:
   - `complexHorizontalBlockWithItem(block)` - For blocks with horizontal rotation (most radios)
   - `complexBlockWithItem(block)` - For blocks without rotation (solar panels, antenna poles)
   - Models referenced must exist in `src/main/resources/assets/radiocraft/models/block/`
   - Automatically creates item models that reference the block model

2. **RadiocraftLanguageProvider** - Generates localization files:
   - Uses a `Map<String, Consumer<LanguageProvider>>` pattern for multiple locales
   - Currently supports `en_us` locale
   - Add translations to the static initializer using `provider.add()` or `provider.addItem()/addBlock()`
   - Supports dynamic generation (e.g., license class names from enum values)
   - Generated to `src/generated/resources/assets/radiocraft/lang/en_us.json`

3. **RadiocraftRecipesProvider** - Generates crafting recipes:
   - Uses `ShapedRecipeBuilder` for shaped crafting (most recipes)
   - Uses `ShapelessRecipeBuilder` for shapeless crafting (all-band radio variants)
   - **Pattern**: Create private methods like `buildXxxRecipe(RecipeOutput)` for each recipe
   - Always include `.unlockedBy()` to define recipe unlock criteria
   - Use `RecipeCategory.MISC` for non-standard categories
   - Generated to `src/generated/resources/data/radiocraft/recipe/`

4. **RadiocraftBlockTagsProvider** - Generates block tags:
   - Tags defined in `RadiocraftTags.Blocks` class
   - Key tags:
     - `POWER_BLOCKS` - Blocks that connect to power networks
     - `COAX_BLOCKS` - Blocks that connect via coaxial cable
     - `ANTENNA_BLOCKS` - Blocks that can hold antennas
     - `ANTENNA_WIRE_HOLDERS` - Blocks that support antenna wire
   - Generated to `src/generated/resources/data/radiocraft/tags/block/`

#### Registration in Main Class
Data providers are registered in `Radiocraft.gatherData()`:
```java
public static void gatherData(GatherDataEvent event) {
    DataGenerator gen = event.getGenerator();
    ExistingFileHelper existingFileHelper = event.getExistingFileHelper();
    
    gen.addProvider(event.includeServer(), new RadiocraftBlockTagsProvider(...));
    gen.addProvider(event.includeClient(), new RadiocraftLanguageProvider(...));
    gen.addProvider(event.includeClient(), new RadiocraftBlockstateProvider(...));
    gen.addProvider(true, new RadiocraftRecipesProvider(...)); // Both sides
}
```

#### Adding New Content - Complete Workflow

**Example: Adding a new radio type "HF Radio 160m"**

1. **Register the block** in `RadiocraftBlocks.java`:
   ```java
   public static final DeferredBlock<HFRadio160mBlock> HF_RADIO_160M = 
       BLOCKS.register("hf_radio_160m", () -> new HFRadio160mBlock(...));
   ```

2. **Register the item** in `RadiocraftItems.java`:
   ```java
   public static final DeferredItem<BlockItem> HF_RADIO_160M = 
       ITEMS.registerSimpleBlockItem(RadiocraftBlocks.HF_RADIO_160M);
   ```

3. **Add to creative tab** in `RadiocraftTabs.java`:
   ```java
   output.accept(RadiocraftItems.HF_RADIO_160M.get());
   ```

4. **Add blockstate** in `RadiocraftBlockstateProvider.java`:
   ```java
   complexHorizontalBlockWithItem(RadiocraftBlocks.HF_RADIO_160M.get());
   ```

5. **Add translation** in `RadiocraftLanguageProvider.java`:
   ```java
   provider.addBlock(RadiocraftBlocks.HF_RADIO_160M, "HF Radio (160m)");
   ```

6. **Add recipe** in `RadiocraftRecipesProvider.java`:
   ```java
   private void buildHfRadio160mRecipe(RecipeOutput recipeOutput) {
       ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RadiocraftItems.HF_RADIO_160M.get(), 1)
           .pattern("CCC")
           .pattern("#RT")
           .pattern("IBI")
           .define('#', RadiocraftItems.WIRE.get())
           .define('C', Items.COPPER_INGOT)
           .define('R', RadiocraftItems.RADIO_CRYSTAL.get())
           .define('T', Items.TINTED_GLASS)
           .define('I', Items.IRON_INGOT)
           .define('B', RadiocraftItems.HF_CIRCUIT_BOARD.get())
           .unlockedBy("has_hf_circuit_board", has(RadiocraftItems.HF_CIRCUIT_BOARD.get()))
           .save(recipeOutput);
   }
   
   // Call in buildRecipes():
   buildHfRadio160mRecipe(recipeOutput);
   ```

7. **Add to tags** (if applicable) in `RadiocraftBlockTagsProvider.java`:
   ```java
   tag(RadiocraftTags.Blocks.POWER_BLOCKS)
       .add(RadiocraftBlocks.HF_RADIO_160M.get());
   ```

8. **Create the model** at `src/main/resources/assets/radiocraft/models/block/hf_radio_160m.json`

9. **Run data generation**:
   ```powershell
   .\gradlew.bat runData
   ```

10. **Commit the generated files** in `src/generated/resources/` along with your code changes

#### Important Data Generation Notes

- **Generated files are source-controlled**: Always commit `src/generated/resources/` changes
- **Manual JSON editing is discouraged**: Use datagen providers instead
- **Recipe naming conflicts**: Use custom names in `.save(recipeOutput, "custom_name")` for multiple recipes creating the same item
- **Tag inheritance**: Block tags don't automatically create item tags - create separate `ItemTagsProvider` if needed
- **ExistingFileHelper**: Validates that referenced models/textures exist - failures indicate missing assets
- **Locale patterns**: Use `Radiocraft.translationKey(prefix, suffix)` for consistent translation keys
- **Recipe unlocking**: Always use `.unlockedBy()` - missing this causes the recipe to never unlock in survival

#### Common Datagen Patterns

**Shaped Recipe with Item Tags:**
```java
ShapedRecipeBuilder.shaped(RecipeCategory.MISC, result, count)
    .pattern("WWW")
    .define('W', ItemTags.PLANKS) // Accepts any plank type
    .unlockedBy("has_planks", has(ItemTags.PLANKS))
    .save(recipeOutput);
```

**Multiple Recipes for Same Item:**
```java
// Must provide unique names to avoid conflicts
ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, result, 1)
    .requires(ingredient1)
    .unlockedBy("has_ingredient", has(ingredient1))
    .save(recipeOutput, "radiocraft:result_from_variant_a");
    
ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, result, 1)
    .requires(ingredient2)
    .unlockedBy("has_ingredient", has(ingredient2))
    .save(recipeOutput, "radiocraft:result_from_variant_b");
```

**Dynamic Translation Keys:**
```java
// For enums or sets of related items
Arrays.stream(MyEnum.values()).forEach(value -> {
    String key = Radiocraft.translationKey("category", value.name().toLowerCase());
    provider.add(key, formatName(value));
});
```

### Project Structure
```
src/main/java/com/arrl/radiocraft/
├── api/                           # Public API interfaces
│   ├── antenna/IAntenna.java      # Antenna contract
│   ├── benetworks/                # Network object API
│   └── blockentities/radio/       # Radio interfaces
├── common/
│   ├── blockentities/             # All block entities
│   │   └── radio/RadioBlockEntity.java  # Base radio class
│   ├── radio/
│   │   ├── voice/RadiocraftVoicePlugin.java  # SVC integration
│   │   ├── antenna/               # Antenna network, packets, types
│   │   ├── solar/SolarEventManager.java      # Propagation events
│   │   └── morse/CWBuffer.java    # Morse code handling
│   ├── be_networks/               # Block entity networks (power, coax)
│   │   └── network_objects/       # Network behavior implementations
│   ├── blocks/, items/            # Game content registration
│   ├── menus/                     # Container menus for UIs
│   └── init/Radiocraft*.java      # Deferred registries
├── client/screens/                # UI screens
└── Radiocraft.java                # Main mod class
```

### Key Dependencies
- **NeoForge**: 21.1.194 (Minecraft 1.21.1)
- **Simple Voice Chat**: 6787187 (CurseMaven) + API 2.5.0
- **Parchment**: 2024.07.28 (mappings)
- **The One Probe**: 12.0.4-6 (optional power display)
- **Jade**: 15.10.0 (optional tooltip integration)

## Important Implementation Notes

### 1. Voice Channel Management
The mod creates **dynamic audio channels** per radio conversation:
```java
// BEVoiceReceiver creates a channel when radio starts receiving
voiceChannel = RadiocraftVoicePlugin.API.createGroup(channelUUID, channelName, null);
voiceChannel.addPlayer(serverPlayer); // Add listener
voiceChannel.send(audioPacket);       // Play received audio
```
Channels are destroyed when radios turn off or change frequency.

### 2. CW (Morse Code) Buffers
Morse transmission uses a **buffered approach**:
- `CWSendBuffer` accumulates dots/dashes with timing
- `CWReceiveBuffer` decodes timing into text
- CW packets use `Collection<CWBuffer>` to handle multi-key simultaneous presses
- Range bonus: CW has 25% more range than voice (simulates bandwidth efficiency)

### 3. Solar Events and Propagation
`SolarEventManager` modifies HF propagation:
- Events loaded from JSON (`data/radiocraft/solar_events/`)
- Affects noise floor, skip range, and 10m band opening
- Server tick handler advances time-based events
- Config allows disabling geomagnetic storms

### 4. Handheld Radio Persistence
`PlayerRadio` manages VHF handhelds attached to players:
- Stored in `IVHFHandheldCapability` (NeoForge capability)
- Searches player inventory + Curios API slots
- Battery drain occurs in `VHFHandheldItem.inventoryTick()` (TODO: not implemented)
- Multi-location logic: hand, off-hand, belt slot, chest slot

### 5. Static Generation
The mod uses **coaxial cable placement** for antenna connections:
- Coax can travel "up one block at a time" (ladder-like)
- `WireBlock` handles power transmission (electrocution on water contact)
- Antenna isolator/connectors attach antenna wire to blocks

## Testing & Debugging

### In-Game Testing
1. **Power Flow**: Use Charge Controller GUI to monitor FE/tick transfer
2. **Antenna SWR**: Radio UI shows antenna match quality (affects static volume)
3. **Voice Transmission**: Use two clients (`runClient` + `runClient2`) to test radio comms
4. **Frequency Tuning**: Use mouse wheel in radio UI to step through frequencies

### Common Pitfalls
- **Radio won't transmit**: Check `isPTTDown()`, `ssbEnabled`, `networkObject.isPowered`, and antenna SWR
- **No voice received**: Verify antenna network connection via coax, check frequency match
- **Static too loud**: Check antenna grounding (not yet implemented) or SWR > 3.0
- **Power not flowing**: Ensure ChargeController is enabled (POWERED blockstate = true)

## Code Style Conventions
- **Java 21** target with four-space indentation
- **Package structure**: `com.arrl.radiocraft.common.<subsystem>`
- **Naming**: `UpperCamelCase` for classes, `lowerCamelCase` for methods, `UPPER_SNAKE_CASE` for constants
- **Documentation**: Javadoc on all public API classes/methods
- **Network objects**: Always suffix with `NetworkObject` (e.g., `RadioNetworkObject`)
- **Synchronization**: Document thread safety for VoIP-accessed fields

## Current Development State
- ✅ Core HF/VHF radio system functional
- ✅ Voice transmission via Simple Voice Chat integration
- ✅ Antenna network with propagation physics
- ✅ Power system with solar/battery/charge controller
- ✅ Multi-block antenna construction (dipole, vertical, loop, etc.)
- ✅ SWR calculation and antenna tuning
- 🚧 Small battery charging implementation (placeholder tooltip)
- 🚧 Handheld battery drain (marked TODO in `VHFHandheldItem`)
- 🚧 Administrative callsign commands (basic `/callsign` exists)
- 🚧 VHF repeater blocks (recipe exists, functionality incomplete)
- 🚧 Digital interface/TNC for APRS (items registered, no UI)
- 🚧 Antenna analyzer tool (not implemented)

## Next Steps (from todo.md)
**Phase 1 Priorities:**
1. Implement SWR consequences (fire/destruction on high SWR)
2. Complete solar event propagation effects on HF radios
3. Add runtime effects for antenna tuner efficiency loss
4. Implement `/bandcalculate` and admin callsign commands
5. Hook small battery energy tracking and drain

**Phase 2 Priorities:**
1. Complete large battery behavior (stacking, discharge, explosion)
2. Wire electrocution on water contact
3. VHF repeater sleep/wake cycle and callsign beaconing
4. Multi-channel scan indicator for receivers
5. APRS messaging and digital interface TNC UI

When working on this codebase, always consider the interplay between NeoForge block entities, the BENetwork capability system, Simple Voice Chat's plugin API, and the realistic radio physics simulation goals. The mod aims for educational accuracy while maintaining Minecraft's "vanilla+" aesthetic.
