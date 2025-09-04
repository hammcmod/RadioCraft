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
    int frequencyKiloHertz; //TODO change to allow server to control frequency steps

    public SHandheldRadioUpdatePacket(int index, boolean powered, boolean ptt, float gain, float micGain, int frequencyKiloHertz) {
        this.index = index;
        this.powered = powered;
        this.ptt = ptt;
        this.gain = gain;
        this.micGain = micGain;
        this.frequencyKiloHertz = frequencyKiloHertz;
    }

    //constructor for StreamCodec decoding
    private SHandheldRadioUpdatePacket(Integer index, Byte packed, Byte gain, Byte micGain, Integer frequencyKiloHertz) {
        this.index = index;
        this.powered = (packed&0x1)==0x1;
        this.ptt = (packed&0x2)==0x2;
        // byte & 0xFF here will implicitly cast byte to int and get rid of sign, restoring 0-255 range
        this.gain = (float)((gain & 0xFF) / 10.0f);
        this.micGain = (float)((micGain & 0xFF) / 10.0f);
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

    // gain is a float multiplier but let's mostly care around 1x as 10
    // since we don't need more resolution than one decimal, we can use a byte and cheapen the network load
    // this gives us a range of 0.0x to 25.5x gain
    private Byte getGain() {
        // i am not using Byte.MIN_VALUE or Byte.MAX_VALUE because those are -128 to 127 (signed)
        // the cast here will make it signed but it's correctly handled in the constructor above
        return (byte)Math.max(0, Math.min(255, Math.round(this.gain * 10)));
    }

    private Byte getMicGain() {
        return (byte)Math.max(0, Math.min(255, Math.round(this.micGain * 10)));
    }

    public static final StreamCodec<ByteBuf, SHandheldRadioUpdatePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            SHandheldRadioUpdatePacket::getIndex,
            ByteBufCodecs.BYTE,
            SHandheldRadioUpdatePacket::packBools,
            ByteBufCodecs.BYTE,
            SHandheldRadioUpdatePacket::getGain,
            ByteBufCodecs.BYTE,
            SHandheldRadioUpdatePacket::getMicGain,
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
            cap.setGain(this.gain);
            cap.setMicGain(this.micGain);
            cap.setFrequencyKiloHertz(this.frequencyKiloHertz); //TODO server should handle frequency steps

        });
    }

    public static void updateServer(int inventoryIndex, IVHFHandheldCapability cap) {
        PacketDistributor.sendToServer(new SHandheldRadioUpdatePacket(inventoryIndex, cap.isPowered(), cap.isPTTDown(), cap.getGain(), cap.getMicGain(), cap.getFrequencyKiloHertz()));
    }
}
