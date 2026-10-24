package com.arrl.radiocraft.common.network.serverbound;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.radio.voice.handheld.PlayerRadioManager;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

/** Keeps the server informed while the player holds the "talk on worn radio" key (a handheld worn in a Curios slot). */
public record SWornPttPacket(boolean held) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SWornPttPacket> TYPE = new CustomPacketPayload.Type<>(Radiocraft.id("s_worn_ptt"));

    public static final StreamCodec<ByteBuf, SWornPttPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, SWornPttPacket::held,
            SWornPttPacket::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> PlayerRadioManager.get(context.player().getUUID()).ifPresent(radio -> radio.setWornPttHeld(held)));
    }

    public static void send(boolean held) {
        PacketDistributor.sendToServer(new SWornPttPacket(held));
    }
}
