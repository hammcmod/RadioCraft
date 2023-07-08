package com.arrl.radiocraft.client.screens;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.client.screens.widgets.ToggleButton;
import com.arrl.radiocraft.common.init.RadiocraftPackets;
import com.arrl.radiocraft.common.menus.AbstractHFRadioMenu;
import com.arrl.radiocraft.common.network.packets.ServerboundRadioPacket;
import com.arrl.radiocraft.common.network.packets.ServerboundTogglePacket;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class HFRadio10mScreen extends AbstractContainerScreen<AbstractHFRadioMenu> {

	private static final ResourceLocation TEXTURE = Radiocraft.location("textures/gui/hf_radio_10m.png");
	private static final ResourceLocation WIDGETS_TEXTURE = Radiocraft.location("textures/gui/hf_radio_10m_widgets.png");
	private final AbstractHFRadioMenu container;

	public HFRadio10mScreen(AbstractHFRadioMenu container, Inventory playerInventory, Component title) {
		super(container, playerInventory, title);
		this.container = container;

		this.imageWidth = 248;
		this.imageHeight = 130;
	}

	@Override
	protected void init() {
		super.init();
		addRenderableWidget(new ToggleButton(container.isPowered(), leftPos + 14, topPos + 15, 14, 17, 0, 0, WIDGETS_TEXTURE, 128, 128, // Power button
			(button) -> RadiocraftPackets.sendToServer(new ServerboundTogglePacket(container.blockEntity.getBlockPos())))
		);
		addRenderableWidget(new ToggleButton(false, leftPos + 200, topPos + 67, 32, 17, 0, 35, WIDGETS_TEXTURE, 128, 128, (button) -> {})); // CW Button
		addRenderableWidget(new ToggleButton(container.blockEntity.isTransmitting(), leftPos + 200, topPos + 87, 32, 17, 0, 70, WIDGETS_TEXTURE, 128, 128, (button) -> { // SSB button
					boolean isTransmitting = container.isTransmitting();
					container.blockEntity.setReceiving(isTransmitting);
					container.blockEntity.setTransmitting(!isTransmitting);
					RadiocraftPackets.sendToServer(new ServerboundRadioPacket(container.blockEntity.getBlockPos(), isTransmitting, !isTransmitting));
				})
		);
	}

	@Override
	public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
		renderBackground(poseStack);
		super.render(poseStack, mouseX, mouseY, partialTicks);

		if(isHovering(33, 18, 25, 11, mouseX, mouseY))
			renderTooltip(poseStack, Component.translatable(Radiocraft.translationKey("screen", "radio.tx")), mouseX, mouseY);
		if(isHovering(62, 18, 25, 11, mouseX, mouseY))
			renderTooltip(poseStack, Component.translatable(Radiocraft.translationKey("screen", "radio.rx")), mouseX, mouseY);
	}

	@Override
	protected void renderBg(PoseStack poseStack, float partialTicks, int x, int y) {
		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.setShaderTexture(0, TEXTURE);

		int edgeSpacingX = (this.width - this.imageWidth) / 2;
		int edgeSpacingY = (this.height - this.imageHeight) / 2;
		blit(poseStack, edgeSpacingX, edgeSpacingY, 0, 0, this.imageWidth, this.imageHeight);

		if(container.isPowered()) {
			if(container.isTransmitting())
				blit(poseStack, leftPos + 30, topPos + 15, 0, 131, 30, 17);
			if(container.isReceiving())
				blit(poseStack, leftPos + 60, topPos + 15, 30, 131, 29, 17);
		}
	}

	@Override
	protected void renderLabels(PoseStack poseStack, int x, int y) {}

	protected boolean isHovering(int x, int y, int width, int height, double mouseX, double mouseY) {
		mouseX -= leftPos;
		mouseY -= topPos;
		return mouseX >= (x - 1) && mouseX < (x + width + 1) && mouseY >= (y - 1) && mouseY < (y + height + 1);
	}

}