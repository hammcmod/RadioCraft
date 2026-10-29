package com.arrl.radiocraft.common.blockentities;

import com.arrl.radiocraft.RadiocraftServerConfig;
import com.arrl.radiocraft.api.antenna.IAntennaPacket;
import com.arrl.radiocraft.common.blocks.DirectionFinderBlock;
import com.arrl.radiocraft.common.blocks.DirectionFinderModuleBlock;
import com.arrl.radiocraft.common.init.RadiocraftBlockEntities;
import com.arrl.radiocraft.common.menus.DirectionFinderMenu;
import com.arrl.radiocraft.common.radio.Band;
import com.arrl.radiocraft.common.radio.DirectionFinding;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * A direction finder: an amber terminal tuned to one frequency, with three to eight antenna modules
 * ({@link DirectionFinderModuleBlockEntity}) touching it or each other. Every module needs an antenna of exactly the
 * same type as the first one's; only the first (the reference module) is listened to, the others stand for the array
 * the bearing is measured with. While a signal comes in on the frequency the modules lock on one at a time, and from
 * three locked the set gives a bearing to the transmitter, better with every module past three
 * ({@link DirectionFinding#sigma}). Bearings typed in from other direction finders make a fix (worked out on the
 * screen, see {@link DirectionFinding#triangulate}); the map enabled direction finder draws it on a map.
 */
public class DirectionFinderBlockEntity extends BlockEntity implements MenuProvider, com.arrl.radiocraft.api.benetworks.INetworkObjectProvider {

	/** Ticks of signal each module takes to lock on, one after the other. */
	public static final int LOCK_TICKS = 50;
	/** Ticks between the notes of the all-locked chime. */
	private static final int CHIME_GAP = 4;
	/** Ticks without a signal after which locks start to drop, one a second. */
	public static final int HOLD_TICKS = 100;
	/** A signal heard within this many ticks still counts as coming in. */
	public static final int HEARD_TICKS = 10;
	/** Weakest signal that moves the needle. */
	public static final double MIN_SIGNAL = 0.01D;
	/** Bearings typed in from other stations, at most. */
	public static final int MAX_ENTRIES = 6;

	/** What a module is doing, for its lamp and the screen. */
	public enum ModuleStatus { NO_ANTENNA, MISMATCH, READY, LOCKED }

	/** A bearing typed in from another direction finder. */
	public record Entry(int x, int z, double bearing, double sigma) {
		CompoundTag save() {
			CompoundTag tag = new CompoundTag();
			tag.putInt("x", x);
			tag.putInt("z", z);
			tag.putDouble("bearing", bearing);
			tag.putDouble("sigma", sigma);
			return tag;
		}

		static Entry load(CompoundTag tag) {
			return new Entry(tag.getInt("x"), tag.getInt("z"), tag.getDouble("bearing"), tag.getDouble("sigma"));
		}

		public DirectionFinding.Line line() {
			return new DirectionFinding.Line(x + 0.5D, z + 0.5D, bearing, sigma);
		}
	}

	// Saved.
	private boolean on = false;
	private float frequency = 14_000_000.0F;
	private boolean bootPending = false;
	private final List<Entry> entries = new ArrayList<>();

	/** A bearing from a direction finder at the other end of a microwave link: who sent it, and the line. */
	public record Linked(String from, DirectionFinding.Line line) {}

	// Synced to the client.
	/** Whether the power wire feeds it this tick; the screen stays dark without it. */
	private boolean powered = false;
	private final List<Linked> linked = new ArrayList<>();
	private final List<BlockPos> modules = new ArrayList<>();
	private final List<ModuleStatus> statuses = new ArrayList<>();
	@Nullable
	private ResourceLocation antennaType = null;
	private int locked = 0;
	private double bearing = Double.NaN;
	private double sigma = Double.NaN;
	private boolean hearing = false;
	private double meter = 0.0D;

	// Server only.
	private int lockTicks = 0;
	private int quietTicks = 0;
	private double errorOffset = 0.0D;
	@Nullable
	private BlockPos lockedSource = null;
	private boolean dirty = false;
	private final Set<BlockPos> claimed = new HashSet<>();
	/** When the all-locked chime started, -1 for none playing. */
	private long chimeAt = -1L;

	// Written on the voice chat thread by the reference module, read on the game thread.
	@Nullable
	private volatile BlockPos reference = null;
	private volatile long heardAt = -1L;
	private volatile double heardStrength = 0.0D;
	@Nullable
	private volatile BlockPos heardFrom = null;
	private volatile boolean heardSkip = false;

	public DirectionFinderBlockEntity(BlockPos pos, BlockState state) {
		super(RadiocraftBlockEntities.DIRECTION_FINDER.get(), pos, state);
	}

	public boolean hasMap() {
		return getBlockState().getBlock() instanceof DirectionFinderBlock block && block.hasMap();
	}

	// -------------------- RECEIVING --------------------

	/**
	 * A module's antenna heard a signal (any thread). Only the reference module counts, and only on the frequency
	 * (within half an SSB channel on HF, a channel step on 2m).
	 */
	public void hear(BlockPos module, IAntennaPacket packet) {
		if(!isOn() || !module.equals(reference) || level == null)
			return;
		Band band = packet.getBand();
		double tolerance = band != null && band.isVhf() ? RadiocraftServerConfig.VHF_FREQUENCY_STEP.get() / 2.0D : 1500.0D;
		if(Math.abs(packet.getFrequency() - frequency) > tolerance || packet.getStrength() < MIN_SIGNAL)
			return;
		if(packet.getSource() == null || packet.getSource().getAntennaPos() == null)
			return;
		BlockPos from = packet.getSource().getAntennaPos().world(); // Where it really is (it may be on a Sable ship).
		heardFrom = from;
		heardStrength = packet.getStrength();
		heardSkip = band != null && from.distSqr(here()) > (double)band.losRange() * band.losRange();
		heardAt = level.getGameTime();
	}

	// -------------------- TICK --------------------

	public static void tick(Level level, BlockPos pos, BlockState state, BlockEntity blockEntity) {
		if(level instanceof ServerLevel serverLevel && blockEntity instanceof DirectionFinderBlockEntity finder)
			finder.serverTick(serverLevel);
	}

	private void sound(SoundEvent sound, float pitch, float volume) {
		level.playSound(null, worldPosition, sound, SoundSource.BLOCKS, volume, pitch);
	}

	/** Every module locked: DING ding DING, high, low, high. */
	private void playChime(long now) {
		if(chimeAt < 0L)
			return;
		long step = now - chimeAt;
		if(step % CHIME_GAP == 0L)
			sound(SoundEvents.NOTE_BLOCK_BELL.value(), step == CHIME_GAP ? 1.0F : 1.5F, step == CHIME_GAP ? 0.3F : 0.45F);
		if(step >= 2L * CHIME_GAP)
			chimeAt = -1L;
	}

	private void serverTick(ServerLevel serverLevel) {
		long now = serverLevel.getGameTime();
		refreshPower();
		if((now + worldPosition.asLong()) % 10L == 0L)
			refreshModules(serverLevel);
		if((now + worldPosition.asLong()) % 20L == 0L)
			refreshLinked(serverLevel);

		int usable = usableModules();
		boolean nowHearing = isOn() && heardAt >= 0 && now - heardAt <= HEARD_TICKS && reference != null;
		if(nowHearing != hearing) {
			hearing = nowHearing;
			dirty = true;
			if(hearing)
				sound(SoundEvents.NOTE_BLOCK_BIT.value(), 1.4F, 0.4F); // Signal found.
		}
		if(!isOn()) {
			if(locked != 0 || !Double.isNaN(bearing)) {
				locked = 0;
				bearing = sigma = Double.NaN;
				dirty = true;
			}
		}
		else if(hearing) {
			quietTicks = 0;
			BlockPos from = heardFrom;
			if(from != null && (lockedSource == null || lockedSource.distSqr(from) > 24 * 24)) { // Another station: start over.
				lockedSource = from;
				locked = 0;
				lockTicks = 0;
				dirty = true;
			}
			if(locked < usable && ++lockTicks >= LOCK_TICKS) {
				locked++;
				lockTicks = 0;
				dirty = true;
				sound(SoundEvents.NOTE_BLOCK_BIT.value(), 0.6F, 0.35F); // A low beep and the click of the lock.
				sound(SoundEvents.LEVER_CLICK, 1.8F, 0.25F);
				if(locked == usable)
					chimeAt = now;
				double s = DirectionFinding.sigma(locked, heardSkip, heardStrength < 0.15D);
				errorOffset = Double.isInfinite(s) ? 0.0D : serverLevel.random.nextGaussian() * s;
				sigma = s;
			}
			if(locked >= DirectionFinding.MIN_MODULES && from != null) { // Follows a moving station, with this lock's error.
				BlockPos here = here();
				double fresh = DirectionFinding.normalize(DirectionFinding.bearing(here.getX() + 0.5D, here.getZ() + 0.5D,
						from.getX() + 0.5D, from.getZ() + 0.5D) + errorOffset);
				if(Double.isNaN(bearing) || DirectionFinding.difference(fresh, bearing) >= 0.5D) {
					bearing = fresh;
					dirty = true;
				}
			}
		}
		else if(locked > 0 && ++quietTicks > HOLD_TICKS && quietTicks % 20 == 0) {
			locked--; // Locks drop away in the quiet; the last bearing stays on the screen.
			dirty = true;
		}
		if(locked > usable) {
			locked = usable;
			dirty = true;
		}

		playChime(now);

		double newMeter = hearing ? Math.min(1.0D, heardStrength) : 0.0D;
		if(Math.abs(newMeter - meter) >= 0.05D || (newMeter == 0.0D) != (meter == 0.0D)) {
			meter = newMeter;
			dirty = true;
		}

		updateLamps(serverLevel);
		updateDisplay(serverLevel);
		if(dirty && now % 4L == 0L) {
			dirty = false;
			setChanged();
			BlockState state = getBlockState();
			serverLevel.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
		}
	}

	@Override
	public void onLoad() {
		super.onLoad();
		if(level != null && !level.isClientSide)
			getNetworkObject(level, worldPosition); // On the power wire: its supply, and a microwave radio can serve it there.
	}

	@Override
	public com.arrl.radiocraft.api.benetworks.BENetworkObject createNetworkObject() {
		return new com.arrl.radiocraft.common.be_networks.network_objects.DirectionFinderNetworkObject(level, worldPosition);
	}

	@Override
	public void onNetworkObjectCreated(Level level, BlockPos pos, com.arrl.radiocraft.api.benetworks.BENetworkObject networkObject) {
		if(networkObject instanceof com.arrl.radiocraft.common.be_networks.network_objects.DirectionFinderNetworkObject finder)
			finder.wantsOn = on; // The POWER button is saved here, not on the object.
	}

	/** The network object, if the capability has it (always on a loaded server block entity). */
	@Nullable
	private com.arrl.radiocraft.common.be_networks.network_objects.DirectionFinderNetworkObject networkObject() {
		if(level == null || level.isClientSide)
			return null;
		return getNetworkObject(level, worldPosition) instanceof com.arrl.radiocraft.common.be_networks.network_objects.DirectionFinderNetworkObject finder ? finder : null;
	}

	/**
	 * Whether the wire feeds it this tick (see {@link com.arrl.radiocraft.common.be_networks.network_objects.DirectionFinderNetworkObject}): switched on with
	 * nothing on the wire it stays dark, like a radio with no battery.
	 */
	private void refreshPower() {
		com.arrl.radiocraft.common.be_networks.network_objects.DirectionFinderNetworkObject finder = networkObject();
		boolean now = finder != null && finder.isPowered;
		if(now != powered) {
			powered = now;
			dirty = true;
		}
	}

	/**
	 * Bearings from every direction finder this one reaches over microwave (relayed through other sites,
	 * {@code MicrowaveRoutes}), tuned to the same frequency: added to the fix as if typed in. With interference on the
	 * way a bearing may not get through this time; the last one stays until it does.
	 */
	private void refreshLinked(ServerLevel serverLevel) {
		List<Linked> now = new ArrayList<>();
		for(com.arrl.radiocraft.common.radio.microwave.MicrowaveRoutes.Route route : com.arrl.radiocraft.common.radio.microwave.MicrowaveRoutes.from(serverLevel, worldPosition)) {
			if(!(route.station() instanceof DirectionFinderBlockEntity other) || other == this || !other.isOn() || !other.hasBearing()
					|| Math.abs(other.getFrequency() - frequency) >= 1.0F)
				continue;
			String from = route.last().identity();
			Linked previous = linked.stream().filter(l -> l.from().equals(from)).findFirst().orElse(null);
			if(!route.getsThrough() && previous != null) {
				now.add(previous);
				continue;
			}
			BlockPos at = other.here();
			now.add(new Linked(from, new DirectionFinding.Line(at.getX() + 0.5D, at.getZ() + 0.5D, other.getBearing(), other.getSigma())));
		}
		if(!now.equals(linked)) {
			linked.clear();
			linked.addAll(now);
			dirty = true;
		}
	}

	/** Bearings received over microwave links. */
	public List<Linked> getLinked() {
		return List.copyOf(linked);
	}

	/** The modules attached: touching this or an attached module, nearest first, up to {@link DirectionFinding#MAX_MODULES}. */
	private void refreshModules(ServerLevel serverLevel) {
		List<BlockPos> found = new ArrayList<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		Set<BlockPos> seen = new HashSet<>();
		queue.add(worldPosition);
		seen.add(worldPosition);
		while(!queue.isEmpty() && found.size() < DirectionFinding.MAX_MODULES) {
			BlockPos at = queue.poll();
			for(Direction direction : Direction.values()) {
				BlockPos next = at.relative(direction);
				if(!seen.add(next) || !serverLevel.isLoaded(next))
					continue;
				if(!(serverLevel.getBlockEntity(next) instanceof DirectionFinderModuleBlockEntity module))
					continue;
				BlockPos owner = module.getFinder();
				if(owner != null && !owner.equals(worldPosition) && serverLevel.isLoaded(owner)
						&& serverLevel.getBlockEntity(owner) instanceof DirectionFinderBlockEntity other && other.claimed.contains(next))
					continue; // Another direction finder's.
				if(found.size() < DirectionFinding.MAX_MODULES) {
					found.add(next);
					queue.add(next);
				}
			}
		}

		// Let go of modules no longer attached, and claim the new ones.
		for(BlockPos old : claimed)
			if(!found.contains(old) && serverLevel.isLoaded(old) && serverLevel.getBlockEntity(old) instanceof DirectionFinderModuleBlockEntity module
					&& worldPosition.equals(module.getFinder())) {
				module.setFinder(null);
				setLamp(serverLevel, old, DirectionFinderModuleBlock.Lamp.OFF);
			}
		claimed.clear();
		claimed.addAll(found);

		ResourceLocation type = null;
		List<ModuleStatus> newStatuses = new ArrayList<>();
		List<ResourceLocation> types = new ArrayList<>();
		for(BlockPos at : found) {
			DirectionFinderModuleBlockEntity module = (DirectionFinderModuleBlockEntity)Objects.requireNonNull(serverLevel.getBlockEntity(at));
			module.setFinder(worldPosition);
			ResourceLocation moduleType = module.getAntennaType();
			types.add(moduleType);
			if(type == null)
				type = moduleType; // The first module with an antenna sets the type the rest must match.
		}
		for(ResourceLocation moduleType : types)
			newStatuses.add(moduleType == null ? ModuleStatus.NO_ANTENNA : moduleType.equals(type) ? ModuleStatus.READY : ModuleStatus.MISMATCH);
		int first = newStatuses.indexOf(ModuleStatus.READY); // The one actually listened to.
		reference = first < 0 ? null : found.get(first);

		if(!found.equals(modules) || !newStatuses.equals(statusesWithoutLocks()) || !Objects.equals(type, antennaType)) {
			modules.clear();
			modules.addAll(found);
			statuses.clear();
			statuses.addAll(newStatuses);
			antennaType = type;
			dirty = true;
		}
	}

	private List<ModuleStatus> statusesWithoutLocks() {
		return statuses.stream().map(status -> status == ModuleStatus.LOCKED ? ModuleStatus.READY : status).toList();
	}

	/** Modules with the right antenna: as many as can lock on. None while the reference module cannot hear. */
	public int usableModules() {
		if(reference == null)
			return 0;
		return (int)statuses.stream().filter(status -> status == ModuleStatus.READY || status == ModuleStatus.LOCKED).count();
	}

	/** Green for each locked module (the first ones that are ready), red for one without the right antenna. */
	private void updateLamps(ServerLevel serverLevel) {
		int lit = 0;
		for(int i = 0; i < modules.size(); i++) {
			ModuleStatus status = statuses.get(i);
			if(status == ModuleStatus.READY || status == ModuleStatus.LOCKED) {
				ModuleStatus wanted = isOn() && lit < locked ? ModuleStatus.LOCKED : ModuleStatus.READY;
				if(wanted == ModuleStatus.LOCKED)
					lit++;
				if(wanted != status) {
					statuses.set(i, wanted);
					dirty = true;
				}
			}
			DirectionFinderModuleBlock.Lamp lamp = !isOn() ? DirectionFinderModuleBlock.Lamp.OFF
					: statuses.get(i) == ModuleStatus.LOCKED ? DirectionFinderModuleBlock.Lamp.GREEN
					: statuses.get(i) == ModuleStatus.READY ? DirectionFinderModuleBlock.Lamp.OFF
					: DirectionFinderModuleBlock.Lamp.RED;
			setLamp(serverLevel, modules.get(i), lamp);
		}
	}

	private static void setLamp(ServerLevel serverLevel, BlockPos at, DirectionFinderModuleBlock.Lamp lamp) {
		BlockState state = serverLevel.getBlockState(at);
		if(state.getBlock() instanceof DirectionFinderModuleBlock && state.getValue(DirectionFinderModuleBlock.LAMP) != lamp)
			serverLevel.setBlock(at, state.setValue(DirectionFinderModuleBlock.LAMP, lamp), Block.UPDATE_CLIENTS);
	}

	private void updateDisplay(ServerLevel serverLevel) {
		BlockState state = getBlockState();
		if(!(state.getBlock() instanceof DirectionFinderBlock))
			return;
		DirectionFinderBlock.Display wanted = !isOn() ? DirectionFinderBlock.Display.OFF
				: hasBearing() ? DirectionFinderBlock.Display.COMPASS : DirectionFinderBlock.Display.ON;
		if(state.getValue(DirectionFinderBlock.DISPLAY) != wanted)
			serverLevel.setBlock(worldPosition, state.setValue(DirectionFinderBlock.DISPLAY, wanted), Block.UPDATE_CLIENTS);
	}

	// -------------------- CONTROLS (FROM THE SCREEN) --------------------

	public void togglePower() {
		on = !on;
		bootPending = on;
		locked = 0;
		lockTicks = 0;
		lockedSource = null;
		bearing = sigma = Double.NaN;
		com.arrl.radiocraft.common.be_networks.network_objects.DirectionFinderNetworkObject finder = networkObject();
		if(finder != null) {
			finder.wantsOn = on;
			finder.resolvePower(); // So the screen sees the result without waiting a tick.
			powered = finder.isPowered;
		}
		markDirtyNow();
	}

	public void markBooted() {
		if(bootPending) {
			bootPending = false;
			markDirtyNow();
		}
	}

	/** Moves the frequency {@code steps} steps (the band's step: 1 kHz on HF, a channel on 2m), within its band. */
	public void tune(int steps) {
		Band band = bandOf(frequency);
		int step = band.isVhf() ? RadiocraftServerConfig.VHF_FREQUENCY_STEP.get() : RadiocraftServerConfig.HF_FREQUENCY_STEP.get();
		setFrequency(com.arrl.radiocraft.common.blockentities.radio.RadioBlockEntity.snapToStep(band, frequency, step, steps));
	}

	/** To the next band up ({@code direction} 1) or down (-1), at its bottom edge. */
	public void changeBand(int direction) {
		List<Band> bands = bands();
		int index = bands.indexOf(bandOf(frequency));
		Band next = bands.get(Math.floorMod(index + direction, bands.size()));
		setFrequency(next.minFrequency());
	}

	/** Tunes straight to {@code hertz} if it is inside a band; returns whether it was. */
	public boolean setFrequency(float hertz) {
		for(Band band : bands())
			if(hertz >= band.minFrequency() && hertz <= band.maxFrequency()) {
				frequency = hertz;
				locked = 0;
				lockTicks = 0;
				lockedSource = null;
				bearing = sigma = Double.NaN;
				markDirtyNow();
				return true;
			}
		return false;
	}

	public boolean addEntry(Entry entry) {
		if(entries.size() >= MAX_ENTRIES || !Double.isFinite(entry.bearing()) || entry.sigma() <= 0.0D)
			return false;
		entries.add(new Entry(entry.x(), entry.z(), DirectionFinding.normalize(entry.bearing()), Math.min(45.0D, entry.sigma())));
		markDirtyNow();
		return true;
	}

	public void removeEntry(int index) {
		if(index >= 0 && index < entries.size()) {
			entries.remove(index);
			markDirtyNow();
		}
	}

	private void markDirtyNow() {
		setChanged();
		if(level != null && !level.isClientSide) {
			BlockState state = getBlockState();
			level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
		}
	}

	// -------------------- READING (BOTH SIDES) --------------------

	/** The bands a direction finder covers, lowest first. Static data, so the client can use it too. */
	public static List<Band> bands() {
		return Band.getDefaults().stream().sorted(Comparator.comparingDouble(Band::minFrequency)).toList();
	}

	public static Band bandOf(float hertz) {
		List<Band> bands = bands();
		for(Band band : bands)
			if(hertz >= band.minFrequency() && hertz <= band.maxFrequency())
				return band;
		return bands.getFirst();
	}

	/** Running: switched on and fed by its power wire (see {@link #isSwitchedOn}). */
	public boolean isOn() {
		return on && powered;
	}

	/** The POWER button, whatever the wire is doing. */
	public boolean isSwitchedOn() {
		return on;
	}

	/** True while the wire feeds it (meaningful with {@link #isSwitchedOn}). */
	public boolean isPowered() {
		return powered;
	}

	public boolean isBootPending() {
		return bootPending;
	}

	public float getFrequency() {
		return frequency;
	}

	public List<BlockPos> getModules() {
		return List.copyOf(modules);
	}

	public List<ModuleStatus> getStatuses() {
		return List.copyOf(statuses);
	}

	@Nullable
	public ResourceLocation getAntennaType() {
		return antennaType;
	}

	public int getLocked() {
		return locked;
	}

	public boolean isHearing() {
		return hearing;
	}

	/** Received signal, 0 to 1, for the meter. */
	public double getMeter() {
		return meter;
	}

	/** True once three modules have locked on (the bearing may be the last one, held after the signal stopped). */
	public boolean hasBearing() {
		return !Double.isNaN(bearing);
	}

	/** The bearing, compass degrees, NaN with none. */
	public double getBearing() {
		return bearing;
	}

	/** How good the bearing is: degrees, one standard deviation. */
	public double getSigma() {
		return sigma;
	}

	public List<Entry> getEntries() {
		return List.copyOf(entries);
	}

	/** Where this direction finder really is: its block, or its place in the world when on a Sable ship. */
	public BlockPos here() {
		return com.arrl.radiocraft.compat.sable.Sublevels.world(level, worldPosition);
	}

	/** This station's bearing line, plus every one typed in: what the fix is made from. */
	public List<DirectionFinding.Line> lines() {
		List<DirectionFinding.Line> lines = new ArrayList<>();
		if(hasBearing()) {
			BlockPos here = here();
			lines.add(new DirectionFinding.Line(here.getX() + 0.5D, here.getZ() + 0.5D, bearing, sigma));
		}
		for(Entry entry : entries)
			lines.add(entry.line());
		for(Linked from : linked)
			lines.add(from.line());
		return lines;
	}

	// -------------------- SAVE, SYNC, MENU --------------------

	@Override
	public void setRemoved() {
		super.setRemoved();
		if(level != null && !level.isClientSide) // Free the modules for another direction finder.
			for(BlockPos at : claimed)
				if(level.isLoaded(at) && level.getBlockEntity(at) instanceof DirectionFinderModuleBlockEntity module && worldPosition.equals(module.getFinder()))
					module.setFinder(null);
	}

	@Override
	protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
		super.saveAdditional(tag, registries);
		tag.putBoolean("on", on);
		tag.putFloat("frequency", frequency);
		tag.putBoolean("bootPending", bootPending);
		ListTag list = new ListTag();
		for(Entry entry : entries)
			list.add(entry.save());
		tag.put("entries", list);
	}

	@Override
	protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
		super.loadAdditional(tag, registries);
		on = tag.getBoolean("on");
		if(tag.contains("frequency"))
			frequency = tag.getFloat("frequency");
		bootPending = tag.getBoolean("bootPending");
		entries.clear();
		for(Tag entry : tag.getList("entries", Tag.TAG_COMPOUND))
			entries.add(Entry.load((CompoundTag)entry));
		if(tag.contains("modules")) { // The client's copy of what the server sees.
			modules.clear();
			for(long at : tag.getLongArray("modules"))
				modules.add(BlockPos.of(at));
			statuses.clear();
			for(byte status : tag.getByteArray("statuses"))
				statuses.add(ModuleStatus.values()[Math.max(0, Math.min(ModuleStatus.values().length - 1, status))]);
			antennaType = tag.contains("antennaType") ? ResourceLocation.tryParse(tag.getString("antennaType")) : null;
			locked = tag.getInt("locked");
			bearing = tag.getDouble("bearing");
			sigma = tag.getDouble("sigma");
			hearing = tag.getBoolean("hearing");
			meter = tag.getDouble("meter");
			powered = tag.getBoolean("powered");
			linked.clear();
			for(Tag entry : tag.getList("linked", Tag.TAG_COMPOUND)) {
				CompoundTag line = (CompoundTag)entry;
				linked.add(new Linked(line.getString("from"), new DirectionFinding.Line(line.getDouble("x"), line.getDouble("z"), line.getDouble("bearing"), line.getDouble("sigma"))));
			}
		}
	}

	@Override
	public @NotNull CompoundTag getUpdateTag(@NotNull HolderLookup.Provider registries) {
		CompoundTag tag = saveWithoutMetadata(registries);
		tag.putLongArray("modules", modules.stream().mapToLong(BlockPos::asLong).toArray());
		byte[] bytes = new byte[statuses.size()];
		for(int i = 0; i < bytes.length; i++)
			bytes[i] = (byte)statuses.get(i).ordinal();
		tag.putByteArray("statuses", bytes);
		if(antennaType != null)
			tag.putString("antennaType", antennaType.toString());
		tag.putInt("locked", locked);
		tag.putBoolean("powered", powered);
		tag.putDouble("bearing", bearing);
		tag.putDouble("sigma", sigma);
		tag.putBoolean("hearing", hearing);
		tag.putDouble("meter", meter);
		ListTag links = new ListTag();
		for(Linked from : linked) {
			CompoundTag line = new CompoundTag();
			line.putString("from", from.from());
			line.putDouble("x", from.line().x());
			line.putDouble("z", from.line().z());
			line.putDouble("bearing", from.line().bearing());
			line.putDouble("sigma", from.line().sigma());
			links.add(line);
		}
		tag.put("linked", links);
		return tag;
	}

	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public @NotNull Component getDisplayName() {
		return Component.translatable(hasMap() ? "container.radiocraft.map_direction_finder" : "container.radiocraft.direction_finder");
	}

	@Override
	public @Nullable AbstractContainerMenu createMenu(int id, @NotNull Inventory inventory, @NotNull Player player) {
		return new DirectionFinderMenu(id, this);
	}
}
