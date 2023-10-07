package com.arrl.radiocraft.common.network.packets.serverbound;

import com.arrl.radiocraft.common.network.RadiocraftPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent.Context;

import java.util.function.Supplier;

public class SRadioCWPacket implements RadiocraftPacket {

	private final BlockPos pos;
	private final boolean value;

	public SRadioCWPacket(BlockPos pos, boolean value) {
		this.pos = pos;
		this.value = value;
	}

	@Override
	public void encode(FriendlyByteBuf buffer) {
		buffer.writeBlockPos(pos);
		buffer.writeBoolean(value);
	}

	public static SRadioCWPacket decode(FriendlyByteBuf buffer) {
		return new SRadioCWPacket(buffer.readBlockPos(), buffer.readBoolean());
	}

	@Override
	public void handle(Supplier<Context> context) {
		context.get().enqueueWork(() -> {
			BlockEntity be = context.get().getSender().getLevel().getBlockEntity(pos);

			if(be instanceof AbstractRadioBlockEntity radio) {
				radio.setCWEnabled(value);

				if(value && radio.getSSBEnabled())
					radio.setSSBEnabled(false);
			}
		});

		context.get().setPacketHandled(true);
	}

}
