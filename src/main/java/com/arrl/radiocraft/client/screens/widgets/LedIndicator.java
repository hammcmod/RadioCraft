package com.arrl.radiocraft.client.screens.widgets;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class LedIndicator extends AbstractWidget {

    boolean isOn = false;

    int uOff = -1;
    int vOff = -1;
    int uOn = -1;
    int vOn = -1;
    int textureWidth;
    int textureHeight;
    private ResourceLocation TEXTURE;

    /**
     * Creates a new LED indicator which renders transparency when the LED is off (useful if the base texture has the "off LED" pre-rendered)
     * Note: Remember to call `addRenderableWidget()` when you create the instance of this widget in your screen
     *
     * @param name Name of the LED
     * @param x X position of the LED
     * @param y Y position of the LED
     * @param width Width of the LED
     * @param height Height of the LED
     * @param uOn U position of the LED when on
     * @param vOn V position of the LED when on
     * @param texture Texture
     * @param textureWidth Texture width
     * @param textureHeight Texture height
     */
    public LedIndicator(Component name, int x, int y, int width, int height, int uOn, int vOn, ResourceLocation texture, int textureWidth, int textureHeight) {
        super(x, y, width, height, name);
        this.uOn = uOn;
        this.vOn = vOn;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        this.uOff = uOn;
        this.vOff = vOn + height;
        this.isOn = true;
        TEXTURE = texture;
    }

    /**
     * Creates a new LED indicator which renders both the "off" and "on" textures depending on the state.
     * Note: Remember to call `addRenderableWidget()` when you create the instance of this widget in your screen
     *
     * @param name Name of the LED
     * @param x X position of the LED
     * @param y Y position of the LED
     * @param width Width of the LED
     * @param height Height of the LED
     * @param uOff U position of the LED when off
     * @param vOff V position of the LED when off
     * @param uOn U position of the LED when on
     * @param vOn V position of the LED when on
     * @param texture Texture
     * @param textureWidth Texture width
     * @param textureHeight Texture height
     */
    public LedIndicator(Component name, int x, int y, int width, int height, int uOff, int vOff, int uOn, int vOn, ResourceLocation texture, int textureWidth, int textureHeight) {
        super(x, y, width, height, name);
        this.uOff = uOff;
        this.vOff = vOff;
        this.uOn = uOn;
        this.vOn = vOn;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        this.isOn = false;
        TEXTURE = texture;
    }

    @Override
    protected void renderWidget(@NotNull GuiGraphics guiGraphics, int i, int i1, float v) {
        if (this.vOff == -1) {
            // Render with no "off" texture, render transparency if it's off
            if (isOn) {
                guiGraphics.blit(TEXTURE, this.getX(), this.getY(), uOn, vOn, width, height, textureWidth, textureHeight);
            }
        } else {
            // Render both the "off" and "on" texture depending on the state.
            if (isOn) {
                guiGraphics.blit(TEXTURE, this.getX(), this.getY(), uOn, vOn, width, height, textureWidth, textureHeight);
            } else {
                guiGraphics.blit(TEXTURE, this.getX(), this.getY(), uOff, vOff, width, height, textureWidth, textureHeight);
            }
        }
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput narrationElementOutput) {

    }

    public void setIsOn(boolean isOn) {
        this.isOn = isOn;
    }

    public boolean getIsOn() {
        return isOn;
    }
}
