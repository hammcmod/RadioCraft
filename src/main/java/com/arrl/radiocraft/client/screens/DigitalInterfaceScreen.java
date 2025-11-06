package com.arrl.radiocraft.client.screens;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.client.screens.widgets.ImageButton;
import com.arrl.radiocraft.common.menus.DigitalInterfaceMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

public class DigitalInterfaceScreen extends AbstractContainerScreen<DigitalInterfaceMenu> {

	// Tab textures
	private static final ResourceLocation TEXTURE_RTTY = Radiocraft.id("textures/gui/digital_interface_rtty_send.png");
	private static final ResourceLocation TEXTURE_ARPS = Radiocraft.id("textures/gui/digital_interface_arps.png");
	private static final ResourceLocation TEXTURE_MSG = Radiocraft.id("textures/gui/digital_interface_msg.png");
	private static final ResourceLocation TEXTURE_FILES = Radiocraft.id("textures/gui/digital_interface_files.png");
	
	// Widget textures - cada aba pode ter seus próprios widgets específicos
	private static final ResourceLocation WIDGETS_RTTY = TEXTURE_RTTY;
	private static final ResourceLocation WIDGETS_ARPS = TEXTURE_ARPS;
	private static final ResourceLocation WIDGETS_MSG = TEXTURE_MSG;
	private static final ResourceLocation WIDGETS_FILES = TEXTURE_FILES;

	// Tab indices
	private static final int TAB_RTTY = 0;
	private static final int TAB_ARPS = 1;
	private static final int TAB_MSG = 2;
	private static final int TAB_FILES = 3;

	private ResourceLocation currentTexture;
	private ResourceLocation currentWidgetsTexture;
	private int currentTextureWidth;
	private int currentTextureHeight;

	public DigitalInterfaceScreen(DigitalInterfaceMenu menu, Inventory playerInventory, Component title) {
		super(menu, playerInventory, title);
		this.imageWidth = 217;
		this.imageHeight = 132;
		updateTexture();
	}

	@Override
	protected void init() {
		super.init();

		// Tab buttons - usando WIDGETS_RTTY como textura padrão para os botões das abas
		// Os botões das abas são comuns a todas as abas e ficam na textura principal
		addRenderableWidget(new ImageButton(leftPos + 8, topPos + 4, 35, 15, 1, 213, WIDGETS_RTTY, 225, 168, (btn) -> selectTab(TAB_ARPS)));
		addRenderableWidget(new ImageButton(leftPos + 60, topPos + 4, 35, 15, 36, 213, WIDGETS_RTTY, 225, 168, (btn) -> selectTab(TAB_MSG)));
		addRenderableWidget(new ImageButton(leftPos + 112, topPos + 4, 35, 15, 71, 213, WIDGETS_RTTY, 225, 168, (btn) -> selectTab(TAB_RTTY)));
		addRenderableWidget(new ImageButton(leftPos + 164, topPos + 4, 35, 15, 106, 213, WIDGETS_RTTY, 225, 168, (btn) -> selectTab(TAB_FILES)));

		updateTexture();
	}

	private void selectTab(int tabIndex) {
		menu.setSelectedTab(tabIndex);
		updateTexture();
	}

	private void updateTexture() {
		switch (menu.getSelectedTab()) {
			case TAB_ARPS:
				currentTexture = TEXTURE_ARPS;
				currentWidgetsTexture = WIDGETS_ARPS;
				currentTextureWidth = 217;
				currentTextureHeight = 202;
				break;
			case TAB_MSG:
				currentTexture = TEXTURE_MSG;
				currentWidgetsTexture = WIDGETS_MSG;
				currentTextureWidth = 224;
				currentTextureHeight = 167;
				break;
			case TAB_FILES:
				currentTexture = TEXTURE_FILES;
				currentWidgetsTexture = WIDGETS_FILES;
				currentTextureWidth = 217;
				currentTextureHeight = 155;
				break;
			default: // TAB_RTTY
				currentTexture = TEXTURE_RTTY;
				currentWidgetsTexture = WIDGETS_RTTY;
				currentTextureWidth = 225;
				currentTextureHeight = 168;
				break;
		}
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
		guiGraphics.blit(currentTexture, edgeSpacingX, edgeSpacingY, 0, 0, this.imageWidth, this.imageHeight, currentTextureWidth, currentTextureHeight);
	}

	@Override
	protected void renderLabels(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
		// Don't render the default title and inventory labels
	}
}
