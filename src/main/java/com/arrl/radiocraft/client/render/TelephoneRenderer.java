package com.arrl.radiocraft.client.render;

import com.arrl.radiocraft.common.blockentities.TelephoneBlockEntity;
import com.arrl.radiocraft.common.blocks.TelephoneBlock;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/** The handset's cord, from the phone to the hand of whoever has it up. */
public class TelephoneRenderer implements BlockEntityRenderer<TelephoneBlockEntity> {

	public TelephoneRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public void render(@NotNull TelephoneBlockEntity phone, float partialTick, @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int light, int overlay) {
		UUID holder = phone.getHandsetHolder();
		if(holder == null || phone.getLevel() == null)
			return;
		Player player = phone.getLevel().getPlayerByUUID(holder);
		if(player == null)
			return;
		CordRenderer.render(phone.getLevel(), phone.getBlockPos(), cordStart(phone.getBlockPos(), phone.getBlockState()), player.getRopeHoldPosition(partialTick), poseStack, buffer);
	}

	/** Where the cord leaves the phone: low on the cradle side of a desk phone, the bottom of a wall phone. */
	static Vec3 cordStart(BlockPos pos, BlockState state) {
		Vec3 centre = pos.getCenter();
		if(!state.hasProperty(TelephoneBlock.FACING))
			return centre;
		Direction facing = state.getValue(TelephoneBlock.FACING);
		if(state.getValue(TelephoneBlock.WALL)) // Low on the front of the phone, the wall behind it.
			return centre.add(-facing.getStepX() * 0.3D, -0.3D, -facing.getStepZ() * 0.3D);
		Direction left = facing.getClockWise(); // The cradle is on the phone's left as you face it.
		return centre.add(left.getStepX() * 0.2D, -0.4D, left.getStepZ() * 0.2D);
	}

	/** Big enough to keep drawing the cord while its far end is off to the side. */
	@Override
	public @NotNull AABB getRenderBoundingBox(@NotNull TelephoneBlockEntity phone) {
		AABB box = new AABB(phone.getBlockPos());
		return phone.getHandsetHolder() == null ? box : box.inflate(TelephoneBlockEntity.CORD_LENGTH + 2.0D);
	}
}
