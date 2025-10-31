package com.arrl.radiocraft.client.screens;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.menus.DigitalInterfaceMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

public class DigitalInterfaceScreen extends AbstractContainerScreen<DigitalInterfaceMenu> {

	// Tab textures
	private static final ResourceLocation TEXTURE_RTTY = Radiocraft.id("textures/gui/digital_interface_rtty.png");
	private static final ResourceLocation TEXTURE_ARPS = Radiocraft.id("textures/gui/digital_interface_arps.png");
	private static final ResourceLocation TEXTURE_MSG = Radiocraft.id("textures/gui/digital_interface_msg.png");
	private static final ResourceLocation TEXTURE_FILES = Radiocraft.id("textures/gui/digital_interface_files.png");

	// Tab indices
	private static final int TAB_RTTY = 0;
	private static final int TAB_ARPS = 1;
	private static final int TAB_MSG = 2;
	private static final int TAB_FILES = 3;

	private ResourceLocation currentTexture;

	public DigitalInterfaceScreen(DigitalInterfaceMenu menu, Inventory playerInventory, Component title) {
		super(menu, playerInventory, title);
		this.imageWidth = 176;
		this.imageHeight = 166;
		updateTexture();
	}

	@Override
	protected void init() {
		super.init();

		// Tab buttons at the top
		int buttonWidth = 40;
		int buttonHeight = 20;
		int startX = leftPos + 8;
		int startY = topPos - 18;

		addRenderableWidget(Button.builder(Component.translatable("gui.radiocraft.tab.arps"), 
			button -> selectTab(TAB_ARPS))
			.bounds(startX, startY, buttonWidth, buttonHeight)
			.build());

		addRenderableWidget(Button.builder(Component.translatable("gui.radiocraft.tab.msg"), 
			button -> selectTab(TAB_MSG))
			.bounds(startX + buttonWidth + 2, startY, buttonWidth, buttonHeight)
			.build());

		addRenderableWidget(Button.builder(Component.translatable("gui.radiocraft.tab.rtty"), 
			button -> selectTab(TAB_RTTY))
			.bounds(startX + (buttonWidth + 2) * 2, startY, buttonWidth, buttonHeight)
			.build());

		addRenderableWidget(Button.builder(Component.translatable("gui.radiocraft.tab.files"), 
			button -> selectTab(TAB_FILES))
			.bounds(startX + (buttonWidth + 2) * 3, startY, buttonWidth, buttonHeight)
			.build());

		updateTexture();
	}

	private void selectTab(int tabIndex) {
		menu.setSelectedTab(tabIndex);
		updateTexture();
	}

	private void updateTexture() {
		currentTexture = switch (menu.getSelectedTab()) {
			case TAB_ARPS -> TEXTURE_ARPS;
			case TAB_MSG -> TEXTURE_MSG;
			case TAB_FILES -> TEXTURE_FILES;
			default -> TEXTURE_RTTY;
		};
	}

	@Override
	public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		updateTexture(); // Update texture in case it changed from server
		super.render(guiGraphics, mouseX, mouseY, partialTick);
		renderTooltip(guiGraphics, mouseX, mouseY);
	}

	@Override
	protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.setShaderTexture(0, currentTexture);

		int edgeSpacingX = (this.width - this.imageWidth) / 2;
		int edgeSpacingY = (this.height - this.imageHeight) / 2;
		guiGraphics.blit(currentTexture, edgeSpacingX, edgeSpacingY, 0, 0, this.imageWidth, this.imageHeight);
	}

	@Override
	protected void renderLabels(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
		// Don't render the default title and inventory labels
	}
}
