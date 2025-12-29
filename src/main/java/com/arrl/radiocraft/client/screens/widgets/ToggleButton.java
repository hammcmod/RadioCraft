package com.arrl.radiocraft.client.screens.widgets;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;

public class ToggleButton extends AbstractWidget {

	private final ResourceLocation resourceLocation;
	private final int u;
	private final int v;
	private final int textureWidth;
	private final int textureHeight;
	private final OnInteract onPress;
	private final boolean invertToggled;

	public boolean isToggled;

	public ToggleButton(boolean isToggled, int x, int y, int width, int height, int u, int v, ResourceLocation texLocation, int texWidth, int texHeight, OnInteract onPress) {
		this(isToggled, x, y, width, height, u, v, texLocation, texWidth, texHeight, false, onPress);
	}

	public ToggleButton(boolean isToggled, int x, int y, int width, int height, int u, int v, ResourceLocation texLocation, int texWidth, int texHeight, boolean invertToggled, OnInteract onPress) {
		super(x, y, width, height, CommonComponents.EMPTY);
		this.textureWidth = texWidth;
		this.textureHeight = texHeight;
		this.u = u;
		this.v = v;
		this.resourceLocation = texLocation;
		this.isToggled = isToggled;
		this.onPress = onPress;
		this.invertToggled = invertToggled;
	}

	@Override
	protected void renderWidget(@NotNull GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
		int xBlit = !isHovered() ? u : u + width;
		boolean toggledFrame = invertToggled ? !isToggled : isToggled;
		int yBlit = toggledFrame ? v + height : v;
		pGuiGraphics.blit(this.resourceLocation, this.getX(), this.getY(), xBlit, yBlit, width, height, textureWidth, textureHeight);
	}

	@Override
	public void onClick(double x, double y, int button) {
		super.onClick(x, y, button);
		if (button == GLFW_MOUSE_BUTTON_LEFT) {
			isToggled = !isToggled;
			onPress.execute(this);
		}
	}

	@Override
	protected void updateWidgetNarration(@NotNull NarrationElementOutput narrationElementOutput) {
		this.defaultButtonNarrationText(narrationElementOutput);
	}

	@FunctionalInterface
	public interface OnInteract {
		void execute(ToggleButton button);
	}

}
