package com.arrl.radiocraft.client.screens;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.client.screens.widgets.ToggleButton;
import com.arrl.radiocraft.common.init.RadiocraftPackets;
import com.arrl.radiocraft.common.menus.ChargeControllerMenu;
import com.arrl.radiocraft.common.network.packets.ServerboundTogglePacket;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class ChargeControllerScreen extends AbstractContainerScreen<ChargeControllerMenu> {

	private static final ResourceLocation TEXTURE = Radiocraft.location("textures/gui/charge_controller.png");
	private static final ResourceLocation WIDGETS = Radiocraft.location("textures/gui/charge_controller_widgets.png");

	private static final int GAUGE_X = 20;
	private static final int GAUGE_Y = 68;
	private static final int GAUGE_WIDTH = 56;
	private static final int GAUGE_HEIGHT = 37;

	private static final int BOLT_X = 43;
	private static final int BOLT_Y = 107;
	private static final int BOLT_U = 0;
	private static final int BOLT_V = 131;
	private static final int BOLT_WIDTH = 10;
	private static final int BOLT_HEIGHT = 14;

	private final ChargeControllerMenu container;


	public ChargeControllerScreen(ChargeControllerMenu container, Inventory playerInventory, Component title) {
		super(container, playerInventory, title);
		this.container = container;

		this.imageWidth = 248;
		this.imageHeight = 130;
	}

	@Override
	protected void init() {
		super.init();
		addRenderableWidget(new ToggleButton(container.blockEntity.getPoweredOn(), leftPos + 93, topPos + 24, 63, 84, 0, 0, WIDGETS, 256, 256,
				(button) -> RadiocraftPackets.sendToServer(new ServerboundTogglePacket(container.blockEntity.getBlockPos())))
		);
	}

	@Override
	public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
		renderBackground(poseStack);
		super.render(poseStack, mouseX, mouseY, partialTicks);

		if(isHovering(GAUGE_X, GAUGE_Y, GAUGE_WIDTH, GAUGE_HEIGHT, mouseX, mouseY))
			renderTooltip(poseStack, Component.translatable(Radiocraft.translationKey("screen", "chargecontroller.power"), container.getPowerTick()), mouseX, mouseY);
	}

	@Override
	protected void renderBg(PoseStack poseStack, float partialTicks, int x, int y) {
		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.setShaderTexture(0, TEXTURE);

		int edgeSpacingX = (this.width - this.imageWidth) / 2;
		int edgeSpacingY = (this.height - this.imageHeight) / 2;
		blit(poseStack, edgeSpacingX, edgeSpacingY, 0, 0, this.imageWidth, this.imageHeight);

		if(container.blockEntity.getPoweredOn()) {
			blit(poseStack, leftPos + BOLT_X, topPos + BOLT_Y, BOLT_U, BOLT_V, BOLT_WIDTH, BOLT_HEIGHT);
		}
	}

	protected boolean isHovering(int x, int y, int width, int height, double mouseX, double mouseY) {
		mouseX -= leftPos;
		mouseY -= topPos;
		return mouseX >= (x - 1) && mouseX < (x + width + 1) && mouseY >= (y - 1) && mouseY < (y + height + 1);
	}

	@Override
	protected void renderLabels(PoseStack poseStack, int mouseX, int mouseY) {
	}

}