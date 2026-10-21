package com.arrl.radiocraft.client.screens.widgets;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_RIGHT;

/**
 * A rotary switch with a fixed number of positions, drawn with one sprite per position. Left click (or scroll up) turns
 * it one step clockwise, right click (or scroll down) one step back; it stops at both ends like a real band switch.
 */
public class BandKnob extends AbstractWidget {

	/**
	 * Where to find one position's sprite on the sheet and where to draw it relative to the widget's top left.
	 */
	public record Sprite(int u, int v, int width, int height, int offsetX, int offsetY) {}

	private final ResourceLocation texture;
	private final int textureWidth;
	private final int textureHeight;
	private final Sprite[] sprites;
	private final IntSupplier position;
	private final IntConsumer onTurn;

	public BandKnob(int x, int y, int width, int height, ResourceLocation texture, int textureWidth, int textureHeight, Sprite[] sprites, IntSupplier position, IntConsumer onTurn) {
		super(x, y, width, height, CommonComponents.EMPTY);
		this.texture = texture;
		this.textureWidth = textureWidth;
		this.textureHeight = textureHeight;
		this.sprites = sprites.clone();
		this.position = position;
		this.onTurn = onTurn;
	}

	@Override
	protected void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		int index = position.getAsInt();
		if(index < 0 || index >= sprites.length)
			return; // Unknown position: leave the knob painted on the panel.
		Sprite sprite = sprites[index];
		graphics.blit(texture, getX() + sprite.offsetX(), getY() + sprite.offsetY(), sprite.u(), sprite.v(), sprite.width(), sprite.height(), textureWidth, textureHeight);
	}

	@Override
	protected boolean isValidClickButton(int button) {
		return button == GLFW_MOUSE_BUTTON_LEFT || button == GLFW_MOUSE_BUTTON_RIGHT;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if(!active || !visible || !isValidClickButton(button) || !clicked(mouseX, mouseY))
			return false;
		playDownSound(net.minecraft.client.Minecraft.getInstance().getSoundManager());
		turn(button == GLFW_MOUSE_BUTTON_LEFT ? 1 : -1);
		return true;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if(!isMouseOver(mouseX, mouseY) || scrollY == 0.0D)
			return false;
		turn(scrollY > 0.0D ? 1 : -1);
		return true;
	}

	private void turn(int steps) {
		int next = position.getAsInt() + steps;
		if(next >= 0 && next < sprites.length)
			onTurn.accept(next);
	}

	@Override
	protected void updateWidgetNarration(@NotNull NarrationElementOutput output) {
		this.defaultButtonNarrationText(output);
	}
}
