package com.arrl.radiocraft.common.network.packets.clientbound;

import com.arrl.radiocraft.api.capabilities.IVHFHandheldCapability;
import com.arrl.radiocraft.api.capabilities.RadiocraftCapabilities;
import com.arrl.radiocraft.common.network.RadiocraftPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.network.NetworkEvent.Context;

import java.util.function.Supplier;

public class CHandheldFrequencyPacket implements RadiocraftPacket {

	private final int index;
	private final int value;

	public CHandheldFrequencyPacket(int index, int value) {
		this.index = index;
		this.value = value;
	}

	@Override
	public void encode(FriendlyByteBuf buffer) {
		buffer.writeInt(index);
		buffer.writeInt(value);
	}

	public static CHandheldFrequencyPacket decode(FriendlyByteBuf buffer) {
		return new CHandheldFrequencyPacket(buffer.readInt(), buffer.readInt());
	}

	@Override
	public void handle(Supplier<Context> context) {
		context.get().enqueueWork(() -> {
			ItemStack item = Minecraft.getInstance().player.getInventory().getItem(index);

			LazyOptional<IVHFHandheldCapability> optional = item.getCapability(RadiocraftCapabilities.VHF_HANDHELDS);
			optional.ifPresent(cap -> cap.setFrequency(value));
		});
		context.get().setPacketHandled(true);
	}

}
