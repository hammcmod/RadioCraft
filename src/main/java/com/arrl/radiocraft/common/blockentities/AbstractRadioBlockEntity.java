package com.arrl.radiocraft.common.blockentities;

import com.arrl.radiocraft.common.radio.Radio;
import com.arrl.radiocraft.common.radio.RadioManager;
import com.arrl.radiocraft.common.radio.RadioNetwork;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.packets.MicrophonePacket;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.Map;

public abstract class AbstractRadioBlockEntity extends AbstractPowerBlockEntity {

	private Radio radioData;

	public AbstractRadioBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int capacity, int maxTransfer) {
		super(type, pos, state, capacity, maxTransfer);
	}

	public Radio getRadio() {
		if(radioData == null)
			radioData = createRadio();

		return radioData;
	}

	public RadioNetwork getRadioNetwork() {
		return level != null ? RadioManager.getNetwork(level) : null;
	}

	@Override
	public void onLoad() {
		super.onLoad();
		if(!level.isClientSide())
			getRadioNetwork().putRadio(worldPosition, getRadio());
	}

	@Override
	public void onChunkUnloaded() {
		super.onChunkUnloaded();
		getRadioNetwork().removeRadio(worldPosition);
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
					if(targetRadio.getReceiveChannel() == null)
						targetRadio.openChannel(api, level, pos.getX(), pos.getY(), pos.getZ());

					if(targetRadio.isReceiving())
						targetRadio.receive(packet, 1);
				}
			}
		}
	}

	public abstract Radio createRadio();

}
