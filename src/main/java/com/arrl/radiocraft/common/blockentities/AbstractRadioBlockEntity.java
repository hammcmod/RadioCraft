package com.arrl.radiocraft.common.blockentities;

import com.arrl.radiocraft.common.radio.Radio;
import com.arrl.radiocraft.common.radio.RadioManager;
import com.arrl.radiocraft.common.radio.RadioNetwork;
import de.maxhenkel.voicechat.api.packets.MicrophonePacket;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

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
		getRadioNetwork().putRadio(worldPosition, getRadio());
	}

	@Override
	public void onChunkUnloaded() {
		super.onChunkUnloaded();
		getRadioNetwork().removeRadio(worldPosition);
	}

	public void acceptVoicePacket(MicrophonePacket packet) {
		Radio radio = getRadio();
		if(radio.isTransmitting()) {
			radio.send(packet);
		}
	}

	public abstract Radio createRadio();

}
