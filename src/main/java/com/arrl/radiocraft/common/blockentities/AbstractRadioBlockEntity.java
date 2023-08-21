package com.arrl.radiocraft.common.blockentities;

import com.arrl.radiocraft.RadiocraftServerConfig;
import com.arrl.radiocraft.client.blockentity.AbstractRadioBlockEntityClientHandler;
import com.arrl.radiocraft.common.benetworks.BENetwork;
import com.arrl.radiocraft.common.benetworks.BENetwork.BENetworkEntry;
import com.arrl.radiocraft.common.benetworks.power.PowerNetwork;
import com.arrl.radiocraft.common.init.RadiocraftData;
import com.arrl.radiocraft.common.radio.Band;
import com.arrl.radiocraft.common.radio.Radio;
import com.arrl.radiocraft.common.radio.RadioManager;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public abstract class AbstractRadioBlockEntity extends AbstractPowerBlockEntity implements ITogglableBE {

	private final List<BENetworkEntry> antennas = Collections.synchronizedList(new ArrayList<>());

	private boolean isPowered = false;
	private boolean ssbEnabled = false;
	private boolean cwEnabled = false;

	private Radio radio; // Acts as a container for voip channel info
	private final int wavelength;
	private final int receiveUsePower;
	private final int transmitUsePower;
	private boolean shouldOverDraw = false; // Use this for overdraws as voice thread will be the one calling it and game logic should run on server thread.

	private int frequency; // Frequency the radio is currently using (in kHz)

	private boolean isReceivingVoice = false; // Only gets read clientside to determine the static sounds.

	private boolean isPTTDown = false; // Used by PTT button packets

	protected final ContainerData fields = new ContainerData() {

		@Override
		public int get(int index) {
			return switch(index) {
				case 0 -> receiveUsePower;
				case 1 -> transmitUsePower;
				case 2 -> frequency;
				default -> 0;
			};
		}

		@Override
		public void set(int index, int value) {
			if(index == 2)
				frequency = value;
		}

		@Override
		public int getCount() {
			return 3;
		}

	};


	public AbstractRadioBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int receiveUsePower, int transmitUsePower, int wavelength) {
		super(type, pos, state, transmitUsePower, transmitUsePower);
		this.receiveUsePower = receiveUsePower;
		this.transmitUsePower = transmitUsePower;
		this.wavelength = wavelength;
		this.frequency = RadiocraftData.BANDS.getValue(wavelength).minFrequency();
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
				if(be.ssbEnabled) {
					if(be.isPTTDown) {
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
	}

	/**
	 * Called when radio is turned on via the UI
	 */
	public void powerOn() {
		if(tryConsumePower(getReceiveUsePower(), true)) {
			setReceivingVoice(ssbEnabled);
		}
	}

	/**
	 * Called when the radio is turned off via the UI or has insufficient power
	 */
	public void powerOff() {
		setReceivingVoice(false);
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
			setChanged();
		}
	}



	public boolean isPowered() {
		return isPowered;
	}

	public int getReceiveUsePower() {
		return receiveUsePower;
	}

	public int getTransmitUsePower() {
		return transmitUsePower;
	}

	public boolean isReceivingVoice() {
		return isReceivingVoice;
	}

	public boolean isPTTDown() {
		return isPTTDown;
	}

	public boolean getSSBEnabled() {
		return ssbEnabled;
	}

	public boolean getCWEnabled() {
		return cwEnabled;
	}

	public int getFrequency() {
		return frequency;
	}



	public void setReceivingVoice(boolean value) {
		getRadio().setReceiving(value);
		if(isReceivingVoice != value) {
			isReceivingVoice = value;
			updateBlock();
		}
	}

	public void setPTTDown(boolean value) {
		if(ssbEnabled)
			setReceivingVoice(!value); // Do not receive voice while attempting to transmit voice.

		if(isPTTDown != value) {
			isPTTDown = value;
			updateBlock();
		}
	}

	public void setSSBEnabled(boolean value) {
		if(value) {
			if(!isPTTDown)
				setReceivingVoice(true);
		}
		else {
			setReceivingVoice(false);
		}

		if(ssbEnabled != value) {
			ssbEnabled = value;
			updateBlock();
			setChanged();
		}
	}

	public void setCWEnabled(boolean value) {
		if(cwEnabled != value) {
			cwEnabled = value;
			updateBlock();
			setChanged();
		};
	}

	public void updateFrequency(int stepCount) {
		Band band = RadiocraftData.BANDS.getValue(wavelength);
		int step = RadiocraftServerConfig.FREQUENCY_STEP.get();
		int min = band.minFrequency();
		int max = (band.maxFrequency() - band.minFrequency()) / step * step + min; // This calc looks weird but it's integer division, throws away remainder to ensure the freq doesn't do a "half step" to max.

		frequency = Mth.clamp(frequency + step * stepCount, min, max);
		setChanged();
	}


	@Override
	protected void saveAdditional(CompoundTag nbt) {
		super.saveAdditional(nbt);
		nbt.putBoolean("isPowered", isPowered);
		nbt.putBoolean("ssbEnabled", ssbEnabled);
		nbt.putBoolean("cwEnabled", cwEnabled);
		nbt.putInt("frequency", frequency);
	}

	@Override
	public void load(CompoundTag nbt) {
		super.load(nbt);
		isPowered = nbt.getBoolean("isPowered");
		ssbEnabled = nbt.getBoolean("ssbEnabled");
		cwEnabled = nbt.getBoolean("cwEnabled");
		frequency = nbt.getInt("frequency");

		Band band = RadiocraftData.BANDS.getValue(wavelength);
		if(frequency > band.maxFrequency() || frequency < band.minFrequency() || (frequency - band.minFrequency()) % RadiocraftServerConfig.FREQUENCY_STEP.get() != 0)
			frequency = band.minFrequency(); // Reset frequency if the saved one was either out of bands or not aligned to the correct step size.
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
		nbt.putBoolean("isReceivingVoice", isReceivingVoice);
		nbt.putBoolean("isPowered", isPowered);
		nbt.putBoolean("ssbEnabled", ssbEnabled);
		nbt.putBoolean("cwEnabled", cwEnabled);
		nbt.putBoolean("isPTTDown", isPTTDown);
		return nbt;
	}

	@Override
	public void handleUpdateTag(CompoundTag nbt) {
		isReceivingVoice = nbt.getBoolean("isReceivingVoice");
		isPowered = nbt.getBoolean("isPowered");
		ssbEnabled = nbt.getBoolean("ssbEnabled");
		cwEnabled = nbt.getBoolean("cwEnabled");
		isPTTDown = nbt.getBoolean("isPTTDown");
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
		if(ssbEnabled) {
			if(isPTTDown && isPowered) {
				if(antennas.size() == 1)
					((AntennaBlockEntity) antennas.get(0).getNetworkItem()).transmitAudioPacket(level, rawAudio, wavelength, frequency, sourcePlayer);
				else if(antennas.size() > 1)
					overdraw();
			}
		}
	}

}
