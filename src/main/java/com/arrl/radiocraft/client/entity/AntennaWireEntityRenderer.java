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

    public AntennaWireEntityRenderer(EntityRendererProvider.Context pContext) {
        super(pContext);
    }

    @Override
    public boolean shouldRender(@NotNull AntennaWire wire, @NotNull Frustum camera, double camX, double camY, double camZ) {
        Entity wireHolder = wire.getWireHolder();

        if(!wire.blockPosition().equals(wire.getEndPos())) // If wire is fully connected, render.
            return true;
        else // Also render if the wire holder is within the culling frustum.
            return wireHolder != null && camera.isVisible(wireHolder.getBoundingBoxForCulling());
    }

    @Override
    public void render(@NotNull AntennaWire wire, float yaw, float partialTick, @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight) {
        super.render(wire, yaw, partialTick, poseStack, buffer, packedLight);
        Player holder = wire.getWireHolder();
        if(holder != null)
            renderAntennaWire(wire, partialTick, poseStack, buffer, holder);
        else
            renderAntennaWire(wire, partialTick, poseStack, buffer, wire.getEndPart());
    }

    @Override
    public ResourceLocation getTextureLocation(AntennaWire entity) {
        return null;
    }


    /**
     * I don't know why this method did not already exist on BlockPos,
     * but there's no facilities for converting a Vec3 to Vec3i?
     *
     * @param vec3 the position
     * @return the BlockPos
     */
    private static BlockPos blockPosFromVec3(Vec3 vec3) {
        int x = (int) vec3.x;
        int y = (int) vec3.y;
        int z = (int) vec3.z;

        return new BlockPos(x, y, z);
    }


    /**
     * This method is used to render the wire between the two antenna couplers
     */
    private <E extends Entity> void renderAntennaWire(E toEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffer, E fromEntity) {

        double lerpBodyAngle = (Mth.lerp(partialTick, toEntity.yRotO, toEntity.getYRot()) * Mth.DEG_TO_RAD) + Mth.HALF_PI;
        Vec3 leashOffset = toEntity.getLeashOffset(partialTick);
        double xAngleOffset = Math.cos(lerpBodyAngle) * leashOffset.z + Math.sin(lerpBodyAngle) * leashOffset.x;
        double zAngleOffset = Math.sin(lerpBodyAngle) * leashOffset.z - Math.cos(lerpBodyAngle) * leashOffset.x;

        Vec3 toPos = new Vec3(toEntity.getX() + xAngleOffset, toEntity.getY() + leashOffset.y, toEntity.getZ() + zAngleOffset);

        Vec3 fromPos = fromEntity.getRopeHoldPosition(partialTick);
        float xDiff = (float)(fromPos.x - toPos.x);
        float yDiff = (float)(fromPos.y - toPos.y);
        float zDiff = (float)(fromPos.z - toPos.z);

        float offsetMod = (float) (Mth.fastInvSqrt(xDiff * xDiff + zDiff * zDiff) * 0.025F / 2.0F);
        float xOffset = zDiff * offsetMod;
        float zOffset = xDiff * offsetMod;

        BlockPos toEyePos = new BlockPos(AntennaWireEntityRenderer.blockPosFromVec3(toEntity.getEyePosition(partialTick)));
        BlockPos fromEyePos = new BlockPos(AntennaWireEntityRenderer.blockPosFromVec3(fromEntity.getEyePosition(partialTick)));
        int toBlockLight = toEntity.level().getBrightness(LightLayer.BLOCK, toEyePos);
        int fromBlockLight = fromEntity.level().getBrightness(LightLayer.BLOCK, fromEyePos);
        int toSkyLight = toEntity.level().getBrightness(LightLayer.SKY, toEyePos);
        int fromSkyLight = fromEntity.level().getBrightness(LightLayer.SKY, fromEyePos);

        VertexConsumer consumer = buffer.getBuffer(RenderType.leash());
        Matrix4f posMatrix = poseStack.last().pose();

        int stepCount = fromEntity instanceof Player ? 24 : (int)Math.round(new Vec3(xDiff, yDiff, zDiff).length()) * 3;

        poseStack.pushPose();
        poseStack.translate(xAngleOffset, leashOffset.y, zAngleOffset);; // Move poseStack to start of the wire

        for(int i = 0; i <= stepCount; ++i) // Add top vert pairs
            addVertexPair(consumer, posMatrix, xDiff, yDiff, zDiff, toBlockLight, fromBlockLight, toSkyLight, fromSkyLight, 0.025F, 0.025F, xOffset, zOffset, i, false, stepCount);

        for(int i = stepCount; i >= 0; --i) // Add bottom vert pairs
            addVertexPair(consumer, posMatrix, xDiff, yDiff, zDiff, toBlockLight, fromBlockLight, toSkyLight, fromSkyLight, 0.025F, 0.0F, xOffset, zOffset, i, true, stepCount);

        poseStack.popPose();
    }

    private static void addVertexPair(VertexConsumer consumer, Matrix4f matrix, float xDif, float yDif, float zDif,
                                      int toBlockLight, int fromBlockLight, int toSkyLight, int fromSkyLight,
                                      float width, float yOffset, float xOffset, float zOffset, int distance, boolean isKnot, int stepCount) {
        float f = (float)distance / stepCount;
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
        consumer.addVertex(matrix, x - xOffset, y + yOffset, z + zOffset).setColor(red, green, blue, 1.0F).setUv(packedLight, 0f);
        consumer.addVertex(matrix, x + xOffset, y + width - yOffset, z - zOffset).setColor(red, green, blue, 1.0F).setUv(packedLight, 0f);
    }

}
