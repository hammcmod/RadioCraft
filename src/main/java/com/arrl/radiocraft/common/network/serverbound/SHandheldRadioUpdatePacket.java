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
    float ctcssTone;
    int offsetHz;
    float rxTone;

    public SHandheldRadioUpdatePacket(int index, boolean powered, boolean ptt, float gain, float micGain, float frequencyHertz, float ctcssTone, int offsetHz, float rxTone) {
        this.index = index;
        this.powered = powered;
        this.ptt = ptt;
        this.gain = gain;
        this.micGain = micGain;
        this.frequencyHertz = frequencyHertz;
        this.ctcssTone = ctcssTone;
        this.offsetHz = offsetHz;
        this.rxTone = rxTone;
    }

    //constructor for StreamCodec decoding
    private SHandheldRadioUpdatePacket(ByteBuf buf) {
        this.index = buf.readInt();
        byte packed = buf.readByte();
        this.powered = (packed&0x1)==0x1;
        this.ptt = (packed&0x2)==0x2;
        this.gain = buf.readFloat();
        this.micGain = buf.readFloat();
        this.frequencyHertz = buf.readFloat();
        this.ctcssTone = buf.readFloat();
        this.offsetHz = buf.readInt();
        this.rxTone = buf.readFloat();
    }

    private void write(ByteBuf buf) {
        buf.writeInt(index);
        buf.writeByte(packBools());
        buf.writeFloat(gain);
        buf.writeFloat(micGain);
        buf.writeFloat(frequencyHertz);
        buf.writeFloat(ctcssTone);
        buf.writeInt(offsetHz);
        buf.writeFloat(rxTone);
    }

    public float getFrequencyHertz() {
        return frequencyHertz;
    }

    public int getIndex() {
        return index;
    }

    private Byte packBools() {
        return (byte) ((this.powered ? 0x1 : 0) + (this.ptt ? 0x2 : 0));
    }

    private float getGain() {
        return this.gain;
    }

    private float getMicGain() {
        return this.micGain;
    }

    public static final StreamCodec<ByteBuf, SHandheldRadioUpdatePacket> STREAM_CODEC = StreamCodec.of((buf, packet) -> packet.write(buf), SHandheldRadioUpdatePacket::new);


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
            cap.setCtcssTone(this.ctcssTone);
            cap.setOffsetHz(this.offsetHz);
            cap.setRxTone(this.rxTone);

        });
    }

    public static void updateServer(int inventoryIndex, IVHFHandheldCapability cap) {
        PacketDistributor.sendToServer(new SHandheldRadioUpdatePacket(inventoryIndex, cap.isPowered(), cap.isPTTDown(), cap.getGain(), cap.getMicGain(), cap.getFrequencyHertz(), cap.getCtcssTone(), cap.getOffsetHz(), cap.getRxTone()));
    }
}
