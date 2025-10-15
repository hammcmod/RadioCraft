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

    // 1200-tick cycle (1 minute) for smooth red->green transition
    long time = level.getGameTime();

    // Blink every 10 ticks (half second)
    boolean ledOn = (time / 10L) % 2L == 0L;

    // Always perform an emissive pass. When ledOn is true, compute a smooth tint;
    // otherwise use black (no visible glow) between pulses.

    // Use the glowmask texture and apply a dynamic tint that smoothly interpolates
    // from red to green over a 1200-tick cycle. Between pulses render black (off).
    RenderType emissiveRenderType = RenderType.eyes(getGlowTexture(animatable));
    VertexConsumer emissiveBuffer = bufferSource.getBuffer(emissiveRenderType);

        float redTint = 0.0f;
        float greenTint = 0.0f;
        float blueTint = 0.0f;

        if (ledOn) {
            // Smooth progress over the full 1200-tick cycle (1 minute). Include partialTick for smoothness.
            float progress = ((time % 1200L) + partialTick) / 1200.0f; // 0..1 across red->green
            // Interpolate from red (1,0) to green (0,1)
            redTint = 1.0f - progress;
            greenTint = progress;
            blueTint = 0.0f;
        } else {
            // Off (black)
            redTint = 0.0f;
            greenTint = 0.0f;
            blueTint = 0.0f;
        }


    int r = Math.max(0, Math.min(255, (int) (redTint * 255.0f)));
    int g = Math.max(0, Math.min(255, (int) (greenTint * 255.0f)));
    int b = Math.max(0, Math.min(255, (int) (blueTint * 255.0f)));
    int colour = (255 << 24) | (r << 16) | (g << 8) | b;

    // Render with fullbright so tint appears strong regardless of environment.
    int fullBright = 15728880; // packed light value for full brightness (15,15)

        poseStack.pushPose();

    getRenderer().reRender(bakedModel, poseStack, bufferSource, animatable, emissiveRenderType, emissiveBuffer, partialTick, fullBright, packedOverlay, colour);
        poseStack.popPose();
    }
}