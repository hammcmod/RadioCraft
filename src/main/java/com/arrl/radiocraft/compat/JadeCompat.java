package com.arrl.radiocraft.compat;

import com.arrl.radiocraft.common.blocks.AntennaCenterBlock;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

import java.util.ArrayList;

@WailaPlugin
public class JadeCompat implements IWailaPlugin {

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(AntennaWireProvider.INSTANCE, AntennaCenterBlock.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(AntennaWireProvider.INSTANCE, AntennaCenterBlock.class);
    }
}