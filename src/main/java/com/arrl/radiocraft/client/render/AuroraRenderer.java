package com.arrl.radiocraft.client.render;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.client.RadiocraftClientValues;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.FogType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

/**
 * Draws the aurora in the night sky while one is lit ({@link RadiocraftClientValues#AURORA_ACTIVE}) and the player is
 * near or past the aurora latitude. Procedural: a few curtains of additive quads toward the nearer pole (north is -Z),
 * bright green along a sharp lower edge, fading up through teal into violet, with slowly drifting vertical rays. It fades
 * in over the last 1000 blocks before the latitude, with nightfall, and out in rain.
 */
@EventBusSubscriber(modid = Radiocraft.MOD_ID, value = Dist.CLIENT)
public class AuroraRenderer {

	private static final float RADIUS = 100.0F; // Same distance as the stars.
	private static final int SEGMENTS = 96;
	private static final int CURTAINS = 3;
	/** Blocks before the aurora latitude over which it fades in. */
	private static final float FADE_BLOCKS = 1000.0F;
	/** Milliseconds for the aurora to fade most of the way in or out when it starts or ends. */
	private static final float FADE_MILLIS = 4000.0F;

	private static float intensity = 0.0F;
	private static long lastFrameMillis = -1L;

	@SubscribeEvent
	public static void onRenderLevelStage(RenderLevelStageEvent event) {
		if(event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY)
			return;
		ClientLevel level = Minecraft.getInstance().level;
		if(level == null)
			return;

		long now = Util.getMillis();
		float dt = lastFrameMillis < 0 ? 0.0F : Math.min(1000.0F, now - lastFrameMillis);
		lastFrameMillis = now;
		float target = RadiocraftClientValues.AURORA_ACTIVE ? 1.0F : 0.0F;
		intensity += (target - intensity) * (1.0F - (float)Math.exp(-dt / FADE_MILLIS));

		Camera camera = event.getCamera();
		float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
		float visibility = intensity * latitudeFade(camera.getPosition().z) * nightFactor(level, partialTick) * (1.0F - level.getRainLevel(partialTick));
		if(visibility < 0.005F || level.effects().skyType() != DimensionSpecialEffects.SkyType.NORMAL || camera.getFluidInCamera() != FogType.NONE)
			return;

		float seconds = (level.getGameTime() + partialTick) / 20.0F;
		float pole = camera.getPosition().z > 0.0D ? 1.0F : -1.0F; // Look toward the nearer pole: north (-Z) in the north.
		PoseStack poseStack = new PoseStack();
		poseStack.mulPose(event.getModelViewMatrix());
		draw(poseStack.last().pose(), seconds, pole, visibility);
	}

	/** 0 nearer the equator than the fade band, 1 at and past the aurora latitude. */
	private static float latitudeFade(double z) {
		float distanceFromEquator = (float)Math.abs(z);
		return Mth.clamp((distanceFromEquator - (RadiocraftClientValues.AURORA_LATITUDE - FADE_BLOCKS)) / FADE_BLOCKS, 0.0F, 1.0F);
	}

	/** 0 by day, 1 once the stars are fully out. */
	private static float nightFactor(ClientLevel level, float partialTick) {
		return Mth.clamp(level.getStarBrightness(partialTick) * 2.0F, 0.0F, 1.0F);
	}

	private static void draw(Matrix4f matrix, float t, float pole, float visibility) {
		RenderSystem.enableBlend();
		RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE); // Additive: light adds up.
		RenderSystem.depthMask(false);
		RenderSystem.disableCull();
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		FogRenderer.setupNoFog();

		BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		for(int k = 0; k < CURTAINS; k++)
			curtain(buffer, matrix, k, t, pole, visibility);
		BufferUploader.drawWithShader(buffer.buildOrThrow());

		RenderSystem.enableCull();
		RenderSystem.depthMask(true);
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableBlend();
	}

	/**
	 * One curtain: a band across the polar sky, {@link #SEGMENTS} columns wide, each column four rows tall (a soft edge
	 * below the bright base, the base, the green body and the violet top).
	 */
	private static void curtain(BufferBuilder buffer, Matrix4f matrix, int k, float t, float pole, float visibility) {
		float span = 150.0F - 25.0F * k; // Degrees of sky the curtain covers.
		float centre = 18.0F * (k - 1) + 10.0F * Mth.sin(t * 0.013F + k * 2.1F); // Drifts slowly around the pole.
		float baseElevation = 9.0F + 7.0F * k;
		float strength = visibility * (k == 0 ? 1.0F : 0.7F);

		float[][] previous = null;
		for(int i = 0; i <= SEGMENTS; i++) {
			float u = (float)i / SEGMENTS;
			// Folds: the curtain swings sideways in slow waves, like a ribbon in wind.
			float azimuth = centre + (u - 0.5F) * span + 7.0F * Mth.sin(u * 8.0F + t * 0.11F + k * 1.7F) + 3.0F * Mth.sin(u * 23.0F - t * 0.23F + k);
			float bottom = baseElevation + 3.0F * Mth.sin(u * 5.0F + t * 0.07F + k);
			float height = 24.0F + 9.0F * Mth.sin(u * 3.0F - t * 0.05F + k * 0.8F) + 5.0F * Mth.sin(u * 11.0F + t * 0.17F);
			float envelope = (float)Math.pow(Mth.sin((float)Math.PI * u), 0.7D); // Fade out at both ends.
			float rays = 0.45F + 0.55F * (0.5F + 0.5F * Mth.sin(u * 61.0F + t * 0.8F + k * 2.0F)) * (0.6F + 0.4F * Mth.sin(u * 13.0F - t * 0.25F + k));
			float a = strength * envelope * rays;

			float[][] column = {
					point(azimuth, bottom - 2.5F, pole, 0.30F, 1.00F, 0.55F, 0.0F),
					point(azimuth, bottom, pole, 0.35F, 1.00F, 0.55F, 0.85F * a),
					point(azimuth, bottom + height * 0.35F, pole, 0.20F, 0.95F, 0.70F, 0.45F * a),
					point(azimuth, bottom + height, pole, 0.65F, 0.25F, 0.90F, 0.0F)
			};
			if(previous != null)
				for(int row = 0; row < column.length - 1; row++) {
					vertex(buffer, matrix, previous[row]);
					vertex(buffer, matrix, column[row]);
					vertex(buffer, matrix, column[row + 1]);
					vertex(buffer, matrix, previous[row + 1]);
				}
			previous = column;
		}
	}

	/** A point on the sky sphere, {@code azimuth} degrees around from the pole and {@code elevation} degrees up. */
	private static float[] point(float azimuth, float elevation, float pole, float r, float g, float b, float a) {
		float az = azimuth * Mth.DEG_TO_RAD;
		float el = elevation * Mth.DEG_TO_RAD;
		float horizontal = Mth.cos(el) * RADIUS;
		return new float[] { Mth.sin(az) * horizontal, Mth.sin(el) * RADIUS, pole * Mth.cos(az) * horizontal, r, g, b, a };
	}

	/** Adds {@code p} (x, y, z, r, g, b, a in sky space) through the sky's rotation. */
	private static void vertex(BufferBuilder buffer, Matrix4f matrix, float[] p) {
		buffer.addVertex(matrix, p[0], p[1], p[2]).setColor(p[3], p[4], p[5], p[6]);
	}
}
