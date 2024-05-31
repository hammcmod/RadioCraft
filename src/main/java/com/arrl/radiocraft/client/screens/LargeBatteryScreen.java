package com.arrl.radiocraft.client.screens;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.menus.LargeBatteryMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class LargeBatteryScreen extends AbstractContainerScreen<LargeBatteryMenu> {

	private static final ResourceLocation TEXTURE = Radiocraft.id("textures/gui/large_battery.png");
	private final LargeBatteryMenu menu;

	public LargeBatteryScreen(LargeBatteryMenu menu, Inventory playerInventory, Component title) {
		super(menu, playerInventory, title);
		this.menu = menu;

		this.imageWidth = 186;
		this.imageHeight = 122;
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
	protected void renderLabels(PoseStack poseStack, int pX, int pY) {
		String powerString = (int)Math.round(menu.getCurrentPower() / (double)menu.getMaxPower() * 100) + "%";

		float scale = 2.0F;
		poseStack.pushPose();
		poseStack.scale(scale, scale, scale);

		int xPos = 95 - (font.width(powerString) / 2);
		int yPos = 37 - (font.lineHeight / 2);

		this.font.draw(poseStack, powerString, xPos / scale, yPos / scale, 0xFFFFFF);

		poseStack.popPose();
	}

}