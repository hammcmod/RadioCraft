package com.arrl.radiocraft.client.screens.widgets;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class MeterNeedleIndicator extends AbstractWidget {

    public enum MeterNeedleType {
        METER_HORIZONTAL,
        METER_VERTICAL,
        METER_ROTATION
    }

    double value = 0.0;
    int u;
    int v;
    int textureWidth;
    int textureHeight;
    MeterNeedleType mnt;
    int meterInnerRadius = -1; // Where the base of the needle is rendered from X, Y
    double startingAngle = -1.0; // Where the needle rests
    double endingAngle = -1.0; // Where the needle is at 100%
    int meterWidth = -1; // Where the needle is rendered when it moves side to side
    int meterHeight = -1; // Where the needle is rendered when it moves up and down
    private ResourceLocation TEXTURE;

    // Physics simulation fields
    private double currentNeedlePosition = 0.0;
    private double needleVelocity = 0.0;
    private long lastUpdateTime = 0;
    private double dampingFactor = 0.85; // Controls how much the needle slows down (0.0 = no damping, 1.0 = instant stop)
    private double springConstant = 0.3; // Controls how strongly the needle is pulled toward target (higher = faster response)
    private double maxVelocity = 0.05; // Maximum velocity per millisecond to prevent overly fast movement
    private double minMovementThreshold = 0.001; // Minimum movement threshold to prevent endless tiny movements

    /**
     * Creates a new needle indicator which renders a horizontally or vertically moved needle onto a meter texture
     * Note: Remember to call `addRenderableWidget()` when you create the instance of this widget in your screen
     *
     * @param name Name of the needle
     * @param mnt Type of needle to render (must be METER_HORIZONTAL, METER_VERTICAL)
     * @param meterDimension width or height (depending on the type) of the full scale needle movement
     * @param x Upper left X position of the needle
     * @param y Upper left Y position of the LED
     * @param width Width of the needle texture
     * @param height Height of the needle texture
     * @param u U position of the needle texture
     * @param v V position of the needle texture
     * @param texture Texture
     * @param textureWidth Texture width
     * @param textureHeight Texture height
     */
    public MeterNeedleIndicator(Component name, MeterNeedleType mnt, int meterDimension, int x, int y, int width, int height, int u, int v, ResourceLocation texture, int textureWidth, int textureHeight) {
        super(x, y, width, height, name);
        this.u = u;
        this.v = v;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        TEXTURE = texture;
        this.mnt = mnt;
        if (mnt == MeterNeedleType.METER_ROTATION) throw new IllegalArgumentException("METER_ROTATION cannot be created with this constructor. Use the other constructor instead.");
        if (mnt == MeterNeedleType.METER_HORIZONTAL) meterWidth = meterDimension;
        if (mnt == MeterNeedleType.METER_VERTICAL) meterHeight = meterDimension;

        // Initialize timing
        lastUpdateTime = System.currentTimeMillis();
    }

    /**
     * Creates a new rotational needle indicator which renders a rotated needle onto a meter texture
     * Note: Remember to call `addRenderableWidget()` when you create the instance of this widget in your screen
     *
     * @param name Name of the needle
     * @param meterInnerRadius Distance from center to the base of the needle
     * @param startingAngle Angle in degrees where the needle rests (0%)
     * @param endingAngle Angle in degrees where the needle is at 100%
     * @param x Upper left X position of the needle
     * @param y Upper left Y position of the needle
     * @param width Width of the needle texture
     * @param height Height of the needle texture
     * @param u U position of the needle texture
     * @param v V position of the needle texture
     * @param texture Texture
     * @param textureWidth Texture width
     * @param textureHeight Texture height
     */
    public MeterNeedleIndicator(Component name, int meterInnerRadius, double startingAngle, double endingAngle, int x, int y, int width, int height, int u, int v, ResourceLocation texture, int textureWidth, int textureHeight) {
        super(x, y, width, height, name);
        this.u = u;
        this.v = v;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        TEXTURE = texture;
        this.mnt = MeterNeedleType.METER_ROTATION;
        this.meterInnerRadius = meterInnerRadius;
        this.startingAngle = startingAngle;
        this.endingAngle = endingAngle;

        // Initialize timing
        lastUpdateTime = System.currentTimeMillis();
    }

    /**
     * Simulates a mechanically activated needle meter with physics-based movement
     * @return The current animated needle position (0.0 to 1.0)
     */
    private double getNeedlePercentage() {
        long currentTime = System.currentTimeMillis();

        // Skip physics update if no time has passed
        if (lastUpdateTime == 0) {
            lastUpdateTime = currentTime;
            return currentNeedlePosition;
        }

        double deltaTime = currentTime - lastUpdateTime;
        lastUpdateTime = currentTime;

        // Skip if time delta is too large (likely due to pause/lag)
        if (deltaTime > 100) {
            return currentNeedlePosition;
        }

        // Calculate the difference between target and current position
        double positionDifference = value - currentNeedlePosition;

        // Apply spring force towards target position
        double springForce = positionDifference * springConstant;

        // Update velocity with spring force
        needleVelocity += springForce * deltaTime * 0.001; // Convert to per-millisecond

        // Apply damping to velocity
        needleVelocity *= Math.pow(dampingFactor, deltaTime * 0.01);

        // Clamp velocity to maximum
        if (Math.abs(needleVelocity) > maxVelocity) {
            needleVelocity = Math.signum(needleVelocity) * maxVelocity;
        }

        // Update position based on velocity
        currentNeedlePosition += needleVelocity * deltaTime;

        // Clamp position to valid range
        currentNeedlePosition = Math.max(0.0, Math.min(1.0, currentNeedlePosition));

        // Stop tiny movements to prevent endless oscillation
        if (Math.abs(positionDifference) < minMovementThreshold && Math.abs(needleVelocity) < minMovementThreshold) {
            needleVelocity = 0.0;
            currentNeedlePosition = value;
        }

        return currentNeedlePosition;
    }

    /**
     * Sets the target value for the needle (0.0 to 1.0)
     * @param targetValue The target percentage value
     */
    public void setValue(double targetValue) {
        this.value = Math.max(0.0, Math.min(1.0, targetValue));
    }

    /**
     * Gets the current target value
     * @return The target value (0.0 to 1.0)
     */
    public double getValue() {
        return value;
    }

    /**
     * Configures the physics parameters for needle movement
     * @param springConstant How strongly the needle is pulled toward target (0.1 to 1.0, default: 0.3)
     * @param dampingFactor How much the needle slows down (0.0 to 1.0, default: 0.85)
     * @param maxVelocity Maximum velocity per millisecond (default: 0.05)
     */
    public void configurePhysics(double springConstant, double dampingFactor, double maxVelocity) {
        this.springConstant = Math.max(0.01, Math.min(1.0, springConstant));
        this.dampingFactor = Math.max(0.0, Math.min(1.0, dampingFactor));
        this.maxVelocity = Math.max(0.001, maxVelocity);
    }

    /**
     * Instantly snaps the needle to the target position (useful for initialization)
     */
    public void snapToTarget() {
        currentNeedlePosition = value;
        needleVelocity = 0.0;
    }

    @Override
    protected void renderWidget(@NotNull GuiGraphics guiGraphics, int i, int i1, float v) {
        // Update needle position before rendering
        double needlePos = getNeedlePercentage();

        switch (mnt) {
            case METER_HORIZONTAL:
                int horizontalOffset = (int) (needlePos * meterWidth);
                guiGraphics.blit(TEXTURE, this.getX() + horizontalOffset, this.getY(), u, v, width, height, textureWidth, textureHeight);
                break;
            case METER_VERTICAL:
                int verticalOffset = (int) (needlePos * meterHeight);
                guiGraphics.blit(TEXTURE, this.getX(), this.getY() + verticalOffset, u, v, width, height, textureWidth, textureHeight);
                break;
            case METER_ROTATION:
                // Calculate current rotation angle based on needle position
                double currentAngle = startingAngle + (needlePos * (endingAngle - startingAngle));

                // Convert angle to radians
                double angleRadians = Math.toRadians(currentAngle);

                // Calculate the center point of the meter (where the needle pivots)
                int centerX = this.getX() + (width / 2);
                int centerY = this.getY() + (height / 2);

                // Save the current transformation matrix
                guiGraphics.pose().pushPose();

                // Translate to the center point
                guiGraphics.pose().translate(centerX, centerY, 0);

                // Rotate around the center
                guiGraphics.pose().mulPose(com.mojang.math.Axis.ZP.rotation((float) angleRadians));

                // Translate back, adjusting for the needle's inner radius offset
                guiGraphics.pose().translate(-width / 2, -meterInnerRadius, 0);

                // Render the rotated needle
                guiGraphics.blit(TEXTURE, 0, 0, u, v, width, height, textureWidth, textureHeight);

                // Restore the transformation matrix
                guiGraphics.pose().popPose();
                break;
        }
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput narrationElementOutput) {

    }
}