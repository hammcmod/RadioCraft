package com.arrl.radiocraft.client.render;

import com.arrl.radiocraft.entity.AntennaWireEntity;
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
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

/**
    @author MoreThanHidden
    This class is used to render the AntennaWireEntity
    @see net.minecraft.client.renderer.entity.LeashKnotRenderer
    @see net.minecraft.client.renderer.entity.MobRenderer (renderLeash)
 */
public class AntennaWireEntityRenderer extends EntityRenderer<AntennaWireEntity> {

    public static final int WIRE_RENDER_STEPS = 24;
    public AntennaWireEntityRenderer(EntityRendererProvider.Context pContext) {
        super(pContext);
    }

    @Override
    public boolean shouldRender(@NotNull AntennaWireEntity targetAntennaCoupler, @NotNull Frustum camera, double camX, double camY, double camZ) {
        if (super.shouldRender(targetAntennaCoupler, camera, camX, camY, camZ)) {
            return true;
        } else {
            Entity sourceAntennaCoupler = targetAntennaCoupler.getWireHolder();
            return sourceAntennaCoupler != null && camera.isVisible(sourceAntennaCoupler.getBoundingBoxForCulling());
        }
    }

    @Override
    public void render(@NotNull AntennaWireEntity targetAntennaCoupler, float yaw, float partialTick, @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight) {
        super.render(targetAntennaCoupler, yaw, partialTick, poseStack, buffer, packedLight);
        Entity sourceAntennaCoupler = targetAntennaCoupler.getWireHolder();
        if (sourceAntennaCoupler != null) {
            this.renderAntennaWire(targetAntennaCoupler, partialTick, poseStack, buffer, sourceAntennaCoupler);
        }
    }

    @Override
    public ResourceLocation getTextureLocation(AntennaWireEntity pEntity) {
        return null;
    }

    /**
     * This method is used to render the wire between the two antenna couplers
     */
    private <E extends Entity> void renderAntennaWire(E targetAntennaCoupler, float pPartialTicks, PoseStack pMatrixStack, MultiBufferSource pBuffer, E sourceAntennaCoupler) {
        pMatrixStack.pushPose();
        Vec3 vec3 = sourceAntennaCoupler.getRopeHoldPosition(pPartialTicks);
        double d0 = (double)(targetAntennaCoupler.yRotO * ((float)Math.PI / 180F)) + (Math.PI / 2D);
        Vec3 vec31 = targetAntennaCoupler.getLeashOffset(pPartialTicks);
        double d1 = Math.cos(d0) * vec31.z + Math.sin(d0) * vec31.x;
        double d2 = Math.sin(d0) * vec31.z - Math.cos(d0) * vec31.x;
        double d3 = Mth.lerp(pPartialTicks, targetAntennaCoupler.xo, targetAntennaCoupler.getX()) + d1;
        double d4 = Mth.lerp(pPartialTicks, targetAntennaCoupler.yo, targetAntennaCoupler.getY()) + vec31.y;
        double d5 = Mth.lerp(pPartialTicks, targetAntennaCoupler.zo, targetAntennaCoupler.getZ()) + d2;
        pMatrixStack.translate(d1, vec31.y, d2);
        float f = (float)(vec3.x - d3);
        float f1 = (float)(vec3.y - d4);
        float f2 = (float)(vec3.z - d5);
        VertexConsumer vertexconsumer = pBuffer.getBuffer(RenderType.leash());
        Matrix4f matrix4f = pMatrixStack.last().pose();
        float f4 = Mth.fastInvSqrt(f * f + f2 * f2) * 0.025F / 2.0F;
        float f5 = f2 * f4;
        float f6 = f * f4;
        BlockPos blockpos = new BlockPos(targetAntennaCoupler.getEyePosition(pPartialTicks));
        BlockPos blockpos1 = new BlockPos(sourceAntennaCoupler.getEyePosition(pPartialTicks));
        int i = targetAntennaCoupler.level.getBrightness(LightLayer.BLOCK, blockpos);
        int j = sourceAntennaCoupler.level.getBrightness(LightLayer.BLOCK, blockpos1);
        int k = targetAntennaCoupler.level.getBrightness(LightLayer.SKY, blockpos);
        int l = targetAntennaCoupler.level.getBrightness(LightLayer.SKY, blockpos1);

        for(int i1 = 0; i1 <= WIRE_RENDER_STEPS; ++i1) {
            addVertexPair(vertexconsumer, matrix4f, f, f1, f2, i, j, k, l, 0.025F, 0.025F, f5, f6, i1, false);
        }

        for(int j1 = WIRE_RENDER_STEPS; j1 >= 0; --j1) {
            addVertexPair(vertexconsumer, matrix4f, f, f1, f2, i, j, k, l, 0.025F, 0.0F, f5, f6, j1, true);
        }

        pMatrixStack.popPose();
    }

    private static void addVertexPair(VertexConsumer pConsumer, Matrix4f pMatrix, float x, float y, float z, int p_174313_, int p_174314_, int p_174315_, int p_174316_, float p_174317_, float p_174318_, float p_174319_, float p_174320_, int distance, boolean p_174322_) {
        float f = (float)distance / WIRE_RENDER_STEPS;
        int i = (int)Mth.lerp(f, (float)p_174313_, (float)p_174314_);
        int j = (int)Mth.lerp(f, (float)p_174315_, (float)p_174316_);
        int k = LightTexture.pack(i, j);
        float baseColor = distance % 2 == (p_174322_ ? 1 : 0) ? 0.7F : 1.0F;
        float r = 0.4F * baseColor;
        float g = 0.4F * baseColor;
        float b = 0.4F * baseColor;
        float f5 = x * f;
        float f6 = y > 0.0F ? y * f * f : y - y * (1.0F - f) * (1.0F - f);
        float f7 = z * f;
        pConsumer.vertex(pMatrix, f5 - p_174319_, f6 + p_174318_, f7 + p_174320_).color(r, g, b, 1.0F).uv2(k).endVertex();
        pConsumer.vertex(pMatrix, f5 + p_174319_, f6 + p_174317_ - p_174318_, f7 - p_174320_).color(r, g, b, 1.0F).uv2(k).endVertex();
    }

}
