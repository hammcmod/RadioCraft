package com.arrl.radiocraft.common.network.packets;

import com.arrl.radiocraft.common.blockentities.AbstractRadioBlockEntity;
import com.arrl.radiocraft.common.network.RadiocraftPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent.Context;

import java.util.function.Supplier;

public class ServerboundRadioPacket implements RadiocraftPacket {

	private final BlockPos pos;
	private final boolean isReceiving;
	private final boolean isTransmitting;

	public ServerboundRadioPacket(BlockPos pos, boolean isReceiving, boolean isTransmitting) {
		this.pos = pos;
		this.isReceiving = isReceiving;
		this.isTransmitting = isTransmitting;
	}

	@Override
	public void encode(FriendlyByteBuf buffer) {
		buffer.writeBlockPos(pos);
		buffer.writeBoolean(isReceiving);
		buffer.writeBoolean(isTransmitting);
	}

	public static ServerboundRadioPacket decode(FriendlyByteBuf buffer) {
		return new ServerboundRadioPacket(buffer.readBlockPos(), buffer.readBoolean(), buffer.readBoolean());
	}

	@Override
	public void handle(Supplier<Context> context) {
		context.get().enqueueWork(() -> {
			BlockEntity be = context.get().getSender().getLevel().getBlockEntity(pos);

			if(be instanceof AbstractRadioBlockEntity radio) {
				radio.setReceiving(isReceiving);
				radio.setTransmitting(isTransmitting);
			}
		});
		context.get().setPacketHandled(true);
	}
}
