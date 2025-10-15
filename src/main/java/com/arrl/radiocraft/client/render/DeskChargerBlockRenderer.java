package com.arrl.radiocraft.client.render;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.blockentities.DeskChargerBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.model.DefaultedBlockGeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/** Minimal Geo renderer for the desk charger - no animations. */
public class DeskChargerBlockRenderer extends GeoBlockRenderer<DeskChargerBlockEntity> {

    public DeskChargerBlockRenderer(BlockEntityRendererProvider.Context context) {
        super(new DeskChargerModel());
    // Replace the line below
    // addRenderLayer(new AutoGlowingGeoLayer<>(this));
    // with this one:
    addRenderLayer(new BlinkingAutoGlowingGeoLayer<>(this));
    }

    public static class DeskChargerModel extends DefaultedBlockGeoModel<DeskChargerBlockEntity> {
        public DeskChargerModel() {
            super(ResourceLocation.fromNamespaceAndPath(Radiocraft.MOD_ID, "desk_charger"));
        }
    }

    @Override
    public void render(DeskChargerBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        Level level = blockEntity.getLevel();
        BlockPos pos = blockEntity.getBlockPos();

        if (level != null) {
            packedLight = LevelRenderer.getLightColor(level, pos);
        }

        super.render(blockEntity, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
    }
}