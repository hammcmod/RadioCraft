package com.arrl.radiocraft.client.render;

import com.arrl.radiocraft.common.blockentities.MicrowaveDishBlockEntity;
import com.arrl.radiocraft.common.blocks.MicrowaveHornBlock;
import com.arrl.radiocraft.common.radio.DirectionFinding;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * The microwave horn, drawn whole from its placed part and turned left or right to where it points: its 16 part
 * models (the blocks themselves draw nothing, {@link MicrowaveHornBlock#getRenderShape}) turned about the middle of its
 * base by the azimuth's difference from the way it was placed. Pointing up or down happens inside the horn and does
 * not show. The blocks, and so the hitboxes, stay where they were placed.
 */
public class MicrowaveHornRenderer implements BlockEntityRenderer<MicrowaveDishBlockEntity> {

	public MicrowaveHornRenderer(BlockEntityRendererProvider.Context context) {}

	@Override
	public void render(MicrowaveDishBlockEntity horn, float partialTick, @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight, int packedOverlay) {
		Level level = horn.getLevel();
		BlockState originState = horn.getBlockState();
		if(level == null || !(originState.getBlock() instanceof MicrowaveHornBlock block))
			return;
		BlockPos origin = horn.getBlockPos();
		Direction facing = originState.getValue(MicrowaveHornBlock.FACING);
		double placed = DirectionFinding.normalize(facing.toYRot() + 180.0D); // The azimuth it was placed pointing.
		float turn = (float)(horn.getAzimuth() - placed);
		double[] feed = MicrowaveHornBlock.feedXZ(origin, facing);
		double cx = feed[0] - origin.getX(), cz = feed[1] - origin.getZ(); // The middle of the base, from the origin block.

		BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
		RandomSource random = RandomSource.create();
		poseStack.pushPose();
		poseStack.translate(cx, 0.0D, cz);
		poseStack.mulPose(Axis.YP.rotationDegrees(-turn)); // Compass degrees run clockwise seen from above.
		poseStack.translate(-cx, 0.0D, -cz);
		for(Map.Entry<BlockPos, BlockState> part : block.parts(origin, facing)) {
			BlockPos at = part.getKey();
			BlockState state = level.getBlockState(at);
			if(!state.is(block))
				continue; // Half placed or half broken: draw what is there.
			BakedModel model = dispatcher.getBlockModel(state);
			int light = LevelRenderer.getLightColor(level, at);
			poseStack.pushPose();
			poseStack.translate(at.getX() - origin.getX(), at.getY() - origin.getY(), at.getZ() - origin.getZ());
			random.setSeed(state.getSeed(at));
			for(RenderType renderType : model.getRenderTypes(state, random, ModelData.EMPTY))
				dispatcher.getModelRenderer().renderModel(poseStack.last(), buffer.getBuffer(renderType), state, model, 1.0F, 1.0F, 1.0F,
						light, packedOverlay, ModelData.EMPTY, renderType);
			poseStack.popPose();
		}
		poseStack.popPose();
	}

	/** The whole horn, with room to turn: it is drawn from one block but stands two by two and four high. */
	@Override
	public @NotNull AABB getRenderBoundingBox(@NotNull MicrowaveDishBlockEntity horn) {
		BlockPos origin = horn.getBlockPos();
		return new AABB(origin).inflate(3.0D, 0.0D, 3.0D).expandTowards(0.0D, MicrowaveHornBlock.SIZE_Y, 0.0D);
	}

	@Override
	public boolean shouldRenderOffScreen(@NotNull MicrowaveDishBlockEntity horn) {
		return true;
	}

	/** A landmark: drawn out to 256 blocks, not the 64 most block entities stop at (still within render distance). */
	@Override
	public int getViewDistance() {
		return 256;
	}
}
