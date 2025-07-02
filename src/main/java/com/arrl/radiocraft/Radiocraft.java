package com.arrl.radiocraft;

import com.arrl.radiocraft.common.init.*;
import com.arrl.radiocraft.common.network.RadiocraftNetworking;
import com.arrl.radiocraft.compat.TopCompat;
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
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.slf4j.Logger;

import java.util.Random;

@Mod(Radiocraft.MOD_ID)
public class Radiocraft {

    public static final String MOD_ID = "radiocraft";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final Random RANDOM = new Random();
    public static final boolean IS_DEVELOPMENT_ENV = System.getenv("RADIOCRAFT_DEV_ENV") != null;
    public static final String NETWORK_VERSION = "1";

    public Radiocraft(IEventBus modEventBus) {
        registerRegistries(modEventBus);

        TopCompatRegistry tcr = new TopCompatRegistry();
        modEventBus.register(tcr);

        ModLoadingContext.get().getActiveContainer().registerConfig(ModConfig.Type.COMMON, CommonConfig.SPEC, Radiocraft.MOD_ID + "-common.toml");
        ModLoadingContext.get().getActiveContainer().registerConfig(ModConfig.Type.SERVER, RadiocraftServerConfig.SPEC, Radiocraft.MOD_ID + "-server.toml");
    }

    // This is probably not the best/easiest way to do this, but I couldn't get it to cooperate otherwise, and this is perfectly valid.
    static class TopCompatRegistry {
        @SubscribeEvent
        public void commonSetup(FMLCommonSetupEvent event) {
            TopCompat.register();
        }
    }

    // Registering deferred registries to the event bus
    private static void registerRegistries(IEventBus modEventBus) {

        RadiocraftAntennaTypes.register();
        BENetworkTypes.register();
        RadiocraftBlocks.BLOCKS.register(modEventBus);
        RadiocraftItems.ITEMS.register(modEventBus);
        RadiocraftBlockEntities.BLOCK_ENTITY_TYPES.register(modEventBus);
        RadiocraftEntityTypes.ENTITIES.register(modEventBus);
        RadiocraftMenuTypes.MENU_TYPES.register(modEventBus);
        RadiocraftSoundEvents.SOUND_EVENTS.register(modEventBus);
        RadiocraftTabs.CREATIVE_TABS.register(modEventBus);
        RadiocraftDataComponent.DATA_COMPONENTS.register(modEventBus);

        modEventBus.addListener(Radiocraft::gatherData);
        modEventBus.addListener(Radiocraft::registerPackets);
    }

    //Networking registration is done using the subscribeEvent method as it doesn't seem to have a differed registry implementation
    //Placed here for locational consistency with other registrations
    @SubscribeEvent
    public static void registerPackets(final RegisterPayloadHandlersEvent event) {
        // Sets the current network version
        final PayloadRegistrar registrar = event.registrar(NETWORK_VERSION);
        RadiocraftNetworking.register(registrar);
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
