package com.arrl.radiocraft.client.screens;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.client.screens.widgets.HoldButton;
import com.arrl.radiocraft.client.screens.widgets.ImageButton;
import com.arrl.radiocraft.client.screens.widgets.ToggleButton;
import com.arrl.radiocraft.common.menus.AbstractHFRadioMenu;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class HFRadio10mScreen extends AbstractHFRadioScreen {

	public HFRadio10mScreen(AbstractHFRadioMenu container, Inventory playerInventory, Component title) {
		super(container, playerInventory, title, Radiocraft.location("textures/gui/hf_radio_10m.png"), Radiocraft.location("textures/gui/hf_radio_10m_widgets.png"));

		this.imageWidth = 250;
		this.imageHeight = 147;
	}

	@Override
	protected void init() {
		super.init();
		addRenderableWidget(new ToggleButton(container.isPowered(), leftPos + 13, topPos + 14, 14, 17, 0, 0, widgetsTexture, 256, 256, this::onPressPower)); // Power button

		addRenderableWidget(new ToggleButton(container.blockEntity.getCWEnabled(), leftPos + 197, topPos + 66, 34, 19, 0, 34, widgetsTexture, 256, 256, this::onPressCW)); // CW Button
		addRenderableWidget(new ToggleButton(container.blockEntity.getSSBEnabled(), leftPos + 197, topPos + 86, 34, 19, 0, 72, widgetsTexture, 256, 256, this::onPressSSB)); // SSB button
		addRenderableWidget(new HoldButton(leftPos + 128, topPos + 110, 51, 19, 0, 110, widgetsTexture, 256, 256, this::onPressPTT, this::onReleasePTT)); // PTT button
		addRenderableWidget(new ImageButton(leftPos + 129, topPos + 93, 25, 17, 0, 148, widgetsTexture, 256, 256, this::doNothing)); // Frequency up button
		addRenderableWidget(new ImageButton(leftPos + 154, topPos + 93, 25, 17, 0, 182, widgetsTexture, 256, 256, this::doNothing)); // Frequency down button
	}

	@Override
	protected void renderAdditionalTooltips(PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
		if(isHovering(33, 18, 25, 11, mouseX, mouseY))
			renderTooltip(poseStack, Component.translatable(Radiocraft.translationKey("screen", "radio.tx")), mouseX, mouseY);
		if(isHovering(62, 18, 25, 11, mouseX, mouseY))
			renderTooltip(poseStack, Component.translatable(Radiocraft.translationKey("screen", "radio.rx")), mouseX, mouseY);
	}

	@Override
	protected void renderAdditionalBg(PoseStack poseStack, float partialTicks, int x, int y) {
		if(container.isPowered()) {
			if(container.isTransmitting())
				blit(poseStack, leftPos + 30, topPos + 15, 1, 148, 29, 15);
			if(container.isReceiving())
				blit(poseStack, leftPos + 59, topPos + 15, 30, 148, 29, 15);
		}	}

	@Override
	protected void renderLabels(PoseStack poseStack, int x, int y) {}

	protected boolean isHovering(int x, int y, int width, int height, double mouseX, double mouseY) {
		mouseX -= leftPos;
		mouseY -= topPos;
		return mouseX >= (x - 1) && mouseX < (x + width + 1) && mouseY >= (y - 1) && mouseY < (y + height + 1);
	}

}