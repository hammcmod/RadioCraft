package com.arrl.radiocraft.client.screens;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.menus.HFRadio10mMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class HfRadio10mScreen extends AbstractContainerScreen<HFRadio10mMenu> {

	private static final ResourceLocation TEXTURE = Radiocraft.location("textures/gui/temp_power.png");
	private final HFRadio10mMenu container;

	public HfRadio10mScreen(HFRadio10mMenu container, Inventory playerInventory, Component title) {
		super(container, playerInventory, title);
		this.container = container;

		this.imageWidth = 176;
		this.imageHeight = 88;
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
		this.blit(poseStack, edgeSpacingX, edgeSpacingY, 0, 0, this.imageWidth, this.imageHeight);
	}

	@Override
	protected void renderLabels(PoseStack matrixStack, int pX, int pY) {
		String receiveString = String.format("Receive: %s FE/t", container.getReceiveTick());
		String transmitString = String.format("Transmit: %s FE/t", container.getTransmitTick());

		int xOffsetReceive = this.font.width(receiveString) / 2;
		int xOffsetTransmit = this.font.width(transmitString) / 2;
		int yOffset = this.font.lineHeight + 1;

		this.font.draw(matrixStack, receiveString, (float)this.imageWidth / 2 - xOffsetReceive, (float)this.imageHeight / 2 - yOffset, ChatFormatting.DARK_GRAY.getColor());
		this.font.draw(matrixStack, transmitString, (float)this.imageWidth / 2 - xOffsetTransmit, (float)this.imageHeight / 2 + 1, ChatFormatting.DARK_GRAY.getColor());
	}

}