package com.arrl.radiocraft.common.network.serverbound;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.api.capabilities.IVHFHandheldCapability;
import com.arrl.radiocraft.common.capabilities.RadiocraftCapabilities;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public class SHandheldRadioUpdatePacket implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SHandheldRadioUpdatePacket> TYPE = new CustomPacketPayload.Type<>(Radiocraft.id("s_handheld_radio_update"));

    int index;
    boolean powered;
    boolean ptt;
    float gain;
    float micGain;
    float frequencyHertz; //TODO change to allow server to control frequency steps
    boolean vox;

    public SHandheldRadioUpdatePacket(int index, boolean powered, boolean ptt, float gain, float micGain, float frequencyHertz, boolean vox) {
        this.index = index;
        this.powered = powered;
        this.ptt = ptt;
        this.gain = gain;
        this.micGain = micGain;
        this.frequencyHertz = frequencyHertz;
        this.vox = vox;
    }

    //constructor for StreamCodec decoding
    private SHandheldRadioUpdatePacket(Integer index, Byte packed, Float gain, Float micGain, Float frequencyHertz) {
        this.index = index;
        this.powered = (packed&0x1)==0x1;
        this.ptt = (packed&0x2)==0x2;
        this.vox = (packed&0x4)==0x4;
        this.gain = gain;
        this.micGain = micGain;
        this.frequencyHertz = frequencyHertz;
    }

    public float getFrequencyHertz() {
        return frequencyHertz;
    }

    public int getIndex() {
        return index;
    }

    private Byte packBools() {
        return (byte) ((this.powered ? 0x1 : 0) + (this.ptt ? 0x2 : 0) + (this.vox ? 0x4 : 0));
    }

    private float getGain() {
        return this.gain;
    }

    private float getMicGain() {
        return this.micGain;
    }

    public static final StreamCodec<ByteBuf, SHandheldRadioUpdatePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            SHandheldRadioUpdatePacket::getIndex,
            ByteBufCodecs.BYTE,
            SHandheldRadioUpdatePacket::packBools,
            ByteBufCodecs.FLOAT,
            SHandheldRadioUpdatePacket::getGain,
            ByteBufCodecs.FLOAT,
            SHandheldRadioUpdatePacket::getMicGain,
            ByteBufCodecs.FLOAT,
            SHandheldRadioUpdatePacket::getFrequencyHertz,
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
            cap.setGain(this.gain);
            cap.setMicGain(this.micGain);
            cap.setFrequencyHertz(this.frequencyHertz); //TODO server should handle frequency steps
            cap.setVoxEnabled(this.vox);

        });
    }

    public static void updateServer(int inventoryIndex, IVHFHandheldCapability cap) {
        PacketDistributor.sendToServer(new SHandheldRadioUpdatePacket(inventoryIndex, cap.isPowered(), cap.isPTTDown(), cap.getGain(), cap.getMicGain(), cap.getFrequencyHertz(), cap.isVoxEnabled()));
    }
}
