package com.arrl.radiocraft.common.blockentities.radio;

import com.arrl.radiocraft.api.blockentities.radio.ICWReceiver;
import com.arrl.radiocraft.api.blockentities.radio.ICWTransmitter;
import com.arrl.radiocraft.api.capabilities.IBENetworks;
import com.arrl.radiocraft.client.blockentity.RadioBlockEntityClientHandler;
import com.arrl.radiocraft.common.be_networks.network_objects.AntennaNetworkObject;
import com.arrl.radiocraft.common.be_networks.network_objects.RadioNetworkObject;
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
			cwSendBuffer = new CWSendBuffer(level.dimension(), worldPosition);
		return cwSendBuffer;
	}

	@Override
	public boolean canReceiveCW() {
		return cwEnabled;
	}

	@Override
	public void receiveCW(AntennaCWPacket packet) {
		if(!level.isClientSide && getCWEnabled()) {
			List<AntennaNetworkObject> antennas = ((RadioNetworkObject)IBENetworks.getObject(this.level, worldPosition)).getAntennas();
			if(antennas.size() == 1);
				// Send necessary buffers to clients tracking this BE, these will get re-ordered and then played back on the client.
				//RadiocraftPackets.sendToTrackingChunk(new CWBufferPacket(level.dimension(), worldPosition, packet.getBuffers(), (float)packet.getStrength()), level.getChunkAt(worldPosition));
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
		if(level instanceof ServerLevel serverLevel) {
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
