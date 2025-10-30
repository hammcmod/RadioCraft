package com.arrl.radiocraft.common.items;

import com.arrl.radiocraft.Radiocraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.model.DefaultedBlockGeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;

/**
 * BlockItem for the Desk Charger that uses GeckoLib for rendering in hand/inventory/GUI.
 * Uses the same model as the block (geo/block/desk_charger.geo.json).
 */
public class DeskChargerBlockItem extends BlockItem implements GeoItem {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public DeskChargerBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private GeoItemRenderer<DeskChargerBlockItem> renderer;

            @Override
            public @Nullable BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                if (this.renderer == null) {
                    // Uses the block model from geo/block/desk_charger.geo.json
                    this.renderer = new GeoItemRenderer<>(new DefaultedBlockGeoModel<>(
                        Radiocraft.id("desk_charger")
                    )) {
                        @Override
                        public void renderByItem(net.minecraft.world.item.ItemStack stack, 
                                                net.minecraft.world.item.ItemDisplayContext transformType,
                                                com.mojang.blaze3d.vertex.PoseStack poseStack,
                                                net.minecraft.client.renderer.MultiBufferSource bufferSource,
                                                int packedLight, int packedOverlay) {
                            // Hide Radio bone when rendering as item (no radio in hand/inventory)
                            getGeoModel().getBone("Radio").ifPresent(bone -> bone.setHidden(true));
                            super.renderByItem(stack, transformType, poseStack, bufferSource, packedLight, packedOverlay);
                        }
                    };
                    // Use BlinkingAutoGlowingGeoLayer for consistent LED rendering with placed blocks.
                    // Items display a steady red LED since they lack BlockEntity energy logic.
                    this.renderer.addRenderLayer(new com.arrl.radiocraft.client.render.BlinkingAutoGlowingGeoLayer<>(this.renderer));
                }
                return this.renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // No animations needed for the item form
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
