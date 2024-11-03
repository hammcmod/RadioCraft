package com.arrl.radiocraft.client.screens.widgets;

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
	protected void renderWidget(@NotNull GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
		if(isMouseDown) {
			double xMouseDiff = pMouseX - xMouseLast;

			if(xMouseDiff > 10) {
				onValueUp.execute(this);
				xMouseLast = pMouseX;
				isRotated = !isRotated;
				Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_HAT, 1.0F));
			}
			else if(xMouseDiff < -10) {
				onValueDown.execute(this);
				xMouseLast = pMouseX;
				isRotated = !isRotated;
				Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_HAT, 1.0F));
			}
		}
		int xBlit = !isHovered() && !isMouseDown ? u : u + width;
		int yBlit = !isRotated ? v : v + height;

		pGuiGraphics.blit(this.resourceLocation, this.getX(), this.getY(), xBlit, yBlit, width, height, textureWidth, textureHeight);
	}

	@Override
	public void onClick(double x, double y, int button) {
		if (button == GLFW_MOUSE_BUTTON_LEFT) {
			isMouseDown = true;
			xMouseLast = (int)Math.round(x);
		}
	}


	@Override
	public void onRelease(double x, double y) {
		if(isMouseDown) {
			Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.3F));
			isMouseDown = false;
		}
	}

	@Override
	protected void updateWidgetNarration(@NotNull NarrationElementOutput narrationElementOutput) {
		this.defaultButtonNarrationText(narrationElementOutput);
	}

	@FunctionalInterface
	public interface OnValueChanged {
		void execute(Dial dial);
	}

}
