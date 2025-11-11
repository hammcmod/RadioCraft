package com.arrl.radiocraft.client.screens.radios;

import com.arrl.radiocraft.client.RadiocraftClientValues;
import com.arrl.radiocraft.client.screens.widgets.Dial;
import com.arrl.radiocraft.client.screens.widgets.HoldButton;
import com.arrl.radiocraft.client.screens.widgets.ToggleButton;
import com.arrl.radiocraft.client.screens.widgets.ValueButton;
import com.arrl.radiocraft.common.menus.RadioMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("empty") // Some parts are not implemented; this is okay as we'll fix this later when block radios are worked on
public abstract class RadioScreen<T extends RadioMenu<?>> extends AbstractContainerScreen<T> {

	protected final ResourceLocation texture;
	protected final ResourceLocation widgetsTexture;
	protected final T menu;

	public RadioScreen(T menu, Inventory inventory, Component title, ResourceLocation texture, ResourceLocation widgetsTexture) {
		super(menu, inventory, title);
		this.menu = menu;
		this.texture = texture;
		this.widgetsTexture = widgetsTexture;
	}

	@Override
	public void onClose() {
		super.onClose();
		RadiocraftClientValues.SCREEN_PTT_PRESSED = false; // Make sure to stop recording player's mic when the UI is closed, in case they didn't let go of PTT
		RadiocraftClientValues.SCREEN_VOICE_ENABLED = false;
		RadiocraftClientValues.SCREEN_CW_ENABLED = false;
	}

	@Override
	public void render(@NotNull GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
		super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
		renderAdditionalTooltips(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
	}

	/**
	 * Use to render additional tooltips like lights.
	 */
	protected void renderAdditionalTooltips(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) { }


	@Override
	public void renderBackground(@NotNull GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
		super.renderBackground(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.setShaderTexture(0, texture);

		int edgeSpacingX = (this.width - this.imageWidth) / 2;
		int edgeSpacingY = (this.height - this.imageHeight) / 2;
		pGuiGraphics.blit(this.texture, edgeSpacingX, edgeSpacingY, 0, 0, this.imageWidth, this.imageHeight);

		renderAdditionalBackground(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
	}

	/**
	 * Use to render additional background elements like lights.
	 */
	protected void renderAdditionalBackground(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) { }

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		boolean value = super.mouseReleased(mouseX, mouseY, button);

		for(GuiEventListener listener : children()) { // Janky fix to make the dials detect a mouse release which isn't over them, forge allows for a mouse to go down but not back up.
			if(!listener.isMouseOver(mouseX, mouseY)) {
				if(listener instanceof Dial)
					listener.mouseReleased(mouseX, mouseY, button);
			}
		}

		return value;
	}

	protected boolean isHovering(int x, int y, int width, int height, double mouseX, double mouseY) {
		mouseX -= leftPos;
		mouseY -= topPos;
		return mouseX >= (x - 1) && mouseX < (x + width + 1) && mouseY >= (y - 1) && mouseY < (y + height + 1);
	}

	/**
	 * Callback to do nothing, for readability.
	 */
	protected void doNothing(AbstractWidget button) {}

	/**
	 * Callback for pressing a PTT button.
	 */
	protected void onPressPTT(HoldButton button) {
		//RadiocraftPackets.sendToServer(new SRadioPTTPacket(menu.blockEntity.getBlockPos(), true));
		RadiocraftClientValues.SCREEN_PTT_PRESSED = true;
	}

	/**
	 * Callback for releasing a PTT button.
	 */
	protected void onReleasePTT(HoldButton button) {
		//RadiocraftPackets.sendToServer(new SRadioPTTPacket(menu.blockEntity.getBlockPos(), false));
		RadiocraftClientValues.SCREEN_PTT_PRESSED = false;
	}

	/**
	 * Callback to toggle power on a device.
	 */
	protected void onPressPower(ToggleButton button) {
		//RadiocraftPackets.sendToServer(new STogglePacket(menu.blockEntity.getBlockPos()));
	}

	/**
	 * Callback for SSB toggle buttons.
	 */
	protected void onPressSSB(ValueButton button) {
		boolean ssbEnabled = menu.blockEntity.getSSBEnabled();

		//RadiocraftPackets.sendToServer(new SRadioSSBPacket(menu.blockEntity.getBlockPos(), !ssbEnabled));
		menu.blockEntity.setSSBEnabled(!ssbEnabled); // Update instantly for GUI, server will re-sync this value though.
	}

	/**
	 * Callback for raising frequency by one step on the dial.
	 */
	protected void onFrequencyDialUp(Dial dial) {
		if(menu.isPowered());
			//RadiocraftPackets.sendToServer(new SFrequencyPacket(menu.blockEntity.getBlockPos(), 1));
	}

	/**
	 * Callback for lowering frequency by one step on the dial.
	 */
	protected void onFrequencyDialDown(Dial dial) {
		if(menu.isPowered());
			//RadiocraftPackets.sendToServer(new SFrequencyPacket(menu.blockEntity.getBlockPos(), -1));
	}

	/**
	 * Callback for frequency up buttons.
	 */
	protected void onFrequencyButtonUp(Button button) {
		if(menu.isPowered());
			//RadiocraftPackets.sendToServer(new SFrequencyPacket(menu.blockEntity.getBlockPos(), 1));
	}

	/**
	 * Callback for frequency down buttons.
	 */
	protected void onFrequencyButtonDown(Button button) {
		if(menu.isPowered());
			//RadiocraftPackets.sendToServer(new SFrequencyPacket(menu.blockEntity.getBlockPos(), -1));
	}

}