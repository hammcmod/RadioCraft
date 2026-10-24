package com.arrl.radiocraft.common.blockentities;

import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.item.component.MapItemColor;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.ChatFormatting;
import com.arrl.radiocraft.common.radio.data.AprsMapPainter;
import com.arrl.radiocraft.RadiocraftServerConfig;
import com.arrl.radiocraft.common.blocks.AprsTerminalBlock;
import com.arrl.radiocraft.common.blocks.AprsTerminalBlock.Display;
import com.arrl.radiocraft.common.init.RadiocraftBlockEntities;
import com.arrl.radiocraft.common.menus.AprsTerminalMenu;
import com.arrl.radiocraft.common.radio.data.AprsIcon;
import com.arrl.radiocraft.common.radio.data.TncPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The APRS terminal (an H19 on the station bus): the map of stations heard, messages, and this station's beacon
 * (on demand, on a redstone pulse or on a timer). It sends and hears through the radio interfaces on its
 * {@link StationBus}, under their callsign, and sets their digipeat switch. Its screen shows CONTACT when a new station
 * is heard, MSG when a message for this station arrives (both until someone opens it; CONTACT also goes out after
 * {@link RadiocraftServerConfig#APRS_CONTACT_LAMP_MINUTES}), otherwise READY while a radio interface on its bus has a
 * VHF radio switched on.
 */
public class AprsTerminalBlockEntity extends BlockEntity implements MenuProvider, StationTerminal {

	/** Beacons and messages each keep the last this many. */
	public static final int HISTORY = 20;
	/** Ticks between checks of the screen's light. */
	private static final int DISPLAY_CHECK_TICKS = 10;
	/** Ticks between repaints of the APRS maps this terminal made. */
	private static final int MAP_PAINT_TICKS = 100;
	/** Most map sets one terminal keeps painting; a new one past this takes over from the oldest. */
	public static final int MAX_MAP_SETS = 8;

	/** APRS terminals loaded on the server, for the admin commands that reach all of them. */
	private static final Set<AprsTerminalBlockEntity> LOADED = ConcurrentHashMap.newKeySet();

	private String beaconComment = "";
	private AprsIcon icon = AprsIcon.HOUSE;
	private boolean beaconEnabled = false;
	private boolean contactBuzzer = true;
	private boolean messageBuzzer = true;
	private boolean unread = false;
	private long newContactGameTime = -1L; // When the CONTACT light came on; -1 when it is off.
	private long lastBeaconGameTime = -1L;

	private final List<Heard> beacons = new ArrayList<>(); // Newest first, one per station.
	private final List<Logged> messages = new ArrayList<>(); // Newest first: those for this station and those it sent.
	private final List<MapSet> mapSets = new ArrayList<>(); // The APRS maps it made and keeps painting, oldest first.
	private boolean syncPending = false;

	// Server only, not saved.
	private final SeenPackets seen = new SeenPackets();
	private boolean wasRedstonePowered = false;

	/** A beacon this station heard and when, in game time. */
	public record Heard(TncPacket packet, long gameTime) {}

	/** A message this station received ({@code outgoing} false) or sent. */
	public record Logged(TncPacket packet, boolean outgoing, long gameTime) {}

	public AprsTerminalBlockEntity(BlockPos pos, BlockState state) {
		super(RadiocraftBlockEntities.APRS_TERMINAL.get(), pos, state);
	}

	public static Collection<AprsTerminalBlockEntity> loaded() {
		return List.copyOf(LOADED);
	}

	@Override
	public void onLoad() {
		super.onLoad();
		if(level != null && !level.isClientSide)
			LOADED.add(this);
	}

	@Override
	public void setRemoved() {
		super.setRemoved();
		LOADED.remove(this);
	}

	@Override
	public void onChunkUnloaded() {
		super.onChunkUnloaded();
		LOADED.remove(this);
	}

	// -------------------- TICK --------------------

	public static <T extends BlockEntity> void tick(Level level, BlockPos pos, BlockState state, T t) {
		if(t instanceof AprsTerminalBlockEntity terminal && level instanceof ServerLevel serverLevel)
			terminal.serverTick(serverLevel);
	}

	private void serverTick(ServerLevel level) {
		long now = level.getGameTime();
		if(syncPending && now % 2 == 0) {
			syncPending = false;
			BlockState state = getBlockState();
			level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
		}

		boolean redstone = level.hasNeighborSignal(worldPosition);
		if(redstone && !wasRedstonePowered)
			beacon(); // A rising edge sends a beacon.
		wasRedstonePowered = redstone;

		long interval = RadiocraftServerConfig.TNC_BEACON_INTERVAL_SECONDS.get() * 20L;
		if(beaconEnabled && (lastBeaconGameTime < 0 || now - lastBeaconGameTime >= interval || now < lastBeaconGameTime))
			beacon();

		long contactTicks = RadiocraftServerConfig.APRS_CONTACT_LAMP_MINUTES.get() * 1200L;
		if(newContactGameTime >= 0 && contactTicks > 0 && (now - newContactGameTime >= contactTicks || now < newContactGameTime)) {
			newContactGameTime = -1L;
			setChanged();
		}
		if(now % DISPLAY_CHECK_TICKS == 0)
			updateDisplay();
		if(now % MAP_PAINT_TICKS == 0 && !mapSets.isEmpty())
			paintMaps(level);
	}

	// -------------------- APRS MAPS --------------------

	/**
	 * A set of APRS maps, {@code size} by {@code size}, painted as one picture: a wall of item frames. The ids are row by
	 * row from the top left (north west).
	 */
	public record MapSet(int size, int[] ids) {}

	/**
	 * A new set of APRS maps, {@code size} by {@code size} (1, 2 or 3), for a framed or held display of the stations
	 * heard: vanilla maps (so frames, hands and the cartography table all work) that this terminal paints from now on.
	 * Copies share them and stay live. Each map's tooltip says where it goes in the wall.
	 */
	public List<ItemStack> createMaps(ServerLevel level, int size) {
		int[] ids = new int[size * size];
		for(int i = 0; i < ids.length; i++) {
			MapId id = level.getFreeMapId();
			level.setMapData(id, MapItemSavedData.createFresh(0.0D, 0.0D, (byte)0, false, false, level.dimension()).locked());
			ids[i] = id.id();
		}
		mapSets.add(new MapSet(size, ids));
		while(mapSets.size() > MAX_MAP_SETS)
			mapSets.removeFirst();
		setChanged();
		paintMaps(level);
		List<ItemStack> stacks = new ArrayList<>();
		for(int i = 0; i < ids.length; i++) {
			ItemStack stack = new ItemStack(Items.FILLED_MAP);
			stack.set(DataComponents.MAP_ID, new MapId(ids[i]));
			stack.set(DataComponents.ITEM_NAME, size == 1 ? Component.translatable("item.radiocraft.aprs_map")
					: Component.translatable("item.radiocraft.aprs_map.wall", size, size));
			stack.set(DataComponents.MAP_COLOR, new MapItemColor(0x3A3A3A));
			List<Component> lore = new ArrayList<>();
			if(size > 1)
				lore.add(Component.translatable("item.radiocraft.aprs_map.tile", i / size + 1, i % size + 1, size, size).withStyle(ChatFormatting.WHITE));
			lore.add(Component.translatable("item.radiocraft.aprs_map.from", worldPosition.getX(), worldPosition.getY(), worldPosition.getZ()).withStyle(ChatFormatting.GRAY));
			stack.set(DataComponents.LORE, new ItemLore(lore));
			stacks.add(stack);
		}
		return stacks;
	}

	/** One APRS map (see {@link #createMaps}). */
	public ItemStack createMap(ServerLevel level) {
		return createMaps(level, 1).getFirst();
	}

	/** Repaints every map set this terminal made, each as one picture fitted to home and the stations heard. */
	public void paintMaps(ServerLevel level) {
		List<AprsMapPainter.Station> stations = new ArrayList<>();
		for(Heard heard : beacons)
			if(heard.packet().position() != null)
				stations.add(new AprsMapPainter.Station(heard.packet().from(), heard.packet().position()));
		List<BlockPos> positions = stations.stream().map(AprsMapPainter.Station::pos).toList();
		Map<Integer, AprsMapPainter.Fit> fits = new HashMap<>(); // Sets of one size share a picture.
		Map<Integer, byte[]> pictures = new HashMap<>();
		for(MapSet set : mapSets) {
			int pixels = set.size() * AprsMapPainter.SIZE;
			AprsMapPainter.Fit fit = fits.computeIfAbsent(set.size(), ignored -> AprsMapPainter.fit(worldPosition, positions, pixels, pixels));
			byte[] colors = pictures.computeIfAbsent(set.size(), ignored -> AprsMapPainter.paint(level, fit, worldPosition, stations));
			for(int i = 0; i < set.ids().length; i++) {
				MapItemSavedData data = level.getMapData(new MapId(set.ids()[i]));
				if(data != null)
					AprsMapPainter.applyTile(data, fit, colors, i % set.size(), i / set.size());
			}
		}
	}

	public List<MapSet> getMapSets() {
		return List.copyOf(mapSets);
	}

	/** What the screen shows: MSG over CONTACT over READY. */
	public Display getDisplay() {
		if(unread)
			return Display.MSG;
		if(newContactGameTime >= 0)
			return Display.CONTACT;
		if(level != null && StationBus.find(level, worldPosition).interfaces().stream().anyMatch(RadioInterfaceBlockEntity::hasVhfRadioOn))
			return Display.READY;
		return Display.OFF;
	}

	/** Puts the screen's light in the block state, and tells comparators, if it changed. */
	public void updateDisplay() {
		if(level == null || level.isClientSide)
			return;
		BlockState state = getBlockState();
		Display display = getDisplay();
		if(state.hasProperty(AprsTerminalBlock.DISPLAY) && state.getValue(AprsTerminalBlock.DISPLAY) != display) {
			level.setBlock(worldPosition, state.setValue(AprsTerminalBlock.DISPLAY, display), Block.UPDATE_ALL);
			level.updateNeighbourForOutputSignal(worldPosition, state.getBlock());
		}
	}

	// -------------------- SENDING --------------------

	/** The radio interfaces and other terminals this one is joined to. */
	public StationBus bus() {
		return level == null ? new StationBus(List.of(), List.of()) : StationBus.find(level, worldPosition);
	}

	public String getCallsign() {
		return bus().callsign();
	}

	private boolean transmit(StationBus bus, TncPacket packet) {
		seen.remember(packet.id());
		return bus.transmit(packet);
	}

	private long newId() {
		return level == null ? System.nanoTime() : level.getRandom().nextLong();
	}

	/** Beacons this station's position and comment. False if there is no callsign on the bus or no radio on to send it. */
	public boolean beacon() {
		StationBus bus = bus();
		if(level == null || bus.callsign().isEmpty())
			return false;
		lastBeaconGameTime = level.getGameTime();
		setChanged();
		return transmit(bus, TncPacket.beacon(newId(), bus.callsign(), worldPosition, beaconComment, icon.name()));
	}

	/** Sends {@code text} to {@code to} and logs it. False if there is no callsign, no recipient or no radio on. */
	public boolean sendMessage(String to, String text) {
		StationBus bus = bus();
		if(level == null || bus.callsign().isEmpty())
			return false;
		TncPacket packet = TncPacket.message(newId(), bus.callsign(), to, text);
		if(packet.to().isEmpty() || packet.text().isEmpty() || !transmit(bus, packet))
			return false;
		log(new Logged(packet, true, level.getGameTime()));
		sync();
		return true;
	}

	// -------------------- RECEIVING --------------------

	@Override
	public void receive(TncPacket packet) {
		take(packet, bus());
	}

	/** A packet put straight into this terminal (admin commands): kept like one off the air. */
	public void receiveDirect(TncPacket packet) {
		take(packet, bus());
	}

	/** True if a radio interface on this terminal's bus has a radio on and tuned within 1 kHz of {@code hz}. */
	public boolean isListeningOn(float hz) {
		return bus().interfaces().stream().anyMatch(radioInterface -> radioInterface.isListeningOn(hz));
	}

	private void take(TncPacket packet, StationBus bus) {
		if(level == null || !seen.remember(packet.id()))
			return;
		long now = level.getGameTime();
		if(packet.kind() == TncPacket.Kind.BEACON) {
			boolean known = beacons.removeIf(heard -> heard.packet().from().equals(packet.from()));
			beacons.addFirst(new Heard(packet, now));
			trim(beacons);
			if(!known) {
				newContactGameTime = now;
				if(contactBuzzer)
					buzz(1.4F);
			}
		}
		else if(packet.kind() == TncPacket.Kind.MESSAGE && bus.isFor(packet.to())) {
			log(new Logged(packet, false, now));
			unread = true;
			if(messageBuzzer)
				buzz(0.8F);
		}
		else
			return;
		setChanged();
		sync();
		updateDisplay();
	}

	private void log(Logged entry) {
		messages.addFirst(entry);
		trim(messages);
		setChanged();
	}

	private static void trim(List<?> list) {
		while(list.size() > HISTORY)
			list.removeLast();
	}

	private void buzz(float pitch) {
		if(level != null)
			level.playSound(null, worldPosition, SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.BLOCKS, 0.6F, pitch);
	}

	// -------------------- SETTINGS --------------------

	/** Applies the screen's settings; only the keys present change, and every value is checked. */
	public void applySettings(CompoundTag tag) {
		if(tag.contains("comment"))
			beaconComment = TncPacket.sanitizeText(tag.getString("comment"), TncPacket.BEACON_TEXT_MAX);
		if(tag.contains("icon"))
			icon = AprsIcon.byName(tag.getString("icon"));
		if(tag.contains("beaconEnabled")) {
			beaconEnabled = tag.getBoolean("beaconEnabled");
			lastBeaconGameTime = -1L; // Switching it on beacons right away.
		}
		if(tag.contains("contactBuzzer"))
			contactBuzzer = tag.getBoolean("contactBuzzer");
		if(tag.contains("messageBuzzer"))
			messageBuzzer = tag.getBoolean("messageBuzzer");
		if(tag.contains("digipeat")) // A setting of the radio interfaces, made from here as on a real terminal.
			for(RadioInterfaceBlockEntity radioInterface : bus().interfaces())
				radioInterface.setDigipeatEnabled(tag.getBoolean("digipeat"));
		setChanged();
		sync();
	}

	/** Someone opened the terminal: the CONTACT and MSG lights go out. */
	public void markSeen() {
		if(!unread && newContactGameTime < 0)
			return;
		unread = false;
		newContactGameTime = -1L;
		setChanged();
		sync();
		updateDisplay();
	}

	/** Comparator output: 15 while a message waits to be read. */
	public int getComparatorOutput() {
		return unread ? 15 : 0;
	}

	/** True if a radio interface on the bus is digipeating. */
	public boolean isDigipeatEnabled() {
		return bus().interfaces().stream().anyMatch(RadioInterfaceBlockEntity::isDigipeatEnabled);
	}

	/** True while a radio interface on the bus has a repeat going out. */
	public boolean isRepeating() {
		return bus().interfaces().stream().anyMatch(RadioInterfaceBlockEntity::isRepeating);
	}

	public String getBeaconComment() {
		return beaconComment;
	}

	public AprsIcon getIcon() {
		return icon;
	}

	public boolean isBeaconEnabled() {
		return beaconEnabled;
	}

	public boolean isContactBuzzer() {
		return contactBuzzer;
	}

	public boolean isMessageBuzzer() {
		return messageBuzzer;
	}

	public boolean hasUnread() {
		return unread;
	}

	public boolean hasNewContact() {
		return newContactGameTime >= 0;
	}

	public List<Heard> getBeacons() {
		return List.copyOf(beacons);
	}

	public List<Logged> getMessages() {
		return List.copyOf(messages);
	}

	// -------------------- MENU --------------------

	@Override
	public @NotNull Component getDisplayName() {
		return Component.translatable("container.radiocraft.aprs_terminal");
	}

	@Nullable
	@Override
	public AbstractContainerMenu createMenu(int id, @NotNull Inventory playerInventory, @NotNull Player player) {
		return new AprsTerminalMenu(id, this);
	}

	// -------------------- SAVE & SYNC --------------------

	/** Sends the new state to clients on the next even tick, so a burst of changes goes as one update. */
	private void sync() {
		if(level != null && !level.isClientSide)
			syncPending = true;
	}

	@Override
	protected void saveAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		tag.putString("comment", beaconComment);
		tag.putString("icon", icon.name());
		tag.putBoolean("beaconEnabled", beaconEnabled);
		tag.putBoolean("contactBuzzer", contactBuzzer);
		tag.putBoolean("messageBuzzer", messageBuzzer);
		tag.putBoolean("unread", unread);
		tag.putLong("newContact", newContactGameTime);
		tag.putLong("lastBeacon", lastBeaconGameTime);
		ListTag beaconTags = new ListTag();
		for(Heard heard : beacons) {
			CompoundTag entry = heard.packet().save();
			entry.putLong("heardAt", heard.gameTime());
			beaconTags.add(entry);
		}
		tag.put("beacons", beaconTags);
		ListTag messageTags = new ListTag();
		for(Logged logged : messages) {
			CompoundTag entry = logged.packet().save();
			entry.putLong("loggedAt", logged.gameTime());
			entry.putBoolean("outgoing", logged.outgoing());
			messageTags.add(entry);
		}
		tag.put("messages", messageTags);
		ListTag mapTags = new ListTag();
		for(MapSet set : mapSets) {
			CompoundTag entry = new CompoundTag();
			entry.putInt("size", set.size());
			entry.putIntArray("ids", set.ids());
			mapTags.add(entry);
		}
		tag.put("mapSets", mapTags);
	}

	@Override
	protected void loadAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		beaconComment = TncPacket.sanitizeText(tag.getString("comment"), TncPacket.BEACON_TEXT_MAX);
		icon = AprsIcon.byName(tag.getString("icon"));
		beaconEnabled = tag.getBoolean("beaconEnabled");
		contactBuzzer = !tag.contains("contactBuzzer") || tag.getBoolean("contactBuzzer");
		messageBuzzer = !tag.contains("messageBuzzer") || tag.getBoolean("messageBuzzer");
		unread = tag.getBoolean("unread");
		newContactGameTime = tag.contains("newContact") ? tag.getLong("newContact") : -1L;
		lastBeaconGameTime = tag.contains("lastBeacon") ? tag.getLong("lastBeacon") : -1L;
		beacons.clear();
		for(Tag entry : tag.getList("beacons", Tag.TAG_COMPOUND)) {
			CompoundTag compound = (CompoundTag)entry;
			beacons.add(new Heard(TncPacket.load(compound), compound.getLong("heardAt")));
		}
		messages.clear();
		for(Tag entry : tag.getList("messages", Tag.TAG_COMPOUND)) {
			CompoundTag compound = (CompoundTag)entry;
			messages.add(new Logged(TncPacket.load(compound), compound.getBoolean("outgoing"), compound.getLong("loggedAt")));
		}
		mapSets.clear();
		for(Tag entry : tag.getList("mapSets", Tag.TAG_COMPOUND)) {
			CompoundTag compound = (CompoundTag)entry;
			int size = compound.getInt("size");
			int[] ids = compound.getIntArray("ids");
			if(size >= 1 && ids.length == size * size)
				mapSets.add(new MapSet(size, ids));
		}
		for(int id : tag.getIntArray("maps")) // Single maps, from before map walls.
			mapSets.add(new MapSet(1, new int[] { id }));
	}

	@Override
	public @NotNull CompoundTag getUpdateTag(@NotNull HolderLookup.Provider registries) {
		return saveWithoutMetadata(registries);
	}

	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}
}
