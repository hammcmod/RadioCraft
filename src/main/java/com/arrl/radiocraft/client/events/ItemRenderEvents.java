
package com.arrl.radiocraft.client.events;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.init.RadiocraftItems;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.function.Supplier;

public class ItemRenderEvents {

    private static final ResourceLocation DEV_BACKGROUND_TEXTURE = ResourceLocation.fromNamespaceAndPath(Radiocraft.MOD_ID, "textures/gui/dev_item_background.png");

    // Separate lists for different item types to avoid generic conflicts
    private static final List<Supplier<? extends Item>> DEV_REGULAR_ITEMS = List.of(
            RadiocraftItems.HAND_MICROPHONE,
            RadiocraftItems.HF_CIRCUIT_BOARD,
            RadiocraftItems.FERRITE_CORE,
            RadiocraftItems.COAXIAL_CORE,
            RadiocraftItems.ANTENNA_ANALYZER,
            RadiocraftItems.WATERPROOF_WIRE
    );

    private static final List<Supplier<BlockItem>> DEV_BLOCK_ITEMS = List.of(
            RadiocraftItems.SOLAR_PANEL,
            RadiocraftItems.LARGE_BATTERY,
            RadiocraftItems.CHARGE_CONTROLLER,
            RadiocraftItems.SOLAR_WEATHER_STATION,
            RadiocraftItems.VHF_BASE_STATION,
            RadiocraftItems.VHF_RECEIVER,
            RadiocraftItems.VHF_REPEATER,
            RadiocraftItems.HF_RADIO_10M,
            RadiocraftItems.HF_RADIO_20M,
            RadiocraftItems.HF_RADIO_40M,
            RadiocraftItems.HF_RADIO_80M,
            RadiocraftItems.HF_RECEIVER,
            RadiocraftItems.ALL_BAND_RADIO,
            RadiocraftItems.QRP_RADIO_20M,
            RadiocraftItems.QRP_RADIO_40M,
            RadiocraftItems.DUPLEXER,
            RadiocraftItems.ANTENNA_TUNER,
            RadiocraftItems.ANTENNA_CONNECTOR,
            RadiocraftItems.BALUN_ONE_TO_ONE,
            RadiocraftItems.BALUN_TWO_TO_ONE,
            RadiocraftItems.COAX_WIRE,
            RadiocraftItems.DIGITAL_INTERFACE,
            RadiocraftItems.YAGI_ANTENNA,
            RadiocraftItems.J_POLE_ANTENNA,
            RadiocraftItems.SLIM_JIM_ANTENNA
    );

    private static final List<DeferredHolder<Item, ?>> DEV_SPECIAL_ITEMS = List.of(
            RadiocraftItems.ANTENNA_POLE,
            RadiocraftItems.SMALL_BATTERY,
            RadiocraftItems.ANTENNA_WIRE
    );

    // Lazily initialized set of actual items
    private static Set<Item> devItems = null;

    private static Set<Item> getDevItems() {
        if (devItems == null) {
            devItems = new HashSet<>();

            // Add regular items
            for (Supplier<? extends Item> holder : DEV_REGULAR_ITEMS) {
                try {
                    Item item = holder.get();
                    if (item != null) {
                        devItems.add(item);
                    }
                } catch (Exception e) {
                    // During data generation, items might not be bound yet
                }
            }

            // Add block items
            for (Supplier<BlockItem> holder : DEV_BLOCK_ITEMS) {
                try {
                    Item item = holder.get();
                    if (item != null) {
                        devItems.add(item);
                    }
                } catch (Exception e) {
                    // During data generation, items might not be bound yet
                }
            }

            // Add special items
            for (DeferredHolder<Item, ?> holder : DEV_SPECIAL_ITEMS) {
                try {
                    Item item = holder.get();
                    if (item != null) {
                        devItems.add(item);
                    }
                } catch (Exception e) {
                    // During data generation, items might not be bound yet
                }
            }
        }
        return devItems;
    }

    public static boolean isDevItem(ItemStack itemStack) {
        return Radiocraft.IS_DEVELOPMENT_ENV && getDevItems().contains(itemStack.getItem());
    }

    public static void renderDevBackground(GuiGraphics guiGraphics, int x, int y) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.5F); // Semi-transparent
        guiGraphics.blit(DEV_BACKGROUND_TEXTURE, x, y, 0, 0, 16, 16, 16, 16);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F); // Reset color
    }
}