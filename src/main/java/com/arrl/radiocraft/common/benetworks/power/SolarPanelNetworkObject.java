package com.arrl.radiocraft.common.benetworks.power;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.RadiocraftCommonConfig;
import com.arrl.radiocraft.api.benetworks.BENetwork;
import com.arrl.radiocraft.api.benetworks.power.PowerBENetwork;
import com.arrl.radiocraft.api.benetworks.power.PowerNetworkObject;
import com.arrl.radiocraft.api.capabilities.IBENetworks;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public class SolarPanelNetworkObject extends PowerNetworkObject {

    public static final ResourceLocation TYPE = Radiocraft.location("solar_panel");

    private boolean canSeeSky = false;
    public int lastPowerTick = 0;

    public SolarPanelNetworkObject() {
        super(0, 0, 0);
    }

    @Override
    public boolean isIndirectConsumer() {
        return false;
    }

    @Override
    public boolean isDirectConsumer() {
        return false;
    }

    public void setCanSeeSky(boolean value) {
        this.canSeeSky = value;
    }

    public int getLastPowerTick() {
        return lastPowerTick;
    }

    @Override
    public void generate(Level level, BlockPos pos) {
        if(level.isDay() && level.canSeeSky(pos)) {
            int powerGenerated = energyStorage.getMaxReceive();
            if(level.isRaining())
                powerGenerated = (int)(powerGenerated * RadiocraftCommonConfig.SOLAR_PANEL_RAIN_MULTIPLIER.get());

            lastPowerTick = powerGenerated;

            for(BENetwork n : networks.values()) {
                if(n instanceof PowerBENetwork network) {
                    powerGenerated -= network.pushPower(powerGenerated, true, false);

                    if(powerGenerated <= 0)
                        return;
                }
            }
        }
    }

    @Override
    public void save(CompoundTag nbt) {
        super.save(nbt);
        nbt.putBoolean("canSeeSky", canSeeSky);
    }

    @Override
    public void load(IBENetworks cap, CompoundTag nbt) {
        super.load(cap, nbt);
        canSeeSky = nbt.getBoolean("canSeeSky");
    }

}
