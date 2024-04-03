package com.arrl.radiocraft.common.network.packets.serverbound;

import com.arrl.radiocraft.api.capabilities.IVHFHandheldCapability;
import com.arrl.radiocraft.api.capabilities.RadiocraftCapabilities;
import com.arrl.radiocraft.common.network.RadiocraftPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.network.NetworkEvent.Context;

import java.util.function.Supplier;

public class SHandheldPTTPacket implements RadiocraftPacket {

	private final int index;
	private final boolean value;

	public SHandheldPTTPacket(int index, boolean value) {
		this.index = index;
		this.value = value;
	}

	@Override
	public void encode(FriendlyByteBuf buffer) {
		buffer.writeInt(index);
		buffer.writeBoolean(value);
	}

	public static SHandheldPTTPacket decode(FriendlyByteBuf buffer) {
		return new SHandheldPTTPacket(buffer.readInt(), buffer.readBoolean());
	}

	@Override
	public void handle(Supplier<Context> context) {
		context.get().enqueueWork(() -> {
			ItemStack item = context.get().getSender().getInventory().getItem(index);

			LazyOptional<IVHFHandheldCapability> optional = item.getCapability(RadiocraftCapabilities.VHF_HANDHELDS);
			optional.ifPresent(cap -> cap.setPTTDown(value));
		});
		context.get().setPacketHandled(true);
	}

}
