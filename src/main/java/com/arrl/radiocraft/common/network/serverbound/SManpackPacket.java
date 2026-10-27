package com.arrl.radiocraft.common.network.serverbound;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.RadiocraftServerConfig;
import com.arrl.radiocraft.common.datacomponents.ManpackState;
import com.arrl.radiocraft.common.init.RadiocraftDataComponent;
import com.arrl.radiocraft.common.items.ManpackMicItem;
import com.arrl.radiocraft.common.radio.Band;
import com.arrl.radiocraft.common.radio.voice.handheld.ManpackRadio;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

/**
 * A control on the HF manpack's screen: {@code power}, {@code mode} (SSB/CW), {@code band} (10m/20m), {@code tune}
 * (steps), {@code gain} and {@code mic_gain} (the new value), {@code ptt} (1 down, 0 up), {@code take_mic},
 * {@code hang_mic}. Applies to the manpack the player has to hand ({@link ManpackRadio#find}).
 */
public record SManpackPacket(String action, float value) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<SManpackPacket> TYPE = new CustomPacketPayload.Type<>(Radiocraft.id("s_manpack"));

	public static final StreamCodec<ByteBuf, SManpackPacket> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8, SManpackPacket::action,
			ByteBufCodecs.FLOAT, SManpackPacket::value,
			SManpackPacket::new
	);

	@Override
	public @NotNull Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public void handle(IPayloadContext context) {
		context.enqueueWork(() -> {
			Player player = context.player();
			ItemStack stack = ManpackRadio.find(player);
			if(stack.isEmpty())
				return;
			ManpackState state = ManpackRadio.stateOf(stack);
			switch(action) {
				case "power" -> {
					if(!state.on() && state.energy() <= 0 && RadiocraftServerConfig.RADIO_POWER_REQUIRED.get())
						return; // A flat battery: the switch does nothing.
					state = state.withOn(!state.on());
				}
				case "mode" -> state = state.withCw(!state.cw());
				case "band" -> state = state.withBand(!state.tenMetres());
				case "tune" -> {
					Band band = Band.getBand(state.bandName());
					if(band != null)
						state = state.withFrequency(com.arrl.radiocraft.common.blockentities.radio.RadioBlockEntity.snapToStep(band, state.frequency(),
								RadiocraftServerConfig.HF_FREQUENCY_STEP.get(), Math.max(-1000, Math.min(1000, Math.round(value)))));
				}
				case "gain" -> state = state.withGain(Math.max(0.0F, Math.min(2.0F, value)));
				case "mic_gain" -> state = state.withMicGain(Math.max(0.0F, Math.min(2.0F, value)));
				case "ptt" -> {
					ManpackRadio.get(player.getUUID()).ifPresent(radio -> radio.setScreenPtt(value > 0.5F));
					return;
				}
				case "take_mic" -> {
					if(state.micOut() || !player.getMainHandItem().isEmpty())
						return;
					player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ManpackMicItem.create());
					player.level().playSound(null, player.blockPosition(), SoundEvents.LEASH_KNOT_PLACE, SoundSource.PLAYERS, 0.6F, 1.4F);
					state = state.withMicOut(true);
				}
				case "hang_mic" -> {
					ManpackMicItem.removeFrom(player);
					state = state.withMicOut(false);
				}
				default -> {
					return;
				}
			}
			stack.set(RadiocraftDataComponent.MANPACK_STATE.get(), state);
		});
	}

	/** Server side: the handset back on its hook (sneak right click with it). */
	public static void hangMic(Player player) {
		ManpackMicItem.removeFrom(player);
		ItemStack stack = ManpackRadio.find(player);
		if(!stack.isEmpty())
			stack.set(RadiocraftDataComponent.MANPACK_STATE.get(), ManpackRadio.stateOf(stack).withMicOut(false));
		player.level().playSound(null, player.blockPosition(), SoundEvents.LEASH_KNOT_PLACE, SoundSource.PLAYERS, 0.6F, 1.0F);
	}

	public static void send(String action, float value) {
		PacketDistributor.sendToServer(new SManpackPacket(action, value));
	}
}
