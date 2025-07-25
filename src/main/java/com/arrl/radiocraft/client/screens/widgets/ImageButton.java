package com.arrl.radiocraft.client.screens.widgets;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

public class ImageButton extends Button {
	private final ResourceLocation resourceLocation;
	private final int u;
	private final int v;
	private final int textureWidth;
	private final int textureHeight;

	/**
	 * Creates a button with a texture.
	 * @param x The x position of this widget with relation to the screen
	 * @param y The y position of this widget with relation to the screen
	 * @param width The width of this widget
	 * @param height The height of this widget
	 * @param u The u position of the texture to render. This is relative to the texture file
	 * @param v The v position of the texture to render. This is relative to the texture file
	 * @param resourceLocation The resource file in question
	 * @param textureWidth The width of the texture file
	 * @param textureHeight The height of the texture file
	 * @param onPress Interface function to consume the action generated by the press
	 */
	public ImageButton(int x, int y, int width, int height, int u, int v, ResourceLocation resourceLocation, int textureWidth, int textureHeight, Button.OnPress onPress) {
		super(x, y, width, height, CommonComponents.EMPTY, onPress, DEFAULT_NARRATION);
		this.resourceLocation = resourceLocation;
		this.u = u;
		this.v = v;
		this.textureWidth = textureWidth;
		this.textureHeight = textureHeight;
	}

	@Override
	protected void renderWidget(@NotNull GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
		if (isHovered()) pGuiGraphics.blit(this.resourceLocation, getX(), getY(), u, v, width, height, textureWidth, textureHeight);
	}

	@OnlyIn(Dist.CLIENT)
	public interface OnPress {
		void onPress(Button pButton);
	}

}
