package com.arrl.radiocraft.common.blockentities;

import com.arrl.radiocraft.client.blockentity.AbstractRadioBlockEntityClientHandler;
import com.arrl.radiocraft.common.benetworks.BENetwork;
import com.arrl.radiocraft.common.benetworks.BENetwork.BENetworkEntry;
import com.arrl.radiocraft.common.benetworks.power.PowerNetwork;
import com.arrl.radiocraft.common.radio.Radio;
import com.arrl.radiocraft.common.radio.antenna.RadioManager;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public abstract class AbstractRadioBlockEntity extends AbstractPowerBlockEntity implements ITogglableBE {

	private Radio radio; // Acts as a container for voip channel info
	private final List<BENetworkEntry> antennas = Collections.synchronizedList(new ArrayList<>());
	private final int wavelength;
	private int receiveUsePower;
	private int transmitUsePower;
	private boolean shouldOverDraw = false; // Use this for overdraws as voice thread will be the one calling it and game logic should run on server thread.

	private boolean isPowered = false; // Only read clientside for the UI
	private boolean isReceiving = false; // Only read clientside to determine the static sounds.
	private boolean isTransmitting = false; // Only read clientside for the UI

	protected final ContainerData fields = new ContainerData() {
		@Override
		public int get(int index) {
			if(index == 0)
				return receiveUsePower;
			else if(index == 1)
				return transmitUsePower;
			return 0;
		}

		@Override
		public void set(int index, int value) {
			if(index == 0)
				receiveUsePower = value;
			else if(index == 1)
				transmitUsePower = value;
		}

		@Override
		public int getCount() {
			return 2;
		}
	};


	public AbstractRadioBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int receiveUsePower, int transmitUsePower, int wavelength) {
		super(type, pos, state, transmitUsePower, transmitUsePower);
		this.receiveUsePower = receiveUsePower;
		this.transmitUsePower = transmitUsePower;
		this.wavelength = wavelength;
	}

	public Radio getRadio() {
		if(radio == null)
			radio = createRadio();

		return radio;
	}

	public static <T extends BlockEntity> void tick(Level level, BlockPos pos, BlockState state, T t) {
		if(!level.isClientSide) {
			if(t instanceof AbstractRadioBlockEntity be && be.isPowered) {
				Radio radio = be.getRadio();

				if(be.shouldOverDraw) {
					// Overdraw logic here (not being done yet)
					be.shouldOverDraw = false;
				}

				// Radio tick logic doesn't need anything special, all the voice communications are handled outside the tick loop.
				if(radio.isTransmitting()) {
					if(!be.tryConsumePower(be.getTransmitUsePower(), false)) // Turns off if it can't pull enough power for transmission.
						be.powerOff();
				}
				else if(radio.isReceiving()) {
					if(!be.tryConsumePower(be.getReceiveUsePower(), false)) // Turns off if it can't pull enough power for receiving.
						be.powerOff();
				}
			}
		}
	}

	/**
	 * Called when radio is turned on via the UI
	 */
	public void powerOn() {
		if(tryConsumePower(getReceiveUsePower(), true)) {
			setReceiving(false);
			setTransmitting(true);
		}
	}

	/**
	 * Called when the radio is turned off via the UI or has insufficient power
	 */
	public void powerOff() {
		setReceiving(true);
		setTransmitting(false);
	}

	@Override
	public void toggle() {
		isPowered = !isPowered;
		if(!level.isClientSide) {
			if(isPowered)
				powerOn();
			else
				powerOff();
			updateBlock();
		}
	}

	public void setTransmitting(boolean value) {
		getRadio().setTransmitting(value);
		if(isTransmitting != value) {
			isTransmitting = value;
			updateBlock();
		}
	}

	public void setReceiving(boolean value) {
		getRadio().setReceiving(value);
		if(isReceiving != value) {
			isReceiving = value;
			updateBlock();
		}
	}

	public int getReceiveUsePower() {
		return receiveUsePower;
	}

	public int getTransmitUsePower() {
		return transmitUsePower;
	}

	public boolean isReceiving() {
		return isReceiving;
	}

	public boolean isTransmitting() {
		return isTransmitting;
	}

	public boolean isPowered() {
		return isPowered;
	}

	@Override
	public void onLoad() {
		super.onLoad();
		if(level.isClientSide())
			AbstractRadioBlockEntityClientHandler.startRadioStatic(this);
		else {
			RadioManager.addRadio(level, this);
			updateBlock();
		}
	}

	@Override
	protected void saveAdditional(CompoundTag nbt) {
		super.saveAdditional(nbt);
		nbt.putBoolean("isPowered", isPowered);
		nbt.putBoolean("isReceiving", isReceiving);
		nbt.putBoolean("isTransmitting", isTransmitting);
	}

	@Override
	public void load(CompoundTag nbt) {
		super.load(nbt);
		isPowered = nbt.getBoolean("isPowered");
		isReceiving = nbt.getBoolean("isReceiving");
		isTransmitting = nbt.getBoolean("isTransmitting");
	}

	@Override
	public void setRemoved() {
		if(!level.isClientSide)
			RadioManager.removeRadio(level, this);
		super.setRemoved();
	}

	@Override
	public void onChunkUnloaded() {
		if(!level.isClientSide)
			RadioManager.removeRadio(level, this);
		super.onChunkUnloaded();
	}

	@Nullable
	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
		CompoundTag nbt = pkt.getTag();
		handleUpdateTag(nbt);
	}

	@Override
	public CompoundTag getUpdateTag() {
		CompoundTag nbt = new CompoundTag();
		nbt.putBoolean("isReceiving", isReceiving);
		nbt.putBoolean("isTransmitting", isTransmitting);
		nbt.putBoolean("isPowered", isPowered);
		return nbt;
	}

	@Override
	public void handleUpdateTag(CompoundTag nbt) {
		isReceiving = nbt.getBoolean("isReceiving");
		isTransmitting = nbt.getBoolean("isTransmitting");
		isPowered = nbt.getBoolean("isPowered");
	}

	private void updateBlock() {
		if(level != null && !level.isClientSide) {
			BlockState state = level.getBlockState(worldPosition);
			level.sendBlockUpdated(worldPosition, state, state, 2);
		}
	}

	public Radio createRadio() {
		return new Radio(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ());
	}

	@Override
	public void networkUpdated(BENetwork network) {
		super.networkUpdated(network);
		updateConnectedAntennas();
	}

	private void updateConnectedAntennas() {
		antennas.clear();
		for(Set<BENetwork> side : networks.values()) {
			for(BENetwork network : side) {
				if(!(network instanceof PowerNetwork)) {
					for(BENetworkEntry entry : network.getConnections()) {
						if(entry.getNetworkItem() instanceof AntennaBlockEntity)
							antennas.add(entry);
					}
				}
			}
		}
	}

	/**
	 * Mark radio to overdraw in the next tick.
	 */
	public void overdraw() {
		shouldOverDraw = true;
	}

	/**
	 * Process voice packet to broadcast to other radios. Called from voice thread.
	 */
	public void acceptVoicePacket(de.maxhenkel.voicechat.api.ServerLevel level, short[] rawAudio, UUID sourcePlayer) {
		Radio radio = getRadio();
		if(radio.isTransmitting() && isPowered) {
			if(antennas.size() == 1)
				((AntennaBlockEntity)antennas.get(0).getNetworkItem()).transmitAudioPacket(level, rawAudio, wavelength, 1000, sourcePlayer);
			else if(antennas.size() > 1)
				overdraw();
		}
	}

}
