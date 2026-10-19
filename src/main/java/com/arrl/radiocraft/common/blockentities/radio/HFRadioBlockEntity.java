package com.arrl.radiocraft.common.blockentities.radio;

import com.arrl.radiocraft.api.blockentities.radio.ICWReceiver;
import com.arrl.radiocraft.api.blockentities.radio.ICWTransmitter;
import com.arrl.radiocraft.api.capabilities.IBENetworks;
import com.arrl.radiocraft.client.blockentity.RadioBlockEntityClientHandler;
import com.arrl.radiocraft.common.be_networks.network_objects.AntennaNetworkObject;
import com.arrl.radiocraft.common.be_networks.network_objects.RadioNetworkObject;
import com.arrl.radiocraft.common.network.clientbound.CCWBufferPacket;
import com.arrl.radiocraft.common.radio.Band;
import com.arrl.radiocraft.common.radio.antenna.AntennaCWPacket;
import com.arrl.radiocraft.common.radio.morse.CWBuffer;
import com.arrl.radiocraft.common.radio.morse.CWReceiveBuffer;
import com.arrl.radiocraft.common.radio.morse.CWSendBuffer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Collection;
import java.util.List;

public abstract class HFRadioBlockEntity extends RadioBlockEntity implements ICWReceiver, ICWTransmitter {

	protected boolean cwEnabled = false;

	protected CWReceiveBuffer cwReceiveBuffer;
	protected CWSendBuffer cwSendBuffer;

	public HFRadioBlockEntity(BlockEntityType<? extends HFRadioBlockEntity> type, BlockPos pos, BlockState state, Band band) {
		super(type, pos, state, band);
	}

	// -------------------- CW/MORSE IMPLEMENTATION --------------------

	@Override
	public CWReceiveBuffer getCWReceiveBuffer() {
		if(cwReceiveBuffer == null)
			cwReceiveBuffer = new CWReceiveBuffer();
		return cwReceiveBuffer;
	}

	@Override
	public CWSendBuffer getCWSendBuffer() {
		if(cwSendBuffer == null)
			cwSendBuffer = new CWSendBuffer(worldPosition);
		return cwSendBuffer;
	}

	private boolean isPowered() {
		return getNetworkObject(level, worldPosition) instanceof RadioNetworkObject networkObject && networkObject.isPowered;
	}

	@Override
	public boolean canReceiveCW() {
		return cwEnabled && isPowered();
	}

	/** True while the radio is on and in CW mode, which is all keying needs (there is no PTT for CW). */
	public boolean canTransmitCW() {
		return cwEnabled && isPowered();
	}

	@Override
	public void receiveCW(AntennaCWPacket packet) {
		if(level instanceof ServerLevel serverLevel && canReceiveCW()) {
			List<AntennaNetworkObject> antennas = ((RadioNetworkObject)IBENetworks.getObject(this.level, worldPosition)).getAntennas();
			if(antennas.size() == 1) {
				recordSignal(packet.getStrength());
				// Clients tracking this radio re-order the buffers and play them back through the morse sound instance.
				CCWBufferPacket.sendToTracking(serverLevel, worldPosition, List.copyOf(packet.getBuffers()), (float)packet.getStrength());
			}
			else if(antennas.size() > 1)
				overdraw();
		}
	}

	public boolean getCWEnabled() {
		return cwEnabled;
	}

	public void setCWEnabled(boolean value) {
		if(value && ssbEnabled)
			ssbEnabled = false; // Disable SSB if it as enabled here, don't need to sync here as it's done in super method.
		if(cwEnabled != value) {
			cwEnabled = value;
			if(level != null)
				updateIsReceiving(); // SSB may have just been switched off above, which stops voice receive.
			updateBlock();
			setChanged();
		}
	}

	@Override
	public void setSSBEnabled(boolean value) {
		if(value && cwEnabled)
			cwEnabled = false; // Disable CW if it as enabled here, don't need to sync here as it's done in super method.
		super.setSSBEnabled(value);
	}

	@Override
	public void transmitMorse(Collection<CWBuffer> buffers) {
		if(level instanceof ServerLevel serverLevel && canTransmitCW()) {
			List<AntennaNetworkObject> antennas = ((RadioNetworkObject)IBENetworks.getObject(this.level, worldPosition)).getAntennas();
			if(antennas.size() == 1)
				antennas.get(0).transmitCWPacket(serverLevel, buffers, band, frequency);
			else if(antennas.size() > 1)
				overdraw();
		}
	}

	@Override
	protected void additionalTick() {
		if(level.isClientSide)
			getCWSendBuffer().tick();
	}

	// -------------------- BE & SYNC IMPLEMENTATION --------------------

	@Override
	protected void setupSaveTag(CompoundTag nbt) {
		super.setupSaveTag(nbt);
		nbt.putBoolean("cwEnabled", cwEnabled);
	}

	@Override
	protected void readSaveTag(CompoundTag nbt) {
		super.readSaveTag(nbt);
		cwEnabled = nbt.getBoolean("cwEnabled");
	}

	@Override
	protected void setupSoundInstances() {
		super.setupSoundInstances();
		RadioBlockEntityClientHandler.startMorseSoundInstance(this);
		RadioBlockEntityClientHandler.startStaticSoundInstance(this);
	}

	@Override
	public boolean shouldPlayStatic() {
		return getSSBEnabled() || getCWEnabled();
	}
}
