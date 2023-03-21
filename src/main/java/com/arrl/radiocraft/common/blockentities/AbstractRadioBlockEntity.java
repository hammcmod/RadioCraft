package com.arrl.radiocraft.common.blockentities;

import com.arrl.radiocraft.client.blockentity.AbstractRadioBlockEntityClientHandler;
import com.arrl.radiocraft.common.radio.Radio;
import com.arrl.radiocraft.common.radio.RadioManager;
import com.arrl.radiocraft.common.radio.RadioNetwork;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.packets.MicrophonePacket;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public abstract class AbstractRadioBlockEntity extends AbstractPowerBlockEntity {

	private Radio radioData; // Radio acts as a container for connection info/voip channels
	private final int receiveUsePower;
	private final int transmitUsePower;

	public boolean isReceiving = false; // This is only read clientside to determine the static sounds.


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

	public RadioNetwork getRadioNetwork() {
		return level != null ? RadioManager.getNetwork(level) : null;
	}

	public static <T extends BlockEntity> void tick(Level level, BlockPos pos, BlockState state, T t) {
		if(!level.isClientSide) {
			if(t instanceof AbstractRadioBlockEntity be) {
				Radio radio = be.getRadio();

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
			setTransmitting(false);
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
		if(!level.isClientSide())
			getRadioNetwork().putRadio(worldPosition, getRadio());
		else
			AbstractRadioBlockEntityClientHandler.startRadioStatic(this);
	}

	@Override
	public void setRemoved() {
		super.setRemoved();
		if(!level.isClientSide())
			getRadioNetwork().removeRadio(worldPosition);
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
	 * @param packet
	 */
	public void acceptVoicePacket(VoicechatServerApi api, de.maxhenkel.voicechat.api.ServerLevel level, MicrophonePacket packet) {
		Radio radio = getRadio();
		if(radio.isTransmitting()) {
			Map<BlockPos, Integer> connections = radio.getConnections();
			for(BlockPos pos : connections.keySet()) {
				if(((ServerLevel)level.getServerLevel()).getChunkAt(pos).getBlockEntity(pos, LevelChunk.EntityCreationType.IMMEDIATE) instanceof AbstractRadioBlockEntity be) {
					Radio targetRadio = be.getRadio();

					if(targetRadio.isReceiving()) {
						if(targetRadio.getReceiveChannel() == null)
							targetRadio.openChannel(api, level, pos.getX(), pos.getY(), pos.getZ());
						targetRadio.receive(packet, 1);
					}
				}
			}
		}
	}

	public abstract Radio createRadio();

}
