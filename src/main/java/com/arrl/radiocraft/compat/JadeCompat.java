package com.arrl.radiocraft.compat;

import com.arrl.radiocraft.common.blocks.antennas.AntennaCenterBlock;
import com.arrl.radiocraft.common.blocks.WireBlock;
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
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(AntennaWireProvider.INSTANCE, AntennaCenterBlock.class);
        registration.registerBlockComponent(AntennaInfoProvider.INSTANCE, AntennaCenterBlock.class);
        registration.registerBlockComponent(CoaxWireProvider.INSTANCE, WireBlock.class);
        registration.registerBlockComponent(RadioProvider.INSTANCE, RadioBlock.class);
    }
}
