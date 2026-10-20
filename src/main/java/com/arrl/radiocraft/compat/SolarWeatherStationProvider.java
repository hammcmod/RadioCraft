package com.arrl.radiocraft.compat;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.blockentities.SolarWeatherStationBlockEntity;
import com.arrl.radiocraft.common.radio.solar.SolarEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/** Jade lines for the solar weather station: the current conditions, their comparator level and whether 10m is open. */
public enum SolarWeatherStationProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if(!data.contains("event")) {
            tooltip.add(Component.literal("Conditions: no data"));
            return;
        }
        ResourceLocation key = ResourceLocation.tryParse(data.getString("event"));
        tooltip.add(Component.literal("Conditions: ").append(SolarWeatherStationBlockEntity.eventName(key))
                .append(Component.literal(" (noise floor " + data.getInt("noise") + "%, level " + data.getInt("level") + ")")));
        tooltip.add(Component.literal(SolarWeatherStationBlockEntity.REPORTED_BAND + " band: " + (data.getBoolean("open") ? "open" : "closed")));
    }

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if(!(accessor.getBlockEntity() instanceof SolarWeatherStationBlockEntity be))
            return;
        SolarEvent event = be.getCurrentEvent();
        ResourceLocation key = be.getCurrentEventKey();
        if(event == null || key == null)
            return;
        data.putString("event", key.toString());
        data.putInt("noise", Math.round(event.getNoise() * 100.0F));
        data.putInt("level", be.getConditionLevel());
        data.putBoolean("open", be.isReportedBandOpen());
    }

    @Override
    public ResourceLocation getUid() {
        return Radiocraft.id("solar_weather_station");
    }
}
