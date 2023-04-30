package com.arrl.radiocraft.common.blockentities;

import com.arrl.radiocraft.client.blockentity.AbstractRadioBlockEntityClientHandler;
import com.arrl.radiocraft.common.init.RadiocraftAntennaTypes;
import com.arrl.radiocraft.common.radio.AntennaManager;
import com.arrl.radiocraft.common.radio.AntennaNetwork;
import com.arrl.radiocraft.common.radio.Radio;
import com.arrl.radiocraft.common.radio.antenna.Antenna;
import com.arrl.radiocraft.common.radio.antenna.types.DipoleAntennaType.DipoleAntennaData;
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

public abstract class AbstractRadioBlockEntity extends AbstractPowerBlockEntity {

	private Radio radioData; // Acts as a container for voip channel info
	private Antenna<?> antenna; // Multiple antennas will be possible later-- temporarily gives itself a dipole for testing purposes.
	private int receiveUsePower;
	private int transmitUsePower;

	public boolean isReceiving = false; // This is only read clientside to determine the static sounds.

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


	public AbstractRadioBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int receiveUsePower, int transmitUsePower) {
		super(type, pos, state, transmitUsePower, transmitUsePower);
		this.receiveUsePower = receiveUsePower;
		this.transmitUsePower = transmitUsePower;
	}

	public Radio getRadio() {
		if(radioData == null)
			radioData = createRadio();

		return radioData;
	}

	public static <T extends BlockEntity> void tick(Level level, BlockPos pos, BlockState state, T t) {
		if(!level.isClientSide) {
			if(t instanceof AbstractRadioBlockEntity be) {
				Radio radio = be.getRadio();

				if(be.antenna == null) { // For debug purposes all antennas will use a 5 length dipole
					be.antenna = new Antenna<>(RadiocraftAntennaTypes.DIPOLE, pos, new DipoleAntennaData(5, 5));
					AntennaNetwork network = AntennaManager.getNetwork(level);
					network.addAntenna(pos, be.antenna);
					be.antenna.setNetwork(network);
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
			setReceiving(true);
			setTransmitting(true);
		}
	}

	/**
	 * Called when the radio is turned off via the UI or has insufficient power
	 */
	public void powerOff() {
		setReceiving(false);
		setTransmitting(false);
	}


	/**
	 * Toggle the transmit capability, override if making a repeater
	 */
	public void toggleTransmitting() {
		Radio radio = getRadio();
		radio.setTransmitting(!radio.isTransmitting());
		radio.setReceiving(!radio.isTransmitting());
	}

	public void setTransmitting(boolean value) {
		getRadio().setTransmitting(value);
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

	@Override
	public void onLoad() {
		super.onLoad();
		if(level.isClientSide())
			AbstractRadioBlockEntityClientHandler.startRadioStatic(this);
	}

	@Override
	public void setRemoved() {
		super.setRemoved();
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
		return nbt;
	}

	@Override
	public void handleUpdateTag(CompoundTag nbt) {
		isReceiving = nbt.getBoolean("isReceiving");
	}

	private void updateBlock() {
		if(level != null && !level.isClientSide) {
			BlockState state = level.getBlockState(worldPosition);
			level.sendBlockUpdated(worldPosition, state, state, 2);
		}
	}

	/**
	 * Process voice packet to broadcast to other radios
	 */
	public void acceptVoicePacket(de.maxhenkel.voicechat.api.ServerLevel level, short[] rawAudio) {
		Radio radio = getRadio();
		if(radio.isTransmitting()) {
			if(antenna != null)
				antenna.transmitAudioPacket(rawAudio, 10, 1000);
		}
	}

	public abstract Radio createRadio();

}
