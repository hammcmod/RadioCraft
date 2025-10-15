package com.arrl.radiocraft.client.render;

import com.arrl.radiocraft.common.blockentities.DeskChargerBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/**
 * Layer that delegates to AutoGlowingGeoLayer only when the desk charger LED should be lit.
 * The LED state is derived from the world game time so it blinks: normal -> emissive -> normal...
 */
public class BlinkingAutoGlowingGeoLayer<T extends GeoAnimatable> extends AutoGlowingGeoLayer<T> {

    public BlinkingAutoGlowingGeoLayer(GeoRenderer<T> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, T animatable, BakedGeoModel bakedModel, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        try {
            if (animatable instanceof DeskChargerBlockEntity be) {
                if (be.getLevel() != null) {
                    long time = be.getLevel().getGameTime();
                    // Blink every 10 ticks (0.5s): on for 10 ticks, off for 10 ticks
                    boolean ledOn = ((time / 10L) % 2L) == 0L;
                    if (ledOn) {
                        super.render(poseStack, animatable, bakedModel, renderType, bufferSource, buffer, partialTick, packedLight, packedOverlay);
                    }
                    return;
                }
            }
        } catch (Exception e) {
            // Ignore any unexpected casting issues so rendering doesn't break
        }
        // Default: do nothing (no glow)
    }
}