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
import java.util.concurrent.ConcurrentLinkedQueue;

public abstract class HFRadioBlockEntity extends RadioBlockEntity implements ICWReceiver, ICWTransmitter {

	protected boolean cwEnabled = false;

	protected CWReceiveBuffer cwReceiveBuffer;
	protected CWSendBuffer cwSendBuffer;
	protected final ConcurrentLinkedQueue<QueuedCW> parrotCWQueue = new ConcurrentLinkedQueue<>();

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
			if(antennas.size() <= 1) { // None: it came in through the bare connector.
				recordSignal(packet.getStrength());
				List<CWBuffer> buffers = List.copyOf(packet.getBuffers());
				if(isParrot())
					parrotCWQueue.add(new QueuedCW(buffers, lastSignalGameTime + parrotDelayTicks));
				// Clients tracking this radio re-order the buffers and play them back through the morse sound instance.
				CCWBufferPacket.sendToTracking(serverLevel, worldPosition, buffers, (float)packet.getStrength());
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
		if(level instanceof ServerLevel serverLevel && canTransmitCW() && !isBlown()) {
			List<AntennaNetworkObject> antennas = ((RadioNetworkObject)IBENetworks.getObject(this.level, worldPosition)).getAntennas();
			if(antennas.size() == 1) {
				antennas.get(0).transmitCWPacket(serverLevel, buffers, band, getTunedFrequency());
				recordTransmit(); // Keying is RF on the air too, for the transmit side of redstone interference.
			}
			else if(antennas.size() > 1)
				overdraw();
			else if(bareConnector != null) // No antenna: a few blocks out of the bare connector.
				bareConnector.transmitCWPacket(serverLevel, buffers, band, getTunedFrequency());
		}
	}

	@Override
	protected void additionalTick() {
		if(level.isClientSide)
			getCWSendBuffer().tick();
	}

	@Override
	protected void tickParrot(ServerLevel serverLevel) {
		super.tickParrot(serverLevel);
		long now = serverLevel.getGameTime();
		for(QueuedCW next = parrotCWQueue.peek(); next != null && next.dueTick() <= now; next = parrotCWQueue.peek()) {
			parrotCWQueue.poll();
			transmitMorse(next.buffers());
			recordParrotEcho(serverLevel);
		}
	}

	@Override
	public void setParrotDelay(int delayTicks) {
		super.setParrotDelay(delayTicks);
		if(!isParrot())
			parrotCWQueue.clear();
	}

	/** Morse waiting to be echoed by a parrot. */
	protected record QueuedCW(List<CWBuffer> buffers, long dueTick) {}

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
		RadioBlockEntityClientHandler.startInterferenceHumSoundInstance(this);
	}

	@Override
	public boolean shouldPlayStatic() {
		return getSSBEnabled() || getCWEnabled();
	}
}
