package com.arrl.radiocraft;

import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.init.RadiocraftItems;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(Radiocraft.MODID)
public class Radiocraft {

    public static final String MODID = "radiocraft";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Radiocraft() {
        MinecraftForge.EVENT_BUS.register(this);
    }

}
