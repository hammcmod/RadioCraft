package com.arrl.radiocraft.client.screens.widgets;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.IntConsumer;

/**
 * A decorative screw widget that shows a normal state and a hover state.
 * Always renders, changing texture when hovered.
 * Supports click-and-drag rotation based on horizontal mouse movement.
 */
public class ScrewButton extends AbstractWidget {
    private final ResourceLocation resourceLocation;
    private final int uNormal;
    private final int vNormal;
    private final int uHover;
    private final int vHover;
    private final int textureWidth;
    private final int textureHeight;
    
    private boolean isDragging = false;
    private double lastMouseX = 0;
    private float rotation = 0.0f; // Rotation in degrees
    /** Degrees of turn that make one step for {@link #onTurn}. */
    public static final float DEGREES_PER_STEP = 15.0f;
    @Nullable
    private IntConsumer onTurn = null;
    private float unspent = 0.0f; // Degrees turned since the last whole step.

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
        this.active = true;
    }

    /**
     * Creates a screw widget following the Dial pattern where hover texture is positioned at u + width.
     * @param x The x position of this widget with relation to the screen
     * @param y The y position of this widget with relation to the screen
     * @param width The width of this widget
     * @param height The height of this widget
     * @param u The u position of the normal texture (hover will be at u + width)
     * @param v The v position of the texture
     * @param resourceLocation The resource file in question
     * @param textureWidth The width of the texture file
     * @param textureHeight The height of the texture file
     */
    public ScrewButton(int x, int y, int width, int height, int u, int v, ResourceLocation resourceLocation, int textureWidth, int textureHeight) {
        this(x, y, width, height, u, v, u + width, v, resourceLocation, textureWidth, textureHeight);
    }

    @Override
    protected void renderWidget(@NotNull GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        if (isDragging) {
            double deltaX = pMouseX - lastMouseX;
            
            if (Math.abs(deltaX) > 0.5) {
                turn((float) deltaX * 2.0f);
                
                lastMouseX = pMouseX;
            }
        }
        
        var pose = pGuiGraphics.pose();
        pose.pushPose();
        
        float centerX = this.getX() + width / 2.0f;
        float centerY = this.getY() + height / 2.0f;
        
        pose.translate(centerX, centerY, 0);
        pose.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(rotation));
        pose.translate(-centerX, -centerY, 0);
        
        boolean hovered = isHovered() || isDragging;
        int u = hovered ? uHover : uNormal;
        int v = hovered ? vHover : vNormal;
        
        int indicatorOffset = -2;
        int totalHeight = height + 2;
        pGuiGraphics.blit(this.resourceLocation, this.getX(), this.getY() + indicatorOffset, 
            u, v + indicatorOffset, width, totalHeight, textureWidth, textureHeight);
        
        pose.popPose();
    }

    @Override
    public void onClick(double pMouseX, double pMouseY) {
        isDragging = true;
        lastMouseX = pMouseX;
    }

    @Override
    public void onRelease(double pMouseX, double pMouseY) {
        isDragging = false;
    }

    @Override
    protected void onDrag(double pMouseX, double pMouseY, double pDragX, double pDragY) {
        if (isDragging) {
            double deltaX = pMouseX - lastMouseX;
            turn((float) deltaX * 2.0f);
            
            lastMouseX = pMouseX;
        }
    }

    @Override
    public boolean mouseDragged(double pMouseX, double pMouseY, int pButton, double pDragX, double pDragY) {
        if (isDragging) {
            double deltaX = pMouseX - lastMouseX;
            turn((float) deltaX * 2.0f);
            
            lastMouseX = pMouseX;
            return true;
        }
        return super.mouseDragged(pMouseX, pMouseY, pButton, pDragX, pDragY);
    }

    @Override
    public boolean mouseReleased(double pMouseX, double pMouseY, int pButton) {
        if (isDragging) {
            isDragging = false;
            this.onRelease(pMouseX, pMouseY);
            return true;
        }
        return super.mouseReleased(pMouseX, pMouseY, pButton);
    }

    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        boolean clicked = this.clicked(pMouseX, pMouseY);
        boolean validButton = this.isValidClickButton(pButton);
        
        if (validButton && clicked) {
            isDragging = true;
            lastMouseX = pMouseX;
            this.playDownSound(net.minecraft.client.Minecraft.getInstance().getSoundManager());
            this.onClick(pMouseX, pMouseY);
            return true;
        }
        return false;
    }

    /**
     * Makes the screw a control: {@code onTurn} gets +1 for every {@link #DEGREES_PER_STEP} turned clockwise (dragging
     * right or scrolling up) and -1 the other way.
     */
    public ScrewButton onTurn(IntConsumer onTurn) {
        this.onTurn = onTurn;
        return this;
    }

    /** Sets how far round the screw is drawn, in degrees. */
    public void setRotation(float degrees) {
        rotation = ((degrees % 360.0f) + 360.0f) % 360.0f;
    }

    private void turn(float degrees) {
        setRotation(rotation + degrees);
        if (onTurn == null)
            return;
        unspent += degrees;
        while (unspent >= DEGREES_PER_STEP) {
            unspent -= DEGREES_PER_STEP;
            onTurn.accept(1);
        }
        while (unspent <= -DEGREES_PER_STEP) {
            unspent += DEGREES_PER_STEP;
            onTurn.accept(-1);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (onTurn == null || !isMouseOver(mouseX, mouseY) || scrollY == 0.0D)
            return false;
        turn(scrollY > 0.0D ? DEGREES_PER_STEP : -DEGREES_PER_STEP);
        return true;
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput narrationElementOutput) {
        // Decorative widget, no narration needed
    }
}
