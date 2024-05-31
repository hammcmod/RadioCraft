package com.arrl.radiocraft.common.be_networks.network_objects;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.api.benetworks.BENetwork;
import com.arrl.radiocraft.api.benetworks.BENetworkObject;
import com.arrl.radiocraft.api.benetworks.PowerNetworkObject;
import com.arrl.radiocraft.api.capabilities.IBENetworks;
import com.arrl.radiocraft.common.be_networks.ICoaxNetworkObject;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.*;

public class RadioNetworkObject extends PowerNetworkObject implements ICoaxNetworkObject {

    public static final ResourceLocation TYPE = Radiocraft.id("radio");

    public boolean isPowered = false;

    protected final List<AntennaNetworkObject> antennas = Collections.synchronizedList(new ArrayList<>());
    protected boolean isTransmitting = false;
    protected int transmitUse;
    protected int receiveUse;

    public RadioNetworkObject(Level level, BlockPos pos, int transmitUse, int receiveUse) {
        super(level, pos, transmitUse, transmitUse, transmitUse);
        this.transmitUse = transmitUse;
        this.receiveUse = receiveUse;
    }

    @Override
    public boolean isIndirectConsumer() {
        return true;
    }

    @Override
    public boolean isDirectConsumer() {
        return false;
    }

    @Override
    public void tick(Level level, BlockPos pos) {
        if(isPowered)
            isPowered = tryConsumeEnergy(isTransmitting ? transmitUse : receiveUse, false);
    }

    @Override
    public void save(CompoundTag nbt) {
        super.save(nbt);
        nbt.putBoolean("isPowered", isPowered);
        nbt.putBoolean("isTransmitting", isTransmitting);
        nbt.putInt("transmitUse", transmitUse);
        nbt.putInt("receiveUse", receiveUse);
    }

    @Override
    public void load(IBENetworks cap, CompoundTag nbt) {
        super.load(cap, nbt);
        isPowered = nbt.getBoolean("isPowered");
        isTransmitting = nbt.getBoolean("isTransmitting");
        transmitUse = nbt.getInt("transmitUse");
        receiveUse = nbt.getInt("receiveUse");
    }

    public void setTransmitting(boolean value) {
        this.isTransmitting = value;
    }

    public boolean canPowerOn() {
        return tryConsumeEnergy(receiveUse, true);
    }

    @Override
    public void onNetworkUpdateAdd(BENetwork network, BENetworkObject object) {
        if(object instanceof AntennaNetworkObject antenna)
            antennas.add(antenna);
    }

    @Override
    public void onNetworkUpdateRemove(BENetwork network, BENetworkObject object) {
        if(object instanceof AntennaNetworkObject antenna)
            antennas.remove(antenna);
    }

    @Override
    public void onNetworkAdd(BENetwork network) {
        if(network.getType() == BENetwork.COAXIAL_TYPE) {
            for(BENetworkObject object : network.getNetworkObjects()) {
                if(object instanceof AntennaNetworkObject antenna)
                    antennas.add(antenna);
            }
        }
    }

    @Override
    public void onNetworkRemove(BENetwork network) {
        if(network.getType() == BENetwork.COAXIAL_TYPE) {
            for(BENetworkObject object : network.getNetworkObjects()) {
                if(object instanceof AntennaNetworkObject antenna)
                    antennas.remove(antenna);
            }
        }
    }

    public List<AntennaNetworkObject> getAntennas() {
        return this.antennas;
    }

    @Override
    public ResourceLocation getType() {
        return TYPE;
    }
}
