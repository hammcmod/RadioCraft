package com.arrl.radiocraft.common.be_networks.network_objects;

import com.arrl.radiocraft.common.capabilities.BasicEnergyStorage;
import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.RadiocraftServerConfig;
import com.arrl.radiocraft.api.benetworks.BENetwork;
import com.arrl.radiocraft.api.benetworks.BENetworkObject;
import com.arrl.radiocraft.api.benetworks.PowerBENetwork;
import com.arrl.radiocraft.api.benetworks.PowerNetworkObject;
import com.arrl.radiocraft.api.capabilities.IBENetworks;
import com.arrl.radiocraft.common.be_networks.ICoaxNetworkObject;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A radio on the coax (for its antennas) and on the power network (for its supply). The player's switch is
 * {@link #wantsOn}; {@link #isPowered} is whether the radio actually runs, which {@link #resolvePower} decides every
 * tick from the energy the power network has pushed into this object's storage: {@code receiveUse} per tick while
 * listening, {@code transmitUse} while transmitting. Without enough energy the radio <em>browns out</em>: it stays
 * off (no static, no receive) and the front panel cycles "on for a second, PWR, dark" ({@link #getDisplayPhase}) so
 * the player can see it is starved rather than broken, retrying at the start of every cycle. The server config
 * {@code radios_need_power} turns the whole draw off.
 */
public class RadioNetworkObject extends PowerNetworkObject implements ICoaxNetworkObject {

    public static final ResourceLocation TYPE = Radiocraft.id("radio");

    /** Front panel dark. */
    public static final int PHASE_OFF = 0;
    /** Front panel lit with the frequency: running, or the first second of a brown-out cycle. */
    public static final int PHASE_ON = 1;
    /** Front panel showing "PWR": the radio is switched on but starved. */
    public static final int PHASE_PWR = 2;
    /** Length of one brown-out cycle in ticks: on, PWR, dark, each a second, then try the power again. */
    public static final int BROWNOUT_CYCLE_TICKS = 60;
    private static final int BROWNOUT_PWR_FROM = 20;
    private static final int BROWNOUT_DARK_FROM = 40;

    /** The player's power switch. */
    public boolean wantsOn = false;
    /** Whether the radio is actually running. Read everywhere; written only by {@link #resolvePower}. */
    public boolean isPowered = false;
    private int brownoutTicks = 0;

    protected final Set<AntennaNetworkObject> antennas = Collections.synchronizedSet(new HashSet<>());
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
        resolvePower();
    }

    /** Flips the player's switch and settles the power at once, so the caller sees the result without waiting a tick. */
    public void setWantsOn(boolean value) {
        if(wantsOn != value) {
            wantsOn = value;
            brownoutTicks = 0;
        }
        resolvePower();
    }

    /**
     * Decides {@link #isPowered} for this tick. A running radio keeps running while it can pay its draw; a starved one
     * only tries again at the start of each brown-out cycle, so a trickle of power does not make it flicker.
     */
    public void resolvePower() {
        if(!wantsOn) {
            isPowered = false;
            brownoutTicks = 0;
            return;
        }
        if(!RadiocraftServerConfig.RADIO_POWER_REQUIRED.get()) {
            isPowered = true;
            brownoutTicks = 0;
            return;
        }
        if(isPowered || brownoutTicks % BROWNOUT_CYCLE_TICKS == 0) {
            if(tryConsumeEnergy(isTransmitting ? transmitUse : receiveUse, false)) {
                isPowered = true;
                brownoutTicks = 0;
                return;
            }
            isPowered = false;
        }
        brownoutTicks++;
    }

    /**
     * What the front panel shows: {@link #PHASE_ON}, {@link #PHASE_PWR} or {@link #PHASE_OFF}. With no battery on the
     * wire at all the panel stays dark whatever the switch says; the PWR cycle is for a battery that cannot keep up.
     */
    public int getDisplayPhase() {
        if(isPowered)
            return PHASE_ON;
        if(!wantsOn || !hasBattery())
            return PHASE_OFF;
        int t = brownoutTicks % BROWNOUT_CYCLE_TICKS;
        return t < BROWNOUT_PWR_FROM ? PHASE_ON : t < BROWNOUT_DARK_FROM ? PHASE_PWR : PHASE_OFF;
    }

    /** True if a battery sits on any power network this radio is wired to. */
    public boolean hasBattery() {
        for(BENetwork network : networks.values()) {
            if(network instanceof PowerBENetwork) {
                for(BENetworkObject object : network.getNetworkObjects())
                    if(object instanceof BatteryNetworkObject)
                        return true;
            }
        }
        return false;
    }

    /** True while the switch is on but the power network cannot feed the radio. */
    public boolean isBrownedOut() {
        return wantsOn && !isPowered;
    }

    @Override
    public void save(CompoundTag nbt) {
        super.save(nbt);
        nbt.putBoolean("wantsOn", wantsOn);
        nbt.putBoolean("isPowered", isPowered);
        nbt.putBoolean("isTransmitting", isTransmitting);
        nbt.putInt("transmitUse", transmitUse);
        nbt.putInt("receiveUse", receiveUse);
    }

    @Override
    public void load(IBENetworks cap, CompoundTag nbt) {
        super.load(cap, nbt);
        wantsOn = nbt.getBoolean("wantsOn");
        isPowered = nbt.getBoolean("isPowered");
        isTransmitting = nbt.getBoolean("isTransmitting");
        transmitUse = nbt.getInt("transmitUse");
        receiveUse = nbt.getInt("receiveUse");
        // VHF base stations placed before 2026-10-21 saved their two draws the wrong way round, with a buffer only
        // as big as the receive draw, and browned out forever. Put them right: transmit is the larger, and the
        // buffer must hold it.
        if(transmitUse < receiveUse) {
            int swap = transmitUse;
            transmitUse = receiveUse;
            receiveUse = swap;
        }
        if(energyStorage == null || energyStorage.getMaxEnergyStored() < transmitUse)
            energyStorage = new BasicEnergyStorage(transmitUse, transmitUse, transmitUse,
                    energyStorage == null ? 0 : Math.min(transmitUse, energyStorage.getEnergyStored()));
    }

    public void setTransmitting(boolean value) {
        this.isTransmitting = value;
    }

    public int getReceiveUse() {
        return receiveUse;
    }

    public int getTransmitUse() {
        return transmitUse;
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
        synchronized (this.antennas) {
            return List.copyOf(this.antennas);
        }
    }

    @Override
    public ResourceLocation getType() {
        return TYPE;
    }
}
