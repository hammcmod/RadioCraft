package com.arrl.radiocraft.common.network.packets.serverbound;

import com.arrl.radiocraft.common.blockentities.radio.RadioBlockEntity;
import com.arrl.radiocraft.common.network.RadiocraftPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent.Context;

import java.util.function.Supplier;

public class SFrequencyPacket implements RadiocraftPacket {

	private final BlockPos pos;
	private final int value;

	public SFrequencyPacket(BlockPos pos, int value) {
		this.pos = pos;
		this.value = value;
	}

	@Override
	public void encode(FriendlyByteBuf buffer) {
		buffer.writeBlockPos(pos);
		buffer.writeInt(value);
	}

	public static SFrequencyPacket decode(FriendlyByteBuf buffer) {
		return new SFrequencyPacket(buffer.readBlockPos(), buffer.readInt());
	}

	@Override
	public void handle(Supplier<Context> context) {
		context.get().enqueueWork(() -> {
			BlockEntity be = context.get().getSender().getLevel().getBlockEntity(pos);

			if(be instanceof RadioBlockEntity radio)
				radio.updateFrequency(value);
		});
		context.get().setPacketHandled(true);
	}

}
