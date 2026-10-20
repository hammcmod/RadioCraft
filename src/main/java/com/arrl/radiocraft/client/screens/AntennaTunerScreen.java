package com.arrl.radiocraft.client.screens;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.client.screens.widgets.*;
import com.arrl.radiocraft.common.blockentities.AntennaTunerBlockEntity;
import com.arrl.radiocraft.common.menus.AntennaTunerMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public class AntennaTunerScreen extends AbstractContainerScreen<AntennaTunerMenu> {

	private static final ResourceLocation TEXTURE = Radiocraft.id("textures/gui/antenna_tuner.png");

	/** Where the readout lines go: the empty panel to the right of the meter, above the dials. */
	private static final int READOUT_X = 150;
	private static final int READOUT_Y = 20;
	private static final int READOUT_LINE = 11;
	private static final int READOUT_COLOR = 0xFFD8E0D8;
	private static final int READOUT_WARN_COLOR = 0xFFFF9A5A;

	public AntennaTunerScreen(AntennaTunerMenu menu, Inventory playerInventory, Component title) {
		super(menu, playerInventory, title);
		this.imageWidth = 256;
		this.imageHeight = 128;
	}

	@Override
	protected void init() {
		super.init();

		addRenderableWidget(new Dial(leftPos + 23, topPos + 76, 42, 45, 48, 137, TEXTURE, 256, 256, (dial) -> {}, (dial) -> {})); // LeftDial
		addRenderableWidget(new Dial(leftPos + 106, topPos + 76, 42, 45, 48, 137, TEXTURE, 256, 256, (dial) -> {}, (dial) -> {})); // MiddleDial
		addRenderableWidget(new Dial(leftPos + 188, topPos + 76, 42, 45, 48, 137, TEXTURE, 256, 256, (dial) -> {}, (dial) -> {})); // RightDial
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

	/**
	 * The tuner's readout instead of the default title and inventory labels: what the antenna presents, what the radio
	 * sees, and a word for the state (matched, out of range, no radio...). The dials are decorative; the tuner is
	 * automatic.
	 */
	@Override
	protected void renderLabels(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
		int status = menu.getStatus();
		Component state = Component.translatable(Radiocraft.translationKey("screen", "antenna_tuner.status." + status));
		boolean warn = status != AntennaTunerBlockEntity.STATUS_MATCHED;
		guiGraphics.drawString(font, state, READOUT_X, READOUT_Y, warn ? READOUT_WARN_COLOR : READOUT_COLOR, false);

		if(status == AntennaTunerBlockEntity.STATUS_MATCHED || status == AntennaTunerBlockEntity.STATUS_OUT_OF_RANGE || status == AntennaTunerBlockEntity.STATUS_DISABLED) {
			Component antenna = Component.translatable(Radiocraft.translationKey("screen", "antenna_tuner.antenna_swr"), String.format(Locale.ROOT, "%.1f", menu.getRawSwr()));
			Component radio = Component.translatable(Radiocraft.translationKey("screen", "antenna_tuner.radio_swr"), String.format(Locale.ROOT, "%.1f", menu.getTunedSwr()));
			guiGraphics.drawString(font, antenna, READOUT_X, READOUT_Y + READOUT_LINE, READOUT_COLOR, false);
			guiGraphics.drawString(font, radio, READOUT_X, READOUT_Y + 2 * READOUT_LINE, READOUT_COLOR, false);
		}
	}
}
