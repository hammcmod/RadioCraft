package com.arrl.radiocraft.client.screens;

import com.arrl.radiocraft.common.blockentities.IonosphericProcessorBlockEntity;
import com.arrl.radiocraft.common.menus.IonosphericProcessorMenu;
import com.arrl.radiocraft.common.network.serverbound.SStationPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;

/**
 * The Ionospheric Processor's terminal: an amber text screen in the rack, drawn without a texture. It shows whether the
 * ionosonde is reading the sky, the radio it sends through, any blackout forecast and what it has sent; the bottom
 * rows are its controls, clicked or keyed: [1] timed bulletins, [2] blackout alerts, [3] send a bulletin now.
 */
public class IonosphericProcessorScreen extends StationPanelScreen<IonosphericProcessorMenu> {

	private static final int AMBER = 0xFFFFB020;
	private static final int DIM = 0xFF8C5A0C;
	private static final int INK = 0xFF140A00;
	private static final int BEZEL = 0xFF2C2A26;
	private static final int BEZEL_EDGE = 0xFF45423C;
	private static final int GLASS = 0xFF0C0700;

	private static final int WIDTH = 236, HEIGHT = 150;
	private static final int SCREEN_X = 10, SCREEN_Y = 10, SCREEN_W = WIDTH - 20, SCREEN_H = HEIGHT - 20;
	private static final int TEXT_X = SCREEN_X + 6, ROW = 10;
	/** The control rows' click boxes: {x, y, width, height} from the panel's corner. */
	private static final int[] BULLETINS = { SCREEN_X + 3, SCREEN_Y + 4 + ROW * 8, SCREEN_W - 6, ROW };
	private static final int[] ALERTS = { SCREEN_X + 3, SCREEN_Y + 4 + ROW * 9, SCREEN_W - 6, ROW };
	private static final int[] SEND = { SCREEN_X + 3, SCREEN_Y + 4 + ROW * 10, SCREEN_W - 6, ROW };

	@Nullable
	private TerminalWhirr whirr;

	public IonosphericProcessorScreen(IonosphericProcessorMenu menu, Inventory playerInventory, Component title) {
		super(menu, playerInventory, title);
		imageWidth = WIDTH;
		imageHeight = HEIGHT;
	}

	private IonosphericProcessorBlockEntity processor() {
		return menu.blockEntity;
	}

	@Override
	protected void init() {
		super.init();
		if(whirr == null)
			whirr = TerminalWhirr.start(0.15F);
	}

	@Override
	public void removed() {
		super.removed();
		if(whirr != null) {
			whirr.finish();
			whirr = null;
		}
	}

	@Override
	protected void containerTick() {
		super.containerTick();
		if(whirr != null)
			whirr.tick();
	}

	private void act(String action) {
		SStationPacket.send(processor().getBlockPos(), action);
		if(minecraft != null)
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_HAT.value(), 1.8F, 0.12F));
	}

	@Override
	protected boolean panelClicked(double mouseX, double mouseY, int button) {
		if(button != 0)
			return false;
		if(inside(BULLETINS, mouseX, mouseY))
			act("bulletins");
		else if(inside(ALERTS, mouseX, mouseY))
			act("alerts");
		else if(inside(SEND, mouseX, mouseY))
			act("send");
		else
			return false;
		return true;
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		switch(keyCode) {
			case GLFW.GLFW_KEY_1, GLFW.GLFW_KEY_KP_1 -> act("bulletins");
			case GLFW.GLFW_KEY_2, GLFW.GLFW_KEY_KP_2 -> act("alerts");
			case GLFW.GLFW_KEY_3, GLFW.GLFW_KEY_KP_3 -> act("send");
			default -> {
				return super.keyPressed(keyCode, scanCode, modifiers);
			}
		}
		return true;
	}

	@Override
	protected void renderBg(@NotNull GuiGraphics g, float partialTick, int mouseX, int mouseY) {
		int x = leftPos, y = topPos;
		g.fill(x, y, x + WIDTH, y + HEIGHT, BEZEL_EDGE);
		g.fill(x + 2, y + 2, x + WIDTH - 2, y + HEIGHT - 2, BEZEL);
		g.fill(x + SCREEN_X, y + SCREEN_Y, x + SCREEN_X + SCREEN_W, y + SCREEN_Y + SCREEN_H, GLASS);

		IonosphericProcessorBlockEntity p = processor();
		String key = "screen.radiocraft.ionospheric_processor.";
		row(g, 0, Component.translatable(key + "title", p.sender()).getString(), AMBER);
		row(g, 1, "-".repeat(33), DIM);
		row(g, 2, Component.translatable(key + (p.isOnline() ? "sounder_online" : "sounder_offline")).getString(), p.isOnline() ? AMBER : DIM);
		row(g, 3, p.getRadioMhz() > 0.0F
				? Component.translatable(key + "radio", String.format(Locale.ROOT, "%.3f", p.getRadioMhz())).getString()
				: Component.translatable(key + "no_radio").getString(), p.getRadioMhz() > 0.0F ? AMBER : DIM);
		row(g, 4, p.getBlackoutMinutes() >= 0
				? Component.translatable(key + "forecast_blackout", p.getBlackoutMinutes()).getString()
				: Component.translatable(key + "forecast_quiet").getString(), p.getBlackoutMinutes() >= 0 ? AMBER : DIM);
		row(g, 5, Component.translatable(key + "sent", p.getBulletinsSent(), p.getAlertsSent()).getString(), DIM);
		row(g, 6, "-".repeat(33), DIM);
		control(g, BULLETINS, 8, Component.translatable(key + "bulletins", onOff(p.hasTimedBulletins())).getString(), mouseX, mouseY);
		control(g, ALERTS, 9, Component.translatable(key + "alerts", onOff(p.hasBlackoutAlerts())).getString(), mouseX, mouseY);
		control(g, SEND, 10, Component.translatable(key + "send").getString(), mouseX, mouseY);
		if((System.currentTimeMillis() / 500L) % 2L == 0L) // The cursor.
			g.fill(x + TEXT_X, y + SCREEN_Y + 4 + ROW * 11 + 1, x + TEXT_X + 5, y + SCREEN_Y + 4 + ROW * 11 + 8, AMBER);
	}

	private static String onOff(boolean on) {
		return Component.translatable(on ? "screen.radiocraft.ionospheric_processor.on" : "screen.radiocraft.ionospheric_processor.off").getString();
	}

	private void row(GuiGraphics g, int row, String text, int colour) {
		g.drawString(font, text.toUpperCase(Locale.ROOT), leftPos + TEXT_X, topPos + SCREEN_Y + 5 + ROW * row, colour, false);
	}

	/** A control row: inverse video under the mouse. */
	private void control(GuiGraphics g, int[] box, int row, String text, int mouseX, int mouseY) {
		boolean hover = inside(box, mouseX, mouseY);
		if(hover)
			fillAt(g, box, leftPos, topPos, AMBER);
		row(g, row, text, hover ? INK : AMBER);
	}

	@Override
	protected void renderLabels(@NotNull GuiGraphics g, int mouseX, int mouseY) {
		// All drawn on the glass in renderBg.
	}
}
