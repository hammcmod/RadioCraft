package com.arrl.radiocraft.client.screens.widgets;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;

public class HoldButton extends AbstractWidget {

	private final ResourceLocation resourceLocation;
	private final int u;
	private final int v;
	private final int textureWidth;
	private final int textureHeight;
	private final OnInteract onPress;
	private final OnInteract onRelease;

	private boolean isPressed = false;

	public HoldButton(int x, int y, int width, int height, int u, int v, ResourceLocation texLocation, int texWidth, int texHeight, OnInteract onPress, OnInteract onRelease) {
		super(x, y, width, height, CommonComponents.EMPTY);
		this.onPress = onPress;
		this.onRelease = onRelease;
		this.resourceLocation = texLocation;
		this.u = u;
		this.v = v;
		this.textureWidth = texWidth;
		this.textureHeight = texHeight;
	}

	@Override
	public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.setShaderTexture(0, this.resourceLocation);

		int xBlit = !isHoveredOrFocused() ? u : u + width;
		int yBlit = !isPressed ? v : v + height;

		RenderSystem.enableDepthTest();
		blit(poseStack, this.getX(), this.getY(), xBlit, yBlit, width, height, textureWidth, textureHeight);
	}

	@Override
	public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
		super.render(poseStack, mouseX, mouseY, partialTick);
		if(isPressed) {
			if(!clicked(mouseX, mouseY))
				onRelease(mouseX, mouseY);
		}
	}

	@Override
	public void onClick(double x, double y) {
		isPressed = true;
		onPress.execute(this);
	}


	@Override
	public void onRelease(double x, double y) {
		if(isPressed) {
			Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.3F));
			isPressed = false;
			onRelease.execute(this);
		}
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
		this.defaultButtonNarrationText(narrationElementOutput);
	}

	@FunctionalInterface
	public interface OnInteract {
		void execute(HoldButton button);
	}

}
