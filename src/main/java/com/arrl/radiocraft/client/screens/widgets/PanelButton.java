package com.arrl.radiocraft.client.screens.widgets;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.CommonComponents;
import org.jetbrains.annotations.NotNull;

/**
 * A button that is already painted on the radio's panel: it draws nothing itself, just clicks.
 */
public class PanelButton extends AbstractWidget {

	private final Runnable onPress;

	public PanelButton(int x, int y, int width, int height, Runnable onPress) {
		super(x, y, width, height, CommonComponents.EMPTY);
		this.onPress = onPress;
	}

	@Override
	public void onClick(double mouseX, double mouseY, int button) {
		onPress.run();
	}

	@Override
	protected void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		if(isHovered()) // A faint highlight so it is clear the painted button is a button.
			graphics.fill(getX(), getY(), getX() + width, getY() + height, 0x20FFFFFF);
	}

	@Override
	protected void updateWidgetNarration(@NotNull NarrationElementOutput output) {
		this.defaultButtonNarrationText(output);
	}
}
