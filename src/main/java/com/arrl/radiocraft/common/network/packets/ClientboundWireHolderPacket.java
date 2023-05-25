package com.arrl.radiocraft.common.network.packets;

import com.arrl.radiocraft.common.network.RadiocraftPacket;
import com.arrl.radiocraft.entity.AntennaWireEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ClientboundWireHolderPacket implements RadiocraftPacket {

    private final int wireId;
    private final BlockPos targetPos;

    public ClientboundWireHolderPacket(int wireId, BlockPos targetPos) {
        this.wireId = wireId;
        this.targetPos = targetPos;
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(wireId);
        if(targetPos == null)
            buffer.writeLong(0);
        else
            buffer.writeLong(targetPos.asLong());
    }

    public static ClientboundWireHolderPacket decode(FriendlyByteBuf buffer) {
        int wireId = buffer.readVarInt();
        long targetPos = buffer.readLong();
        if(targetPos == 0)
            return new ClientboundWireHolderPacket(wireId, null);
        return new ClientboundWireHolderPacket(wireId, BlockPos.of(targetPos));
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Entity entity = Minecraft.getInstance().level.getEntity(wireId);
            if(entity instanceof AntennaWireEntity wireEntity)
                wireEntity.setWireHolder(targetPos);
        });
        ctx.get().setPacketHandled(true);
    }
}
