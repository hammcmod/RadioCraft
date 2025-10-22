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

/**
 * Custom GeckoLib render layer that applies emissive glowing effects with dynamic LED coloring.
 * <p>
 * This layer handles the LED indicator rendering for the Desk Charger, providing:
 * <ul>
 *   <li>Color interpolation from red (low charge) to green (full charge)</li>
 *   <li>Blinking animation while charging</li>
 *   <li>Steady lighting when fully charged or idle</li>
 *   <li>Off state (dark) when no radio is present or no energy available</li>
 * </ul>
 * <p>
 * For items (non-BlockEntity contexts), displays a steady red LED as a visual indicator.
 */
public class BlinkingAutoGlowingGeoLayer<T extends GeoAnimatable> extends GeoRenderLayer<T> {

    public BlinkingAutoGlowingGeoLayer(GeoRenderer<T> renderer) {
        super(renderer);
    }

    /**
     * Obtains the glow texture location by appending "_glowmask" suffix to the base texture.
     *
     * @param animatable The animatable object being rendered
     * @return ResourceLocation pointing to the glowmask texture
     */
    protected ResourceLocation getGlowTexture(T animatable) {
        ResourceLocation texture = getRenderer().getTextureLocation(animatable);
        return ResourceLocation.fromNamespaceAndPath(texture.getNamespace(), texture.getPath().replace(".png", "_glowmask.png"));
    }

    @Override
    public void render(PoseStack poseStack, T animatable, BakedGeoModel bakedModel, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        float redTint = 1.0f;
        float greenTint = 0.0f;
        float blueTint = 0.0f;

        if (animatable instanceof DeskChargerBlockEntity be) {
            Level level = be.getLevel();
            if (level == null) return;

            var stack = be.inventory.getStackInSlot(0);
            if (stack == null || stack.isEmpty()) {
                redTint = greenTint = blueTint = 0.0f;
            } else {
                var radioEnergy = stack.getCapability(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.ITEM);
                var blockEnergy = be.energyStorage;

                if (radioEnergy != null && blockEnergy != null) {
                    int radioStored = radioEnergy.getEnergyStored();
                    int radioMax = radioEnergy.getMaxEnergyStored();
                    int blockStored = blockEnergy.getEnergyStored();

                    if (radioStored <= 0 || blockStored <= 0) {
                        redTint = greenTint = blueTint = 0.0f;
                    } else {
                        float charge = (float) radioStored / (float) Math.max(1, radioMax);
                        redTint = 1.0f - charge;
                        greenTint = charge;
                        blueTint = 0.0f;

                        boolean charging = radioStored < radioMax && (blockStored > 0 || be.isInfinite());

                        if (charging) {
                            long t = level.getGameTime();
                            int period = 8;
                            int onFor = 4;
                            boolean lightOn = (t % period) < onFor;
                            
                            if (!lightOn) {
                                redTint = greenTint = blueTint = 0.0f;
                            }
                        }
                    }
                }
            }
        }

        int r = Math.max(0, Math.min(255, (int) (redTint * 255.0f)));
        int g = Math.max(0, Math.min(255, (int) (greenTint * 255.0f)));
        int b = Math.max(0, Math.min(255, (int) (blueTint * 255.0f)));
        int color = (255 << 24) | (r << 16) | (g << 8) | b;

        // fullBright = (15 << 20) | (15 << 4) = 0xF000F0 = 15728880 (sky light 15, block light 15)
        int fullBright = 15728880;

        RenderType emissiveRenderType = RenderType.entityTranslucentEmissive(getGlowTexture(animatable));
        VertexConsumer emissiveBuffer = bufferSource.getBuffer(emissiveRenderType);

        poseStack.pushPose();
        getRenderer().reRender(bakedModel, poseStack, bufferSource, animatable, emissiveRenderType, emissiveBuffer, partialTick, fullBright, packedOverlay, color);
        poseStack.popPose();
    }
}