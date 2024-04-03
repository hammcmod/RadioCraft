package com.arrl.radiocraft.client.screens.widgets;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.resources.ResourceLocation;

public class ToggleButton extends AbstractWidget {

	private final ResourceLocation resourceLocation;
	private final int u;
	private final int v;
	private final int textureWidth;
	private final int textureHeight;
	private final OnInteract onPress;

	public boolean isToggled;

	public ToggleButton(boolean isToggled, int x, int y, int width, int height, int u, int v, ResourceLocation texLocation, int texWidth, int texHeight, OnInteract onPress) {
		super(x, y, width, height, CommonComponents.EMPTY);
		this.textureWidth = texWidth;
		this.textureHeight = texHeight;
		this.u = u;
		this.v = v;
		this.resourceLocation = texLocation;
		this.isToggled = isToggled;
		this.onPress = onPress;
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
		onPress.execute(this);
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
		this.defaultButtonNarrationText(narrationElementOutput);
	}

	@FunctionalInterface
	public interface OnInteract {
		void execute(ToggleButton button);
	}

}
