package com.arrl.radiocraft.client.screens;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.client.screens.widgets.Dial;
import com.arrl.radiocraft.client.screens.widgets.ToggleButton;
import com.arrl.radiocraft.common.be_networks.network_objects.RadioNetworkObject;
import com.arrl.radiocraft.common.blockentities.VHFRepeaterBlockEntity;
import com.arrl.radiocraft.common.menus.VHFRepeaterMenu;
import com.arrl.radiocraft.common.network.serverbound.SBlockRadioControlPacket;
import com.arrl.radiocraft.common.network.serverbound.SRepeaterSettingsPacket;
import com.arrl.radiocraft.common.radio.Band;
import com.arrl.radiocraft.common.radio.voice.Ctcss;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * The repeater's front panel. Left: POWER, PRT RX / PRT TX (input and output frequency), CW ID (callsign and how often
 * it is sent), CTC RX / CTC TX (access and output tones), MIC (talk out through the repeater from here), TX
 * (transmitter on, i.e. repeating) and the monitor SPEAKER, with the hang time and timeout knobs under them. Right: the
 * input and output frequencies with a knob each, and four status windows: power, duplexer, input and transmitter.
 * The settings buttons open a panel over the face.
 */
public class VHFRepeaterScreen extends AbstractContainerScreen<VHFRepeaterMenu> {

	private static final ResourceLocation TEXTURE = Radiocraft.id("textures/gui/vhf_repeater.png");
	private static final ResourceLocation WIDGETS_TEXTURE = Radiocraft.id("textures/gui/vhf_repeater_widgets.png");

	private static final int[] HANG_STEPS = { 0, 10, 20, 30, 40, 50, 60, 80, 100 };
	private static final int[] TIMEOUT_STEPS = { 0, 30 * 20, 60 * 20, 2 * 60 * 20, 3 * 60 * 20, 5 * 60 * 20, 10 * 60 * 20 };
	private static final int[] ID_STEPS = { 0, 5 * 60 * 20, 10 * 60 * 20, 15 * 60 * 20, 30 * 60 * 20 };

	private static final int LIT = 0x66FF66;
	private static final int AMBER = 0xFFB040;
	private static final int RED = 0xFF5533;
	private static final int DIM = 0x445544;

	/** What the settings panel over the face is showing, if anything. */
	private enum Panel { NONE, RX_FREQ, TX_FREQ, CALLSIGN, RX_TONE, TX_TONE }

	private Panel panel = Panel.NONE;
	private final List<ToggleButton> toggles = new ArrayList<>();
	private final List<Supplier<Boolean>> toggleStates = new ArrayList<>();
	@Nullable
	private EditBox editBox;
	private float pendingTone;
	private int pendingIdInterval;
	private Dial hangDial;
	private Dial timeoutDial;
	private Dial rxDial;
	private Dial txDial;

	public VHFRepeaterScreen(VHFRepeaterMenu menu, Inventory playerInventory, Component title) {
		super(menu, playerInventory, title);
		this.imageWidth = 256;
		this.imageHeight = 151;
	}

	private VHFRepeaterBlockEntity repeater() {
		return menu.blockEntity;
	}

	@Override
	protected void init() {
		super.init();
		toggles.clear();
		toggleStates.clear();
		editBox = null;
		if(panel == Panel.NONE)
			initFace();
		else
			initPanel();
	}

	// -------------------- FACE --------------------

	private void initFace() {
		toggle(18, 29, 0, 0, menu::isPowered, () -> SBlockRadioControlPacket.updateServer(repeater().getBlockPos(), SBlockRadioControlPacket.Action.TOGGLE_POWER, true));
		toggle(41, 29, 0, 42, () -> false, () -> open(Panel.RX_FREQ));
		toggle(64, 29, 0, 84, () -> false, () -> open(Panel.TX_FREQ));
		toggle(18, 50, 0, 126, () -> !repeater().getCallsign().isEmpty(), () -> open(Panel.CALLSIGN));
		toggle(41, 50, 0, 168, () -> repeater().getRxTone() > 0, () -> open(Panel.RX_TONE));
		toggle(64, 50, 40, 0, () -> repeater().getTxTone() > 0, () -> open(Panel.TX_TONE));
		toggle(18, 71, 40, 42, repeater()::isMicEnabled, () -> send("mic", !repeater().isMicEnabled()));
		toggle(41, 71, 40, 84, repeater()::isTxEnabled, () -> send("tx", !repeater().isTxEnabled()));
		toggle(64, 71, 40, 126, repeater()::isSpeakerEnabled, () -> send("speaker", !repeater().isSpeakerEnabled()));

		hangDial = addRenderableWidget(new Dial(leftPos + 21, topPos + 93, 15, 17, 44, 171, WIDGETS_TEXTURE, 256, 256,
				d -> sendInt("hang", stepIn(HANG_STEPS, repeater().getHangTicks(), 1)), d -> sendInt("hang", stepIn(HANG_STEPS, repeater().getHangTicks(), -1))));
		timeoutDial = addRenderableWidget(new Dial(leftPos + 66, topPos + 93, 15, 17, 44, 171, WIDGETS_TEXTURE, 256, 256,
				d -> sendInt("timeout", stepIn(TIMEOUT_STEPS, repeater().getTimeoutTicks(), 1)), d -> sendInt("timeout", stepIn(TIMEOUT_STEPS, repeater().getTimeoutTicks(), -1))));
		rxDial = addRenderableWidget(new Dial(leftPos + 167, topPos + 58, 15, 17, 44, 171, WIDGETS_TEXTURE, 256, 256,
				d -> stepFrequency("rxFrequency", repeater().getRxFrequency(), 1), d -> stepFrequency("rxFrequency", repeater().getRxFrequency(), -1)));
		txDial = addRenderableWidget(new Dial(leftPos + 206, topPos + 58, 15, 17, 44, 171, WIDGETS_TEXTURE, 256, 256,
				d -> stepFrequency("txFrequency", repeater().getTxFrequency(), 1), d -> stepFrequency("txFrequency", repeater().getTxFrequency(), -1)));
	}

	private void toggle(int x, int y, int u, int v, Supplier<Boolean> state, Runnable onPress) {
		ToggleButton button = addRenderableWidget(new ToggleButton(state.get(), leftPos + x, topPos + y, 20, 21, u, v, WIDGETS_TEXTURE, 256, 256, b -> onPress.run()));
		toggles.add(button);
		toggleStates.add(state);
	}

	private void open(Panel which) {
		panel = which;
		pendingTone = which == Panel.RX_TONE ? repeater().getRxTone() : repeater().getTxTone();
		pendingIdInterval = repeater().getIdIntervalTicks();
		rebuildWidgets();
	}

	private void close() {
		panel = Panel.NONE;
		rebuildWidgets();
	}

	/** The next value along {@code steps} from the one nearest {@code current}, stopping at both ends. */
	private static int stepIn(int[] steps, int current, int direction) {
		int nearest = 0;
		for(int i = 1; i < steps.length; i++)
			if(Math.abs(steps[i] - current) < Math.abs(steps[nearest] - current))
				nearest = i;
		return steps[Math.max(0, Math.min(steps.length - 1, nearest + direction))];
	}

	private void stepFrequency(String key, float current, int direction) {
		Band band = repeater().getBand();
		if(band == null)
			return;
		float next = current + direction * (float)repeater().getFrequencyStep();
		sendFloat(key, Math.max(band.minFrequency(), Math.min(band.maxFrequency(), next)));
	}

	private void send(String key, boolean value) {
		CompoundTag tag = new CompoundTag();
		tag.putBoolean(key, value);
		send(tag);
	}

	private void sendInt(String key, int value) {
		CompoundTag tag = new CompoundTag();
		tag.putInt(key, value);
		send(tag);
	}

	private void sendFloat(String key, float value) {
		CompoundTag tag = new CompoundTag();
		tag.putFloat(key, value);
		send(tag);
	}

	private void send(CompoundTag tag) {
		SRepeaterSettingsPacket.send(repeater().getBlockPos(), tag);
		repeater().applySettings(tag.copy()); // Optimistic; the server re-syncs.
	}

	// -------------------- SETTINGS PANEL --------------------

	private static final int PANEL_X = 40;
	private static final int PANEL_Y = 22;
	private static final int PANEL_W = 176;
	private static final int PANEL_H = 104;

	private void initPanel() {
		int x = leftPos + PANEL_X + 8;
		int y = topPos + PANEL_Y + 24;
		int w = PANEL_W - 16;
		switch(panel) {
			case RX_FREQ, TX_FREQ -> {
				float current = panel == Panel.RX_FREQ ? repeater().getRxFrequency() : repeater().getTxFrequency();
				editBox = addRenderableWidget(new EditBox(font, x, y, w, 16, Component.translatable("screen.radiocraft.repeater.frequency")));
				editBox.setMaxLength(8);
				editBox.setValue(formatMHz(current));
				editBox.setFilter(s -> s.matches("[0-9]{0,3}(\\.[0-9]{0,3})?"));
				setInitialFocus(editBox);
				if(panel == Panel.RX_FREQ) { // The usual split: input 600 kHz below or above the output.
					addRenderableWidget(Button.builder(Component.literal("OUT -600"), b -> editBox.setValue(formatMHz(repeater().getTxFrequency() - Ctcss.REPEATER_OFFSET_HZ)))
							.bounds(x, y + 20, w / 2 - 2, 16).build());
					addRenderableWidget(Button.builder(Component.literal("OUT +600"), b -> editBox.setValue(formatMHz(repeater().getTxFrequency() + Ctcss.REPEATER_OFFSET_HZ)))
							.bounds(x + w / 2 + 2, y + 20, w / 2 - 2, 16).build());
				}
			}
			case CALLSIGN -> {
				editBox = addRenderableWidget(new EditBox(font, x, y, w, 16, Component.translatable("screen.radiocraft.repeater.callsign")));
				editBox.setMaxLength(10);
				editBox.setValue(repeater().getCallsign());
				editBox.setFilter(s -> s.matches("[A-Za-z0-9/]*"));
				setInitialFocus(editBox);
				addRenderableWidget(Button.builder(idIntervalLabel(), b -> {
					pendingIdInterval = ID_STEPS[(indexOf(ID_STEPS, pendingIdInterval) + 1) % ID_STEPS.length];
					b.setMessage(idIntervalLabel());
				}).bounds(x, y + 20, w, 16).build());
			}
			case RX_TONE, TX_TONE -> {
				addRenderableWidget(Button.builder(Component.literal("<"), b -> pendingTone = Ctcss.step(pendingTone, -1)).bounds(x, y + 8, 20, 16).build());
				addRenderableWidget(Button.builder(Component.literal(">"), b -> pendingTone = Ctcss.step(pendingTone, 1)).bounds(x + w - 20, y + 8, 20, 16).build());
			}
			default -> {}
		}
		addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> applyPanel()).bounds(x, topPos + PANEL_Y + PANEL_H - 22, w / 2 - 2, 16).build());
		addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> close()).bounds(x + w / 2 + 2, topPos + PANEL_Y + PANEL_H - 22, w / 2 - 2, 16).build());
	}

	private Component idIntervalLabel() {
		return pendingIdInterval == 0
				? Component.translatable("screen.radiocraft.repeater.id_off")
				: Component.translatable("screen.radiocraft.repeater.id_every", pendingIdInterval / 1200);
	}

	private static int indexOf(int[] values, int value) {
		for(int i = 0; i < values.length; i++)
			if(values[i] == value)
				return i;
		return 0;
	}

	private void applyPanel() {
		CompoundTag tag = new CompoundTag();
		switch(panel) {
			case RX_FREQ, TX_FREQ -> {
				try {
					float hz = Float.parseFloat(editBox.getValue()) * 1_000_000.0F;
					tag.putFloat(panel == Panel.RX_FREQ ? "rxFrequency" : "txFrequency", hz);
				}
				catch(NumberFormatException e) {
					return; // Leave the panel open to fix it.
				}
			}
			case CALLSIGN -> {
				tag.putString("callsign", editBox.getValue());
				tag.putInt("idInterval", pendingIdInterval);
			}
			case RX_TONE -> tag.putFloat("rxTone", pendingTone);
			case TX_TONE -> tag.putFloat("txTone", pendingTone);
			default -> {}
		}
		send(tag);
		close();
	}

	private Component panelTitle() {
		return Component.translatable(switch(panel) {
			case RX_FREQ -> "screen.radiocraft.repeater.rx_frequency";
			case TX_FREQ -> "screen.radiocraft.repeater.tx_frequency";
			case CALLSIGN -> "screen.radiocraft.repeater.callsign";
			case RX_TONE -> "screen.radiocraft.repeater.rx_tone";
			case TX_TONE -> "screen.radiocraft.repeater.tx_tone";
			default -> "";
		});
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if(panel != Panel.NONE) {
			if(keyCode == GLFW.GLFW_KEY_ESCAPE) {
				close(); // Escape leaves the panel, not the repeater.
				return true;
			}
			if(keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
				applyPanel();
				return true;
			}
			if(editBox != null && editBox.isFocused()) // Typing 'e' in the callsign must not close the screen.
				return editBox.keyPressed(keyCode, scanCode, modifiers) || editBox.canConsumeInput();
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	// -------------------- RENDERING --------------------

	@Override
	public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		for(int i = 0; i < toggles.size(); i++) // Buttons show the repeater's state, whatever was clicked.
			toggles.get(i).isToggled = toggleStates.get(i).get();
		super.render(guiGraphics, mouseX, mouseY, partialTick);
		if(panel == Panel.NONE)
			renderFaceTooltips(guiGraphics, mouseX, mouseY);
		renderTooltip(guiGraphics, mouseX, mouseY);
	}

	@Override
	protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
		guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
		if(panel != Panel.NONE) { // Under the panel's own widgets, which render after this.
			int x = leftPos + PANEL_X;
			int y = topPos + PANEL_Y;
			guiGraphics.fill(x - 1, y - 1, x + PANEL_W + 1, y + PANEL_H + 1, 0xFF6B4A35);
			guiGraphics.fill(x, y, x + PANEL_W, y + PANEL_H, 0xF0201810);
		}
	}

	@Override
	protected void renderLabels(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
		if(panel != Panel.NONE) {
			guiGraphics.drawCenteredString(font, panelTitle(), PANEL_X + PANEL_W / 2, PANEL_Y + 8, 0xFFE0C080);
			if(panel == Panel.RX_TONE || panel == Panel.TX_TONE)
				guiGraphics.drawCenteredString(font, Ctcss.format(pendingTone) + (pendingTone > 0 ? " Hz" : ""), PANEL_X + PANEL_W / 2, PANEL_Y + 36, LIT);
			return;
		}

		int phase = menu.getDisplayPhase();
		boolean on = phase == RadioNetworkObject.PHASE_ON;
		if(on) {
			drawScaled(guiGraphics, formatMHz(repeater().getRxFrequency()), 173.5F, 48.5F, 0.6F, LIT);
			drawScaled(guiGraphics, formatMHz(repeater().getTxFrequency()), 213.5F, 48.5F, 0.6F, LIT);
		}
		// Status windows: power, duplexer, input, transmitter.
		drawScaled(guiGraphics, phase == RadioNetworkObject.PHASE_PWR ? "PWR" : on ? "ON" : "OFF", 112.5F, 104.0F, 0.75F, phase == RadioNetworkObject.PHASE_PWR ? RED : on ? LIT : DIM);
		boolean duplexer = menu.getData(VHFRepeaterBlockEntity.DATA_DUPLEXER) == 1;
		boolean antenna = menu.getData(VHFRepeaterBlockEntity.DATA_ANTENNA) == 1;
		String dupText = duplexer ? "DUP" : antenna ? "NO DUP" : "NO ANT";
		drawScaled(guiGraphics, dupText, 150.5F, 104.0F, 0.6F, !on ? DIM : duplexer && antenna ? LIT : RED);
		int input = menu.getData(VHFRepeaterBlockEntity.DATA_INPUT);
		drawScaled(guiGraphics, input == 2 ? "TOT" : "RX", 187.0F, 104.0F, 0.75F, input == 0 ? DIM : input == 2 ? RED : LIT);
		drawScaled(guiGraphics, "TX", 219.0F, 104.0F, 0.75F, menu.getData(VHFRepeaterBlockEntity.DATA_TRANSMIT) == 1 ? AMBER : DIM);
	}

	private void renderFaceTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
		Component tip = null;
		if(hangDial != null && hangDial.isHoveredOrChanging())
			tip = Component.translatable("screen.radiocraft.repeater.hang", String.format(Locale.ROOT, "%.1f", repeater().getHangTicks() / 20.0F));
		else if(timeoutDial != null && timeoutDial.isHoveredOrChanging())
			tip = repeater().getTimeoutTicks() == 0
					? Component.translatable("screen.radiocraft.repeater.timeout_off")
					: Component.translatable("screen.radiocraft.repeater.timeout", formatDuration(repeater().getTimeoutTicks()));
		else if(rxDial != null && rxDial.isHoveredOrChanging())
			tip = Component.translatable("screen.radiocraft.repeater.rx_frequency");
		else if(txDial != null && txDial.isHoveredOrChanging())
			tip = Component.translatable("screen.radiocraft.repeater.tx_frequency");
		else {
			String[] keys = { "power", "prt_rx", "prt_tx", "cw_id", "ctc_rx", "ctc_tx", "mic", "tx", "speaker" };
			for(int i = 0; i < toggles.size() && i < keys.length; i++)
				if(toggles.get(i).isHovered())
					tip = buttonTip(keys[i]);
		}
		if(tip != null)
			guiGraphics.renderTooltip(font, tip, mouseX, mouseY);
	}

	private Component buttonTip(String key) {
		VHFRepeaterBlockEntity r = repeater();
		return switch(key) {
			case "prt_rx" -> Component.translatable("screen.radiocraft.repeater.tip.prt_rx", formatMHz(r.getRxFrequency()));
			case "prt_tx" -> Component.translatable("screen.radiocraft.repeater.tip.prt_tx", formatMHz(r.getTxFrequency()));
			case "cw_id" -> Component.translatable("screen.radiocraft.repeater.tip.cw_id", r.getCallsign().isEmpty() ? "-" : r.getCallsign());
			case "ctc_rx" -> Component.translatable("screen.radiocraft.repeater.tip.ctc_rx", Ctcss.format(r.getRxTone()));
			case "ctc_tx" -> Component.translatable("screen.radiocraft.repeater.tip.ctc_tx", Ctcss.format(r.getTxTone()));
			default -> Component.translatable("screen.radiocraft.repeater.tip." + key);
		};
	}

	private static String formatDuration(int ticks) {
		int seconds = ticks / 20;
		return seconds < 60 ? seconds + " s" : seconds / 60 + " min";
	}

	private static String formatMHz(float hz) {
		return String.format(Locale.ROOT, "%.3f", hz / 1_000_000.0F);
	}

	private void drawScaled(GuiGraphics guiGraphics, String text, float cx, float cy, float scale, int colour) {
		guiGraphics.pose().pushPose();
		guiGraphics.pose().translate(cx, cy, 0.0F);
		guiGraphics.pose().scale(scale, scale, 1.0F);
		guiGraphics.drawString(font, text, -font.width(text) / 2, -font.lineHeight / 2, colour, false);
		guiGraphics.pose().popPose();
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		boolean value = super.mouseReleased(mouseX, mouseY, button);
		for(var listener : children()) // Dials need to hear about a release that is not over them.
			if(listener instanceof Dial && !listener.isMouseOver(mouseX, mouseY))
				listener.mouseReleased(mouseX, mouseY, button);
		return value;
	}
}
