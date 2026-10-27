package com.arrl.radiocraft.common.network.serverbound;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.init.RadiocraftDataComponent;
import com.arrl.radiocraft.common.items.HandheldDirectionFinderItem;
import com.arrl.radiocraft.common.radio.Band;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

/** Sets the frequency of the handheld direction finder in one of the player's hands; ignored outside the bands. */
public record SHandheldDfFrequencyPacket(boolean offhand, float hertz) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<SHandheldDfFrequencyPacket> TYPE = new CustomPacketPayload.Type<>(Radiocraft.id("s_handheld_df_frequency"));

	public static final StreamCodec<ByteBuf, SHandheldDfFrequencyPacket> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.BOOL, SHandheldDfFrequencyPacket::offhand,
			ByteBufCodecs.FLOAT, SHandheldDfFrequencyPacket::hertz,
			SHandheldDfFrequencyPacket::new
	);

	@Override
	public @NotNull Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public void handle(IPayloadContext context) {
		context.enqueueWork(() -> {
			ItemStack stack = context.player().getItemInHand(offhand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
			if(stack.getItem() instanceof HandheldDirectionFinderItem && Float.isFinite(hertz) && Band.getBand(hertz) != null)
				stack.set(RadiocraftDataComponent.DF_FREQUENCY.get(), hertz);
		});
	}

	public static void send(InteractionHand hand, float hertz) {
		PacketDistributor.sendToServer(new SHandheldDfFrequencyPacket(hand == InteractionHand.OFF_HAND, hertz));
	}
}
