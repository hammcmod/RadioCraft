package com.arrl.radiocraft;

import com.arrl.radiocraft.common.init.*;
import com.arrl.radiocraft.datagen.RadiocraftBlockstateProvider;
import com.arrl.radiocraft.datagen.RadiocraftLanguageProvider;
import com.arrl.radiocraft.common.init.RadiocraftEntityTypes;
import com.mojang.logging.LogUtils;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider.Factory;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import org.slf4j.Logger;

import java.util.Random;

@Mod(Radiocraft.MOD_ID)
public class Radiocraft {

    public static final String MOD_ID = "radiocraft";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final Random RANDOM = new Random();

    public Radiocraft() {
        registerRegistries();


        ModLoadingContext.get().getActiveContainer().registerConfig(ModConfig.Type.COMMON, CommonConfig.SPEC, Radiocraft.MOD_ID + "-common.toml");
        ModLoadingContext.get().getActiveContainer().registerConfig(ModConfig.Type.SERVER, RadiocraftServerConfig.SPEC, Radiocraft.MOD_ID + "-server.toml");
        //NeoForge.EVENT_BUS.register(this);
    }

    // Registering deferred registries to the event bus
    private static void registerRegistries() {

        IEventBus modEventBus = ModLoadingContext.get().getActiveContainer().getEventBus();

        RadiocraftAntennaTypes.register();
        BENetworkTypes.register();
        RadiocraftBlocks.BLOCKS.register(modEventBus);
        RadiocraftItems.ITEMS.register(modEventBus);
        RadiocraftBlockEntities.BLOCK_ENTITY_TYPES.register(modEventBus);
        RadiocraftEntityTypes.ENTITIES.register(modEventBus);
        RadiocraftMenuTypes.MENU_TYPES.register(modEventBus);
        RadiocraftSoundEvents.SOUND_EVENTS.register(modEventBus);
        RadiocraftTabs.CREATIVE_TABS.register(modEventBus);
        //RadiocraftPackets.registerPackets();

        modEventBus.addListener(Radiocraft::gatherData);
    }

    // Added to mod event bus, for data gen
    public static void gatherData(GatherDataEvent event) {
        DataGenerator gen = event.getGenerator();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();

        gen.addProvider(event.includeClient(), (Factory<RadiocraftLanguageProvider>) output -> new RadiocraftLanguageProvider(output, "en_us")); // Ugly cast due to ambiguous method sigs.
        gen.addProvider(event.includeClient(), (Factory<RadiocraftBlockstateProvider>) output -> new RadiocraftBlockstateProvider(output, existingFileHelper));
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public static String translationKey(String prefix, String suffix) {
        return String.format("%s.%s.%s", prefix, MOD_ID, suffix);
    }

}
