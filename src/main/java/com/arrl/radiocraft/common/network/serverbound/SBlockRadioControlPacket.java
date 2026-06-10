package com.arrl.radiocraft.common.network.serverbound;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.blockentities.radio.RadioBlockEntity;
import com.arrl.radiocraft.common.menus.RadioMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record SBlockRadioControlPacket(long pos, int actionId, int stepCount, boolean value) implements CustomPacketPayload {

    public enum Action {
        TOGGLE_POWER,
        SET_SSB,
        SET_PTT,
        STEP_FREQUENCY
    }

    public static final CustomPacketPayload.Type<SBlockRadioControlPacket> TYPE = new CustomPacketPayload.Type<>(Radiocraft.id("s_block_radio_control"));

    public static final StreamCodec<ByteBuf, SBlockRadioControlPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG,
            SBlockRadioControlPacket::pos,
            ByteBufCodecs.VAR_INT,
            SBlockRadioControlPacket::actionId,
            ByteBufCodecs.INT,
            SBlockRadioControlPacket::stepCount,
            ByteBufCodecs.BOOL,
            SBlockRadioControlPacket::value,
            SBlockRadioControlPacket::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if(actionId < 0 || actionId >= Action.values().length)
                return;

            BlockPos targetPos = BlockPos.of(pos);
            BlockEntity blockEntity = context.player().level().getBlockEntity(targetPos);
            if(!(blockEntity instanceof RadioBlockEntity radio))
                return;

            Action action = Action.values()[actionId];
            if(!canControl(context, radio, targetPos, action))
                return;

            switch(action) {
                case TOGGLE_POWER -> radio.toggle();
                case SET_SSB -> radio.setSSBEnabled(value);
                case SET_PTT -> radio.setPTTDown(value);
                case STEP_FREQUENCY -> radio.updateFrequency(stepCount);
            }
        });
    }

    private boolean canControl(IPayloadContext context, RadioBlockEntity radio, BlockPos targetPos, Action action) {
        if(context.player().containerMenu instanceof RadioMenu<?> menu && menu.blockEntity == radio)
            return true;

        return action == Action.SET_PTT
                && !value
                && context.player().distanceToSqr(targetPos.getX() + 0.5D, targetPos.getY() + 0.5D, targetPos.getZ() + 0.5D) <= 64.0D;
    }

    public static void updateServer(BlockPos pos, Action action, boolean value) {
        PacketDistributor.sendToServer(new SBlockRadioControlPacket(pos.asLong(), action.ordinal(), 0, value));
    }

    public static void updateServer(BlockPos pos, Action action, int stepCount) {
        PacketDistributor.sendToServer(new SBlockRadioControlPacket(pos.asLong(), action.ordinal(), stepCount, false));
    }
}
