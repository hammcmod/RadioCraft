package com.arrl.radiocraft.client.entity;

import com.arrl.radiocraft.common.entities.AntennaWire;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

/**
    @author MoreThanHidden
    This class is used to render the AntennaWire
    @see net.minecraft.client.renderer.entity.LeashKnotRenderer
    @see net.minecraft.client.renderer.entity.MobRenderer (renderLeash)
 */
public class AntennaWireEntityRenderer extends EntityRenderer<AntennaWire> {

    public static final int WIRE_RENDER_STEPS = 24;
    public AntennaWireEntityRenderer(EntityRendererProvider.Context pContext) {
        super(pContext);
    }

    @Override
    public boolean shouldRender(@NotNull AntennaWire wire, @NotNull Frustum camera, double camX, double camY, double camZ) {
        Entity wireHolder = wire.getWireHolder();

        if(!wire.getPos().equals(wire.getEndPos())) // If wire is fully connected, render.
            return true;
        else // Also render if the wire holder is within the culling frustum.
            return wireHolder != null && camera.isVisible(wireHolder.getBoundingBoxForCulling());
    }

    @Override
    public void render(@NotNull AntennaWire wire, float yaw, float partialTick, @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight) {
        super.render(wire, yaw, partialTick, poseStack, buffer, packedLight);
        Player holder = wire.getWireHolder();
        if(holder != null)
            renderAntennaWire(wire, poseStack, buffer, holder);
        else
            renderAntennaWire(wire, poseStack, buffer, wire.getEndPart());
    }

    @Override
    public ResourceLocation getTextureLocation(AntennaWire entity) {
        return null;
    }

    /**
     * This method is used to render the wire between the two antenna couplers
     */
    private <E extends Entity> void renderAntennaWire(E toEntity, PoseStack poseStack, MultiBufferSource buffer, E fromEntity) {
        poseStack.pushPose();

        Vec3 toPos = toEntity.position();
        BlockPos toBlockPos = toEntity.blockPosition();
        Vec3 fromPos = fromEntity.position();
        BlockPos fromBlockPos = fromEntity.blockPosition();

        float xDiff = (float)(fromPos.x - toPos.x);
        float yDiff = (float)(fromPos.y - toPos.y);
        float zDiff = (float)(fromPos.z - toPos.z);

        float offsetMod = Mth.fastInvSqrt(xDiff * xDiff + zDiff * zDiff) * 0.025F / 2.0F;
        float xOffset = zDiff * offsetMod;
        float zOffset = xDiff * offsetMod;

        int toBlockLight = toEntity.level.getBrightness(LightLayer.BLOCK, toBlockPos);
        int fromBlockLight = fromEntity.level.getBrightness(LightLayer.BLOCK, fromBlockPos);
        int toSkyLight = toEntity.level.getBrightness(LightLayer.SKY, toBlockPos);
        int fromSkyLight = fromEntity.level.getBrightness(LightLayer.SKY, fromBlockPos);

        VertexConsumer consumer = buffer.getBuffer(RenderType.leash());
        Matrix4f posMatrix = poseStack.last().pose();
        poseStack.translate(0.0D, 0.0D, 0.0D); // Move poseStack to start of the wire

        for(int i = 0; i <= WIRE_RENDER_STEPS; ++i) // Add top vert pairs
            addVertexPair(consumer, posMatrix, xDiff, yDiff, zDiff, toBlockLight, fromBlockLight, toSkyLight, fromSkyLight, 0.025F, 0.025F, xOffset, zOffset, i, false);

        for(int i = WIRE_RENDER_STEPS; i >= 0; --i) // Add bottom vert pairs
            addVertexPair(consumer, posMatrix, xDiff, yDiff, zDiff, toBlockLight, fromBlockLight, toSkyLight, fromSkyLight, 0.025F, 0.0F, xOffset, zOffset, i, true);

        poseStack.popPose();
    }

    private static void addVertexPair(VertexConsumer consumer, Matrix4f matrix, float xDif, float yDif, float zDif,
                                      int toBlockLight, int fromBlockLight, int toSkyLight, int fromSkyLight,
                                      float width, float yOffset, float xOffset, float zOffset, int distance, boolean isKnot) {
        float f = (float)distance / WIRE_RENDER_STEPS;
        int blockLight = (int)Mth.lerp(f, toBlockLight, fromBlockLight);
        int skyLight = (int)Mth.lerp(f, toSkyLight, fromSkyLight);
        int packedLight = LightTexture.pack(blockLight, skyLight);
        float baseColor = distance % 2 == (isKnot ? 1 : 0) ? 0.7F : 1.0F;
        float red = 0.4F * baseColor;
        float green = 0.4F * baseColor;
        float blue = 0.4F * baseColor;
        float x = xDif * f;
        float y = yDif > 0.0F ? yDif * f * f : yDif - yDif * (1.0F - f) * (1.0F - f);
        float z = zDif * f;
        consumer.vertex(matrix, x - xOffset, y + yOffset, z + zOffset).color(red, green, blue, 1.0F).uv2(packedLight).endVertex();
        consumer.vertex(matrix, x + xOffset, y + width - yOffset, z - zOffset).color(red, green, blue, 1.0F).uv2(packedLight).endVertex();
    }

}
