package com.arrl.radiocraft.client.screens;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.init.RadiocraftPackets;
import com.arrl.radiocraft.common.menus.ChargeControllerMenu;
import com.arrl.radiocraft.common.network.packets.ServerboundTogglePacket;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class ChargeControllerScreen extends AbstractContainerScreen<ChargeControllerMenu> {

	private static final ResourceLocation TEXTURE = Radiocraft.location("textures/gui/charge_controller.png");
	private static final int SWITCH_X = 94;
	private static final int SWITCH_Y = 25;
	private static final int SWITCH_U = 1;
	private static final int SWITCH_V = 131;
	private static final int SWITCH_WIDTH = 63;
	private static final int SWITCH_HEIGHT = 82;

	private final ChargeControllerMenu container;


	public ChargeControllerScreen(ChargeControllerMenu container, Inventory playerInventory, Component title) {
		super(container, playerInventory, title);
		this.container = container;

		this.imageWidth = 248;
		this.imageHeight = 130;
	}

	@Override
	public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
		renderBackground(poseStack);
		super.render(poseStack, mouseX, mouseY, partialTicks);
	}

	@Override
	protected void renderBg(PoseStack poseStack, float partialTicks, int x, int y) {
		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.setShaderTexture(0, TEXTURE);

		int edgeSpacingX = (this.width - this.imageWidth) / 2;
		int edgeSpacingY = (this.height - this.imageHeight) / 2;
		blit(poseStack, edgeSpacingX, edgeSpacingY, 0, 0, this.imageWidth, this.imageHeight);

		if(!container.blockEntity.getPoweredOn()) {
			blit(poseStack, leftPos + SWITCH_X, topPos + SWITCH_Y, SWITCH_U, SWITCH_V, SWITCH_WIDTH, SWITCH_HEIGHT);
		}
	}

	@Override
	public boolean mouseClicked(double x, double y, int button) {
		double xRelative = x - leftPos;
		double yRelative = y - topPos;

		if(xRelative > SWITCH_X && xRelative < SWITCH_X + SWITCH_WIDTH) {
			if(yRelative > SWITCH_Y && yRelative < SWITCH_Y + SWITCH_HEIGHT) {
				RadiocraftPackets.sendToServer(new ServerboundTogglePacket(container.blockEntity.getBlockPos()));
			}
		}

		return super.mouseClicked(x, y, button);
	}

	@Override
	protected void renderLabels(PoseStack matrixStack, int pX, int pY) {
		String powerString = container.getPowerTick() + " FE/t";
		int xOffset = this.font.width(powerString) / 2;
		int yOffset = this.font.lineHeight / 2;
//		this.font.draw(matrixStack, powerString, (float)this.imageWidth / 2 - xOffset, (float)this.imageHeight / 2 - yOffset, ChatFormatting.DARK_GRAY.getColor());
	}

}