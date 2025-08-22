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

/**
 * This packet just keeps the server informed if the client is holding their use key, since this information isn't directly synced otherwise.
 * Use for VHF handheld right click to PTT, can't use the same framework as holding right click on bows as that also slows your movement. 
 * @param held - is the use key held
 */
public record SPlayerClickHoldUpdate(boolean held) implements CustomPacketPayload{

    public static final CustomPacketPayload.Type<SPlayerClickHoldUpdate> TYPE = new CustomPacketPayload.Type<>(Radiocraft.id("s_player_click_hold_update"));

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static final StreamCodec<ByteBuf, SPlayerClickHoldUpdate> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            SPlayerClickHoldUpdate::held,
            SPlayerClickHoldUpdate::new
    );

    public void handle(IPayloadContext context) {
        context.enqueueWork(()->
            PlayerRadioManager.get(context.player().getUUID()).ifPresent(playerRadio -> playerRadio.setUseHeld(this.held))
        );
    }

    public static void updateServer(boolean held){
        PacketDistributor.sendToServer(new SPlayerClickHoldUpdate(held));
    }
}
