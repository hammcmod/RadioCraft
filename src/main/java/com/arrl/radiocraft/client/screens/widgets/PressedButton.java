package com.arrl.radiocraft.client.screens.widgets;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class PressedButton  extends Button {
    private final ResourceLocation resourceLocation;
    private final int u;
    private final int v;
    private final int textureWidth;
    private final int textureHeight;
    private boolean isHeld = false;

    public PressedButton(int x, int y, int width, int height, int u, int v, ResourceLocation resourceLocation, int textureWidth, int textureHeight, Button.OnPress onPress) {
        super(x, y, width, height, CommonComponents.EMPTY, onPress, DEFAULT_NARRATION);
        this.resourceLocation = resourceLocation;
        this.u = u;
        this.v = v;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
    }

    public void onClick(double pMouseX, double pMouseY) {
        super.onClick(pMouseX, pMouseY);
        // This reliably fires, even if the user proceeds to drag.
        isHeld = true;
    }

    public void onRelease(double pMouseX, double pMouseY) {
        super.onRelease(pMouseX, pMouseY);
        // Note: this does not fire if the user drags, even for a pixel. Thus, the onDrag handler.
        isHeld = false;
    }

    public void onDrag(double pMouseX, double pMouseY, double dragX, double dragY) {
        super.onDrag(pMouseX, pMouseY, dragX, dragY);
        /*
        Checks if the mouse is still in the bounds of the button. If not, release the button.\
        We only release it; onDrag can still re-enter the button which would show a re-press,
        which the game doesn't actually process because it watches for when the click occurs.
        (Until the mouse is released, the onDrag will be called back to here for any mouse movement over the whole GUI)
         */
        int left = getX();
        int top = getY();
        int right = getX() + getWidth();
        int bottom = getY() + getHeight();
        if (!(left <= pMouseX) || !(pMouseX <= right) || !(top <= pMouseY) || !(pMouseY <= bottom)) {
            isHeld = false;
        }
    }

    @Override
    public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        // TL;DR When the button is held, render the pressed version, otherwise render the background.
        if (isHeld) {
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderTexture(0, this.resourceLocation);
            RenderSystem.enableDepthTest();
            blit(poseStack, getX(), getY(), u, v, width, height, textureWidth, textureHeight);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public interface OnPress {
        void onPress(Button pButton);
    }

}
