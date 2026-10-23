package com.arrl.radiocraft.client.screens;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jetbrains.annotations.NotNull;

/**
 * A station terminal's panel (APRS terminal, transfer interface), drawn {@link #getScale} times larger than the GUI
 * scale would, as large as fits, in steps of whole window pixels so the pixel art stays sharp. Everything inside works
 * in unscaled screen coordinates: the screen is laid out in a smaller "virtual" window, drawing is scaled up, and the
 * mouse is scaled down on the way in. Subclasses take clicks and scrolls in {@link #panelClicked} and
 * {@link #panelScrolled}, already scaled.
 */
public abstract class StationPanelScreen<M extends AbstractContainerMenu> extends AbstractContainerScreen<M> {

	/** Most of the window the screen may take, either way. */
	private static final double WINDOW_SHARE = 0.8D;
	private float scale = 1.0F;

	protected StationPanelScreen(M menu, Inventory playerInventory, Component title) {
		super(menu, playerInventory, title);
	}

	/** How many times larger than the GUI scale the screen is drawn; its own coordinates are all unscaled. */
	public float getScale() {
		return scale;
	}

	@Override
	protected void init() {
		fitToWindow(); // Before super.init, which centres the panel in width x height.
		super.init();
	}

	/** Picks {@link #scale} for the window and lays the screen out in the scaled-down window it gives. */
	private void fitToWindow() {
		Window window = Minecraft.getInstance().getWindow();
		double guiScale = window.getGuiScale();
		int pixelsPerTexel = (int)Math.floor(Math.min((double)window.getWidth() / imageWidth, (double)window.getHeight() / imageHeight) * WINDOW_SHARE);
		scale = (float)Math.max(1.0D, pixelsPerTexel / guiScale);
		width = (int)(window.getGuiScaledWidth() / scale);
		height = (int)(window.getGuiScaledHeight() / scale);
	}

	@Override
	public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		guiGraphics.pose().pushPose();
		guiGraphics.pose().scale(scale, scale, 1.0F);
		int x = (int)(mouseX / scale), y = (int)(mouseY / scale);
		super.render(guiGraphics, x, y, partialTick);
		renderTooltip(guiGraphics, x, y);
		renderPanelTooltips(guiGraphics, x, y);
		guiGraphics.pose().popPose();
	}

	/** Tooltips for the panel's own controls, in unscaled screen coordinates. */
	protected void renderPanelTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		return super.mouseReleased(mouseX / scale, mouseY / scale, button);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		return super.mouseDragged(mouseX / scale, mouseY / scale, button, dragX / scale, dragY / scale);
	}

	@Override
	public void mouseMoved(double mouseX, double mouseY) {
		super.mouseMoved(mouseX / scale, mouseY / scale);
	}

	@Override
	public final boolean mouseClicked(double rawX, double rawY, int button) {
		double mouseX = rawX / scale, mouseY = rawY / scale;
		return panelClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public final boolean mouseScrolled(double rawX, double rawY, double scrollX, double scrollY) {
		double mouseX = rawX / scale, mouseY = rawY / scale;
		return panelScrolled(mouseX, mouseY, scrollY) || super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	/** A click on the panel, in unscaled screen coordinates; true if it was used. */
	protected boolean panelClicked(double mouseX, double mouseY, int button) {
		return false;
	}

	/** A scroll on the panel, in unscaled screen coordinates; true if it was used. */
	protected boolean panelScrolled(double mouseX, double mouseY, double scrollY) {
		return false;
	}

	// -------------------- HELPERS --------------------

	/** True if the mouse is inside {@code r} ({x, y, width, height} from the panel's corner). */
	protected boolean inside(int[] r, double mouseX, double mouseY) {
		double x = mouseX - leftPos;
		double y = mouseY - topPos;
		return x >= r[0] && x < r[0] + r[2] && y >= r[1] && y < r[1] + r[3];
	}

	protected static void fillAt(GuiGraphics guiGraphics, int[] r, int x, int y, int colour) {
		guiGraphics.fill(x + r[0], y + r[1], x + r[0] + r[2], y + r[1] + r[3], colour);
	}

	protected void text(GuiGraphics guiGraphics, String text, float x, float y, float scale, int colour, boolean centred) {
		guiGraphics.pose().pushPose();
		guiGraphics.pose().translate(x, y, 0.0F);
		guiGraphics.pose().scale(scale, scale, 1.0F);
		guiGraphics.drawString(font, text, centred ? -font.width(text) / 2 : 0, 0, colour, false);
		guiGraphics.pose().popPose();
	}
}
