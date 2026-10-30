package com.arrl.radiocraft.common.blockentities;

import com.arrl.radiocraft.RadiocraftServerConfig;
import com.arrl.radiocraft.api.capabilities.PlayerCallsignData;
import com.arrl.radiocraft.common.blockentities.radio.RadioBlockEntity;
import com.arrl.radiocraft.common.data.PlayerCallsignSavedData;
import com.arrl.radiocraft.common.init.RadiocraftBlockEntities;
import com.arrl.radiocraft.common.menus.RadioInterfaceMenu;
import com.arrl.radiocraft.common.radio.data.TncPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * The radio interface (a TNC, the packet modem): placed against one or more radios, it turns packets into tones on
 * them and back. It holds the station's callsign and a transmit switch, and digipeats (a packet heard goes back out a
 * second later on every radio next to it, once per packet, up to {@link RadiocraftServerConfig#TNC_DIGIPEAT_MAX_HOPS}
 * hops, except messages for this station). What it decodes goes to the terminals on its {@link StationBus}. It needs
 * no power of its own, but the radios it uses must be on.
 */
public class RadioInterfaceBlockEntity extends BlockEntity implements MenuProvider {

	/** Ticks the RPT lamp stays lit after a repeat is due to go out (the burst itself takes about half a second). */
	private static final int REPEAT_LAMP_TICKS = 20;

	private String callsign = "";
	private String callsignSetBy = "";
	private boolean transmitEnabled = true;
	private boolean digipeatEnabled = configDefault(RadiocraftServerConfig.APRS_DIGIPEAT_DEFAULT);
	private long repeatLampUntilGameTime = -1L;
	private boolean syncPending = false;

	// Server only, not saved: what has gone through here (so nothing repeats twice) and repeats waiting their turn.
	private final SeenPackets seen = new SeenPackets();
	private final List<PendingDigipeat> digipeats = new ArrayList<>();

	private record PendingDigipeat(TncPacket packet, long dueGameTime) {}

	public RadioInterfaceBlockEntity(BlockPos pos, BlockState state) {
		super(RadiocraftBlockEntities.RADIO_INTERFACE.get(), pos, state);
	}

	public static <T extends BlockEntity> void tick(Level level, BlockPos pos, BlockState state, T t) {
		if(t instanceof RadioInterfaceBlockEntity radioInterface && level instanceof ServerLevel serverLevel)
			radioInterface.serverTick(serverLevel);
	}

	private void serverTick(ServerLevel level) {
		long now = level.getGameTime();
		if(syncPending && now % 2 == 0) {
			syncPending = false;
			BlockState state = getBlockState();
			level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
		}
		Iterator<PendingDigipeat> it = digipeats.iterator();
		while(it.hasNext()) {
			PendingDigipeat pending = it.next();
			if(now < pending.dueGameTime())
				continue;
			it.remove();
			for(RadioBlockEntity radio : getRadios()) // Back out on every radio, the one it came in on too.
				radio.sendData(pending.packet());
		}
	}

	// -------------------- RADIOS --------------------

	/** Radios on any side of this interface, or on its data wire. */
	public List<RadioBlockEntity> getRadios() {
		if(level == null)
			return List.of();
		return com.arrl.radiocraft.common.be_networks.DataWire.linked(level, worldPosition, RadioBlockEntity.class);
	}

	/**
	 * Sends a packet out through every radio next to it that is on. False if transmitting is switched off or no radio
	 * takes it.
	 */
	public boolean transmit(TncPacket packet) {
		seen.remember(packet.id()); // Our own packet coming back from a digipeater is not news.
		if(!transmitEnabled)
			return false;
		boolean sent = false;
		for(RadioBlockEntity radio : getRadios())
			sent |= radio.sendData(packet);
		return sent;
	}

	/** True if any radio next to this interface is on and tuned within 1 kHz of {@code hz}. */
	public boolean isListeningOn(float hz) {
		for(RadioBlockEntity radio : getRadios())
			if(radio.isPowered() && Math.abs(radio.getFrequency() - hz) < 1000.0F)
				return true;
		return false;
	}

	/** True if a VHF radio next to this interface is switched on. */
	public boolean hasVhfRadioOn() {
		for(RadioBlockEntity radio : getRadios())
			if(radio.isPowered() && radio.getBand() != null && radio.getBand().isVhf())
				return true;
		return false;
	}

	// -------------------- RECEIVING --------------------

	/** A packet one of the radios next to it decoded: passed to the terminals on the bus if it is news, and digipeated. */
	public void receive(RadioBlockEntity radio, TncPacket packet) {
		if(level == null || !seen.remember(packet.id()))
			return;
		if(shouldDigipeat(packet)) {
			long due = level.getGameTime() + RadiocraftServerConfig.TNC_DIGIPEAT_DELAY_TICKS.get();
			digipeats.add(new PendingDigipeat(packet.digipeated(), due));
			repeatLampUntilGameTime = due + REPEAT_LAMP_TICKS; // The RPT lamp: from hearing it until it has gone back out.
			setChanged();
			sync();
		}
		for(StationTerminal terminal : StationBus.find(level, worldPosition).terminals())
			terminal.receive(packet);
	}

	/**
	 * True if this interface repeats {@code packet}: digipeating and transmitting are on, it is a beacon, message or ack,
	 * it has been through fewer than {@link RadiocraftServerConfig#TNC_DIGIPEAT_MAX_HOPS} digipeaters, and it is not a
	 * message or ack for this station.
	 */
	private boolean shouldDigipeat(TncPacket packet) {
		if(!digipeatEnabled || !transmitEnabled || packet.hops() >= RadiocraftServerConfig.TNC_DIGIPEAT_MAX_HOPS.get())
			return false;
		if(packet.kind() == TncPacket.Kind.BEACON)
			return true;
		return (packet.kind() == TncPacket.Kind.MESSAGE || packet.kind() == TncPacket.Kind.ACK) && (callsign.isEmpty() || !packet.to().equals(callsign));
	}

	/** True while the RPT lamp is lit: a packet heard is waiting to go back out, or just went. */
	public boolean isRepeating() {
		return level != null && level.getGameTime() < repeatLampUntilGameTime;
	}

	// -------------------- SETTINGS --------------------

	/** A config's value for a new block, or true if the config is not loaded yet. */
	static boolean configDefault(net.neoforged.neoforge.common.ModConfigSpec.ConfigValue<Boolean> value) {
		try {
			return value.get();
		}
		catch(IllegalStateException e) {
			return true;
		}
	}

	/** Sets this station to {@code player}'s callsign. False (and nothing changes) if they have none. */
	public boolean claim(ServerPlayer player) {
		PlayerCallsignData data = PlayerCallsignSavedData.get(player.serverLevel().getServer().overworld()).getCallsignData(player.getUUID());
		if(data == null || data.callsign() == null || data.callsign().isBlank())
			return false;
		setCallsign(data.callsign(), player.getGameProfile().getName());
		return true;
	}

	/** Gives the station its own callsign, remembering who set it. */
	public void setCallsign(String value, String setBy) {
		callsign = TncPacket.sanitizeCallsign(value);
		callsignSetBy = setBy == null ? "" : setBy;
		setChanged();
		sync();
	}

	public void setTransmitEnabled(boolean value) {
		transmitEnabled = value;
		setChanged();
		sync();
	}

	public void setDigipeatEnabled(boolean value) {
		digipeatEnabled = value;
		setChanged();
		sync();
	}

	public String getCallsign() {
		return callsign;
	}

	public String getCallsignSetBy() {
		return callsignSetBy;
	}

	public boolean isTransmitEnabled() {
		return transmitEnabled;
	}

	public boolean isDigipeatEnabled() {
		return digipeatEnabled;
	}

	// -------------------- MENU --------------------

	@Override
	public @NotNull Component getDisplayName() {
		return Component.translatable("container.radiocraft.radio_interface");
	}

	@Nullable
	@Override
	public AbstractContainerMenu createMenu(int id, @NotNull Inventory playerInventory, @NotNull Player player) {
		return new RadioInterfaceMenu(id, this);
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
		tag.putString("callsign", callsign);
		tag.putString("callsignSetBy", callsignSetBy);
		tag.putBoolean("transmit", transmitEnabled);
		tag.putBoolean("digipeat", digipeatEnabled);
		tag.putLong("repeatLampUntil", repeatLampUntilGameTime);
	}

	@Override
	protected void loadAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		callsign = TncPacket.sanitizeCallsign(tag.getString("callsign"));
		callsignSetBy = tag.getString("callsignSetBy");
		transmitEnabled = !tag.contains("transmit") || tag.getBoolean("transmit");
		digipeatEnabled = tag.getBoolean("digipeat");
		repeatLampUntilGameTime = tag.contains("repeatLampUntil") ? tag.getLong("repeatLampUntil") : -1L;
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
