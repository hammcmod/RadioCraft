package com.arrl.radiocraft.client.screens;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.client.screens.widgets.ScrewButton;
import com.arrl.radiocraft.common.menus.DuplexerMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

public class DuplexerScreen extends AbstractContainerScreen<DuplexerMenu> {

	private static final ResourceLocation TEXTURE = Radiocraft.id("textures/gui/duplexer.png");

	public DuplexerScreen(DuplexerMenu menu, Inventory playerInventory, Component title) {
		super(menu, playerInventory, title);
		this.imageWidth = 250;
		this.imageHeight = 75;
	}

	@Override
	protected void init() {
		super.init();

		// Screw widgets at Y=30
		// Texture positions: normal (0,76), hover with border (20,76)
		// Screws are 21x21 pixels
		// X positions: 7, 18, 60, 103, 146, 188, 230 (first one is the test position at 7)
		addRenderableWidget(new ScrewButton(leftPos + 7, topPos + 30, 21, 21, 0, 76, 20, 76, TEXTURE, 250, 120));
		addRenderableWidget(new ScrewButton(leftPos + 49, topPos + 30, 21, 21, 0, 76, 20, 76, TEXTURE, 250, 120));
		addRenderableWidget(new ScrewButton(leftPos + 92, topPos + 30, 21, 21, 0, 76, 20, 76, TEXTURE, 250, 120));
		addRenderableWidget(new ScrewButton(leftPos + 135, topPos + 30, 21, 21, 0, 76, 20, 76, TEXTURE, 250, 120));
		addRenderableWidget(new ScrewButton(leftPos + 177, topPos + 30, 21, 21, 0, 76, 20, 76, TEXTURE, 250, 120));
		addRenderableWidget(new ScrewButton(leftPos + 219, topPos + 30, 21, 21, 0, 76, 20, 76, TEXTURE, 250, 120));
	}

	@Override
	public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		super.render(guiGraphics, mouseX, mouseY, partialTick);
		renderTooltip(guiGraphics, mouseX, mouseY);
	}

	@Override
	protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.setShaderTexture(0, TEXTURE);

		int edgeSpacingX = (this.width - this.imageWidth) / 2;
		int edgeSpacingY = (this.height - this.imageHeight) / 2;
		guiGraphics.blit(TEXTURE, edgeSpacingX, edgeSpacingY, 0, 0, this.imageWidth, this.imageHeight, 250, 120);
	}

	@Override
	protected void renderLabels(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
		// Don't render the default title and inventory labels
	}
}
