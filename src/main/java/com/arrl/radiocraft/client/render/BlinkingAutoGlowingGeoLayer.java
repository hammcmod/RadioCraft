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
        // Determine LED color based on the VHF handheld energy stored in the slot and block energy
        float redTint = 0.0f;
        float greenTint = 0.0f;
        float blueTint = 0.0f;

        if (animatable instanceof DeskChargerBlockEntity be) {
            Level level = be.getLevel();
            if (level == null) return;

            // get radio in slot
            var stack = be.inventory.getStackInSlot(0);
            if (stack != null && !stack.isEmpty()) {
                var radioEnergy = stack.getCapability(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.ITEM);
                var blockEnergy = be.energyStorage;

                if (radioEnergy != null && blockEnergy != null) {
                    int radioStored = radioEnergy.getEnergyStored();
                    int radioMax = radioEnergy.getMaxEnergyStored();
                    int blockStored = blockEnergy.getEnergyStored();

                    // If either has no energy, LED is black
                    if (radioStored <= 0 || blockStored <= 0) {
                        redTint = greenTint = blueTint = 0.0f;
                    } else {
                        // Use radio charge percentage (0..1) to interpolate from red->green
                        float charge = (float) radioStored / (float) Math.max(1, radioMax);
                        redTint = 1.0f - charge;
                        greenTint = charge;
                        blueTint = 0.0f;

                        // Blinking logic:
                        // - charging (radio not full and charger has energy): fast blink
                        // - full (radioStored >= radioMax): slow blink
                        boolean charging = radioStored < radioMax && (blockStored > 0 || be.isInfinite());
                        boolean full = radioStored >= radioMax;

                        long t = level.getGameTime();
                        boolean lightOn;

                        if (charging) {
                            int period = 8; // ticks
                            int onFor = 4;   // ticks ON
                            lightOn = (t % period) < onFor;
                        } else if (full) {
                            // when fully charged, LED should be steady on (no blinking)
                            lightOn = true;
                        } else {
                            // steady on when not charging and not empty
                            lightOn = true;
                        }

                        if (!lightOn) {
                            redTint = greenTint = blueTint = 0.0f;
                        }
                    }
                }
            }
        }

        int r = Math.max(0, Math.min(255, (int) (redTint * 255.0f)));
        int g = Math.max(0, Math.min(255, (int) (greenTint * 255.0f)));
        int b = Math.max(0, Math.min(255, (int) (blueTint * 255.0f)));
        int colour = (255 << 24) | (r << 16) | (g << 8) | b;

        // Render with fullbright so tint appears strong regardless of environment.
        int fullBright = 15728880; // packed light value for full brightness (15,15)

        RenderType emissiveRenderType = RenderType.eyes(getGlowTexture(animatable));
        VertexConsumer emissiveBuffer = bufferSource.getBuffer(emissiveRenderType);

        poseStack.pushPose();
        getRenderer().reRender(bakedModel, poseStack, bufferSource, animatable, emissiveRenderType, emissiveBuffer, partialTick, fullBright, packedOverlay, colour);
        poseStack.popPose();
    }
}