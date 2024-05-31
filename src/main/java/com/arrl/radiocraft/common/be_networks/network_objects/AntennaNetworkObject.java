package com.arrl.radiocraft.common.be_networks.network_objects;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.api.antenna.AntennaTypes;
import com.arrl.radiocraft.api.antenna.IAntennaType;
import com.arrl.radiocraft.api.benetworks.BENetwork;
import com.arrl.radiocraft.api.benetworks.BENetworkObject;
import com.arrl.radiocraft.api.capabilities.IBENetworks;
import com.arrl.radiocraft.common.be_networks.ICoaxNetworkObject;
import com.arrl.radiocraft.common.blockentities.radio.HFRadioBlockEntity;
import com.arrl.radiocraft.common.blockentities.radio.RadioBlockEntity;
import com.arrl.radiocraft.common.radio.antenna.AntennaCWPacket;
import com.arrl.radiocraft.common.radio.antenna.AntennaNetwork;
import com.arrl.radiocraft.common.radio.antenna.AntennaVoicePacket;
import com.arrl.radiocraft.common.radio.antenna.StaticAntenna;
import com.arrl.radiocraft.common.radio.antenna.networks.AntennaNetworkManager;
import com.arrl.radiocraft.common.radio.morse.CWBuffer;
import de.maxhenkel.voicechat.api.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.*;

public class AntennaNetworkObject extends BENetworkObject implements ICoaxNetworkObject {

    public static final ResourceLocation TYPE = Radiocraft.id("antenna");

    protected final List<RadioNetworkObject> radios = Collections.synchronizedList(new ArrayList<>());
    protected StaticAntenna<?> antenna = null;
    protected ResourceLocation networkId;

    public AntennaNetworkObject(Level level, BlockPos pos, ResourceLocation networkId) {
        super(level, pos);
        this.networkId = networkId;
    }

    public void transmitAudioPacket(ServerLevel level, short[] rawAudio, int wavelength, int frequency, UUID sourcePlayer) {
        antenna.transmitAudioPacket(level, rawAudio, wavelength, frequency, sourcePlayer);
    }

    public void receiveAudioPacket(AntennaVoicePacket packet) {
        if(radios.size() == 1) {
            BlockPos checkPos = radios.get(0).getPos();
            if(level.isLoaded(checkPos)) {
                if(level.getChunkAt(checkPos).getBlockEntity(checkPos, LevelChunk.EntityCreationType.IMMEDIATE) instanceof RadioBlockEntity radio) {
                    if(radio.getFrequency() == packet.getFrequency()) // Only receive if listening to correct frequency.
                        radio.getVoiceReceiver().receive(packet);
                }
            }
        }
        else if(radios.size() > 1)
            overdraw(level);
    }

    public void transmitCWPacket(net.minecraft.server.level.ServerLevel level, Collection<CWBuffer> buffers, int wavelength, int frequency) {
        antenna.transmitCWPacket(level, buffers, wavelength, frequency);
    }

    public void receiveCWPacket(AntennaCWPacket packet) {
        if(radios.size() == 1) {
            if(level.isLoaded(pos)) {
                BlockEntity be = level.getBlockEntity(radios.get(0).getPos());
                if(be instanceof HFRadioBlockEntity radio) {
                    if(radio.getFrequency() == packet.getFrequency()) // Only receive if listening to correct frequency.
                        radio.receiveCW(packet);
                }
            }
        }
        else if(radios.size() > 1)
            overdraw(level);
    }

    public void overdraw(Level level) {
        for(BENetworkObject obj : radios) {
            if(level.isLoaded(pos)) {
                BlockEntity be = level.getBlockEntity(obj.getPos());
                if(be instanceof RadioBlockEntity radio)
                    radio.overdraw();
            }
        }
    }

    @Override
    public void onNetworkUpdateAdd(BENetwork network, BENetworkObject object) {
        if(object instanceof RadioNetworkObject radio)
            radios.add(radio);
    }

    @Override
    public void onNetworkUpdateRemove(BENetwork network, BENetworkObject object) {
        if(object instanceof RadioNetworkObject radio)
            radios.remove(radio);
    }

    @Override
    public void onNetworkAdd(BENetwork network) {
        if(network.getType() == BENetwork.COAXIAL_TYPE) {
            for(BENetworkObject object : network.getNetworkObjects()) {
                if(object instanceof RadioNetworkObject radio)
                    radios.add(radio);
            }
        }
    }

    @Override
    public void onNetworkRemove(BENetwork network) {
        if(network.getType() == BENetwork.COAXIAL_TYPE) {
            for(BENetworkObject object : network.getNetworkObjects()) {
                if(object instanceof RadioNetworkObject radio)
                    radios.remove(radio);
            }
        }
    }

    public double getSWR(int wavelength) {
        if(radios.size() > 1)
            return 10.0D;
        return antenna == null ? 0 : antenna.getSWR(wavelength);
    }

    public StaticAntenna<?> getAntenna() {
        return antenna;
    }

    public void setAntenna(StaticAntenna<?> antenna) {
        AntennaNetwork network = AntennaNetworkManager.getNetwork(level, networkId);

        if(this.antenna != null)
            network.removeAntenna(this.antenna);

        this.antenna = antenna;
        network.addAntenna(this.antenna);
        antenna.setNetwork(network);
    }

    @Override
    public void save(CompoundTag nbt) {
        super.save(nbt);
        if(antenna != null) {
            nbt.putString("antennaType", antenna.type.getId().toString());
            nbt.put("antennaData", antenna.serializeNBT());
        }
        nbt.putString("networkId", networkId.toString());
    }

    @Override
    public void load(IBENetworks cap, CompoundTag nbt) {
        super.load(cap, nbt);
        if(nbt.contains("antennaType")) {
            IAntennaType<?> type = AntennaTypes.getType(new ResourceLocation(nbt.getString("antennaType")));
            if(type != null) {
                antenna = new StaticAntenna<>(type, pos);
                antenna.deserializeNBT(nbt.getCompound("antennaData"));
            }
        }
        networkId = new ResourceLocation(nbt.getString("networkId"));
    }

    @Override
    public ResourceLocation getType() {
        return TYPE;
    }

}
