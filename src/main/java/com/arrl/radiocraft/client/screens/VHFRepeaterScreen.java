package com.arrl.radiocraft.client.screens;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.client.screens.widgets.*;
import com.arrl.radiocraft.common.menus.VHFRepeaterMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

public class VHFRepeaterScreen extends AbstractContainerScreen<VHFRepeaterMenu> {

	private static final ResourceLocation TEXTURE = Radiocraft.id("textures/gui/vhf_repeater.png");
	private static final ResourceLocation WIDGETS_TEXTURE = Radiocraft.id("textures/gui/vhf_repeater_widgets.png");

	public VHFRepeaterScreen(VHFRepeaterMenu menu, Inventory playerInventory, Component title) {
		super(menu, playerInventory, title);
		this.imageWidth = 256;
		this.imageHeight = 151;
	}

	@Override
	protected void init() {
		super.init();

		addRenderableWidget(new ToggleButton(false, leftPos + 18, topPos + 29, 20, 21, 0, 0, WIDGETS_TEXTURE, 256, 256, (btn) -> {}));
		addRenderableWidget(new ToggleButton(false, leftPos + 41, topPos + 29, 20, 21, 0, 42, WIDGETS_TEXTURE, 256, 256, (btn) -> {}));
		addRenderableWidget(new ToggleButton(false, leftPos + 64, topPos + 29, 20, 21, 0, 84, WIDGETS_TEXTURE, 256, 256, (btn) -> {}));
		addRenderableWidget(new ToggleButton(false, leftPos + 18, topPos + 50, 20, 21, 0, 126, WIDGETS_TEXTURE, 256, 256, (btn) -> {}));
		addRenderableWidget(new ToggleButton(false, leftPos + 41, topPos + 50, 20, 21, 0, 168, WIDGETS_TEXTURE, 256, 256, (btn) -> {}));
		addRenderableWidget(new ToggleButton(false, leftPos + 64, topPos + 50, 20, 21, 40, 0, WIDGETS_TEXTURE, 256, 256, (btn) -> {}));
		addRenderableWidget(new ToggleButton(false, leftPos + 18, topPos + 71, 20, 21, 40, 42, WIDGETS_TEXTURE, 256, 256, (btn) -> {}));
		addRenderableWidget(new ToggleButton(false, leftPos + 41, topPos + 71, 20, 21, 40, 84, WIDGETS_TEXTURE, 256, 256, (btn) -> {}));
		addRenderableWidget(new ToggleButton(false, leftPos + 64, topPos + 71, 20, 21, 40, 126, WIDGETS_TEXTURE, 256, 256, (btn) -> {}));
		addRenderableWidget(new ToggleButton(false, leftPos + 21, topPos + 93, 15, 17, 44, 171, WIDGETS_TEXTURE, 256, 256, (btn) -> {}));
		addRenderableWidget(new ToggleButton(false, leftPos + 66, topPos + 93, 15, 17, 44, 171, WIDGETS_TEXTURE, 256, 256, (btn) -> {}));
		addRenderableWidget(new ToggleButton(false, leftPos + 167, topPos + 58, 15, 17, 44, 171, WIDGETS_TEXTURE, 256, 256, (btn) -> {}));
		addRenderableWidget(new ToggleButton(false, leftPos + 206, topPos + 58, 15, 17, 44, 171, WIDGETS_TEXTURE, 256, 256, (btn) -> {}));
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
		guiGraphics.blit(TEXTURE, edgeSpacingX, edgeSpacingY, 0, 0, this.imageWidth, this.imageHeight, 256, 256);
	}

	@Override
	protected void renderLabels(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
		// Don't render the default title and inventory labels
	}
}
