package com.arrl.radiocraft.client.render;

import com.arrl.radiocraft.common.items.TelephoneHandsetItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * A telephone handset held up to the player's ear in third person: drawn on the head, so it follows where they look,
 * with the {@code head} display slot of {@code item/telephone_handset.json} placing it (+x is the player's right, -z their
 * face, as in Blockbench's Head preview). The handset in the hand itself is hidden in third person by
 * {@code item/telephone_handset.json}, and the arm is raised to the face (see {@code ClientSetupEvents}).
 */
public class PhoneEarLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

	public PhoneEarLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
		super(parent);
	}

	/** The handset the player has up, main hand first; empty for none. */
	public static ItemStack handset(AbstractClientPlayer player) {
		if(player.getMainHandItem().getItem() instanceof TelephoneHandsetItem)
			return player.getMainHandItem();
		if(player.getOffhandItem().getItem() instanceof TelephoneHandsetItem)
			return player.getOffhandItem();
		return ItemStack.EMPTY;
	}

	@Override
	public void render(@NotNull PoseStack poseStack, @NotNull MultiBufferSource buffers, int light, @NotNull AbstractClientPlayer player,
					   float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
		ItemStack handset = handset(player);
		if(handset.isEmpty() || player.isInvisible())
			return;
		poseStack.pushPose();
		getParentModel().getHead().translateAndRotate(poseStack);
		// As vanilla's CustomHeadLayer places a carved pumpkin, so the model's head slot reads as it does in Blockbench.
		poseStack.translate(0.0F, -0.25F, 0.0F);
		poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
		poseStack.scale(0.625F, -0.625F, -0.625F);
		Minecraft.getInstance().getItemRenderer().renderStatic(player, handset, ItemDisplayContext.HEAD, false, poseStack, buffers,
				player.level(), light, OverlayTexture.NO_OVERLAY, player.getId());
		poseStack.popPose();
	}
}
