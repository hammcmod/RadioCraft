package com.arrl.radiocraft.client.screens.widgets;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;

import java.util.function.Supplier;

/**
 * Very similar to ToggleButton but the "on" state is determined by a value which can be changed elsewhere.
 */
public class ValueButton extends AbstractWidget {

	private final ResourceLocation resourceLocation;
	private final int u;
	private final int v;
	private final int textureWidth;
	private final int textureHeight;
	private final Supplier<Boolean> valueSupplier;
	private final OnInteract onPress;

	public boolean lastState = false;

	public ValueButton(int x, int y, int width, int height, int u, int v, ResourceLocation texLocation, int texWidth, int texHeight, Supplier<Boolean> valueSupplier, OnInteract onPress) {
		super(x, y, width, height, CommonComponents.EMPTY);
		this.textureWidth = texWidth;
		this.textureHeight = texHeight;
		this.u = u;
		this.v = v;
		this.resourceLocation = texLocation;
		this.valueSupplier = valueSupplier;
		this.onPress = onPress;
	}

	@Override
	public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.setShaderTexture(0, this.resourceLocation);

		boolean value = valueSupplier.get();
		int xBlit = !isHoveredOrFocused() ? u : u + width;
		int yBlit = !value ? v : v + height;

		RenderSystem.enableDepthTest();
		blit(poseStack, this.getX(), this.getY(), xBlit, yBlit, width, height, textureWidth, textureHeight);

		if(lastState != value)
			Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F)); // Will play toggle click by itself when the value changes externally.

		lastState = value;
	}

	@Override
	public void onClick(double x, double y) {
		super.onClick(x, y);
		onPress.execute(this);
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
		this.defaultButtonNarrationText(narrationElementOutput);
	}

	@Override
	public void playDownSound(SoundManager handler) {} // Empty override so the sound doesn't get replayed by the auto handling in render.

	@FunctionalInterface
	public interface OnInteract {
		void execute(ValueButton button);
	}

}