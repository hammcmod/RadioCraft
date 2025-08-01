package com.arrl.radiocraft.common.network.serverbound;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.api.capabilities.IVHFHandheldCapability;
import com.arrl.radiocraft.common.capabilities.RadiocraftCapabilities;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public class SHandheldRadioUpdatePacket implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SHandheldRadioUpdatePacket> TYPE = new CustomPacketPayload.Type<>(Radiocraft.id("s_handheld_radio_update"));

    int index;
    boolean powered;
    boolean ptt;
    int frequencyKiloHertz; //TODO change to allow server to control frequency steps

    public SHandheldRadioUpdatePacket(int index, boolean powered, boolean ptt, int frequencyKiloHertz) {
        this.index = index;
        this.powered = powered;
        this.ptt = ptt;
        this.frequencyKiloHertz = frequencyKiloHertz;
    }

    //constructor for StreamCodec decoding
    private SHandheldRadioUpdatePacket(Integer index, Byte packed, Integer frequencyKiloHertz) {
        this.index = index;
        this.powered = (packed&0x1)==0x1;
        this.ptt = (packed&0x2)==0x2;
        this.frequencyKiloHertz = frequencyKiloHertz;
    }

    public int getFrequencyKiloHertz() {
        return frequencyKiloHertz;
    }

    public int getIndex() {
        return index;
    }

    private Byte packBools() {
        return (byte) ((this.powered ? 0x1 : 0) + (this.ptt ? 0x2 : 0));
    }

    public static final StreamCodec<ByteBuf, SHandheldRadioUpdatePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            SHandheldRadioUpdatePacket::getIndex,
            ByteBufCodecs.BYTE,
            SHandheldRadioUpdatePacket::packBools,
            ByteBufCodecs.INT,
            SHandheldRadioUpdatePacket::getFrequencyKiloHertz,
            SHandheldRadioUpdatePacket::new);


    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(()->{

            ItemStack stack = context.player().getInventory().getItem(this.index);

            if(stack == null){
                Radiocraft.LOGGER.error("Handheld Radio stack is null for player {} despite getting handheld radio update packet", context.player().getName());
                return;
            }

            IVHFHandheldCapability cap = stack.getCapability(RadiocraftCapabilities.VHF_HANDHELDS);

            if(cap == null){
                Radiocraft.LOGGER.error("held item does not have handheld radio capability yet the following player send handheld radio update packet: {}", context.player().getName());
                return;
            }

            cap.setPowered(this.powered);
            cap.setPTTDown(this.ptt);
            cap.setFrequencyKiloHertz(this.frequencyKiloHertz); //TODO server should handle frequency steps

        });
    }
}
