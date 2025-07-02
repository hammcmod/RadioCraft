package com.arrl.radiocraft.compat;

import com.arrl.radiocraft.common.blocks.AntennaCenterBlock;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

import java.util.ArrayList;

@WailaPlugin
public class JadeCompat implements IWailaPlugin {

    @Override
    public void register(IWailaCommonRegistration registration) {
        //TODO register data providers
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        //TODO register component providers, icon providers, callbacks, and config options here

        ArrayList<Class> blocks = new ArrayList<>();

        blocks.add(AntennaCenterBlock.class);

        blocks.forEach(
                clazz -> registration.registerBlockComponent(JadeProbe.getProbe(), clazz)
        );
    }
}