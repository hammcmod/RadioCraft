package com.arrl.radiocraft.client.screens.widgets;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;

/**
 * A toggle button with fully customizable UV coordinates for each state.
 * Supports: OFF normal, OFF hover, ON normal, ON hover
 */
public class CustomToggleButton extends AbstractWidget {

	private final ResourceLocation resourceLocation;
	private final int uOffNormal;
	private final int vOffNormal;
	private final int uOffHover;
	private final int vOffHover;
	private final int uOnNormal;
	private final int vOnNormal;
	private final int uOnHover;
	private final int vOnHover;
	private final int textureWidth;
	private final int textureHeight;
	private final OnInteract onPress;

	public boolean isToggled;

	/**
	 * Creates a custom toggle button with separate UV coordinates for all 4 states.
	 * @param isToggled Initial state (true = ON, false = OFF)
	 * @param x Screen X position
	 * @param y Screen Y position
	 * @param width Button width
	 * @param height Button height
	 * @param uOffNormal U coordinate for OFF normal state
	 * @param vOffNormal V coordinate for OFF normal state
	 * @param uOffHover U coordinate for OFF hover state
	 * @param vOffHover V coordinate for OFF hover state
	 * @param uOnNormal U coordinate for ON normal state
	 * @param vOnNormal V coordinate for ON normal state
	 * @param uOnHover U coordinate for ON hover state
	 * @param vOnHover V coordinate for ON hover state
	 * @param texLocation Texture resource location
	 * @param texWidth Texture file width
	 * @param texHeight Texture file height
	 * @param onPress Callback when button is pressed
	 */
	public CustomToggleButton(boolean isToggled, int x, int y, int width, int height, 
			int uOffNormal, int vOffNormal, int uOffHover, int vOffHover,
			int uOnNormal, int vOnNormal, int uOnHover, int vOnHover,
			ResourceLocation texLocation, int texWidth, int texHeight, OnInteract onPress) {
		super(x, y, width, height, CommonComponents.EMPTY);
		this.textureWidth = texWidth;
		this.textureHeight = texHeight;
		this.uOffNormal = uOffNormal;
		this.vOffNormal = vOffNormal;
		this.uOffHover = uOffHover;
		this.vOffHover = vOffHover;
		this.uOnNormal = uOnNormal;
		this.vOnNormal = vOnNormal;
		this.uOnHover = uOnHover;
		this.vOnHover = vOnHover;
		this.resourceLocation = texLocation;
		this.isToggled = isToggled;
		this.onPress = onPress;
	}

	@Override
	protected void renderWidget(@NotNull GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
		int u, v;
		if (isToggled) {
			// ON state
			if (isHovered()) {
				u = uOnHover;
				v = vOnHover;
			} else {
				u = uOnNormal;
				v = vOnNormal;
			}
		} else {
			// OFF state
			if (isHovered()) {
				u = uOffHover;
				v = vOffHover;
			} else {
				u = uOffNormal;
				v = vOffNormal;
			}
		}
		
		// Don't render if coordinates are negative (transparent/skip)
		if (u >= 0 && v >= 0) {
			pGuiGraphics.blit(this.resourceLocation, this.getX(), this.getY(), u, v, width, height, textureWidth, textureHeight);
		}
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
		void execute(CustomToggleButton button);
	}

}
