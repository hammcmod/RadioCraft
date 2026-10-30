package com.arrl.radiocraft.common.blockentities;

import com.arrl.radiocraft.RadiocraftServerConfig;
import com.arrl.radiocraft.api.benetworks.BENetworkObject;
import com.arrl.radiocraft.api.benetworks.INetworkObjectProvider;
import com.arrl.radiocraft.common.be_networks.network_objects.MicrowaveNetworkObjects;
import com.arrl.radiocraft.common.blocks.MicrowaveRadioBlock;
import com.arrl.radiocraft.common.init.RadiocraftBlockEntities;
import com.arrl.radiocraft.common.init.RadiocraftData;
import com.arrl.radiocraft.common.menus.MicrowaveRadioMenu;
import com.arrl.radiocraft.common.radio.microwave.MicrowaveIdentities;
import com.arrl.radiocraft.common.radio.microwave.MicrowaveMath;
import com.arrl.radiocraft.common.radio.solar.SolarEvent;
import com.arrl.radiocraft.common.radio.solar.SolarEventManager;
import com.arrl.radiocraft.common.radio.terrain.TerrainPropagation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A microwave radio: a digital point to multipoint link over its dishes (or the large horn) on the coax. One radio
 * broadcasts a master link; others search for it in one sweep, show both callsigns and ask to confirm. Each end reads
 * the other's signal level ({@link MicrowaveMath#signal}) whenever it is roughly in view, linked or not, so the dishes
 * can be peaked one end at a time on the ALIGN page; the far end's callsign shows once the signal decodes
 * ({@link MicrowaveMath#SENSITIVITY_DBM}), a link comes up from there and holds down to {@link MicrowaveMath#HOLD_DBM}.
 * Another dish in view within {@code microwave_interference_mhz} spoils it (half of what crosses is lost).
 * <p>
 * Over a link travel APRS packets between H19s, bearings between direction finders, and telephone calls; those
 * stations attach to a radio by touching it or by the power wire it is on ({@link MicrowaveNetworkObjects.StationLink}).
 * Both ends must be loaded for anything to cross.
 */
public class MicrowaveRadioBlockEntity extends BlockEntity implements INetworkObjectProvider, MenuProvider {

	/** What the radio is doing. */
	public enum Phase { IDLE, BROADCASTING, SEARCHING, FOUND, LINKED, NOT_FOUND, LOST }

	/** Sweeps a search makes before giving up. */
	public static final int SWEEPS = 1;
	/** Readings of the signal kept for the ALIGN page's graph, one every {@link #SAMPLE_TICKS}. */
	public static final int HISTORY = 60;
	public static final int SAMPLE_TICKS = 10;

	/** Loaded radios by dimension, server side. */
	private static final Map<ResourceKey<Level>, Map<BlockPos, MicrowaveRadioBlockEntity>> LOADED = new ConcurrentHashMap<>();

	// Saved.
	private String callsign = "";
	private int hexId = -1;
	private int frequency = 6175;
	private Phase phase = Phase.IDLE;
	@Nullable
	private BlockPos master = null; // A subscriber's confirmed master.
	private String masterName = "";

	// Synced.
	private boolean powered = false;
	private int searchTicks = 0, searchLength = 1, sweep = 1;
	@Nullable
	private BlockPos candidate = null;
	private String candidateName = "";
	private final List<String> subscribers = new ArrayList<>();
	private boolean interference = false;
	private String problem = "";
	private final List<DishInfo> dishInfo = new ArrayList<>();
	/** The signal from the far end being aligned on, dBm, and the last {@link #HISTORY} readings, oldest first. */
	private float rssi = (float)MicrowaveMath.NO_SIGNAL_DBM;
	private final float[] history = new float[HISTORY];
	/** Who the signal comes from, for the ALIGN page ("" for nobody in view). */
	private String alignName = "";
	/** Why there is no signal on the ALIGN page ("" while there is one, or nothing to say). */
	private String alignReason = "";

	/** A dish on this radio's coax, as the screen shows it. */
	public record DishInfo(BlockPos pos, float azimuth, float elevation, boolean horn) {}

	// Server only.
	/** The power switch as saved, put back on the network object when it is made again after a reload. */
	private boolean switchedOn = false;
	private final Set<BlockPos> rejected = new LinkedHashSet<>();
	private int lostTicks = 0;
	private boolean dirty = false;

	{
		java.util.Arrays.fill(history, (float)MicrowaveMath.NO_SIGNAL_DBM);
	}

	public MicrowaveRadioBlockEntity(BlockPos pos, BlockState state) {
		super(RadiocraftBlockEntities.MICROWAVE_RADIO.get(), pos, state);
	}

	// -------------------- REGISTRY --------------------

	public static Collection<MicrowaveRadioBlockEntity> loaded(Level level) {
		Map<BlockPos, MicrowaveRadioBlockEntity> map = LOADED.get(level.dimension());
		return map == null ? List.of() : List.copyOf(map.values());
	}

	@Nullable
	public static MicrowaveRadioBlockEntity at(Level level, @Nullable BlockPos pos) {
		if(pos == null)
			return null;
		Map<BlockPos, MicrowaveRadioBlockEntity> map = LOADED.get(level.dimension());
		MicrowaveRadioBlockEntity radio = map == null ? null : map.get(pos);
		return radio == null || radio.isRemoved() ? null : radio;
	}

	@Override
	public void onLoad() {
		super.onLoad();
		if(level != null && !level.isClientSide) {
			LOADED.computeIfAbsent(level.dimension(), k -> new ConcurrentHashMap<>()).put(worldPosition, this);
			getNetworkObject(level, worldPosition);
			if(hexId < 0 && level instanceof ServerLevel serverLevel) // A fresh radio: a random ID, the callsign is up to its owner.
				hexId = serverLevel.random.nextInt(0x10000);
		}
	}

	@Override
	public void setRemoved() {
		super.setRemoved();
		if(level != null && !level.isClientSide) {
			Map<BlockPos, MicrowaveRadioBlockEntity> map = LOADED.get(level.dimension());
			if(map != null)
				map.remove(worldPosition, this);
		}
	}

	@Override
	public void onChunkUnloaded() {
		super.onChunkUnloaded();
		setRemoved();
	}

	/** The block was broken: its callsign and ID pair is free again. */
	public void releaseIdentity() {
		if(level != null && level.getServer() != null && !callsign.isEmpty())
			MicrowaveIdentities.get(level.getServer()).release(MicrowaveIdentities.key(callsign, hexId), MicrowaveIdentities.at(level, worldPosition));
	}

	@Override
	public BENetworkObject createNetworkObject() {
		return new MicrowaveNetworkObjects.Radio(level, worldPosition);
	}

	/** Networks live in memory only: after a reload the switch comes back from the block entity's save. */
	@Override
	public void onNetworkObjectCreated(Level level, BlockPos pos, BENetworkObject networkObject) {
		if(switchedOn && networkObject instanceof MicrowaveNetworkObjects.Radio radio)
			radio.wantsOn = true;
	}

	@Nullable
	private MicrowaveNetworkObjects.Radio radioObject() {
		return level != null && getNetworkObject(level, worldPosition) instanceof MicrowaveNetworkObjects.Radio radio ? radio : null;
	}

	// -------------------- TICK --------------------

	public static void tick(Level level, BlockPos pos, BlockState state, BlockEntity blockEntity) {
		if(level instanceof ServerLevel serverLevel && blockEntity instanceof MicrowaveRadioBlockEntity radio)
			radio.serverTick(serverLevel);
	}

	private void serverTick(ServerLevel serverLevel) {
		MicrowaveNetworkObjects.Radio object = radioObject();
		boolean nowPowered = object != null && object.isPowered;
		if(nowPowered != powered) {
			powered = nowPowered;
			dirty = true;
			if(powered) // Coming back up: carry on as before, finding the link again.
				phase = phase == Phase.BROADCASTING || master == null ? (phase == Phase.BROADCASTING ? Phase.BROADCASTING : Phase.IDLE) : startSearch();
		}
		long now = serverLevel.getGameTime();
		if(powered) {
			switch(phase) {
				case SEARCHING -> {
					if(++searchTicks % 20 == 0)
						dirty = true;
					if(searchTicks >= searchLength)
						finishSweep(serverLevel);
				}
				case LINKED -> {
					if((now + worldPosition.asLong()) % 20L == 0L)
						checkLink(serverLevel);
				}
				case LOST -> {
					if(++lostTicks >= 60)
						phase = startSearch();
				}
				case BROADCASTING -> {
					if((now + worldPosition.asLong()) % 20L == 0L)
						refreshSubscribers(serverLevel);
				}
				default -> {}
			}
		}
		if((now + worldPosition.asLong()) % 20L == 0L)
			refreshDishes(serverLevel);
		if(powered && (now + worldPosition.asLong()) % SAMPLE_TICKS == 0L)
			sample(serverLevel);

		BlockState state = getBlockState();
		if(state.getBlock() instanceof MicrowaveRadioBlock && state.getValue(MicrowaveRadioBlock.LIT) != powered)
			serverLevel.setBlock(worldPosition, state.setValue(MicrowaveRadioBlock.LIT, powered), Block.UPDATE_CLIENTS);
		if(dirty && now % 4L == 0L) {
			dirty = false;
			sync();
		}
	}

	// -------------------- SIGNAL --------------------

	/**
	 * The radio this one is pointed at, for the signal reading: its master while linked (or lost), the master found,
	 * the strongest subscriber of a master, or else, before any link, the loudest radio on its frequency on the other
	 * side (a master for a subscriber, a would-be subscriber for a master), failing that the loudest radio on the
	 * frequency whatever its role, so both ends can align before anyone has been made master.
	 */
	@Nullable
	private MicrowaveRadioBlockEntity alignTarget(ServerLevel serverLevel) {
		MicrowaveRadioBlockEntity target = switch(phase) {
			case LINKED, LOST -> at(serverLevel, master);
			case FOUND -> at(serverLevel, candidate);
			default -> null;
		};
		if(target != null)
			return target;
		List<MicrowaveRadioBlockEntity> heard = phase == Phase.BROADCASTING ? subscribersOf(serverLevel) : List.of();
		List<MicrowaveRadioBlockEntity> onFrequency = loaded(serverLevel).stream().filter(other -> other != this && other.powered && other.frequency == frequency).toList();
		if(heard.isEmpty()) // Not linked: a master hears radios on its frequency trying to reach it, the rest hear masters.
			heard = onFrequency.stream().filter(other -> (phase == Phase.BROADCASTING) != (other.phase == Phase.BROADCASTING)).toList();
		if(heard.isEmpty()) // Nobody made master yet: whatever is on the frequency, so the dishes can be peaked first.
			heard = onFrequency;
		MicrowaveRadioBlockEntity best = null;
		double bestRssi = Double.NEGATIVE_INFINITY;
		for(MicrowaveRadioBlockEntity other : heard) {
			double level = bestSignal(serverLevel, this, other).rssi();
			if(level > bestRssi) {
				best = other;
				bestRssi = level;
			}
		}
		return best;
	}

	/** Takes a reading of the signal for the ALIGN page. The far end's callsign shows only once its signal decodes. */
	private void sample(ServerLevel serverLevel) {
		MicrowaveRadioBlockEntity target = alignTarget(serverLevel);
		MicrowaveMath.Signal signal = target == null ? null : bestSignal(serverLevel, this, target);
		rssi = signal == null ? (float)MicrowaveMath.NO_SIGNAL_DBM : (float)signal.rssi();
		alignName = target == null || rssi < MicrowaveMath.SENSITIVITY_DBM ? "" : target.identity();
		alignReason = signal == null ? (target == null ? "NO RADIO ON " + frequency + " MHZ" : "") : switch(signal.result()) {
			case OUT_OF_RANGE -> "OUT OF RANGE";
			case NOT_FACING -> "NOT FACING: TURN DISHES";
			case BLOCKED -> "PATH BLOCKED BY GROUND";
			default -> "";
		};
		System.arraycopy(history, 1, history, 0, HISTORY - 1);
		history[HISTORY - 1] = rssi;
		dirty = true;
	}

	/** The best signal between any dish of {@code a} and any of {@code b}, with why there is none if there is none. */
	public static MicrowaveMath.Signal bestSignal(Level level, MicrowaveRadioBlockEntity a, MicrowaveRadioBlockEntity b) {
		MicrowaveMath.Signal best = new MicrowaveMath.Signal(MicrowaveMath.Result.OUT_OF_RANGE, MicrowaveMath.NO_SIGNAL_DBM);
		double capture = RadiocraftServerConfig.MICROWAVE_BEAM_WIDTH.get(), loss = blackoutLoss(level);
		MicrowaveMath.Path path = com.arrl.radiocraft.common.radio.microwave.MicrowavePaths.in(level);
		for(MicrowaveDishBlockEntity da : a.dishes(level))
			for(MicrowaveDishBlockEntity db : b.dishes(level)) {
				MicrowaveMath.Signal signal = MicrowaveMath.signalAlong(path, da.end(), db.end(), capture, loss);
				boolean better = signal.rssi() > best.rssi()
						|| (signal.rssi() == best.rssi() && signal.result().ordinal() > best.result().ordinal()); // The furthest it got.
				if(better)
					best = signal;
			}
		return best;
	}

	/** The signal from the far end, dBm ({@link MicrowaveMath#NO_SIGNAL_DBM} for none). */
	public float getRssi() {
		return rssi;
	}

	/** The last {@link #HISTORY} signal readings, oldest first. */
	public float[] getRssiHistory() {
		return history.clone();
	}

	/** Why there is nothing on the meter (for {@code /rcdebug microwave}, not the screens): no radio on the frequency, out of range, not facing, or the ground in the way. "" otherwise. */
	public String getAlignReason() {
		return alignReason;
	}

	/** Who the signal is from, "" for nobody or a signal too weak to decode. */
	public String getAlignName() {
		return alignName;
	}

	/** True while linked on a signal weaker than {@link MicrowaveMath#GOOD_DBM}: calls over it break up. */
	public boolean isMarginal() {
		return isLinked() && rssi < MicrowaveMath.GOOD_DBM;
	}

	private void sync() {
		setChanged();
		if(level != null && !level.isClientSide)
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
	}

	private Phase startSearch() {
		searchTicks = 0;
		sweep = 1;
		rejected.clear();
		candidate = null;
		searchLength = sweepLength();
		dirty = true;
		return Phase.SEARCHING;
	}

	private int sweepLength() {
		int base = RadiocraftServerConfig.MICROWAVE_SEARCH_SECONDS.get() * 20;
		return level == null ? base : base * 3 / 4 + level.random.nextInt(Math.max(1, base / 2));
	}

	/** The end of a sweep: a master found (the one confirmed before links at once), or another sweep, or none. */
	private void finishSweep(ServerLevel serverLevel) {
		MicrowaveRadioBlockEntity found = null;
		for(MicrowaveRadioBlockEntity other : loaded(serverLevel)) {
			if(other == this || !other.powered || other.phase != Phase.BROADCASTING || other.frequency != frequency || rejected.contains(other.worldPosition))
				continue;
			if(linkProblem(serverLevel, this, other) == null) {
				found = other;
				break;
			}
		}
		if(found != null) {
			if(found.worldPosition.equals(master)) { // Finding the link again after it dropped: no need to ask.
				phase = Phase.LINKED;
				masterName = found.identity();
				lostTicks = 0;
				problem = "";
				serverLevel.playSound(null, worldPosition, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, 0.4F, 1.6F);
			}
			else {
				phase = Phase.FOUND;
				candidate = found.worldPosition;
				candidateName = found.identity();
				serverLevel.playSound(null, worldPosition, SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.BLOCKS, 0.5F, 1.4F);
			}
		}
		else if(sweep < SWEEPS) {
			sweep++;
			searchTicks = 0;
			searchLength = sweepLength();
		}
		else if(master != null) { // A link confirmed before (after a reload, say): keep looking for it.
			phase = Phase.LOST;
			lostTicks = 0;
			problem = "MASTER NOT FOUND, RETRYING";
		}
		else
			phase = Phase.NOT_FOUND;
		dirty = true;
	}

	/** A subscriber checks its link every second: lost if the master is gone or the path no longer works. */
	private void checkLink(ServerLevel serverLevel) {
		MicrowaveRadioBlockEntity other = at(serverLevel, master);
		String why = other == null ? "MASTER NOT ANSWERING" : !other.powered ? "MASTER OFF" : other.phase != Phase.BROADCASTING ? "MASTER NOT BROADCASTING"
				: other.frequency != frequency ? "MASTER CHANGED FREQUENCY" : holdProblem(serverLevel, this, other);
		if(why != null) {
			phase = Phase.LOST;
			lostTicks = 0;
			problem = why;
			interference = false;
			dirty = true;
			serverLevel.playSound(null, worldPosition, SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.BLOCKS, 0.5F, 0.6F);
			return;
		}
		boolean spoiled = interfered(serverLevel, this, other);
		if(spoiled != interference || !problem.isEmpty()) {
			interference = spoiled;
			problem = "";
			dirty = true;
		}
	}

	private void refreshSubscribers(ServerLevel serverLevel) {
		List<String> names = new ArrayList<>();
		boolean spoiled = false;
		for(MicrowaveRadioBlockEntity other : subscribersOf(serverLevel)) {
			names.add(other.identity());
			spoiled |= other.interference;
		}
		if(!names.equals(subscribers) || spoiled != interference) {
			subscribers.clear();
			subscribers.addAll(names);
			interference = spoiled;
			dirty = true;
		}
	}

	private List<MicrowaveRadioBlockEntity> subscribersOf(ServerLevel serverLevel) {
		List<MicrowaveRadioBlockEntity> out = new ArrayList<>();
		for(MicrowaveRadioBlockEntity other : loaded(serverLevel))
			if(other != this && other.powered && other.phase == Phase.LINKED && worldPosition.equals(other.master))
				out.add(other);
		return out;
	}

	private void refreshDishes(ServerLevel serverLevel) {
		List<DishInfo> now = new ArrayList<>();
		for(MicrowaveDishBlockEntity dish : dishes(serverLevel))
			now.add(new DishInfo(dish.getBlockPos(), dish.getAzimuth(), dish.getElevation(), dish.isHorn()));
		if(!now.equals(dishInfo)) {
			dishInfo.clear();
			dishInfo.addAll(now);
			dirty = true;
		}
	}

	// -------------------- LINK GEOMETRY --------------------

	/** The loaded dishes and horns on this radio's coax. */
	public List<MicrowaveDishBlockEntity> dishes(Level level) {
		List<MicrowaveDishBlockEntity> out = new ArrayList<>();
		MicrowaveNetworkObjects.Radio object = radioObject();
		if(object == null)
			return out;
		for(BlockPos at : object.dishes())
			if(level.isLoaded(at) && level.getBlockEntity(at) instanceof MicrowaveDishBlockEntity dish)
				out.add(dish);
		return out;
	}

	/** Range a blackout takes off. */
	private static double blackoutLoss(Level level) {
		SolarEvent.SolarEventInstance event = SolarEventManager.getEvent(level);
		if(event == null)
			return 0.0D;
		ResourceLocation id = RadiocraftData.SOLAR_EVENTS.getKey(event.getEvent());
		return id != null && id.getPath().equals("blackout") ? RadiocraftServerConfig.MICROWAVE_BLACKOUT_LOSS.get() : 0.0D;
	}

	/**
	 * Null if some dish of {@code a} links with some dish of {@code b}; otherwise what the radio can tell from its
	 * meter (too weak, or nothing at all). Why there is nothing is not the radio's to know: {@code /rcdebug microwave}.
	 */
	@Nullable
	public static String linkProblem(Level level, MicrowaveRadioBlockEntity a, MicrowaveRadioBlockEntity b) {
		if(a.dishes(level).isEmpty())
			return "NO DISH ON THIS RADIO";
		if(b.dishes(level).isEmpty())
			return "NO DISH AT THE OTHER END";
		return switch(bestSignal(level, a, b).result()) {
			case OK -> null;
			case WEAK -> "SIGNAL TOO WEAK: ALIGN";
			default -> "NO SIGNAL: CHECK DISHES";
		};
	}

	/** As {@link #linkProblem}, for a link already up: it holds down to {@link MicrowaveMath#HOLD_DBM}. */
	@Nullable
	private static String holdProblem(Level level, MicrowaveRadioBlockEntity a, MicrowaveRadioBlockEntity b) {
		String why = linkProblem(level, a, b);
		if(why != null && bestSignal(level, a, b).rssi() >= MicrowaveMath.HOLD_DBM)
			return null;
		return why;
	}

	/**
	 * True if a dish of another radio (not in this link) on a frequency within the interference window is in view of
	 * either end's dish.
	 */
	public static boolean interfered(Level level, MicrowaveRadioBlockEntity a, MicrowaveRadioBlockEntity master) {
		int window = RadiocraftServerConfig.MICROWAVE_INTERFERENCE_MHZ.get();
		double beam = RadiocraftServerConfig.MICROWAVE_BEAM_WIDTH.get(), loss = blackoutLoss(level);
		List<MicrowaveDishBlockEntity> ends = new ArrayList<>(a.dishes(level));
		ends.addAll(master.dishes(level));
		MicrowaveMath.Path path = com.arrl.radiocraft.common.radio.microwave.MicrowavePaths.in(level);
		for(MicrowaveRadioBlockEntity other : loaded(level)) {
			if(other == a || other == master || !other.powered || master.worldPosition.equals(other.master) || other.worldPosition.equals(a.master))
				continue; // The link itself, and the master's other subscribers.
			if(!MicrowaveMath.interferes(other.frequency, a.frequency, window))
				continue;
			for(MicrowaveDishBlockEntity theirs : other.dishes(level))
				for(MicrowaveDishBlockEntity ours : ends) {
					MicrowaveMath.End mine = ours.end(), them = theirs.end();
					double distance = Math.hypot(them.x() - mine.x(), them.z() - mine.z());
					if(distance <= MicrowaveMath.linkRange(mine.range(), them.range(), loss) && MicrowaveMath.inBeam(mine, them.x(), them.y(), them.z(), beam)
							&& path.clear(mine, them))
						return true;
				}
		}
		return false;
	}

	// -------------------- WHAT CROSSES THE LINK --------------------

	/** True while this radio is part of a working link: a linked subscriber, or a master with subscribers. */
	public boolean isLinked() {
		return powered && (phase == Phase.LINKED || (phase == Phase.BROADCASTING && !subscribers.isEmpty()));
	}

	public boolean hasInterference() {
		return interference;
	}

	/**
	 * The other radios this one reaches: a subscriber reaches its master and the master's other subscribers, a master
	 * its subscribers. Empty when not linked.
	 */
	public List<MicrowaveRadioBlockEntity> peers() {
		if(!(level instanceof ServerLevel serverLevel) || !powered)
			return List.of();
		List<MicrowaveRadioBlockEntity> out = new ArrayList<>();
		if(phase == Phase.LINKED) {
			MicrowaveRadioBlockEntity head = at(serverLevel, master);
			if(head == null || !head.powered)
				return List.of();
			out.add(head);
			for(MicrowaveRadioBlockEntity other : head.subscribersOf(serverLevel))
				if(other != this)
					out.add(other);
		}
		else if(phase == Phase.BROADCASTING)
			out.addAll(subscribersOf(serverLevel));
		return out;
	}

	/** True if a payload makes it across: always on a clean link, half the time with interference. */
	public boolean getsThrough() {
		return !interference || level == null || level.random.nextBoolean();
	}

	/** The stations this radio serves: block entities touching it, and those on its data wire. */
	public List<BlockEntity> stations() {
		List<BlockEntity> out = new ArrayList<>();
		if(level == null)
			return out;
		for(Direction direction : Direction.values()) {
			BlockPos at = worldPosition.relative(direction);
			if(level.isLoaded(at) && level.getBlockEntity(at) instanceof BlockEntity be && !(be instanceof MicrowaveRadioBlockEntity))
				out.add(be);
		}
		for(BlockEntity be : com.arrl.radiocraft.common.be_networks.DataWire.wired(level, worldPosition, BlockEntity.class))
			if(!(be instanceof MicrowaveRadioBlockEntity) && !out.contains(be))
				out.add(be);
		return out;
	}

	/**
	 * The other microwave radios at this one's site: touching it, or on its data wire. One data network, like radios
	 * on a site's switch: what comes in on one radio's link goes on out of the others' ({@code MicrowaveRoutes}).
	 */
	public List<MicrowaveRadioBlockEntity> siteMates() {
		List<MicrowaveRadioBlockEntity> out = new ArrayList<>();
		if(level == null)
			return out;
		for(Direction direction : Direction.values()) {
			BlockPos at = worldPosition.relative(direction);
			if(level.isLoaded(at) && level.getBlockEntity(at) instanceof MicrowaveRadioBlockEntity radio)
				out.add(radio);
		}
		for(MicrowaveRadioBlockEntity radio : com.arrl.radiocraft.common.be_networks.DataWire.wired(level, worldPosition, MicrowaveRadioBlockEntity.class))
			if(radio != this && !out.contains(radio))
				out.add(radio);
		return out;
	}

	/** The microwave radios serving the station at {@code pos}: touching it, or on its data wire. */
	public static List<MicrowaveRadioBlockEntity> servingStation(Level level, BlockPos pos) {
		List<MicrowaveRadioBlockEntity> out = new ArrayList<>();
		for(Direction direction : Direction.values()) {
			BlockPos at = pos.relative(direction);
			if(level.isLoaded(at) && level.getBlockEntity(at) instanceof MicrowaveRadioBlockEntity radio)
				out.add(radio);
		}
		for(MicrowaveRadioBlockEntity radio : com.arrl.radiocraft.common.be_networks.DataWire.wired(level, pos, MicrowaveRadioBlockEntity.class))
			if(!out.contains(radio))
				out.add(radio);
		return out;
	}

	// -------------------- CONTROLS (FROM THE SCREEN) --------------------

	public void togglePower() {
		MicrowaveNetworkObjects.Radio object = radioObject();
		if(object == null)
			return;
		object.wantsOn = !object.wantsOn;
		switchedOn = object.wantsOn;
		setChanged();
		object.tick(level, worldPosition);
		powered = object.isPowered; // At once, so the screen's next button press finds it on.
		if(!powered)
			subscribers.clear();
		dirty = true;
	}

	/** Sets the callsign and ID; false if another radio has that pair. */
	public boolean setIdentity(String newCallsign, int newId) {
		if(level == null || level.getServer() == null)
			return false;
		String clean = newCallsign.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9/]", "");
		if(clean.isEmpty() || clean.length() > 10 || newId < 0 || newId > 0xFFFF)
			return false;
		MicrowaveIdentities ids = MicrowaveIdentities.get(level.getServer());
		String previous = callsign.isEmpty() ? null : MicrowaveIdentities.key(callsign, hexId);
		String key = MicrowaveIdentities.key(clean, newId);
		GlobalPos holder = ids.owner(key);
		if(holder != null && !holder.equals(MicrowaveIdentities.at(level, worldPosition))) { // Free it if its holder is gone.
			net.minecraft.server.level.ServerLevel there = level.getServer().getLevel(holder.dimension());
			if(there != null && there.isLoaded(holder.pos()) && !(there.getBlockEntity(holder.pos()) instanceof MicrowaveRadioBlockEntity other
					&& MicrowaveIdentities.key(other.callsign, other.hexId).equals(key)))
				ids.release(key, holder);
		}
		if(!ids.claim(key, previous, MicrowaveIdentities.at(level, worldPosition)))
			return false;
		callsign = clean;
		hexId = newId;
		sync();
		return true;
	}

	public void tune(int steps) {
		setFrequency(MicrowaveMath.tune(frequency, steps));
	}

	public void setFrequency(int mhz) {
		int snapped = MicrowaveMath.MIN_MHZ + Math.round((Math.max(MicrowaveMath.MIN_MHZ, Math.min(MicrowaveMath.MAX_MHZ, mhz)) - MicrowaveMath.MIN_MHZ) / (float)MicrowaveMath.STEP_MHZ) * MicrowaveMath.STEP_MHZ;
		if(snapped == frequency)
			return;
		frequency = snapped;
		if(phase == Phase.LINKED || phase == Phase.FOUND)
			phase = Phase.IDLE; // A new frequency drops the link.
		sync();
	}

	/** Broadcasts a master link: subscribers on the same frequency can find this radio. Needs a callsign. */
	public boolean broadcast() {
		if(!powered || callsign.isEmpty())
			return false;
		phase = Phase.BROADCASTING;
		master = null;
		masterName = "";
		sync();
		return true;
	}

	/** Searches for a master link on this radio's frequency. Needs a callsign. */
	public boolean search() {
		if(!powered || callsign.isEmpty())
			return false;
		master = null;
		masterName = "";
		phase = startSearch();
		sync();
		return true;
	}

	/** Confirms the master found: the link is up. */
	public void confirm() {
		if(phase != Phase.FOUND || candidate == null || level == null)
			return;
		MicrowaveRadioBlockEntity found = at(level, candidate);
		if(found == null || linkProblem(level, this, found) != null) {
			phase = Phase.LOST;
			problem = "MASTER LOST BEFORE CONFIRMING";
			lostTicks = 0;
			sync();
			return;
		}
		master = candidate;
		masterName = candidateName;
		candidate = null;
		phase = Phase.LINKED;
		problem = "";
		level.playSound(null, worldPosition, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, 0.5F, 1.6F);
		sync();
	}

	/** Not this master: keep searching for another. */
	public void reject() {
		if(phase != Phase.FOUND || candidate == null)
			return;
		rejected.add(candidate);
		candidate = null;
		phase = Phase.SEARCHING;
		searchTicks = 0;
		searchLength = sweepLength();
		sync();
	}

	public void disconnect() {
		phase = Phase.IDLE;
		master = null;
		masterName = "";
		candidate = null;
		subscribers.clear();
		interference = false;
		problem = "";
		sync();
	}

	/** Turns and tilts dish number {@code index} on the coax. */
	public void turnDish(int index, double azimuth, double elevation) {
		if(level == null)
			return;
		List<MicrowaveDishBlockEntity> dishes = dishes(level);
		if(index < 0 || index >= dishes.size())
			return;
		MicrowaveDishBlockEntity dish = dishes.get(index);
		dish.setAzimuth(dish.getAzimuth() + azimuth);
		dish.setElevation(dish.getElevation() + elevation);
		if(level instanceof ServerLevel serverLevel) {
			refreshDishes(serverLevel);
			if(powered)
				sample(serverLevel); // The ALIGN page answers each nudge at once.
		}
		sync();
	}

	// -------------------- READING --------------------

	public String identity() {
		return (callsign.isEmpty() ? "NOCALL" : callsign) + "-" + MicrowaveMath.hex(Math.max(0, hexId));
	}

	public String getCallsign() {
		return callsign;
	}

	public int getHexId() {
		return Math.max(0, hexId);
	}

	public int getFrequency() {
		return frequency;
	}

	public Phase getPhase() {
		return phase;
	}

	public boolean isPowered() {
		return powered;
	}

	/** Share of the current sweep done, 0 to 1. */
	public float getSearchProgress() {
		return Math.min(1.0F, searchTicks / (float)Math.max(1, searchLength));
	}

	public int getSweep() {
		return sweep;
	}

	public String getCandidateName() {
		return candidateName;
	}

	public String getMasterName() {
		return masterName;
	}

	public List<String> getSubscribers() {
		return List.copyOf(subscribers);
	}

	public String getProblem() {
		return problem;
	}

	public List<DishInfo> getDishInfo() {
		return List.copyOf(dishInfo);
	}

	/** Readings in tenths of a dB, to sync them compactly. */
	private static int[] toTenths(float[] readings) {
		int[] out = new int[readings.length];
		for(int i = 0; i < readings.length; i++)
			out[i] = Math.round(readings[i] * 10.0F);
		return out;
	}

	// -------------------- SAVE, SYNC, MENU --------------------

	@Override
	protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
		super.saveAdditional(tag, registries);
		tag.putString("callsign", callsign);
		tag.putInt("hexId", hexId);
		tag.putInt("frequency", frequency);
		tag.putString("phase", phase.name());
		if(master != null)
			tag.putLong("master", master.asLong());
		tag.putString("masterName", masterName);
		tag.putBoolean("switchedOn", switchedOn);
	}

	@Override
	protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
		super.loadAdditional(tag, registries);
		callsign = tag.getString("callsign");
		hexId = tag.contains("hexId") ? tag.getInt("hexId") : -1;
		if(tag.contains("frequency"))
			frequency = tag.getInt("frequency");
		try {
			phase = Phase.valueOf(tag.getString("phase"));
		}
		catch(IllegalArgumentException e) {
			phase = Phase.IDLE;
		}
		master = tag.contains("master") ? BlockPos.of(tag.getLong("master")) : null;
		masterName = tag.getString("masterName");
		switchedOn = tag.getBoolean("switchedOn");
		if(tag.contains("powered")) { // The client's copy.
			powered = tag.getBoolean("powered");
			searchTicks = tag.getInt("searchTicks");
			searchLength = Math.max(1, tag.getInt("searchLength"));
			sweep = tag.getInt("sweep");
			candidateName = tag.getString("candidateName");
			subscribers.clear();
			for(Tag name : tag.getList("subscribers", Tag.TAG_STRING))
				subscribers.add(name.getAsString());
			interference = tag.getBoolean("interference");
			problem = tag.getString("problem");
			rssi = tag.contains("rssi") ? tag.getFloat("rssi") : (float)MicrowaveMath.NO_SIGNAL_DBM;
			int[] tenths = tag.getIntArray("rssiHistory");
			for(int i = 0; i < HISTORY; i++)
				history[i] = i < tenths.length ? tenths[i] / 10.0F : (float)MicrowaveMath.NO_SIGNAL_DBM;
			alignName = tag.getString("alignName");
			alignReason = tag.getString("alignReason");
			dishInfo.clear();
			for(Tag entry : tag.getList("dishes", Tag.TAG_COMPOUND)) {
				CompoundTag dish = (CompoundTag)entry;
				dishInfo.add(new DishInfo(BlockPos.of(dish.getLong("pos")), dish.getFloat("azimuth"), dish.getFloat("elevation"), dish.getBoolean("horn")));
			}
		}
	}

	@Override
	public @NotNull CompoundTag getUpdateTag(@NotNull HolderLookup.Provider registries) {
		CompoundTag tag = saveWithoutMetadata(registries);
		tag.putBoolean("powered", powered);
		tag.putInt("searchTicks", searchTicks);
		tag.putInt("searchLength", searchLength);
		tag.putInt("sweep", sweep);
		tag.putString("candidateName", candidateName);
		ListTag names = new ListTag();
		subscribers.forEach(name -> names.add(StringTag.valueOf(name)));
		tag.put("subscribers", names);
		tag.putBoolean("interference", interference);
		tag.putString("problem", problem);
		ListTag dishes = new ListTag();
		for(DishInfo info : dishInfo) {
			CompoundTag dish = new CompoundTag();
			dish.putLong("pos", info.pos().asLong());
			dish.putFloat("azimuth", info.azimuth());
			dish.putFloat("elevation", info.elevation());
			dish.putBoolean("horn", info.horn());
			dishes.add(dish);
		}
		tag.put("dishes", dishes);
		tag.putFloat("rssi", rssi);
		tag.put("rssiHistory", new net.minecraft.nbt.IntArrayTag(toTenths(history)));
		tag.putString("alignName", alignName);
		tag.putString("alignReason", alignReason);
		return tag;
	}

	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public @NotNull Component getDisplayName() {
		return Component.translatable("container.radiocraft.microwave_radio");
	}

	@Override
	public @Nullable AbstractContainerMenu createMenu(int id, @NotNull Inventory inventory, @NotNull Player player) {
		return new MicrowaveRadioMenu(id, this);
	}

	/** Unused here; kept so tests can find radios by identity. */
	public static Map<String, MicrowaveRadioBlockEntity> byIdentity(Level level) {
		Map<String, MicrowaveRadioBlockEntity> out = new HashMap<>();
		for(MicrowaveRadioBlockEntity radio : loaded(level))
			out.put(radio.identity(), radio);
		return out;
	}

	/** Where this radio is, for identities. */
	public GlobalPos globalPos() {
		return MicrowaveIdentities.at(level, worldPosition);
	}
}
