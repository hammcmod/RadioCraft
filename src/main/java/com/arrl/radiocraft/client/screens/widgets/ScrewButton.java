package com.arrl.radiocraft.client.screens.widgets;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/**
 * A decorative screw widget that shows a normal state and a hover state.
 * Always renders, changing texture when hovered.
 */
public class ScrewButton extends AbstractWidget {
    private final ResourceLocation resourceLocation;
    private final int uNormal;
    private final int vNormal;
    private final int uHover;
    private final int vHover;
    private final int textureWidth;
    private final int textureHeight;

    /**
     * Creates a screw widget with normal and hover textures.
     * @param x The x position of this widget with relation to the screen
     * @param y The y position of this widget with relation to the screen
     * @param width The width of this widget
     * @param height The height of this widget
     * @param uNormal The u position of the normal texture
     * @param vNormal The v position of the normal texture
     * @param uHover The u position of the hover texture
     * @param vHover The v position of the hover texture
     * @param resourceLocation The resource file in question
     * @param textureWidth The width of the texture file
     * @param textureHeight The height of the texture file
     */
    public ScrewButton(int x, int y, int width, int height, int uNormal, int vNormal, int uHover, int vHover, ResourceLocation resourceLocation, int textureWidth, int textureHeight) {
        super(x, y, width, height, CommonComponents.EMPTY);
        this.resourceLocation = resourceLocation;
        this.uNormal = uNormal;
        this.vNormal = vNormal;
        this.uHover = uHover;
        this.vHover = vHover;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
    }

    @Override
    protected void renderWidget(@NotNull GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        // Render normal screw by default, hover screw (with border) when hovered
        // Both must be rendered at the exact same screen position
        if (isHovered()) {
            pGuiGraphics.blit(this.resourceLocation, this.getX(), this.getY(), uHover, vHover, width, height, textureWidth, textureHeight);
        } else {
            pGuiGraphics.blit(this.resourceLocation, this.getX(), this.getY(), uNormal, vNormal, width, height, textureWidth, textureHeight);
        }
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput narrationElementOutput) {
        // Decorative widget, no narration needed
    }
}
