package com.arrl.radiocraft.compat;

import com.arrl.radiocraft.common.blocks.TeletypeBlock;
import com.arrl.radiocraft.common.blocks.antennas.AntennaCenterBlock;
import com.arrl.radiocraft.common.blocks.WireBlock;
import com.arrl.radiocraft.common.blocks.power.LargeBatteryBlock;
import com.arrl.radiocraft.common.blocks.power.SolarPanelBlock;
import com.arrl.radiocraft.common.blocks.radios.*;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class JadeCompat implements IWailaPlugin {

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(AntennaWireProvider.INSTANCE, AntennaCenterBlock.class);
        registration.registerBlockDataProvider(AntennaInfoProvider.INSTANCE, AntennaCenterBlock.class);
        registration.registerBlockDataProvider(CoaxWireProvider.INSTANCE, WireBlock.class);
        registration.registerBlockDataProvider(CoaxWireProvider.INSTANCE, com.arrl.radiocraft.common.blocks.WireBundleBlock.class);
        registration.registerBlockDataProvider(RadioProvider.INSTANCE, HFRadio10mBlock.class);
        registration.registerBlockDataProvider(RadioProvider.INSTANCE, HFRadio20mBlock.class);
        registration.registerBlockDataProvider(RadioProvider.INSTANCE, HFRadio40mBlock.class);
        registration.registerBlockDataProvider(RadioProvider.INSTANCE, HFRadio80mBlock.class);
        registration.registerBlockDataProvider(RadioProvider.INSTANCE, HFRadioAllBandBlock.class);
        registration.registerBlockDataProvider(RadioProvider.INSTANCE, HFReceiverBlock.class);
        registration.registerBlockDataProvider(RadioProvider.INSTANCE, QRPRadio20mBlock.class);
        registration.registerBlockDataProvider(RadioProvider.INSTANCE, QRPRadio40mBlock.class);
        registration.registerBlockDataProvider(RadioProvider.INSTANCE, VHFBaseStationBlock.class);
        registration.registerBlockDataProvider(RadioProvider.INSTANCE, VHFReceiverBlock.class);
        registration.registerBlockDataProvider(ParrotControllerProvider.INSTANCE, ParrotControllerBlock.class);
        registration.registerBlockDataProvider(TeletypeProvider.INSTANCE, TeletypeBlock.class);
        registration.registerBlockDataProvider(BatteryProvider.INSTANCE, LargeBatteryBlock.class);
        registration.registerBlockDataProvider(SolarPanelProvider.INSTANCE, SolarPanelBlock.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(AntennaWireProvider.INSTANCE, AntennaCenterBlock.class);
        registration.registerBlockComponent(AntennaInfoProvider.INSTANCE, AntennaCenterBlock.class);
        registration.registerBlockComponent(CoaxWireProvider.INSTANCE, WireBlock.class);
        registration.registerBlockComponent(CoaxWireProvider.INSTANCE, com.arrl.radiocraft.common.blocks.WireBundleBlock.class);
        registration.registerBlockComponent(RadioProvider.INSTANCE, RadioBlock.class);
        registration.registerBlockComponent(ParrotControllerProvider.INSTANCE, ParrotControllerBlock.class);
        registration.registerBlockComponent(TeletypeProvider.INSTANCE, TeletypeBlock.class);
        registration.registerBlockComponent(BatteryProvider.INSTANCE, LargeBatteryBlock.class);
        registration.registerBlockComponent(SolarPanelProvider.INSTANCE, SolarPanelBlock.class);
    }
}
