package com.arrl.radiocraft.compat;

import com.arrl.radiocraft.common.blocks.AbstractPowerNetworkBlock;
import com.arrl.radiocraft.common.blocks.SolarPanelBlock;
import com.arrl.radiocraft.common.blocks.WireBlock;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

import java.util.ArrayList;

@WailaPlugin
public class JadeCompat implements IWailaPlugin {

    @Override
    public void register(IWailaCommonRegistration registration) {
        // Register enhanced network debug provider for power-related blocks
        // registration.registerBlockDataProvider(EnhancedNetworkDebugProvider.INSTANCE, WireBlock.class);
        // registration.registerBlockDataProvider(EnhancedNetworkDebugProvider.INSTANCE, AbstractPowerNetworkBlock.class);
        registration.registerBlockDataProvider(SolarPanelProvider.INSTANCE, SolarPanelBlock.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        // Register enhanced network debug provider for power-related blocks
        // registration.registerBlockComponent(EnhancedNetworkDebugProvider.INSTANCE, WireBlock.class);
        // registration.registerBlockComponent(EnhancedNetworkDebugProvider.INSTANCE, AbstractPowerNetworkBlock.class);
        registration.registerBlockComponent(SolarPanelProvider.INSTANCE, SolarPanelBlock.class);
    }
}