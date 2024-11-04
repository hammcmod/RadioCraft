package com.arrl.radiocraft.client.screens;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.menus.LargeBatteryMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

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
	protected void renderBg(@NotNull GuiGraphics pGuiGraphics, float pPartialTick, int pMouseX, int pMouseY) {

	}

	@Override
	public void render(@NotNull GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
		super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);

		int edgeSpacingX = (this.width - this.imageWidth) / 2;
		int edgeSpacingY = (this.height - this.imageHeight) / 2;
		pGuiGraphics.blit(TEXTURE, edgeSpacingX, edgeSpacingY, 0, 0, this.imageWidth, this.imageHeight);
	}

	@Override
	protected void renderLabels(@NotNull GuiGraphics pGuiGraphics, int pMouseX, int pMouseY) {
		super.renderLabels(pGuiGraphics, pMouseX, pMouseY);

		String powerString = Math.round((float) menu.getCurrentPower() / menu.getMaxPower() * 100) + "%";
		int xPos = 95 - (font.width(powerString) / 2);
		int yPos = 70 - (font.lineHeight / 2);
		pGuiGraphics.drawString(this.font, powerString, xPos, yPos, 0xFFFFFF);
	}
}