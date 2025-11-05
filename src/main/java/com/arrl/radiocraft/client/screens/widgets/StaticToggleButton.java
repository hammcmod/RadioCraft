package com.arrl.radiocraft.client.screens.widgets;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;

/**
 * A toggle button that only renders when in the ON state.
 * The OFF state is assumed to be already drawn in the background GUI texture.
 */
public class StaticToggleButton extends AbstractWidget {

	private final ResourceLocation resourceLocation;
	private final int uOn;
	private final int vOn;
	private final int textureWidth;
	private final int textureHeight;
	private final OnInteract onPress;

	public boolean isToggled;

	/**
	 * Creates a static toggle button.
	 * @param isToggled Initial state (true = ON, false = OFF)
	 * @param x Screen X position
	 * @param y Screen Y position
	 * @param width Button width
	 * @param height Button height
	 * @param uOn Texture U coordinate for ON state sprite
	 * @param vOn Texture V coordinate for ON state sprite
	 * @param texLocation Texture resource location
	 * @param texWidth Texture file width
	 * @param texHeight Texture file height
	 * @param onPress Callback when button is pressed
	 */
	public StaticToggleButton(boolean isToggled, int x, int y, int width, int height, int uOn, int vOn, 
			ResourceLocation texLocation, int texWidth, int texHeight, OnInteract onPress) {
		super(x, y, width, height, CommonComponents.EMPTY);
		this.textureWidth = texWidth;
		this.textureHeight = texHeight;
		this.uOn = uOn;
		this.vOn = vOn;
		this.resourceLocation = texLocation;
		this.isToggled = isToggled;
		this.onPress = onPress;
	}

	@Override
	protected void renderWidget(@NotNull GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
		// Only render when toggled ON (OFF state is already in background)
		if (isToggled) {
			pGuiGraphics.blit(this.resourceLocation, this.getX(), this.getY(), uOn, vOn, width, height, textureWidth, textureHeight);
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
		void execute(StaticToggleButton button);
	}

}
