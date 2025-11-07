package com.arrl.radiocraft.client.screens;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.client.screens.widgets.HoldButton;
import com.arrl.radiocraft.client.screens.widgets.ImageButton;
import com.arrl.radiocraft.client.screens.widgets.ToggleButton;
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
	
	// Widget texture - todos os widgets estão em um único arquivo
	private static final ResourceLocation WIDGETS_TEXTURE = Radiocraft.id("textures/gui/digital_interface_widgets.png");

	// Tab indices
	private static final int TAB_RTTY = 0;
	private static final int TAB_ARPS = 1;
	private static final int TAB_MSG = 2;
	private static final int TAB_FILES = 3;

	private ResourceLocation currentTexture;

	public DigitalInterfaceScreen(DigitalInterfaceMenu menu, Inventory playerInventory, Component title) {
		super(menu, playerInventory, title);
		this.imageWidth = 217;
		this.imageHeight = 133;
		updateTexture();
	}

	@Override
	protected void init() {
		super.init();

		// Tab buttons - todos usam a mesma textura de widgets
		addRenderableWidget(new ToggleButton(menu.getSelectedTab() == TAB_ARPS, leftPos + 16, topPos + 17, 35, 14, 0, 0, WIDGETS_TEXTURE, 217, 80, (btn) -> selectTab(TAB_ARPS)));
		addRenderableWidget(new ToggleButton(menu.getSelectedTab() == TAB_MSG, leftPos + 50, topPos + 17, 36, 14, 144, 0, WIDGETS_TEXTURE, 217, 80, (btn) -> selectTab(TAB_MSG)));
		addRenderableWidget(new ToggleButton(menu.getSelectedTab() == TAB_RTTY, leftPos + 85, topPos + 17, 36, 14, 0, 28, WIDGETS_TEXTURE, 217, 80, (btn) -> selectTab(TAB_RTTY)));
		addRenderableWidget(new ToggleButton(menu.getSelectedTab() == TAB_FILES, leftPos + 120, topPos + 17, 37, 14, 70, 0, WIDGETS_TEXTURE, 217, 80, (btn) -> selectTab(TAB_FILES)));
		
		// Botões gerais (azul, verde, vermelho)
		// addRenderableWidget(new ImageButton(leftPos + 161, topPos + 18, 11, 11, 1, 57, WIDGETS_TEXTURE, 217, 80, (btn) -> onBlueButton()));
		// addRenderableWidget(new ImageButton(leftPos + 174, topPos + 18, 11, 11, 14, 57, WIDGETS_TEXTURE, 217, 80, (btn) -> onGreenButton()));
		// addRenderableWidget(new ImageButton(leftPos + 189, topPos + 17, 11, 11, 29, 56, WIDGETS_TEXTURE, 217, 80, (btn) -> onRedButton()));
		
		// Adicionar botões específicos da aba atual
		addTabSpecificButtons();

		updateTexture();
	}
	
	private void addTabSpecificButtons() {
		// Botões específicos por aba
		switch (menu.getSelectedTab()) {
			case TAB_FILES:
				addRenderableWidget(new HoldButton(leftPos + 78, topPos + 40, 28, 14, 72, 28, WIDGETS_TEXTURE, 217, 80, (btn) -> {}, (btn) -> onFilesSend()));
				
				// LEDs da aba files
				// addRenderableWidget(new ImageButton(leftPos + 27, topPos + 107, 8, 12, 43, 57, WIDGETS_TEXTURE, 217, 80, (btn) -> {}));
				// addRenderableWidget(new ImageButton(leftPos + 42, topPos + 107, 8, 12, 53, 57, WIDGETS_TEXTURE, 217, 80, (btn) -> {}));
				// addRenderableWidget(new ImageButton(leftPos + 57, topPos + 107, 8, 12, 63, 57, WIDGETS_TEXTURE, 217, 80, (btn) -> {}));
				// addRenderableWidget(new ImageButton(leftPos + 72, topPos + 107, 8, 12, 73, 57, WIDGETS_TEXTURE, 217, 80, (btn) -> {}));
				// addRenderableWidget(new ImageButton(leftPos + 87, topPos + 107, 8, 12, 83, 57, WIDGETS_TEXTURE, 217, 80, (btn) -> {}));
				// addRenderableWidget(new ImageButton(leftPos + 102, topPos + 107, 8, 12, 93, 57, WIDGETS_TEXTURE, 217, 80, (btn) -> {}));
				break;
				
			case TAB_RTTY:
				addRenderableWidget(new HoldButton(leftPos + 174, topPos + 113, 28, 14, 72, 28, WIDGETS_TEXTURE, 217, 80, (btn) -> {}, (btn) -> onRttySend()));
				break;
				
			case TAB_MSG:
				// Nenhum botão específico
				break;
				
			case TAB_ARPS:
				// Nenhum botão específico ainda
				break;
		}
	}
	
	private void onBlueButton() {
		// TODO: Implementar funcionalidade do botão azul
	}
	
	private void onGreenButton() {
		// TODO: Implementar funcionalidade do botão verde
	}
	
	private void onRedButton() {
		// TODO: Implementar funcionalidade do botão vermelho
	}
	
	private void onFilesSend() {
		// TODO: Implementar funcionalidade do botão send da aba files
	}
	
	private void onRttySend() {
		// TODO: Implementar funcionalidade do botão send da aba rtty
	}

	private void selectTab(int tabIndex) {
		menu.setSelectedTab(tabIndex);
		updateTexture();
		// Reinicializar a tela para atualizar os botões da nova aba
		this.clearWidgets();
		this.init();
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
		guiGraphics.blit(currentTexture, edgeSpacingX, edgeSpacingY, 0, 0, this.imageWidth, this.imageHeight, 217, 133);
	}

	@Override
	protected void renderLabels(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
		// Don't render the default title and inventory labels
	}
}
