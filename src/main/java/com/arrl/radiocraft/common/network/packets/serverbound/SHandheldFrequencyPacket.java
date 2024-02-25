package com.arrl.radiocraft.common.network.packets.serverbound;

import com.arrl.radiocraft.RadiocraftServerConfig;
import com.arrl.radiocraft.api.capabilities.IVHFHandheldCapability;
import com.arrl.radiocraft.api.capabilities.RadiocraftCapabilities;
import com.arrl.radiocraft.common.init.RadiocraftData;
import com.arrl.radiocraft.common.init.RadiocraftPackets;
import com.arrl.radiocraft.common.network.RadiocraftPacket;
import com.arrl.radiocraft.common.network.packets.clientbound.CHandheldFrequencyPacket;
import com.arrl.radiocraft.common.radio.Band;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.network.NetworkEvent.Context;

import java.util.function.Supplier;

public class SHandheldFrequencyPacket implements RadiocraftPacket {

	private final int index;
	private final int value;

	public SHandheldFrequencyPacket(int index, int value) {
		this.index = index;
		this.value = value;
	}

	@Override
	public void encode(FriendlyByteBuf buffer) {
		buffer.writeInt(index);
		buffer.writeInt(value);
	}

	public static SHandheldFrequencyPacket decode(FriendlyByteBuf buffer) {
		return new SHandheldFrequencyPacket(buffer.readInt(), buffer.readInt());
	}

	@Override
	public void handle(Supplier<Context> context) {
		context.get().enqueueWork(() -> {

			ItemStack item = context.get().getSender().getInventory().getItem(index);

			LazyOptional<IVHFHandheldCapability> optional = item.getCapability(RadiocraftCapabilities.VHF_HANDHELDS);
			optional.ifPresent(cap -> {
				Band band = RadiocraftData.BANDS.getValue(2);
				int step = RadiocraftServerConfig.VHF_FREQUENCY_STEP.get();
				int min = band.minFrequency();
				int max = (band.maxFrequency() - band.minFrequency()) / step * step + min; // This calc looks weird, but it's integer division, throws away remainder to ensure the freq doesn't do a "half step" to max.

				int newFreq = Mth.clamp(cap.getFrequency() + step * value, min, max);
				cap.setFrequency(newFreq);
				RadiocraftPackets.sendToPlayer(new CHandheldFrequencyPacket(index, newFreq), context.get().getSender());
			});
		});
		context.get().setPacketHandled(true);
	}

}
