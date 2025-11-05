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

	public VHFRepeaterScreen(VHFRepeaterMenu menu, Inventory playerInventory, Component title) {
		super(menu, playerInventory, title);
		this.imageWidth = 250;
		this.imageHeight = 153;
	}

	@Override
	protected void init() {
		super.init();

		addRenderableWidget(new StaticToggleButton(false, leftPos + 19, topPos + 30, 18, 19, 0, 154, TEXTURE, 250, 216, (btn) -> {}));
		addRenderableWidget(new StaticToggleButton(false, leftPos + 42, topPos + 30, 18, 19, 23, 154, TEXTURE, 250, 216, (btn) -> {}));
		addRenderableWidget(new StaticToggleButton(false, leftPos + 65, topPos + 30, 18, 19, 46, 154, TEXTURE, 250, 216, (btn) -> {}));
		addRenderableWidget(new StaticToggleButton(false, leftPos + 19, topPos + 51, 18, 19, 0, 175, TEXTURE, 250, 216, (btn) -> {}));
		addRenderableWidget(new StaticToggleButton(false, leftPos + 42, topPos + 51, 18, 19, 23, 175, TEXTURE, 250, 216, (btn) -> {}));
		addRenderableWidget(new StaticToggleButton(false, leftPos + 65, topPos + 51, 18, 19, 46, 175, TEXTURE, 250, 216, (btn) -> {}));
		addRenderableWidget(new StaticToggleButton(false, leftPos + 19, topPos + 72, 18, 19, 0, 196, TEXTURE, 250, 216, (btn) -> {}));
		addRenderableWidget(new StaticToggleButton(false, leftPos + 42, topPos + 72, 18, 19, 23, 196, TEXTURE, 250, 216, (btn) -> {}));
		addRenderableWidget(new StaticToggleButton(false, leftPos + 65, topPos + 72, 18, 19, 46, 196, TEXTURE, 250, 216, (btn) -> {}));
		addRenderableWidget(new StaticToggleButton(false, leftPos + 22, topPos + 94, 13, 15, 68, 155, TEXTURE, 250, 216, (btn) -> {}));
		addRenderableWidget(new StaticToggleButton(false, leftPos + 67, topPos + 94, 13, 15, 68, 155, TEXTURE, 250, 216, (btn) -> {}));
		addRenderableWidget(new StaticToggleButton(false, leftPos + 168, topPos + 59, 13, 15, 68, 155, TEXTURE, 250, 216, (btn) -> {}));
		addRenderableWidget(new StaticToggleButton(false, leftPos + 207, topPos + 59, 13, 15, 68, 155, TEXTURE, 250, 216, (btn) -> {}));
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
		guiGraphics.blit(TEXTURE, edgeSpacingX, edgeSpacingY, 0, 0, this.imageWidth, this.imageHeight, 250, 216);
	}

	@Override
	protected void renderLabels(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
		// Don't render the default title and inventory labels
	}
}
