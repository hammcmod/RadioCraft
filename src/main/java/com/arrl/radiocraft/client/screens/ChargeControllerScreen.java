package com.arrl.radiocraft.client.screens;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.client.screens.widgets.ToggleButton;
import com.arrl.radiocraft.common.init.RadiocraftItems;
import com.arrl.radiocraft.common.menus.ChargeControllerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class ChargeControllerScreen extends AbstractContainerScreen<ChargeControllerMenu> {

	private static final ResourceLocation TEXTURE = Radiocraft.id("textures/gui/charge_controller.png");
	private static final ResourceLocation WIDGETS = Radiocraft.id("textures/gui/charge_controller_widgets.png");

	private static final int COIL_WIDTH = 24;
	private static final int COIL_HEIGHT = 35;
	private static final int COIL_U = 176;
	private static final int COIL_V = 57;

	private static final int GAUGE_X = 68;
	private static final int GAUGE_Y = 75;
	private static final int GAUGE_U = 0;
	private static final int GAUGE_V = 0;
	private static final int GAUGE_WIDTH = 53;
	private static final int GAUGE_HEIGHT = 28;

	private static final int LIGHT_SIZE = 19;
	private static final int LIGHT_X = 21;
	private static final int[] LIGHT_Y = { 25, 48, 71 }; // Red, yellow green
	private static final int LIGHT_U = 176;
	private static final int[] LIGHT_V = { 0, 19, 38 }; // Red, yellow green

	public ChargeControllerScreen(ChargeControllerMenu menu, Inventory playerInventory, Component title) {
		super(menu, playerInventory, title);

		this.imageWidth = 176;
		this.imageHeight = 213;
	}

	@Override
	protected void init() {
		super.init();
		addRenderableWidget(new ToggleButton(menu.blockEntity.getPoweredOn(), leftPos + 138, topPos + 19, 27, 82, 0, 0, WIDGETS, 256, 256,
				(button) -> {})//RadiocraftPackets.sendToServer(new STogglePacket(menu.blockEntity.getBlockPos())))
		);
	}

	@Override
	public void render(@NotNull GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
		//super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);

		int edgeSpacingX = (this.width - this.imageWidth) / 2;
		int edgeSpacingY = (this.height - this.imageHeight) / 2;

		pGuiGraphics.blit(TEXTURE, edgeSpacingX, edgeSpacingY, 0, 0, this.imageWidth, this.imageHeight);

		if(menu.blockEntity.getPoweredOn()) {
			if (menu.getPowerTick() == 0) {
				pGuiGraphics.blit(TEXTURE, leftPos + LIGHT_X, topPos + LIGHT_Y[0], LIGHT_U, LIGHT_V[0], LIGHT_SIZE, LIGHT_SIZE);
			} else {
				pGuiGraphics.blit(TEXTURE, leftPos + LIGHT_X, topPos + LIGHT_Y[2], LIGHT_U, LIGHT_V[2], LIGHT_SIZE, LIGHT_SIZE);
			}

			if (menu.getItems().getFirst() != ItemStack.EMPTY) {
				pGuiGraphics.blit(TEXTURE, leftPos + LIGHT_X, topPos + LIGHT_Y[1], LIGHT_U, LIGHT_V[1], LIGHT_SIZE, LIGHT_SIZE);
			}

			ItemStack stack = menu.getItems().getFirst();
			if (stack.getItem() == RadiocraftItems.SMALL_BATTERY.get()) {
				/*

				CompoundTag nbt = stack.getOrCreateTag();
				float f = nbt.contains("charge") ? (float)nbt.getInt("charge") / CommonConfig.SMALL_BATTERY_CAPACITY.get() : 0.0F;

				int v = 57;
				if(f < 0.66F)
					v += COIL_HEIGHT;
				if(f < 0.33F)
					v += COIL_HEIGHT; // For getting colour.

				int height = Math.round(f * COIL_HEIGHT);
				int off = COIL_HEIGHT - height;

				blit(poseStack, leftPos + 95, topPos + 27 + off, 176, v + off, COIL_WIDTH, height);

				 */
			}
		}

		for (Renderable renderable : this.renderables) {
			renderable.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
		}
	}

	@Override
	protected void renderLabels(@NotNull GuiGraphics pGuiGraphics, int pMouseX, int pMouseY) {
		super.renderLabels(pGuiGraphics, pMouseX, pMouseY);
		if(isHovering(GAUGE_X, GAUGE_Y, GAUGE_WIDTH, GAUGE_HEIGHT, pMouseX, pMouseY))
			pGuiGraphics.renderTooltip(this.font, Component.translatable(Radiocraft.translationKey("screen", "chargecontroller.power"), menu.getPowerTick()), pMouseX - leftPos, pMouseY - topPos);
		if(isHovering(LIGHT_X, LIGHT_Y[0], LIGHT_SIZE, LIGHT_SIZE, pMouseX, pMouseY))
			pGuiGraphics.renderTooltip(this.font, Component.translatable(Radiocraft.translationKey("screen", "chargecontroller.no_output")), pMouseX - leftPos, pMouseY - topPos);
		else if(isHovering(LIGHT_X, LIGHT_Y[1], LIGHT_SIZE, LIGHT_SIZE, pMouseX, pMouseY))
			pGuiGraphics.renderTooltip(this.font, Component.translatable(Radiocraft.translationKey("screen", "chargecontroller.charging")), pMouseX - leftPos, pMouseY - topPos);
		else if(isHovering(LIGHT_X, LIGHT_Y[2], LIGHT_SIZE, LIGHT_SIZE, pMouseX, pMouseY))
			pGuiGraphics.renderTooltip(this.font, Component.translatable(Radiocraft.translationKey("screen", "chargecontroller.output")), pMouseX - leftPos, pMouseY - topPos);
	}

	@Override
	protected void renderBg(@NotNull GuiGraphics pGuiGraphics, float pPartialTick, int pMouseX, int pMouseY) {

	}

	protected boolean isHovering(int x, int y, int width, int height, double mouseX, double mouseY) {
		mouseX -= leftPos;
		mouseY -= topPos;
		return mouseX >= (x - 1) && mouseX < (x + width + 1) && mouseY >= (y - 1) && mouseY < (y + height + 1);
	}

}