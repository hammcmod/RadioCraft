package com.arrl.radiocraft.common.be_networks.network_objects;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.CommonConfig;
import com.arrl.radiocraft.api.benetworks.BENetwork;
import com.arrl.radiocraft.api.benetworks.BENetworkObject;
import com.arrl.radiocraft.api.benetworks.PowerBENetwork;
import com.arrl.radiocraft.api.benetworks.PowerNetworkObject;
import com.arrl.radiocraft.api.capabilities.IBENetworks;
import com.arrl.radiocraft.common.be_networks.IPassThroughNetworkObject;
import com.arrl.radiocraft.api.weather.WeatherSources;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/**
 * A solar panel. Panels join each other and a charge controller just by touching (no wire), and a panel is one node
 * for all its sides, so a row of panels is one network with the controller at its end.
 */
public class SolarPanelNetworkObject extends PowerNetworkObject implements IPassThroughNetworkObject {

    @Override
    public boolean linksDirectlyTo(BENetworkObject other) {
        return other instanceof SolarPanelNetworkObject || other instanceof ChargeControllerNetworkObject;
    }


    public static final ResourceLocation TYPE = Radiocraft.id("solar_panel");

    private boolean canSeeSky = false;
    /** Severe weather over the panel, checked every {@link #SEVERE_CHECK_TICKS} (a full weather report is not cheap). */
    private boolean severe = false;
    private static final int SEVERE_CHECK_TICKS = 40;
    public int lastPowerTick = 0;

    public SolarPanelNetworkObject(Level level, BlockPos pos) {
        super(level, pos, 0, 0, 0);
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
    public void tick(Level level, BlockPos pos) {
        if(level.isDay() && level.canSeeSky(pos)) {
            int powerGenerated = CommonConfig.SOLAR_PANEL_MAX_OUTPUT.get();
            if(WeatherSources.isRaining(level, pos)) // Local rain where a weather mod has storms.
                powerGenerated = (int)(powerGenerated * CommonConfig.SOLAR_PANEL_RAIN_MULTIPLIER.get());
            if((level.getGameTime() + pos.asLong()) % SEVERE_CHECK_TICKS == 0L)
                severe = WeatherSources.isSevere(level, pos);
            if(severe) // Severe weather on top: the sky is darker still, and hail and dust coat the cells.
                powerGenerated = (int)(powerGenerated * CommonConfig.SOLAR_PANEL_SEVERE_MULTIPLIER.get());

            lastPowerTick = powerGenerated;

            for(BENetwork n : networks.values()) {
                if(n instanceof PowerBENetwork network) {
                    powerGenerated -= network.pushPower(powerGenerated, true, false, false);

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

    @Override
    public ResourceLocation getType() {
        return TYPE;
    }
}
