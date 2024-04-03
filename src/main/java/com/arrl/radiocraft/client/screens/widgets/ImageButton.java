package com.arrl.radiocraft.client.screens.widgets;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class ImageButton extends Button {
	private final ResourceLocation resourceLocation;
	private final int u;
	private final int v;
	private final int textureWidth;
	private final int textureHeight;


	public ImageButton(int x, int y, int width, int height, int u, int v, ResourceLocation resourceLocation, int textureWidth, int textureHeight, Button.OnPress onPress) {
		super(x, y, width, height, CommonComponents.EMPTY, onPress, DEFAULT_NARRATION);
		this.resourceLocation = resourceLocation;
		this.u = u;
		this.v = v;
		this.textureWidth = textureWidth;
		this.textureHeight = textureHeight;
	}

	@Override
	public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.setShaderTexture(0, this.resourceLocation);

		int xBlit = !isHoveredOrFocused() ? u : u + width;
//		int yBlit = !isActive() ? v : v + height;

		RenderSystem.enableDepthTest();
		blit(poseStack, getX(), getY(), xBlit, v, width, height, textureWidth, textureHeight);
	}

	@OnlyIn(Dist.CLIENT)
	public interface OnPress {
		void onPress(Button pButton);
	}

}
