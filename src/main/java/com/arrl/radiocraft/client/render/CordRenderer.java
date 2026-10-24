package com.arrl.radiocraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * A thin sagging cord between two points, drawn like a lead (and like the antenna wire, see
 * {@link com.arrl.radiocraft.client.entity.AntennaWireEntityRenderer}): two ribbons at right angles, so it reads from
 * any side, lit by the light at each end. Used for a radio's hand mic.
 */
public final class CordRenderer {

    private static final float WIDTH = 0.02F;
    private static final int STEPS = 24;
    private static final float SHADE = 0.12F; // A dark grey cord.

    private CordRenderer() {}

    /**
     * Draws the cord from {@code from} to {@code to}, both in world coordinates, with the pose stack at
     * {@code origin} (a block entity renderer's block).
     */
    public static void render(Level level, BlockPos origin, Vec3 from, Vec3 to, PoseStack poseStack, MultiBufferSource buffer) {
        float dx = (float)(to.x - from.x), dy = (float)(to.y - from.y), dz = (float)(to.z - from.z);
        int fromLight = LevelRenderer.getLightColor(level, BlockPos.containing(from));
        int toLight = LevelRenderer.getLightColor(level, BlockPos.containing(to));
        VertexConsumer consumer = buffer.getBuffer(RenderType.leash());

        poseStack.pushPose();
        poseStack.translate(from.x - origin.getX(), from.y - origin.getY(), from.z - origin.getZ());
        Matrix4f matrix = poseStack.last().pose();
        float horizontal = Mth.sqrt(dx * dx + dz * dz);
        float sideX = horizontal < 1e-4F ? WIDTH : dz / horizontal * WIDTH;
        float sideZ = horizontal < 1e-4F ? 0.0F : -dx / horizontal * WIDTH;
        ribbon(consumer, matrix, dx, dy, dz, fromLight, toLight, sideX, 0.0F, sideZ);  // Flat ribbon...
        ribbon(consumer, matrix, dx, dy, dz, fromLight, toLight, 0.0F, WIDTH, 0.0F);   // ...and an upright one.
        poseStack.popPose();
    }

    private static void ribbon(VertexConsumer consumer, Matrix4f matrix, float dx, float dy, float dz, int fromLight, int toLight, float ox, float oy, float oz) {
        for(int i = 0; i <= STEPS; i++)
            vertexPair(consumer, matrix, dx, dy, dz, fromLight, toLight, ox, oy, oz, i);
        for(int i = STEPS; i >= 0; i--) // Back along the other edge, so the strip closes on itself like vanilla's lead.
            vertexPair(consumer, matrix, dx, dy, dz, fromLight, toLight, -ox, -oy, -oz, i);
    }

    private static void vertexPair(VertexConsumer consumer, Matrix4f matrix, float dx, float dy, float dz, int fromLight, int toLight, float ox, float oy, float oz, int step) {
        float f = (float)step / STEPS;
        float sag = 0.25F * 4.0F * f * (1.0F - f); // Hangs a quarter of a block at the middle.
        float x = dx * f, y = dy * f - sag, z = dz * f;
        int light = LightTexture.pack((int)Mth.lerp(f, LightTexture.block(fromLight), LightTexture.block(toLight)),
                (int)Mth.lerp(f, LightTexture.sky(fromLight), LightTexture.sky(toLight)));
        float shade = step % 2 == 0 ? SHADE : SHADE * 0.8F;
        consumer.addVertex(matrix, x + ox, y + oy, z + oz).setColor(shade, shade, shade, 1.0F).setLight(light);
        consumer.addVertex(matrix, x - ox, y - oy, z - oz).setColor(shade, shade, shade, 1.0F).setLight(light);
    }
}
