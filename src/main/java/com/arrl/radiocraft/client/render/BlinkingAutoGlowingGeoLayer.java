package com.arrl.radiocraft.client.render;

import com.arrl.radiocraft.common.blockentities.DeskChargerBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class BlinkingAutoGlowingGeoLayer<T extends GeoAnimatable> extends GeoRenderLayer<T> {

    public BlinkingAutoGlowingGeoLayer(GeoRenderer<T> renderer) {
        super(renderer);
    }

    // Method to obtain the glow texture (with suffix _glowmask)
    protected ResourceLocation getGlowTexture(T animatable) {
        ResourceLocation texture = getRenderer().getTextureLocation(animatable);
        return ResourceLocation.fromNamespaceAndPath(texture.getNamespace(), texture.getPath().replace(".png", "_glowmask.png"));
    }

    @Override
    public void render(PoseStack poseStack, T animatable, BakedGeoModel bakedModel, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        Level level = null;
        if (animatable instanceof DeskChargerBlockEntity be) {
            level = be.getLevel();
        }

        if (level == null) {
            return;
        }

    // 60-tick cycle (3 seconds) to alternate colors
    long time = level.getGameTime();
    long cycle = (time / 20L) % 3L; // Change color every 20 ticks (1 second)

    // Blink every 10 ticks (half second)
    boolean ledOn = (time / 10L) % 2L == 0L;

    // Always perform an emissive pass. When ledOn is true, use the color for the current cycle;
    // otherwise use black (no visible glow) between pulses.
        RenderType emissiveRenderType = RenderType.eyes(getGlowTexture(animatable));
        VertexConsumer emissiveBuffer = bufferSource.getBuffer(emissiveRenderType);

        float red = 0.0f;
        float green = 0.0f;
        float blue = 0.0f;

        if (ledOn) {
            if (cycle == 0) { // Red
                red = 1.0f;
            } else if (cycle == 1) { // Yellow
                red = 1.0f;
                green = 1.0f;
            } else { // Green
                green = 1.0f;
            }
        } else {
            // Between pulses, render black (no emissive color)
            red = 0.0f;
            green = 0.0f;
            blue = 0.0f;
        }

        int r = Math.max(0, Math.min(255, (int) (red * 255.0f)));
        int g = Math.max(0, Math.min(255, (int) (green * 255.0f)));
        int b = Math.max(0, Math.min(255, (int) (blue * 255.0f)));
        int colour = (255 << 24) | (r << 16) | (g << 8) | b;

    // Use the renderer's reRender method to draw the emissive glowmask with the chosen color.
    // Apply a tiny Z offset to avoid z-fighting with the base model.
    poseStack.pushPose();
    // Translate a very small amount along view Z to prevent depth fighting.
    poseStack.translate(0.0d, 0.0d, -0.0005d);
    getRenderer().reRender(bakedModel, poseStack, bufferSource, animatable, emissiveRenderType, emissiveBuffer, partialTick, packedLight, packedOverlay, colour);
    poseStack.popPose();
    }
}