package com.arrl.radiocraft.client.screens.widgets;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import org.jetbrains.annotations.NotNull;

import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;

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
	protected void renderWidget(@NotNull GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
		if(isPressed) {
			if(!clicked(pMouseX, pMouseY))
				onRelease(pMouseX, pMouseY);
		}

		int xBlit = !isHovered() ? u : u + width;
		int yBlit = !isPressed ? v : v + height;

		RenderSystem.enableDepthTest();
		pGuiGraphics.blit(this.resourceLocation, this.getX(), this.getY(), xBlit, yBlit, width, height, textureWidth, textureHeight);
	}

	@Override
	public void onClick(double x, double y, int button) {
		if (button == GLFW_MOUSE_BUTTON_LEFT) {
			isPressed = true;
			onPress.execute(this);
		}
	}


	@Override
	public void onRelease(double x, double y) {
		if(isPressed) {
			Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.3f));
			isPressed = false;
			onRelease.execute(this);
		}
	}

	@Override
	protected void updateWidgetNarration(@NotNull NarrationElementOutput narrationElementOutput) {
		this.defaultButtonNarrationText(narrationElementOutput);
	}

	@FunctionalInterface
	public interface OnInteract {
		void execute(HoldButton button);
	}

}
