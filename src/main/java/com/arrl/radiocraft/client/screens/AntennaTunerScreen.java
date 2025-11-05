package com.arrl.radiocraft.client.screens;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.client.screens.widgets.*;
import com.arrl.radiocraft.common.menus.AntennaTunerMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

public class AntennaTunerScreen extends AbstractContainerScreen<AntennaTunerMenu> {

	private static final ResourceLocation TEXTURE = Radiocraft.id("textures/gui/antenna_tuner.png");

	public AntennaTunerScreen(AntennaTunerMenu menu, Inventory playerInventory, Component title) {
		super(menu, playerInventory, title);
		this.imageWidth = 250;
		this.imageHeight = 128;
	}

	@Override
	protected void init() {
		super.init();

		addRenderableWidget(new Dial(leftPos + 23, topPos + 76, 42, 45, 48, 137, TEXTURE, 250, 182, (dial) -> {}, (dial) -> {})); // LeftDial
		addRenderableWidget(new Dial(leftPos + 106, topPos + 76, 42, 45, 48, 137, TEXTURE, 250, 182, (dial) -> {}, (dial) -> {})); // MiddleDial
		addRenderableWidget(new Dial(leftPos + 188, topPos + 76, 42, 45, 48, 137, TEXTURE, 250, 182, (dial) -> {}, (dial) -> {})); // RightDial
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
		guiGraphics.blit(TEXTURE, edgeSpacingX, edgeSpacingY, 0, 0, this.imageWidth, this.imageHeight, 250, 182);
	}

	@Override
	protected void renderLabels(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
		// Don't render the default title and inventory labels
	}
}
