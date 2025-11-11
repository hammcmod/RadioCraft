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
		this.imageHeight = 76;
	}

	@Override
	protected void init() {
		super.init();

		// Screw widgets - coordinates follow Dial pattern with border adjustment
		// Texture position: normal (2,78), hover automatically at (2+width, 78)
		// Widget size includes border: 19x19 pixels
		// Position adjustments: -1px x/y for border, texture u/v also -1px
		addRenderableWidget(new ScrewButton(leftPos + 9, topPos + 32, 19, 19, 2, 78, TEXTURE, 250, 120));
		addRenderableWidget(new ScrewButton(leftPos + 51, topPos + 32, 19, 19, 2, 78, TEXTURE, 250, 120));
		addRenderableWidget(new ScrewButton(leftPos + 94, topPos + 32, 19, 19, 2, 78, TEXTURE, 250, 120));
		addRenderableWidget(new ScrewButton(leftPos + 137, topPos + 32, 19, 19, 2, 78, TEXTURE, 250, 120));
		addRenderableWidget(new ScrewButton(leftPos + 179, topPos + 32, 19, 19, 2, 78, TEXTURE, 250, 120));
		addRenderableWidget(new ScrewButton(leftPos + 221, topPos + 32, 19, 19, 2, 78, TEXTURE, 250, 120));
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
