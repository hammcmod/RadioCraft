package com.arrl.radiocraft.common.blockentities;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.RadiocraftServerConfig;
import com.arrl.radiocraft.api.benetworks.BENetworkObject;
import com.arrl.radiocraft.api.benetworks.INetworkObjectProvider;
import com.arrl.radiocraft.common.be_networks.network_objects.AntennaNetworkObject;
import com.arrl.radiocraft.common.be_networks.network_objects.IonosphericProcessorNetworkObject;
import com.arrl.radiocraft.common.blockentities.radio.RadioBlockEntity;
import com.arrl.radiocraft.common.blocks.IonosphericProcessorBlock;
import com.arrl.radiocraft.common.init.RadiocraftBlockEntities;
import com.arrl.radiocraft.common.init.RadiocraftData;
import com.arrl.radiocraft.common.menus.IonosphericProcessorMenu;
import com.arrl.radiocraft.common.radio.Callsigns;
import com.arrl.radiocraft.common.radio.antenna.types.IonosondeAntennaType;
import com.arrl.radiocraft.common.radio.data.TncPacket;
import com.arrl.radiocraft.common.radio.solar.SolarEvent.SolarEventInstance;
import com.arrl.radiocraft.common.radio.solar.SolarEventManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The Ionospheric Processor (lower half of the two-block rack): the station that measures the sky and sends the solar
 * weather out. It only knows the solar weather while an Ionosonde Antenna is on its coax. It talks through the powered
 * radios touching it:
 * <ul>
 * <li>It answers a {@link TncPacket.Kind#QUERY} for the solar weather (a WEFAX's SOLR WX or a teletype's SOLAR WX far
 * away) with a solar bulletin ({@link TncPacket.Kind#SOLAR}), at most once every {@link #ANSWER_GAP_TICKS}.</li>
 * <li>With blackout alerts on (the default) it warns of a blackout as soon as one is forecast
 * ({@link SolarEventManager#getUpcoming}) with a {@link TncPacket.Kind#ALERT}: WEFAX receivers that hear it print it
 * and beep.</li>
 * <li>With timed bulletins on (off by default) it also sends a bulletin when it comes online, when the solar weather
 * changes and every {@link RadiocraftServerConfig#IONOSPHERIC_BULLETIN_MINUTES} minutes.</li>
 * </ul>
 * WEFAX receivers and teletypes touching it, or touching one of its radios, use it directly. Its screen is an amber
 * terminal with the two switches and SEND NOW.
 */
public class IonosphericProcessorBlockEntity extends BlockEntity implements INetworkObjectProvider, MenuProvider {

	/** How often the processor looks at the sky, in ticks. */
	public static final int CHECK_TICKS = 20;
	/** The least time between two answers to requests: one answer serves everyone listening. */
	public static final int ANSWER_GAP_TICKS = 60;

	private String callsign = "";
	private boolean timedBulletins = false;
	private boolean blackoutAlerts = true;
	private long lastBulletin = Long.MIN_VALUE / 2;
	@Nullable
	private ResourceLocation lastEvent = null;
	private boolean pending = false; // A timed bulletin no radio was on to send: it goes as soon as one is.
	private long lastAnswer = Long.MIN_VALUE / 2;
	private long alertedLocally = -1L, alertedOnAir = -1L; // The start time of the blackout each kind of alert was for.
	private int bulletinsSent = 0;
	private int alertsSent = 0;
	// Shown on the screen (synced): whether the ionosonde is there, the radio's frequency, minutes to a blackout.
	private boolean online = false;
	private float radioMhz = 0.0F;
	private int blackoutMinutes = -1;

	public IonosphericProcessorBlockEntity(BlockPos pos, BlockState state) {
		super(RadiocraftBlockEntities.IONOSPHERIC_PROCESSOR.get(), pos, state);
	}

	@Override
	public void onLoad() {
		super.onLoad();
		if(level != null && !level.isClientSide)
			getNetworkObject(level, worldPosition); // Creates the object and joins the coax.
	}

	@Override
	public BENetworkObject createNetworkObject() {
		return new IonosphericProcessorNetworkObject(level, worldPosition);
	}

	// -------------------- FINDING ONE --------------------

	/** The processor whose rack includes {@code pos} (either half), or null. */
	@Nullable
	public static IonosphericProcessorBlockEntity at(Level level, BlockPos pos) {
		if(!level.isLoaded(pos))
			return null;
		BlockState state = level.getBlockState(pos);
		if(!(state.getBlock() instanceof IonosphericProcessorBlock))
			return null;
		BlockPos lower = state.getValue(IonosphericProcessorBlock.HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
		return level.getBlockEntity(lower) instanceof IonosphericProcessorBlockEntity processor ? processor : null;
	}

	/**
	 * A processor touching the machine at {@code machine} or one of the radios at {@code radios}: such a station reads it
	 * directly, without going on the air. Null if there is none.
	 */
	@Nullable
	public static IonosphericProcessorBlockEntity nearby(Level level, BlockPos machine, List<BlockPos> radios) {
		List<BlockPos> centres = new ArrayList<>();
		centres.add(machine);
		centres.addAll(radios);
		for(BlockPos centre : centres)
			for(Direction direction : Direction.values()) {
				IonosphericProcessorBlockEntity processor = at(level, centre.relative(direction));
				if(processor != null)
					return processor;
			}
		return null;
	}

	// -------------------- THE SKY --------------------

	/** True if an Ionosonde Antenna is on the processor's coax (server side). */
	public boolean hasIonosonde() {
		if(level == null || !(getNetworkObject(level, worldPosition) instanceof IonosphericProcessorNetworkObject processor))
			return false;
		for(AntennaNetworkObject antenna : processor.getAntennas())
			if(antenna.getAntenna() != null && antenna.getAntenna().type instanceof IonosondeAntennaType)
				return true;
		return false;
	}

	/** True while it can measure the sky: an ionosonde on its coax (synced to clients). */
	public boolean isOnline() {
		return online;
	}

	/**
	 * Radios touching either half of the rack, or touching a tuner or duplexer that touches it (a station often has its
	 * tuner between the rack and the radio; reported 2026-10-27).
	 */
	public List<RadioBlockEntity> getRadios() {
		List<RadioBlockEntity> radios = new ArrayList<>();
		if(level == null)
			return radios;
		for(BlockPos half : List.of(worldPosition, worldPosition.above()))
			for(Direction direction : Direction.values()) {
				BlockPos at = half.relative(direction);
				if(!level.isLoaded(at))
					continue;
				if(level.getBlockEntity(at) instanceof RadioBlockEntity radio && !radios.contains(radio))
					radios.add(radio);
				else if(com.arrl.radiocraft.api.capabilities.IBENetworks.getObject(level, at) instanceof com.arrl.radiocraft.common.be_networks.ICoaxPassThrough)
					for(Direction beyond : Direction.values()) {
						BlockPos far = at.relative(beyond);
						if(level.isLoaded(far) && level.getBlockEntity(far) instanceof RadioBlockEntity radio && !radios.contains(radio))
							radios.add(radio);
					}
			}
		return radios;
	}

	private boolean radioOn() {
		return getRadios().stream().anyMatch(RadioBlockEntity::isPowered);
	}

	/** WEFAX receivers touching the rack or one of its radios: they get bulletins and alerts without going on the air. */
	private List<WefaxReceiverBlockEntity> localReceivers() {
		List<WefaxReceiverBlockEntity> receivers = new ArrayList<>();
		if(level == null)
			return receivers;
		List<BlockPos> around = new ArrayList<>(List.of(worldPosition, worldPosition.above()));
		for(RadioBlockEntity radio : getRadios())
			around.add(radio.getBlockPos());
		for(BlockPos centre : around)
			for(Direction direction : Direction.values()) {
				BlockPos at = centre.relative(direction);
				if(level.isLoaded(at) && level.getBlockEntity(at) instanceof WefaxReceiverBlockEntity receiver && !receivers.contains(receiver))
					receivers.add(receiver);
			}
		return receivers;
	}

	/** Comparator output: 1 (blackout) to 6 (great) while online, 0 without its ionosonde or before there is an event. */
	public int getConditionLevel() {
		if(!online || level == null)
			return 0;
		SolarEventInstance instance = SolarEventManager.getEvent(level);
		return RadiocraftData.SOLAR_EVENTS.conditionLevel(instance == null ? null : instance.getEvent());
	}

	public static void tick(Level level, BlockPos pos, BlockState state, IonosphericProcessorBlockEntity processor) {
		if(level instanceof ServerLevel serverLevel && serverLevel.getGameTime() % CHECK_TICKS == Math.floorMod(pos.asLong(), CHECK_TICKS))
			processor.check(serverLevel);
	}

	private void check(ServerLevel serverLevel) {
		boolean nowOnline = hasIonosonde();
		boolean cameOnline = nowOnline && !online;
		if(nowOnline != online) {
			online = nowOnline;
			serverLevel.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
		}
		SolarEventManager.Upcoming upcoming = SolarEventManager.getUpcoming(serverLevel);
		long now = serverLevel.getGameTime();
		boolean blackoutAhead = online && upcoming != null && SolarEventManager.isBlackout(upcoming.event()) && upcoming.startGameTime() > now;
		updateDisplay(blackoutAhead ? (int)Math.ceil((upcoming.startGameTime() - now) / 1200.0D) : -1, cameOnline || !online);
		if(!online)
			return;

		ResourceLocation event = currentEventKey(serverLevel);
		boolean changed = event != null && !event.equals(lastEvent);
		if(changed) {
			lastEvent = event;
			serverLevel.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
		}
		if(timedBulletins) {
			int minutes = RadiocraftServerConfig.IONOSPHERIC_BULLETIN_MINUTES.get();
			boolean due = minutes > 0 && now - lastBulletin >= minutes * 1200L;
			if(cameOnline || changed || due)
				broadcast(serverLevel, true);
			else if(pending && radioOn())
				broadcast(serverLevel, false); // The receivers beside it had this one already.
		}
		if(blackoutAlerts && blackoutAhead)
			alert(serverLevel, upcoming.startGameTime());
	}

	@Nullable
	private static ResourceLocation currentEventKey(Level level) {
		SolarEventInstance instance = SolarEventManager.getEvent(level);
		return instance == null || instance.getEvent() == null ? null : RadiocraftData.SOLAR_EVENTS.getKey(instance.getEvent());
	}

	/** Keeps the screen's frequency, forecast and online lamp up to date, syncing only when something changed. */
	private void updateDisplay(int minutesToBlackout, boolean onlineChanged) {
		float mhz = getRadios().stream().filter(RadioBlockEntity::isPowered).findFirst().map(radio -> radio.getFrequency() / 1_000_000.0F).orElse(0.0F);
		if(onlineChanged || mhz != radioMhz || minutesToBlackout != blackoutMinutes) {
			radioMhz = mhz;
			blackoutMinutes = minutesToBlackout;
			sync();
		}
	}

	// -------------------- SENDING --------------------

	/** The bulletin: the solar weather report at the rack, as plain lines. */
	public List<String> bulletinLines() {
		List<String> lines = new ArrayList<>();
		if(level == null)
			return lines;
		for(Component line : TeletypeBlockEntity.reportLines(level, worldPosition))
			lines.add(line.getString());
		return lines;
	}

	/** A solar bulletin under this processor's callsign. */
	public TncPacket bulletin(ServerLevel serverLevel) {
		return TncPacket.solar(serverLevel.getRandom().nextLong(), sender(), bulletinLines());
	}

	/** Sends {@code packet} out of every powered radio touching the rack. True if any took it. */
	private boolean onAir(TncPacket packet) {
		boolean sent = false;
		for(RadioBlockEntity radio : getRadios())
			sent |= radio.isPowered() && radio.sendData(packet);
		return sent;
	}

	/**
	 * Sends a solar bulletin now: out of every powered radio it touches, and with {@code local} straight to the WEFAX
	 * receivers beside it.
	 *
	 * @return True if it went anywhere.
	 */
	public boolean broadcast(ServerLevel serverLevel, boolean local) {
		if(!online)
			return false;
		TncPacket packet = bulletin(serverLevel);
		boolean sent = onAir(packet);
		pending = !sent;
		if(local)
			for(WefaxReceiverBlockEntity receiver : localReceivers()) {
				receiver.receive(packet);
				sent = true;
			}
		lastBulletin = serverLevel.getGameTime();
		if(sent)
			bulletinsSent++;
		sync();
		return sent;
	}

	/** The blackout warning's lines. */
	public List<String> alertLines(long startGameTime) {
		long minutes = level == null ? 0 : Math.max(1L, (long)Math.ceil((startGameTime - level.getGameTime()) / 1200.0D));
		return List.of("SOLAR ALERT", "RADIO BLACKOUT EXPECTED", "IN ABOUT " + minutes + " MIN", "HF BANDS WILL CLOSE", "ISSUED BY " + sender());
	}

	/** Warns of the blackout starting at {@code start}: once to the receivers beside it, once on the air (when a radio is on). */
	private void alert(ServerLevel serverLevel, long start) {
		if(alertedLocally == start && alertedOnAir == start)
			return;
		TncPacket packet = TncPacket.alert(serverLevel.getRandom().nextLong(), sender(), alertLines(start));
		boolean sent = false;
		if(alertedLocally != start) {
			alertedLocally = start;
			for(WefaxReceiverBlockEntity receiver : localReceivers()) {
				receiver.receive(packet);
				sent = true;
			}
		}
		if(alertedOnAir != start && onAir(packet)) {
			alertedOnAir = start;
			sent = true;
		}
		if(sent) {
			alertsSent++;
			sync();
		}
		else
			setChanged();
	}

	/** A packet a radio touching the rack decoded: a request for the solar weather is answered, if it has the sky. */
	public void receive(TncPacket packet) {
		if(!(level instanceof ServerLevel serverLevel) || packet.kind() != TncPacket.Kind.QUERY || !TncPacket.TOPIC_SOLAR.equals(packet.text()) || !online)
			return;
		long now = serverLevel.getGameTime();
		if(now - lastAnswer < ANSWER_GAP_TICKS)
			return;
		if(onAir(bulletin(serverLevel))) {
			lastAnswer = now;
			bulletinsSent++;
			sync();
		}
	}

	// -------------------- SWITCHES & CALLSIGN --------------------

	public boolean hasTimedBulletins() {
		return timedBulletins;
	}

	public void setTimedBulletins(boolean on) {
		timedBulletins = on;
		if(on)
			lastBulletin = Long.MIN_VALUE / 2; // The first one goes now.
		sync();
	}

	public boolean hasBlackoutAlerts() {
		return blackoutAlerts;
	}

	public void setBlackoutAlerts(boolean on) {
		blackoutAlerts = on;
		sync();
	}

	/** SEND NOW: a bulletin straight away, on the air and to the receivers beside it. */
	public boolean sendNow() {
		boolean sent = level instanceof ServerLevel serverLevel && broadcast(serverLevel, true);
		pending = false;
		return sent;
	}

	/** Who its bulletins come from: its callsign, or failing that a tag for this rack. */
	public String sender() {
		return callsign.isEmpty() ? "IONO" + Integer.toHexString(worldPosition.hashCode() & 0xFFFF).toUpperCase(Locale.ROOT) : callsign;
	}

	public String getCallsign() {
		return callsign;
	}

	/** Takes {@code player}'s callsign if it has none yet. */
	public void claim(ServerPlayer player) {
		if(!callsign.isEmpty() || player.getServer() == null)
			return;
		String own = Callsigns.playerCallsign(player.getServer(), player.getUUID());
		if(own != null && !own.isEmpty()) {
			callsign = TncPacket.sanitizeCallsign(own);
			sync();
		}
	}

	// -------------------- SCREEN (client copies) --------------------

	/** The first powered radio's frequency in MHz, 0 with none (synced). */
	public float getRadioMhz() {
		return radioMhz;
	}

	/** Minutes until a forecast blackout, -1 with none (synced). */
	public int getBlackoutMinutes() {
		return blackoutMinutes;
	}

	public int getBulletinsSent() {
		return bulletinsSent;
	}

	public int getAlertsSent() {
		return alertsSent;
	}

	/** For tests: when the last bulletin went, in game ticks. */
	public long getLastBulletin() {
		return lastBulletin;
	}

	@Override
	public @NotNull Component getDisplayName() {
		return Component.translatable(Radiocraft.translationKey("container", "ionospheric_processor"));
	}

	@Override
	public @Nullable AbstractContainerMenu createMenu(int id, @NotNull Inventory inventory, @NotNull Player player) {
		return new IonosphericProcessorMenu(id, this);
	}

	// -------------------- SAVE & SYNC --------------------

	private void sync() {
		setChanged();
		if(level != null && !level.isClientSide)
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
	}

	@Override
	protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
		super.saveAdditional(tag, registries);
		tag.putString("callsign", callsign);
		tag.putBoolean("timedBulletins", timedBulletins);
		tag.putBoolean("blackoutAlerts", blackoutAlerts);
		tag.putLong("lastBulletin", lastBulletin);
		if(lastEvent != null)
			tag.putString("lastEvent", lastEvent.toString());
		tag.putBoolean("pending", pending);
		tag.putLong("alertedLocally", alertedLocally);
		tag.putLong("alertedOnAir", alertedOnAir);
		tag.putInt("bulletinsSent", bulletinsSent);
		tag.putInt("alertsSent", alertsSent);
		tag.putBoolean("online", online);
		tag.putFloat("radioMhz", radioMhz);
		tag.putInt("blackoutMinutes", blackoutMinutes);
	}

	@Override
	protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
		super.loadAdditional(tag, registries);
		callsign = TncPacket.sanitizeCallsign(tag.getString("callsign"));
		timedBulletins = tag.getBoolean("timedBulletins");
		blackoutAlerts = !tag.contains("blackoutAlerts") || tag.getBoolean("blackoutAlerts");
		lastBulletin = tag.contains("lastBulletin") ? tag.getLong("lastBulletin") : Long.MIN_VALUE / 2;
		lastEvent = tag.contains("lastEvent") ? ResourceLocation.tryParse(tag.getString("lastEvent")) : null;
		pending = tag.getBoolean("pending");
		alertedLocally = tag.contains("alertedLocally") ? tag.getLong("alertedLocally") : -1L;
		alertedOnAir = tag.contains("alertedOnAir") ? tag.getLong("alertedOnAir") : -1L;
		bulletinsSent = tag.getInt("bulletinsSent");
		alertsSent = tag.getInt("alertsSent");
		online = tag.getBoolean("online");
		radioMhz = tag.getFloat("radioMhz");
		blackoutMinutes = tag.contains("blackoutMinutes") ? tag.getInt("blackoutMinutes") : -1;
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
