package com.arrl.radiocraft.client.screens;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.client.screens.widgets.HoldButton;
import com.arrl.radiocraft.common.blockentities.TeletypeBlockEntity;
import com.arrl.radiocraft.common.menus.TeletypeMenu;
import com.arrl.radiocraft.common.network.serverbound.SStationPacket;
import com.arrl.radiocraft.common.radio.data.TncPacket;
import com.arrl.radiocraft.common.radio.voice.Rtty;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * The teletype's screen (to be redesigned; for now the old digital interface's RTTY tab): the paper roll, newest line
 * at the bottom, and the line being typed under it. Every key goes straight out on the air, upper case; Enter or SEND
 * ends the line. The lamps top right: SOLAR WX (blue), claim your callsign (green), paper out (red). The callsign is
 * shown across the top; click it to change it.
 */
public class TeletypeScreen extends StationPanelScreen<TeletypeMenu> {

	private static final ResourceLocation TEXTURE = Radiocraft.id("textures/gui/digital_interface_rtty_send.png");
	private static final ResourceLocation WIDGETS_TEXTURE = Radiocraft.id("textures/gui/digital_interface_widgets.png");

	private static final int INK = 0xFF2B2118; // On the paper.
	private static final int LCD = 0xFFBFE8C4; // On the grey screens.
	private static final int PAPER_OUT = 0xFFB02020;
	private static final int LAMP_RED = 0xC0FF3030;

	// Regions of the texture, relative to the screen's corner: {x, y, width, height}.
	private static final int[] BLUE_LAMP = {161, 17, 11, 11};
	private static final int[] GREEN_LAMP = {174, 17, 12, 11};
	private static final int[] RED_LAMP = {189, 17, 12, 11};
	private static final int[] CALLSIGN = {16, 17, 141, 14};
	private static final int[] PAPER = {25, 45, 171, 55};
	private static final int[] INPUT = {17, 114, 154, 11};
	/** Tops of the bands between the ruled lines of the paper; a line of text per band. */
	private static final int[] BANDS = {46, 55, 64, 73, 82, 91};
	private static final int BAND_HEIGHT = 8;
	private static final float PAPER_SCALE = 0.75F;
	private static final float SMALL = 0.5F;
	private static final int PANEL_HEIGHT = 133;

	private String typed = ""; // This line as typed so far; it is already on the air, so there is no rubbing out.
	private int scroll = 0; // Lines scrolled back from the bottom of the roll.
	private boolean editingCallsign = false;
	private EditBox callsignBox;

	public TeletypeScreen(TeletypeMenu menu, Inventory playerInventory, Component title) {
		super(menu, playerInventory, title);
		this.imageWidth = 217;
		this.imageHeight = PANEL_HEIGHT;
	}

	private TeletypeBlockEntity teletype() {
		return menu.blockEntity;
	}

	@Override
	protected void init() {
		super.init();
		callsignBox = null;
		addRenderableWidget(new HoldButton(leftPos + 174, topPos + 113, 28, 14, 72, 28, WIDGETS_TEXTURE, 217, 80, btn -> endLine(), btn -> {}));
		if(editingCallsign) {
			callsignBox = new EditBox(font, leftPos + CALLSIGN[0] + 2, topPos + CALLSIGN[1] + 1, 70, 12, Component.empty());
			callsignBox.setMaxLength(TncPacket.CALLSIGN_MAX);
			callsignBox.setValue(teletype().getCallsign());
			addRenderableWidget(callsignBox);
			setInitialFocus(callsignBox);
		}
	}

	// -------------------- INPUT --------------------

	@Override
	protected boolean panelClicked(double mouseX, double mouseY, int button) {
		if(button != 0)
			return false;
		if(inside(BLUE_LAMP, mouseX, mouseY)) {
			SStationPacket.send(teletype().getBlockPos(), "solar");
			scroll = 0;
			return true;
		}
		if(inside(GREEN_LAMP, mouseX, mouseY)) {
			sendCallsign("");
			return true;
		}
		if(!editingCallsign && inside(CALLSIGN, mouseX, mouseY)) {
			editingCallsign = true;
			rebuildWidgets();
			return true;
		}
		return false;
	}

	@Override
	protected boolean panelScrolled(double mouseX, double mouseY, double scrollY) {
		if(!inside(PAPER, mouseX, mouseY))
			return false;
		scroll = Math.max(0, scroll + (scrollY > 0 ? 1 : -1)); // Up the roll; clamped when drawn.
		return true;
	}

	private void sendCallsign(String value) {
		CompoundTag tag = new CompoundTag();
		tag.putString("callsign", value);
		SStationPacket.send(teletype().getBlockPos(), "callsign", tag);
	}

	private void finishEditing(boolean apply) {
		if(apply && callsignBox != null)
			sendCallsign(callsignBox.getValue());
		editingCallsign = false;
		rebuildWidgets();
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if(editingCallsign) {
			if(keyCode == GLFW.GLFW_KEY_ESCAPE) {
				finishEditing(false); // Escape leaves the field, not the screen.
				return true;
			}
			if(keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
				finishEditing(true);
				return true;
			}
			return callsignBox == null || callsignBox.keyPressed(keyCode, scanCode, modifiers) || callsignBox.canConsumeInput();
		}
		if(keyCode == GLFW.GLFW_KEY_ESCAPE)
			return super.keyPressed(keyCode, scanCode, modifiers);
		if(keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)
			endLine();
		return true; // The keyboard is the teleprinter's: no closing on 'e', no hotbar keys. Characters come in charTyped.
	}

	/** Every printable key goes straight out on the air, upper case, as typed. */
	@Override
	public boolean charTyped(char codePoint, int modifiers) {
		if(editingCallsign)
			return super.charTyped(codePoint, modifiers);
		String c = Rtty.sanitize(String.valueOf(codePoint), 1);
		if(!c.isEmpty() && c.charAt(0) != '\n') {
			send(c);
			typed = (typed + c).length() > 400 ? (typed + c).substring(200) : typed + c;
		}
		return true;
	}

	private void send(String text) {
		CompoundTag tag = new CompoundTag();
		tag.putString("text", text);
		SStationPacket.send(teletype().getBlockPos(), "rtty", tag);
		scroll = 0; // Back to the bottom of the roll to watch it print.
	}

	/** SEND or Enter: carriage return and line feed, the line is done. */
	private void endLine() {
		if(!typed.isEmpty())
			send("\n");
		typed = "";
	}

	// -------------------- RENDERING --------------------

	@Override
	protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
		guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, PANEL_HEIGHT, 217, 133);
		fillAt(guiGraphics, CALLSIGN, leftPos, topPos, 0xFF2A3034); // Over the old tab row: the callsign plate.
		if(teletype().getPaperLines() == 0 && (System.currentTimeMillis() / 500) % 2 == 0)
			fillAt(guiGraphics, RED_LAMP, leftPos, topPos, LAMP_RED);
	}

	@Override
	protected void renderLabels(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
		TeletypeBlockEntity teletype = teletype();
		if(editingCallsign)
			text(guiGraphics, Component.translatable("screen.radiocraft.teletype.edit_callsign").getString(), CALLSIGN[0] + 76, CALLSIGN[1] + 5, SMALL, LCD, false);
		else
			text(guiGraphics, "DE " + (teletype.getCallsign().isEmpty() ? "------" : teletype.getCallsign()), CALLSIGN[0] + 4, CALLSIGN[1] + 3, 1.0F, LCD, false);

		boolean blink = (System.currentTimeMillis() / 400) % 2 == 0;
		List<FormattedCharSequence> lines = new ArrayList<>();
		int wrap = (int)(PAPER[2] / PAPER_SCALE);
		for(String line : teletype.getRttyLines())
			lines.addAll(font.split(Component.literal(line), wrap));
		String open = teletype.getRttyOpenLine();
		if(open != null)
			lines.addAll(font.split(Component.literal(open + (blink ? "_" : " ")), wrap));
		int rows = BANDS.length; // Newest line on the bottom band, as a teleprinter feeds paper up.
		scroll = Mth.clamp(scroll, 0, Math.max(0, lines.size() - rows));
		int end = lines.size() - scroll;
		int start = Math.max(0, end - rows);
		for(int i = start; i < end; i++)
			onBand(guiGraphics, lines.get(i), PAPER[0], BANDS[rows - (end - i)]);

		int paperLines = teletype.getPaperLines();
		String paper = paperLines > 0 ? Component.translatable("screen.radiocraft.teletype.paper", paperLines).getString()
				: Component.translatable("screen.radiocraft.teletype.paper_out").getString();
		text(guiGraphics, paper, PAPER[0] + PAPER[2] - font.width(paper) * SMALL, PAPER[1] - 2, SMALL, paperLines > 0 ? INK : PAPER_OUT, false);

		String shown = font.plainSubstrByWidth(new StringBuilder(typed + (blink ? "_" : "")).reverse().toString(), (int)((INPUT[2] - 4) / 0.75F));
		text(guiGraphics, new StringBuilder(shown).reverse().toString(), INPUT[0] + 2, INPUT[1] + 2.5F, 0.75F, LCD, false);
	}

	/** A line of text on a paper band whose top row is {@code top}, centred in the band's height. */
	private void onBand(GuiGraphics guiGraphics, FormattedCharSequence line, float x, int top) {
		guiGraphics.pose().pushPose();
		guiGraphics.pose().translate(x, top + (BAND_HEIGHT - 7.0F * PAPER_SCALE) / 2.0F, 0.0F);
		guiGraphics.pose().scale(PAPER_SCALE, PAPER_SCALE, 1.0F);
		guiGraphics.drawString(font, line, 0, 0, INK, false);
		guiGraphics.pose().popPose();
	}

	@Override
	protected void renderPanelTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
		List<Component> tip = new ArrayList<>();
		if(inside(BLUE_LAMP, mouseX, mouseY))
			tip.add(Component.translatable("screen.radiocraft.teletype.tip.solar"));
		else if(inside(GREEN_LAMP, mouseX, mouseY) || (!editingCallsign && inside(CALLSIGN, mouseX, mouseY))) {
			tip.add(Component.translatable("screen.radiocraft.teletype.tip.callsign"));
			if(!teletype().getCallsignSetBy().isEmpty())
				tip.add(Component.translatable("screen.radiocraft.teletype.tip.set_by", teletype().getCallsign(), teletype().getCallsignSetBy()));
		}
		else if(inside(RED_LAMP, mouseX, mouseY) && teletype().getPaperLines() == 0)
			tip.add(Component.translatable("screen.radiocraft.teletype.tip.paper_out"));
		if(!tip.isEmpty())
			guiGraphics.renderComponentTooltip(font, tip, mouseX, mouseY);
	}
}
