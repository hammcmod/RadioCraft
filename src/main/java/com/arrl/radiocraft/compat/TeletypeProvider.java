package com.arrl.radiocraft.compat;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.blockentities.SolarWeatherStationBlockEntity;
import com.arrl.radiocraft.common.radio.solar.BandCondition;
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
        if(data.getBoolean("no_radio")) {
            tooltip.add(Component.translatable("message.radiocraft.solar_weather_station.no_radio"));
            return;
        }
        if(!data.contains("event")) {
            tooltip.add(Component.literal("Conditions: no data"));
            return;
        }
        ResourceLocation key = ResourceLocation.tryParse(data.getString("event"));
        tooltip.add(Component.literal("Conditions: ").append(SolarWeatherStationBlockEntity.eventName(key))
                .append(Component.literal(" (noise floor " + data.getInt("noise") + "%, level " + data.getInt("level") + ")")));
        CompoundTag bands = data.getCompound("bands");
        for(String band : SolarWeatherStationBlockEntity.REPORTED_BANDS) {
            BandCondition condition = BandCondition.byName(bands.getString(band));
            if(condition != null)
                tooltip.add(Component.literal(band + " band: ").append(SolarWeatherStationBlockEntity.conditionName(condition)));
        }
        if(data.contains("aurora"))
            tooltip.add(Component.Serializer.fromJson(data.getString("aurora"), accessor.getLevel().registryAccess()));
    }

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if(!(accessor.getBlockEntity() instanceof SolarWeatherStationBlockEntity be))
            return;
        if(!be.hasRadio()) {
            data.putBoolean("no_radio", true);
            return;
        }
        SolarEvent event = be.getCurrentEvent();
        ResourceLocation key = be.getCurrentEventKey();
        if(event == null || key == null)
            return;
        data.putString("event", key.toString());
        data.putInt("noise", Math.round(event.getNoise() * 100.0F));
        data.putInt("level", be.getConditionLevel());
        CompoundTag bands = new CompoundTag();
        for(String band : SolarWeatherStationBlockEntity.REPORTED_BANDS) {
            BandCondition condition = be.getBandCondition(band);
            if(condition != null)
                bands.putString(band, condition.getSerializedName());
        }
        data.put("bands", bands);
        Component aurora = be.auroraLine();
        if(aurora != null)
            data.putString("aurora", Component.Serializer.toJson(aurora, accessor.getLevel().registryAccess()));
    }

    @Override
    public ResourceLocation getUid() {
        return Radiocraft.id("solar_weather_station");
    }
}
