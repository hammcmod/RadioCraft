package com.arrl.radiocraft.client.screens;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.menus.SolarPanelMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

public class SolarPanelScreen extends AbstractContainerScreen<SolarPanelMenu> {

	private static final ResourceLocation TEXTURE = Radiocraft.id("textures/gui/temp_power.png");
	private final SolarPanelMenu container;

	public SolarPanelScreen(SolarPanelMenu container, Inventory playerInventory, Component title) {
		super(container, playerInventory, title);
		this.container = container;

		this.imageWidth = 176;
		this.imageHeight = 88;
	}

	@Override
	public void render(@NotNull GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
		super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
		int edgeSpacingX = (this.width - this.imageWidth) / 2;
		int edgeSpacingY = (this.height - this.imageHeight) / 2;
		pGuiGraphics.blit(TEXTURE, edgeSpacingX, edgeSpacingY, 0, 0, imageWidth, imageHeight);
	}

	@Override
	protected void renderLabels(@NotNull GuiGraphics pGuiGraphics, int pMouseX, int pMouseY) {
		super.renderLabels(pGuiGraphics, pMouseX, pMouseY);
		String powerString = container.getPowerTick() + " FE/t";
		int xOffset = this.font.width(powerString) / 2;
		int yOffset = this.font.lineHeight / 2;
		pGuiGraphics.drawString(this.font, powerString, this.imageWidth / 2 - xOffset, this.imageHeight / 2 - yOffset, ChatFormatting.DARK_GRAY.getColor());
	}

	@Override
	protected void renderBg(GuiGraphics pGuiGraphics, float pPartialTick, int pMouseX, int pMouseY) {

	}
}