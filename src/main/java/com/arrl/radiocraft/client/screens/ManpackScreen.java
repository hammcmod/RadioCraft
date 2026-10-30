package com.arrl.radiocraft.client.screens;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.client.ManpackClient;
import com.arrl.radiocraft.client.RadiocraftKeys;
import com.arrl.radiocraft.common.datacomponents.ManpackState;
import com.arrl.radiocraft.common.network.serverbound.SManpackCWPacket;
import com.arrl.radiocraft.common.network.serverbound.SManpackPacket;
import com.arrl.radiocraft.common.radio.morse.CWSendBuffer;
import com.arrl.radiocraft.common.radio.voice.handheld.ManpackRadio;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;

/**
 * The HF manpack's front panel, in olive drab like a PRC-25: the frequency window, the BAND switch (10m or 20m), the FREQ
 * knob (mouse wheel, or click its left or right half; SHIFT for 10 kHz), the AF and MIC gain knobs (wheel), the PWR and
 * SSB/CW switches, PTT (held; in CW it is the key, as is the CW key), the TX lamp, and the handset on its hook: click
 * it to take it (the panel closes, hold right click with it to talk), click the empty hook to hang it back up.
 */
public class ManpackScreen extends Screen {

	private static final ResourceLocation TEXTURE = Radiocraft.id("textures/gui/hf_manpack.png");
	private static final ResourceLocation WIDGETS = Radiocraft.id("textures/gui/hf_manpack_widgets.png");
	private static final int W = 280, H = 176;
	private static final int[] WINDOW = {18, 14, 124, 36};
	private static final int[] BAND = {160, 14, 36, 36};
	private static final int[] FREQ = {30, 62, 52, 52};
	private static final int[] AF = {104, 76, 34, 34};
	private static final int[] MIC = {150, 76, 34, 34};
	private static final int[] PWR = {18, 136, 22, 28};
	private static final int[] MODE = {58, 136, 22, 28};
	private static final int[] PTT = {98, 140, 44, 22};
	private static final int[] LAMP = {156, 146, 12, 12};
	private static final int[] HANDSET = {212, 22, 52, 126};
	private static final int[] BAY = {202, 0, 62, 176};
	private static final int AMBER = 0xFFFFB84A, RED = 0xFFFF5040, DIM = 0xFF6E5220;

	private int left, top;
	private boolean pttHeld = false;
	private boolean cwKeyHeld = false;
	private final CWSendBuffer cwSend = new CWSendBuffer(SManpackCWPacket::send);

	public ManpackScreen() {
		super(Component.translatable("item.radiocraft.hf_manpack"));
	}

	public static void open() {
		Minecraft.getInstance().setScreen(new ManpackScreen());
	}

	private static ManpackState state() {
		ItemStack stack = ManpackClient.manpack();
		return stack.isEmpty() ? null : ManpackRadio.stateOf(stack);
	}

	@Override
	protected void init() {
		left = (width - W) / 2;
		top = (height - H) / 2;
	}

	private boolean inside(int[] r, double mouseX, double mouseY) {
		return mouseX >= left + r[0] && mouseX < left + r[0] + r[2] && mouseY >= top + r[1] && mouseY < top + r[1] + r[3];
	}

	private void click(float pitch) {
		if(minecraft != null)
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.LEVER_CLICK, pitch, 0.4F));
	}

	@Override
	public void tick() {
		ManpackState state = state();
		if(state == null) {
			onClose(); // The manpack is no longer to hand.
			return;
		}
		if(state.on() && state.cw() && (pttHeld || cwKeyHeld))
			cwSend.setShouldAccumulate();
		cwSend.tick();
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		ManpackState state = state();
		if(state == null || button != 0)
			return super.mouseClicked(mouseX, mouseY, button);
		if(inside(PWR, mouseX, mouseY)) {
			click(0.8F);
			SManpackPacket.send("power", 0.0F);
		}
		else if(inside(MODE, mouseX, mouseY)) {
			click(1.0F);
			SManpackPacket.send("mode", 0.0F);
		}
		else if(inside(BAND, mouseX, mouseY)) {
			click(0.6F);
			SManpackPacket.send("band", 0.0F);
		}
		else if(inside(FREQ, mouseX, mouseY)) {
			click(1.6F);
			SManpackPacket.send("tune", (mouseX < left + FREQ[0] + FREQ[2] / 2.0D ? -1 : 1) * (hasShiftDown() ? 10 : 1));
		}
		else if(inside(PTT, mouseX, mouseY)) {
			pttHeld = true;
			click(1.4F);
			if(!state.cw())
				SManpackPacket.send("ptt", 1.0F);
		}
		else if(inside(HANDSET, mouseX, mouseY) || inside(BAY, mouseX, mouseY)) {
			if(state.micOut())
				SManpackPacket.send("hang_mic", 0.0F);
			else {
				SManpackPacket.send("take_mic", 0.0F);
				onClose(); // The handset is in the hand now.
			}
		}
		else
			return super.mouseClicked(mouseX, mouseY, button);
		return true;
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if(pttHeld) {
			pttHeld = false;
			SManpackPacket.send("ptt", 0.0F);
			click(1.1F);
			return true;
		}
		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		ManpackState state = state();
		if(state == null)
			return false;
		int direction = scrollY > 0 ? 1 : -1;
		if(inside(FREQ, mouseX, mouseY))
			SManpackPacket.send("tune", direction * (hasShiftDown() ? 10 : 1));
		else if(inside(AF, mouseX, mouseY))
			SManpackPacket.send("gain", state.gain() + direction * 0.1F);
		else if(inside(MIC, mouseX, mouseY))
			SManpackPacket.send("mic_gain", state.micGain() + direction * 0.1F);
		else if(inside(BAND, mouseX, mouseY))
			SManpackPacket.send("band", 0.0F);
		else
			return false;
		click(1.8F);
		return true;
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if(RadiocraftKeys.CW_KEY.matches(keyCode, scanCode)) {
			cwKeyHeld = true;
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
		if(RadiocraftKeys.CW_KEY.matches(keyCode, scanCode)) {
			cwKeyHeld = false;
			return true;
		}
		return super.keyReleased(keyCode, scanCode, modifiers);
	}

	@Override
	public void removed() {
		super.removed();
		if(pttHeld)
			SManpackPacket.send("ptt", 0.0F); // Never leave it keyed.
	}

	@Override
	public void renderBackground(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		super.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
		ManpackState state = state();
		guiGraphics.blit(TEXTURE, left, top, 0, 0, W, H, W, H);
		if(state == null)
			return;
		if(state.on())
			widget(guiGraphics, PWR);
		if(state.cw())
			widget(guiGraphics, MODE);
		if(pttHeld)
			widget(guiGraphics, PTT);
		boolean keyed = state.on() && (ManpackClient.transmitting() || (state.cw() && (pttHeld || cwKeyHeld)));
		if(keyed)
			widget(guiGraphics, LAMP);
		if(state.micOut())
			widget(guiGraphics, BAY);
		pointer(guiGraphics, BAND, state.tenMetres() ? -45.0D : 45.0D, 0.45D);
		float bottom = state.tenMetres() ? ManpackState.TEN_METRE_BOTTOM : ManpackState.TWENTY_METRE_BOTTOM;
		float span = state.tenMetres() ? 1_700_000.0F : 350_000.0F;
		pointer(guiGraphics, FREQ, -135.0D + 270.0D * Math.max(0.0D, Math.min(1.0D, (state.frequency() - bottom) / span)), 0.35D);
		pointer(guiGraphics, AF, -135.0D + 135.0D * state.gain(), 0.45D);
		pointer(guiGraphics, MIC, -135.0D + 135.0D * state.micGain(), 0.45D);
		renderWindow(guiGraphics, state, keyed);
	}

	private void widget(GuiGraphics guiGraphics, int[] r) {
		guiGraphics.blit(WIDGETS, left + r[0], top + r[1], r[0], r[1], r[2], r[3], W, H);
	}

	/** A knob's white pointer line at {@code degrees} (0 straight up, clockwise), from {@code inner} of its radius out. */
	private void pointer(GuiGraphics guiGraphics, int[] knob, double degrees, double inner) {
		double cx = left + knob[0] + knob[2] / 2.0D, cy = top + knob[1] + knob[3] / 2.0D, r = knob[2] / 2.0D - 3.0D;
		double a = Math.toRadians(degrees);
		for(double d = r * inner; d <= r; d += 0.5D) {
			int x = (int)Math.floor(cx + Math.sin(a) * d), y = (int)Math.floor(cy - Math.cos(a) * d);
			guiGraphics.fill(x, y, x + 1, y + 1, 0xFFE8E4D0);
		}
	}

	/** The window: frequency, mode, band, TX, the S-meter and the battery. Dark while it is off. */
	private void renderWindow(GuiGraphics guiGraphics, ManpackState state, boolean keyed) {
		if(!state.on())
			return;
		int x = left + WINDOW[0], y = top + WINDOW[1];
		String freq = String.format(Locale.ROOT, "%.3f", state.frequency() / 1.0E6D);
		guiGraphics.pose().pushPose();
		guiGraphics.pose().translate(x + WINDOW[2] / 2.0F, y + 12, 0.0F);
		guiGraphics.pose().scale(1.6F, 1.6F, 1.0F);
		guiGraphics.drawString(font, freq, -font.width(freq) / 2, -font.lineHeight / 2, keyed ? RED : AMBER, false);
		guiGraphics.pose().popPose();
		small(guiGraphics, state.cw() ? "CW" : "SSB", x + 3, y + 2, AMBER);
		small(guiGraphics, state.bandName().toUpperCase(Locale.ROOT), x + 3, y + 26, AMBER);
		if(keyed)
			small(guiGraphics, "TX", x + WINDOW[2] - 12, y + 2, RED);
		int bars = Math.round(ManpackClient.signal() * 10.0F);
		for(int i = 0; i < 10; i++)
			guiGraphics.fill(x + 30 + i * 6, y + 28, x + 34 + i * 6, y + 32, i < bars ? AMBER : DIM);
		int percent = Math.round(100.0F * state.energy() / Math.max(1, ManpackState.CAPACITY));
		String battery = "BAT " + percent + "%";
		small(guiGraphics, battery, x + WINDOW[2] - 3 - font.width(battery) * 0.6F, y + 26, percent < 15 ? RED : AMBER); // Against the right edge.
	}

	private void small(GuiGraphics guiGraphics, String text, float x, float y, int colour) {
		guiGraphics.pose().pushPose();
		guiGraphics.pose().translate(x, y, 0.0F);
		guiGraphics.pose().scale(0.6F, 0.6F, 1.0F);
		guiGraphics.drawString(font, text, 0, 0, colour, false);
		guiGraphics.pose().popPose();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
