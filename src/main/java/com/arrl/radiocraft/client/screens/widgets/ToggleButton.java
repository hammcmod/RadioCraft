package com.arrl.radiocraft.client.screens.widgets;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.resources.ResourceLocation;

public class ToggleButton extends Button {

	private final ResourceLocation resourceLocation;
	private final int u;
	private final int v;
	private final int textureWidth;
	private final int textureHeight;

	public boolean isToggled;

	public ToggleButton(boolean isToggled, int x, int y, int width, int height, int u, int v, ResourceLocation texLocation, int texWidth, int texHeight, Button.OnPress onPress) {
		super(x, y, width, height, CommonComponents.EMPTY, onPress, DEFAULT_NARRATION);
		this.textureWidth = texWidth;
		this.textureHeight = texHeight;
		this.u = u;
		this.v = v;
		this.resourceLocation = texLocation;
		this.isToggled = isToggled;
	}

	@Override
	public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.setShaderTexture(0, this.resourceLocation);

		int xBlit = !isHoveredOrFocused() ? u : u + width;
		int yBlit = !isToggled ? v : v + height;

		RenderSystem.enableDepthTest();
		blit(poseStack, this.getX(), this.getY(), xBlit, yBlit, width, height, textureWidth, textureHeight);
	}

	@Override
	public void onClick(double x, double y) {
		super.onClick(x, y);
		isToggled = !isToggled;
	}

}
