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

public class Dial extends AbstractWidget {

	private final ResourceLocation resourceLocation;
	private final int u;
	private final int v;
	private final int textureWidth;
	private final int textureHeight;
	private final OnValueChanged onValueUp;
	private final OnValueChanged onValueDown;

	private boolean isRotated;
	private boolean isMouseDown;
	private int xMouseLast;


	public Dial(int x, int y, int width, int height, int u, int v, ResourceLocation texLocation, int texWidth, int texHeight, OnValueChanged onValueUp, OnValueChanged onValueDown) {
		super(x, y, width, height, CommonComponents.EMPTY);
		this.onValueUp = onValueUp;
		this.onValueDown = onValueDown;
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

		int xBlit = !isHoveredOrFocused() && !isMouseDown ? u : u + width;
		int yBlit = !isRotated ? v : v + height;

		RenderSystem.enableDepthTest();
		blit(poseStack, this.getX(), this.getY(), xBlit, yBlit, width, height, textureWidth, textureHeight);
	}

	@Override
	public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
		super.render(poseStack, mouseX, mouseY, partialTick);

		if(isMouseDown) {
			double xMouseDiff = mouseX - xMouseLast;

			if(xMouseDiff > 10) {
				onValueUp.execute(this);
				xMouseLast = mouseX;
				isRotated = !isRotated;
				Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_HAT, 1.0F));
			}
			else if(xMouseDiff < -10) {
				onValueDown.execute(this);
				xMouseLast = mouseX;
				isRotated = !isRotated;
				Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_HAT, 1.0F));
			}
		}
	}

	@Override
	public void onClick(double x, double y) {
		isMouseDown = true;
		xMouseLast = (int)Math.round(x);
	}


	@Override
	public void onRelease(double x, double y) {
		if(isMouseDown) {
			Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.3F));
			isMouseDown = false;
		}
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
		this.defaultButtonNarrationText(narrationElementOutput);
	}

	@FunctionalInterface
	public interface OnValueChanged {
		void execute(Dial dial);
	}

}
