package com.arrl.radiocraft.client.render;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.items.HFManpackItem;
import com.arrl.radiocraft.compat.curios.CuriosCompat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;

/**
 * Draws the HF manpack ({@code block/manpack_radio}) on a player's back while it is worn in the chest slot or the
 * Curios back slot: the pack's 8 by 11 pixel body against the back of the torso, straps forward, its top at the
 * shoulders, the whip standing above, turning with the body.
 */
public class ManpackLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

	public static final ModelResourceLocation MODEL = ModelResourceLocation.standalone(Radiocraft.id("block/manpack_radio"));

	public ManpackLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
		super(parent);
	}

	public static boolean wearing(LivingEntity entity) {
		if(entity.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof HFManpackItem)
			return true;
		return entity instanceof net.minecraft.world.entity.player.Player player && !CuriosCompat.wornManpacks(player).isEmpty();
	}

	@Override
	public void render(@NotNull PoseStack poseStack, @NotNull MultiBufferSource buffers, int light, @NotNull AbstractClientPlayer player,
					   float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
		if(!wearing(player) || player.isInvisible())
			return;
		render(getParentModel(), poseStack, buffers, light);
	}

	/** Draws the pack on {@code model}'s back (also used by the Curios renderer). */
	public static void render(HumanoidModel<?> model, PoseStack poseStack, MultiBufferSource buffers, int light) {
		BakedModel baked = Minecraft.getInstance().getModelManager().getModel(MODEL);
		poseStack.pushPose();
		model.body.translateAndRotate(poseStack);
		poseStack.mulPose(Axis.XP.rotationDegrees(180.0F)); // Model space is y up; the body's is y down, z backwards.
		poseStack.translate(-0.5D, -11.0D / 16.0D, -12.5D / 16.0D); // Centred, top at the shoulders, straps on the back.
		Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(poseStack.last(), buffers.getBuffer(Sheets.cutoutBlockSheet()),
				null, baked, 1.0F, 1.0F, 1.0F, light, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, Sheets.cutoutBlockSheet());
		poseStack.popPose();
	}
}
