package com.arrl.radiocraft.client.render;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.items.DeskChargerBlockItem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.model.DefaultedItemGeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

/**
 * Item renderer for the Desk Charger when held in hand, inventory, or GUI.
 * Always hides the "Radio" bone since items don't contain a radio.
 */
public class DeskChargerItemRenderer extends GeoItemRenderer<DeskChargerBlockItem> {

    private static final DeskChargerItemModel MODEL = new DeskChargerItemModel();

    public DeskChargerItemRenderer() {
        super(MODEL);
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext transformType, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        // Always hide the Radio bone for items
        try {
            var bone = MODEL.getAnimationProcessor().getBone("Radio");
            if (bone != null) {
                bone.setHidden(true);
            }
        } catch (Exception e) {
            Radiocraft.LOGGER.debug("Could not hide Radio bone in item renderer: {}", e.getMessage());
        }
        
        super.renderByItem(stack, transformType, poseStack, bufferSource, packedLight, packedOverlay);
    }

    public static class DeskChargerItemModel extends DefaultedItemGeoModel<DeskChargerBlockItem> {
        public DeskChargerItemModel() {
            super(ResourceLocation.fromNamespaceAndPath(Radiocraft.MOD_ID, "desk_charger"));
        }

        @Override
        public ResourceLocation getModelResource(DeskChargerBlockItem animatable) {
            return super.getModelResource(animatable);
        }

        @Override
        public ResourceLocation getTextureResource(DeskChargerBlockItem animatable) {
            return super.getTextureResource(animatable);
        }

        @Override
        public ResourceLocation getAnimationResource(DeskChargerBlockItem animatable) {
            return super.getAnimationResource(animatable);
        }
    }
}
