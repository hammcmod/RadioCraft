package com.arrl.radiocraft.common.blockentities;

import com.arrl.radiocraft.RadiocraftServerConfig;
import com.arrl.radiocraft.api.capabilities.IBENetworks;
import com.arrl.radiocraft.api.capabilities.PlayerCallsignData;
import com.arrl.radiocraft.common.be_networks.network_objects.DummyLoadNetworkObject;
import com.arrl.radiocraft.common.be_networks.network_objects.RadioNetworkObject;
import com.arrl.radiocraft.common.blockentities.radio.RadioBlockEntity;
import com.arrl.radiocraft.common.data.PlayerCallsignSavedData;
import com.arrl.radiocraft.common.init.RadiocraftBlockEntities;
import com.arrl.radiocraft.common.init.RadiocraftData;
import com.arrl.radiocraft.common.init.RadiocraftSoundEvents;
import com.arrl.radiocraft.common.menus.TeletypeMenu;
import com.arrl.radiocraft.common.radio.data.TncPacket;
import com.arrl.radiocraft.common.radio.solar.Aurora;
import com.arrl.radiocraft.common.radio.solar.BandCondition;
import com.arrl.radiocraft.common.radio.solar.SolarEvent;
import com.arrl.radiocraft.common.radio.solar.SolarEvent.SolarEventInstance;
import com.arrl.radiocraft.common.radio.solar.SolarEventManager;
import com.arrl.radiocraft.common.radio.voice.Rtty;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The Teletype Machine (was the solar weather station): an RTTY teleprinter with its own terminal unit, so it works
 * straight through the radios it touches, no radio interface. What is typed goes out live, character by character,
 * under its own callsign, and prints here too; RTTY heard prints a line per sender, a sheet of paper every
 * {@link #LINES_PER_PAPER} lines. SOLAR WX prints the solar weather, which it reads through a radio with a real
 * antenna next to it (the comparator gives the same, 1 for a blackout to 6 for great).
 */
public class TeletypeBlockEntity extends BlockEntity implements MenuProvider {

	/** The band the station reports on by name, the one the proposal calls out as opening and closing with the weather. */
	public static final String REPORTED_BAND = "10m";
	/** Ticks the printing sound lasts (2.79 s), so it is started again only when it has run out. */
	public static final int PRINT_TICKS = 56;
	/** The HF bands reported, in order. VHF is reported first as "Normal" since the sun does not touch it. */
	public static final List<String> REPORTED_BANDS = List.of("10m", "20m", "40m", "80m");
	/** RTTY lines one sheet of paper takes. */
	public static final int LINES_PER_PAPER = 20;
	/** RTTY lines kept on the roll. */
	public static final int RTTY_HISTORY = 60;
	/** Longest RTTY line before the printer starts a new one by itself. */
	public static final int RTTY_LINE_MAX = 200;
	/** Who report lines print as. */
	public static final String WX = "WX";

	@Nullable
	private SolarEventInstance lastSeenEvent;
	private boolean lastSeenRadio;

	private String callsign = "";
	private String callsignSetBy = "";
	private final ItemStackHandler paper = new ItemStackHandler(1) {
		@Override
		public boolean isItemValid(int slot, @NotNull ItemStack stack) {
			return stack.is(Items.PAPER);
		}

		@Override
		protected void onContentsChanged(int slot) {
			setChanged();
			sync();
		}
	};
	private int paperCredit = 0; // Lines left on the sheet in the printer.
	private final List<String> rttyLines = new ArrayList<>(); // Oldest first.
	private String rttyOpenFrom = null; // Who the line being printed is from; null when no line is open.
	private final StringBuilder rttyOpen = new StringBuilder();
	private long lastPrintSoundGameTime = -1000L;
	private boolean syncPending = false;

	// Server only, not saved.
	private final SeenPackets seen = new SeenPackets();

	public TeletypeBlockEntity(BlockPos pos, BlockState state) {
		super(RadiocraftBlockEntities.TELETYPE.get(), pos, state);
	}

	public static void tick(Level level, BlockPos pos, BlockState state, TeletypeBlockEntity be) {
		if(be.syncPending && level.getGameTime() % 2 == 0) { // At most ten updates a second, however fast RTTY prints.
			be.syncPending = false;
			level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
		}
		SolarEventInstance current = SolarEventManager.getEvent(level);
		boolean radio = level.getGameTime() % 10 == 0 ? be.hasRadio() : be.lastSeenRadio;
		if(current != be.lastSeenEvent || radio != be.lastSeenRadio) {
			be.lastSeenEvent = current;
			be.lastSeenRadio = radio;
			level.updateNeighbourForOutputSignal(pos, state.getBlock()); // Comparators only re-read when told to.
		}
	}

	// -------------------- RTTY --------------------

	/** Radios on any side of the teletype. */
	public List<RadioBlockEntity> getRadios() {
		if(level == null)
			return List.of();
		List<RadioBlockEntity> radios = new ArrayList<>();
		for(Direction direction : Direction.values()) {
			BlockPos at = worldPosition.relative(direction);
			if(level.isLoaded(at) && level.getBlockEntity(at) instanceof RadioBlockEntity radio)
				radios.add(radio);
		}
		return radios;
	}

	/**
	 * Sends RTTY characters as they are typed, and prints them here too (the local copy). False if there is no
	 * callsign, nothing printable, or no radio on to send it.
	 */
	public boolean typeRtty(String text) {
		if(level == null || callsign.isEmpty())
			return false;
		String clean = Rtty.sanitize(text, TncPacket.RTTY_TEXT_MAX);
		if(clean.isEmpty())
			return false;
		TncPacket packet = TncPacket.rtty(level.getRandom().nextLong(), callsign, clean);
		seen.remember(packet.id());
		boolean sent = false;
		for(RadioBlockEntity radio : getRadios())
			sent |= radio.sendData(packet);
		if(!sent)
			return false;
		print(callsign, clean);
		return true;
	}

	/** A packet a radio next to it decoded: RTTY prints, the rest is not for a teleprinter. */
	public void receive(TncPacket packet) {
		if(packet.kind() == TncPacket.Kind.RTTY && seen.remember(packet.id()))
			print(packet.from(), packet.text());
	}

	/** SOLAR WX: prints the solar weather report on the roll, a line each (as {@link #WX}). */
	public void printReport() {
		for(Component line : getReportLines())
			print(WX, Rtty.sanitize(line.getString(), RTTY_LINE_MAX) + "\n");
	}

	/**
	 * Prints RTTY from {@code from}: a line per sender, a newline ends it. Starting a line takes paper; with none, the
	 * characters are lost until there is some.
	 */
	private void print(String from, String text) {
		boolean printed = false;
		for(char c : text.toCharArray()) {
			if(c == '\n') {
				if(rttyOpenFrom != null)
					endLine(true);
				continue;
			}
			if(rttyOpenFrom == null || !rttyOpenFrom.equals(from) || rttyOpen.length() >= RTTY_LINE_MAX) {
				if(rttyOpenFrom != null)
					endLine(false);
				if(!takePaperLine())
					continue;
				rttyOpenFrom = from;
				rttyOpen.setLength(0);
			}
			rttyOpen.append(c);
			printed = true;
		}
		if(printed && level != null && level.getGameTime() - lastPrintSoundGameTime >= PRINT_TICKS) {
			lastPrintSoundGameTime = level.getGameTime();
			level.playSound(null, worldPosition, RadiocraftSoundEvents.TELETYPE.get(), SoundSource.BLOCKS, 0.7F, 1.0F);
		}
		setChanged();
		sync();
	}

	private void endLine(boolean bell) {
		rttyLines.add(rttyOpenFrom + ": " + rttyOpen);
		while(rttyLines.size() > RTTY_HISTORY)
			rttyLines.removeFirst();
		rttyOpenFrom = null;
		rttyOpen.setLength(0);
		if(bell && level != null)
			level.playSound(null, worldPosition, RadiocraftSoundEvents.TELETYPE_DING.get(), SoundSource.BLOCKS, 0.5F, 1.0F);
	}

	/** Takes one line's worth of paper: off the sheet in the printer, or a new sheet from the supply. */
	private boolean takePaperLine() {
		if(paperCredit <= 0) {
			if(paper.extractItem(0, 1, false).isEmpty())
				return false;
			paperCredit = LINES_PER_PAPER;
		}
		paperCredit--;
		return true;
	}

	/** RTTY lines the paper on hand will print: what is left of the sheet in the printer plus the supply. */
	public int getPaperLines() {
		return paperCredit + paper.getStackInSlot(0).getCount() * LINES_PER_PAPER;
	}

	/** Printed RTTY lines, oldest first, not counting the one being printed. */
	public List<String> getRttyLines() {
		return List.copyOf(rttyLines);
	}

	/** The line being printed ("CALL: text"), or null when none is open. */
	@Nullable
	public String getRttyOpenLine() {
		return rttyOpenFrom == null ? null : rttyOpenFrom + ": " + rttyOpen;
	}

	public ItemStackHandler getPaper() {
		return paper;
	}

	/** Puts as much of {@code stack} as fits into the paper supply and returns what is left. */
	public ItemStack insertPaper(ItemStack stack) {
		return paper.insertItem(0, stack, false);
	}

	public void dropPaper() {
		if(level != null)
			Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), paper.getStackInSlot(0));
	}

	// -------------------- CALLSIGN --------------------

	/** Sets the teletype to {@code player}'s callsign. False (and nothing changes) if they have none. */
	public boolean claim(ServerPlayer player) {
		PlayerCallsignData data = PlayerCallsignSavedData.get(player.serverLevel().getServer().overworld()).getCallsignData(player.getUUID());
		if(data == null || data.callsign() == null || data.callsign().isBlank())
			return false;
		setCallsign(data.callsign(), player.getGameProfile().getName());
		return true;
	}

	public void setCallsign(String value, String setBy) {
		callsign = TncPacket.sanitizeCallsign(value);
		callsignSetBy = setBy == null ? "" : setBy;
		setChanged();
		sync();
	}

	public String getCallsign() {
		return callsign;
	}

	public String getCallsignSetBy() {
		return callsignSetBy;
	}

	// -------------------- SOLAR WEATHER --------------------

	/** The solar event in this station's dimension, null before the manager has picked one. */
	@Nullable
	public SolarEvent getCurrentEvent() {
		if(level == null)
			return null;
		SolarEventInstance instance = SolarEventManager.getEvent(level);
		return instance == null ? null : instance.getEvent();
	}

	/** Data pack id of the current event ("radiocraft:good"), null if none. */
	@Nullable
	public ResourceLocation getCurrentEventKey() {
		SolarEvent event = getCurrentEvent();
		return event == null ? null : RadiocraftData.SOLAR_EVENTS.getKey(event);
	}

	/**
	 * Like a real station, it hears the solar weather through a radio: one of any kind next to it, with a real antenna
	 * on its coax (a dummy load radiates nothing, so it does not count). Always true when the server config turns this
	 * off.
	 */
	public boolean hasRadio() {
		if(!RadiocraftServerConfig.TELETYPE_NEEDS_RADIO.get())
			return true;
		if(level == null)
			return false;
		for(Direction direction : Direction.values()) {
			if(IBENetworks.getObject(level, worldPosition.relative(direction)) instanceof RadioNetworkObject radio
					&& radio.getAntennas().stream().anyMatch(antenna -> !(antenna instanceof DummyLoadNetworkObject) && antenna.getAntenna() != null))
				return true;
		}
		return false;
	}

	/**
	 * Comparator output: 1 for the noisiest conditions (blackout) up to 6 for the quietest (great), 0 with no event or
	 * without a radio ({@link #hasRadio()}).
	 */
	public int getConditionLevel() {
		return hasRadio() ? RadiocraftData.SOLAR_EVENTS.conditionLevel(getCurrentEvent()) : 0;
	}

	/**
	 * How {@code band} carries skip here right now, from the event and the time of day, one step worse under an aurora;
	 * null with no event.
	 */
	@Nullable
	public BandCondition getBandCondition(String band) {
		SolarEvent event = getCurrentEvent();
		if(event == null || level == null)
			return null;
		BandCondition condition = event.getCondition(band, level.isDay());
		return Aurora.affects(level, worldPosition) ? condition.worse() : condition;
	}

	/** Whether {@link #REPORTED_BAND} propagates at all right now (any condition but closed). */
	public boolean isReportedBandOpen() {
		BandCondition condition = getBandCondition(REPORTED_BAND);
		return condition != null && condition != BandCondition.CLOSED;
	}

	/** "Aurora: active beyond Z ±10000" or "Aurora expected ...", null when there is none. */
	@Nullable
	public Component auroraLine() {
		if(level == null)
			return null;
		int latitude = Aurora.latitude();
		if(Aurora.isActive(level))
			return Component.translatable("message.radiocraft.solar_weather_station.aurora_active", latitude).withStyle(ChatFormatting.GREEN);
		if(Aurora.isExpected(level))
			return Component.translatable("message.radiocraft.solar_weather_station.aurora_expected", latitude).withStyle(ChatFormatting.LIGHT_PURPLE);
		if(com.arrl.radiocraft.common.radio.solar.SporadicE.isActive(level))
			return Component.translatable("message.radiocraft.solar_weather_station.sporadic_e").withStyle(ChatFormatting.AQUA);
		return null;
	}

	/** "Good", "Fair", "Poor" or "Closed". */
	public static MutableComponent conditionName(BandCondition condition) {
		return Component.translatable("message.radiocraft.solar_weather_station." + condition.getSerializedName());
	}

	/** The event's display name: a translation if the pack has one, else the id's path with a capital ("Good"). */
	public static MutableComponent eventName(@Nullable ResourceLocation key) {
		if(key == null)
			return Component.translatable("solar_event.radiocraft.none");
		String path = key.getPath();
		String fallback = path.isEmpty() ? path : Character.toUpperCase(path.charAt(0)) + path.substring(1);
		return Component.translatableWithFallback("solar_event." + key.getNamespace() + "." + path, fallback);
	}

	/** The report, one line per entry: header, overall conditions, noise floor, then each band. */
	public List<Component> getReportLines() {
		if(!hasRadio())
			return List.of(Component.translatable("message.radiocraft.solar_weather_station.no_radio"));
		SolarEvent event = getCurrentEvent();
		if(event == null)
			return List.of(Component.translatable("message.radiocraft.solar_weather_station.no_data"));
		List<Component> lines = new ArrayList<>();
		lines.add(Component.translatable("message.radiocraft.solar_weather_station.header").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
		lines.add(Component.translatable("message.radiocraft.solar_weather_station.overall", eventName(getCurrentEventKey())));
		lines.add(Component.translatable("message.radiocraft.solar_weather_station.noise", Math.round(event.getNoise() * 100.0F)));
		boolean underAurora = level != null && Aurora.affects(level, worldPosition);
		lines.add(Component.translatable("message.radiocraft.solar_weather_station.band", "VHF",
				Component.translatable(underAurora ? "message.radiocraft.solar_weather_station.aurora" : "message.radiocraft.solar_weather_station.normal")));
		for(String band : REPORTED_BANDS)
			lines.add(Component.translatable("message.radiocraft.solar_weather_station.band", band, conditionName(getBandCondition(band))));
		Component aurora = auroraLine();
		if(aurora != null)
			lines.add(aurora);
		return lines;
	}

	/** The one-line report, kept for tools and tests; SOLAR WX prints {@link #getReportLines()}. */
	public Component getReport() {
		if(!hasRadio())
			return Component.translatable("message.radiocraft.solar_weather_station.no_radio");
		SolarEvent event = getCurrentEvent();
		if(event == null)
			return Component.translatable("message.radiocraft.solar_weather_station.no_data");
		return Component.translatable("message.radiocraft.solar_weather_station.report",
				eventName(getCurrentEventKey()),
				Math.round(event.getNoise() * 100.0F),
				REPORTED_BAND,
				conditionName(getBandCondition(REPORTED_BAND)));
	}

	// -------------------- MENU, SAVE & SYNC --------------------

	@Override
	public @NotNull Component getDisplayName() {
		return Component.translatable("container.radiocraft.teletype");
	}

	@Nullable
	@Override
	public AbstractContainerMenu createMenu(int id, @NotNull Inventory playerInventory, @NotNull Player player) {
		return new TeletypeMenu(id, this);
	}

	/** Sends the new state to clients on the next even tick, so a burst of changes goes as one update. */
	private void sync() {
		if(level != null && !level.isClientSide)
			syncPending = true;
	}

	@Override
	protected void saveAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		tag.putString("callsign", callsign);
		tag.putString("callsignSetBy", callsignSetBy);
		tag.put("paper", paper.serializeNBT(registries));
		tag.putInt("paperCredit", paperCredit);
		ListTag rttyTags = new ListTag();
		for(String line : rttyLines)
			rttyTags.add(StringTag.valueOf(line));
		tag.put("rtty", rttyTags);
		if(rttyOpenFrom != null) {
			tag.putString("rttyOpenFrom", rttyOpenFrom);
			tag.putString("rttyOpen", rttyOpen.toString());
		}
	}

	@Override
	protected void loadAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		callsign = TncPacket.sanitizeCallsign(tag.getString("callsign"));
		callsignSetBy = tag.getString("callsignSetBy");
		if(tag.contains("paper"))
			paper.deserializeNBT(registries, tag.getCompound("paper"));
		paperCredit = tag.getInt("paperCredit");
		rttyLines.clear();
		for(Tag entry : tag.getList("rtty", Tag.TAG_STRING))
			rttyLines.add(entry.getAsString());
		rttyOpenFrom = tag.contains("rttyOpenFrom") ? tag.getString("rttyOpenFrom") : null;
		rttyOpen.setLength(0);
		rttyOpen.append(tag.getString("rttyOpen"));
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
