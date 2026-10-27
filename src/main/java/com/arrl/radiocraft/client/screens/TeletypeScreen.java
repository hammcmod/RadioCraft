package com.arrl.radiocraft.client.screens;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.blockentities.TeletypeBlockEntity;
import com.arrl.radiocraft.common.menus.TeletypeMenu;
import com.arrl.radiocraft.common.network.serverbound.SStationPacket;
import com.arrl.radiocraft.common.radio.voice.Rtty;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * The teletype's screen: the machine, with its paper feeding up out of the platen as lines print, past the paper guide,
 * up the copy holder and, once it reaches the top, over and down behind the machine. Every key goes out on the air (at
 * the machine's six or so characters a second, printing as it goes); Enter is carriage return and line feed. The mouse
 * wheel pulls the paper back down to read what scrolled away. Hovering the machine shows the paper left. The
 * machine powers up with a few relay clicks when it opens, then its motor runs while it is open. New paper (first use, or after it ran out) is fed up to the head with the ratchet
 * clicking before anything can be typed.
 */
public class TeletypeScreen extends StationPanelScreen<TeletypeMenu> {

	private static final ResourceLocation MACHINE = Radiocraft.id("textures/gui/teletype_machine.png");
	private static final ResourceLocation PAPER = Radiocraft.id("textures/gui/teletype_machine_paper.png");
	private static final int TEXTURE_SIZE = 256;
	/** The machine texture's first row drawn (above it is empty). */
	private static final int TOP = 24;

	// In teletype_machine_paper.png: {u, v, width, height}.
	private static final int[] FOLD = {11, 49, 106, 21}; // The paper going over the top of the copy holder.
	private static final int[] TORN_EDGE = {11, 83, 106, 4}; // The top of a fresh roll.
	private static final int[] GUIDE = {7, 192, 113, 26};
	/** Plain paper, and paper with a crease, each a whole number of sprocket-hole steps so they follow on. */
	private static final int CLEAN_V = 115, CREASED_V = 145, PAGE_HEIGHT = 30;
	/** Every this many pages is a creased one. */
	private static final int CREASE_EVERY = 5;

	// On the machine texture.
	private static final int PAPER_X = 74;
	private static final int PAPER_WIDTH = 106;
	private static final int GUIDE_X = PAPER_X - (TORN_EDGE[0] - GUIDE[0]), GUIDE_Y = 141;
	private static final int FOLD_Y = 27;
	/** Paper shows between these: it goes under the fold at the top and into the platen under the guide's bar. */
	private static final int CLIP_TOP = FOLD_Y + 13, CLIP_BOTTOM = 162;
	/** Where the bottom of the line being printed is. */
	private static final int HEAD_Y = 157;
	/** Paper above the first line of a fresh roll. */
	private static final int ROLL_START = 12;
	private static final int LINE_HEIGHT = 5;
	/** Characters across the paper: the printer's line ({@link TeletypeBlockEntity#RTTY_LINE_MAX}). */
	private static final int COLUMNS = TeletypeBlockEntity.RTTY_LINE_MAX;
	private static final int TEXT_LEFT = PAPER_X + 11, TEXT_WIDTH = PAPER_WIDTH - 22;
	private static final float TEXT_SCALE = TEXT_WIDTH / (COLUMNS * 6.0F);
	/** The machine's body, for its tooltip. */
	private static final int[] BODY = {0, 150, 256, 106};

	private static final int INK = 0xFF2B2118;

	private float shownFeed = -1.0F; // Paper fed, in texels, as drawn: it slides towards where the machine has it.
	private long lastFrame = 0L;
	/** Feeding new paper up: when it began, -1 if not, -2 once done (until the machine says it is threaded). */
	private long loadStart = -1L;
	private int loadSteps = 0;
	private static final int LOAD_STEP_MILLIS = 70;
	@org.jetbrains.annotations.Nullable
	private Whir whir = null;

	/** When a key was last pressed: the motor runs louder while someone types. */
	private long lastKeyMillis = 0L;

	/** The motor, running while the screen is open: twice as loud at a keypress, back down over five seconds. */
	private class Whir extends net.minecraft.client.resources.sounds.AbstractTickableSoundInstance {

		private static final float QUIET = 0.15F;
		private static final float FADE_SECONDS = 5.0F;

		Whir() {
			super(com.arrl.radiocraft.common.init.RadiocraftSoundEvents.TELETYPE_BACKGROUND.get(), net.minecraft.sounds.SoundSource.BLOCKS, net.minecraft.util.RandomSource.create());
			looping = true;
			delay = 0;
			volume = QUIET;
			pitch = 1.0F;
			relative = true;
			attenuation = Attenuation.NONE;
		}

		@Override
		public void tick() {
			float since = (System.currentTimeMillis() - lastKeyMillis) / 1000.0F;
			volume = QUIET * (1.0F + Math.max(0.0F, 1.0F - since / FADE_SECONDS));
		}

		void finish() {
			stop();
		}
	}

	/** When the screen opened: the machine powers up, relays clicking, before its motor runs. */
	private final long openedMillis = System.currentTimeMillis();
	/** Milliseconds after opening of each power-up click, and when the motor starts. */
	private static final long[] POWER_CLICKS = {0L, 140L, 230L, 380L, 450L};
	private static final long MOTOR_START = 600L;
	private int clicksPlayed = 0;

	/** The power-up: relay clicks, then the motor. */
	private void powerUp() {
		if(minecraft == null || whir != null)
			return;
		long since = System.currentTimeMillis() - openedMillis;
		for(; clicksPlayed < POWER_CLICKS.length && since >= POWER_CLICKS[clicksPlayed]; clicksPlayed++)
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(clicksPlayed % 2 == 0 ? SoundEvents.COMPARATOR_CLICK : SoundEvents.STONE_BUTTON_CLICK_ON,
					0.7F + clicksPlayed * 0.15F, 0.35F));
		if(since >= MOTOR_START) {
			whir = new Whir();
			minecraft.getSoundManager().play(whir);
		}
	}

	@Override
	public void removed() {
		super.removed();
		if(whir != null) {
			whir.finish();
			whir = null;
		}
	}

	/** True while new paper is being fed up: no typing until it is. */
	private boolean isLoading() {
		return loadStart >= 0L;
	}
	private int scrollLines = 0;

	public TeletypeScreen(TeletypeMenu menu, Inventory playerInventory, Component title) {
		super(menu, playerInventory, title);
		this.imageWidth = TEXTURE_SIZE;
		this.imageHeight = TEXTURE_SIZE - TOP;
	}

	private TeletypeBlockEntity teletype() {
		return menu.blockEntity;
	}

	// -------------------- INPUT --------------------

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if(keyCode == GLFW.GLFW_KEY_ESCAPE)
			return super.keyPressed(keyCode, scanCode, modifiers);
		if(isLoading())
			return true;
		switch(keyCode) { // Scrolling back through the roll.
			case GLFW.GLFW_KEY_PAGE_UP -> {
				scrollLines += 5;
				return true;
			}
			case GLFW.GLFW_KEY_PAGE_DOWN -> {
				scrollLines = Math.max(0, scrollLines - 5);
				return true;
			}
			case GLFW.GLFW_KEY_HOME -> {
				scrollLines = Integer.MAX_VALUE / 2; // Clamped to the top of the roll when drawn.
				return true;
			}
			case GLFW.GLFW_KEY_END -> {
				scrollLines = 0;
				return true;
			}
			default -> {}
		}
		if(keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)
			send("\n");
		return true; // The keyboard is the teleprinter's: no closing on 'e', no hotbar keys. Characters come in charTyped.
	}

	/** Every printable key goes out on the air, upper case. */
	@Override
	public boolean charTyped(char codePoint, int modifiers) {
		String c = Rtty.sanitize(String.valueOf(codePoint), 1);
		if(!isLoading() && !c.isEmpty() && c.charAt(0) != '\n')
			send(c); // The machine clacks as it prints it.
		return true;
	}

	private void send(String text) {
		lastKeyMillis = System.currentTimeMillis();
		CompoundTag tag = new CompoundTag();
		tag.putString("text", text);
		SStationPacket.send(teletype().getBlockPos(), "rtty", tag);
		scrollLines = 0; // Back to the print head to watch it go.
	}


	@Override
	protected boolean panelScrolled(double mouseX, double mouseY, double scrollY) {
		scrollLines = Math.max(0, scrollLines + (scrollY > 0 ? 1 : -1)); // Clamped when drawn.
		return true;
	}

	// -------------------- RENDERING --------------------

	/** Screen y of machine-texture row {@code y}. */
	private int y(int textureY) {
		return topPos + textureY - TOP;
	}

	@Override
	protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
		powerUp();
		guiGraphics.blit(MACHINE, leftPos, topPos, 0, TOP, imageWidth, imageHeight, TEXTURE_SIZE, TEXTURE_SIZE);
		TeletypeBlockEntity teletype = teletype();
		if(loadStart == -1L && !teletype.isPaperLoaded() && teletype.getPaperLines() > 0) {
			loadStart = System.currentTimeMillis(); // New paper: feed it up to the head.
			loadSteps = 0;
		}
		if(loadStart == -2L && teletype.isPaperLoaded())
			loadStart = -1L;
		boolean paperIn = teletype.isPaperLoaded() || loadStart != -1L;
		if(paperIn)
			renderPaper(guiGraphics, teletype);
		blitPaper(guiGraphics, GUIDE, leftPos + GUIDE_X, y(GUIDE_Y));
	}

	@Override
	protected void renderLabels(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
		// All on the paper, in renderBg.
	}

	private void blitPaper(GuiGraphics guiGraphics, int[] r, int x, int y) {
		guiGraphics.blit(PAPER, x, y, r[0], r[1], r[2], r[3], TEXTURE_SIZE, TEXTURE_SIZE);
	}

	private void renderPaper(GuiGraphics guiGraphics, TeletypeBlockEntity teletype) {
		int fed = teletype.getFedLines();
		List<String> lines = new ArrayList<>(teletype.getRttyLines());
		String open = teletype.getRttyOpenLine();
		scrollLines = Mth.clamp(scrollLines, 0, Math.max(0, Math.min(fed, lines.size())));

		// Paper fed: the head is at the bottom of row 'fed'; it slides there rather than jumping.
		float target = ROLL_START + (fed + 1) * LINE_HEIGHT - scrollLines * LINE_HEIGHT;
		long now = System.currentTimeMillis();
		float seconds = lastFrame == 0L ? 1.0F : Math.min(1.0F, (now - lastFrame) / 1000.0F);
		lastFrame = now;
		if(loadStart != -1L) { // New paper, fed up a ratchet step at a time from inside the platen.
			fed = 0;
			lines = List.of();
			open = null;
			target = ROLL_START + LINE_HEIGHT;
			float from = HEAD_Y - CLIP_BOTTOM;
			int steps = (int)Math.ceil((target - from) / 2.0F);
			if(isLoading()) {
				int step = (int)Math.min(steps, (now - loadStart) / LOAD_STEP_MILLIS);
				for(; loadSteps < step; loadSteps++)
					ratchet(loadSteps);
				shownFeed = Math.min(target, from + step * 2.0F);
				if(step >= steps && now - loadStart >= (long)(steps + 4) * LOAD_STEP_MILLIS) {
					loadStart = -2L;
					SStationPacket.send(teletype.getBlockPos(), "load");
				}
			}
			else
				shownFeed = target;
		}
		else if(shownFeed < 0.0F || target < shownFeed - LINE_HEIGHT * 3 && scrollLines == 0)
			shownFeed = target; // Opening the screen, or a fresh roll: no slide.
		else
			shownFeed += (target - shownFeed) * Math.min(1.0F, seconds * 14.0F);

		// Paper coordinate q (texels from the top of the roll) is drawn at HEAD_Y - (shownFeed - q).
		float paperTop = y(HEAD_Y) - shownFeed;
		int clipTop = y(CLIP_TOP), clipBottom = y(CLIP_BOTTOM);
		float scale = getScale();
		guiGraphics.enableScissor((int)(leftPos * scale + PAPER_X * scale), (int)(clipTop * scale), (int)((leftPos + PAPER_X + PAPER_WIDTH) * scale), (int)(clipBottom * scale));
		guiGraphics.pose().pushPose();
		guiGraphics.pose().translate(0.0F, paperTop - (float)Math.floor(paperTop), 0.0F); // The fraction of a texel the slide is at.
		int top = (int)Math.floor(paperTop);
		blitPaper(guiGraphics, TORN_EDGE, leftPos + PAPER_X, top);
		int firstPage = Math.max(0, (clipTop - top - TORN_EDGE[3]) / PAGE_HEIGHT - 1);
		for(int page = firstPage; ; page++) {
			int pageY = top + TORN_EDGE[3] + page * PAGE_HEIGHT;
			if(pageY > clipBottom)
				break;
			int v = page % CREASE_EVERY == CREASE_EVERY - 2 ? CREASED_V : CLEAN_V;
			guiGraphics.blit(PAPER, leftPos + PAPER_X, pageY, TORN_EDGE[0], v, PAPER_WIDTH, PAGE_HEIGHT, TEXTURE_SIZE, TEXTURE_SIZE);
		}

		// The text: closed lines above the head, newest lowest; the open line on the head. Lines from before this roll
		// are not on it.
		List<String> rows = new ArrayList<>();
		int closedShown = Math.min(lines.size(), fed);
		rows.addAll(lines.subList(lines.size() - closedShown, lines.size()));
		boolean blink = (now / 400L) % 2L == 0L;
		rows.add(open == null ? (blink ? "_" : "") : open);
		float headBottom = top + ROLL_START + (fed + 1) * LINE_HEIGHT;
		Matrix4f pose = new Matrix4f(guiGraphics.pose().last().pose()).translate(TEXT_LEFT + leftPos, 0.0F, 0.0F).scale(TEXT_SCALE, TEXT_SCALE, 1.0F);
		for(int i = 0; i < rows.size(); i++) {
			float bottom = headBottom - (rows.size() - 1 - i) * LINE_HEIGHT;
			if(bottom < clipTop || bottom - LINE_HEIGHT > clipBottom)
				continue;
			drawMono(rows.get(i), (bottom - LINE_HEIGHT + 1.0F) / TEXT_SCALE, pose, guiGraphics);
		}
		guiGraphics.flush();
		guiGraphics.pose().popPose();
		guiGraphics.disableScissor();

		if(paperTop < clipTop) // The paper has reached the top: it goes over and down the back.
			blitPaper(guiGraphics, FOLD, leftPos + PAPER_X, y(FOLD_Y));
		if(scrollLines > 0) // How far back up the roll the view is.
			text(guiGraphics, "-" + scrollLines, leftPos + PAPER_X + PAPER_WIDTH - 4, y(CLIP_TOP) + 2, 0.5F, 0xFF8A7A66, true);
	}

	/** A click of the platen's ratchet feeding new paper, the paper rustling now and then. */
	private void ratchet(int step) {
		if(minecraft == null)
			return;
		minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.COMPARATOR_CLICK, 1.6F + (step % 2) * 0.2F, 0.25F));
		if(step % 6 == 0)
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 0.8F, 0.35F));
	}

	/** {@code text} one character to a cell, as the type bars print. */
	private void drawMono(String text, float y, Matrix4f pose, GuiGraphics guiGraphics) {
		for(int i = 0; i < text.length() && i < COLUMNS; i++) {
			String c = String.valueOf(text.charAt(i));
			if(c.equals(" "))
				continue;
			font.drawInBatch(c, i * 6.0F + (6 - font.width(c) + 1) / 2.0F, y, INK, false, pose, guiGraphics.bufferSource(), Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
		}
	}

	@Override
	protected void renderPanelTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
		if(!inside(BODY, mouseX, mouseY + TOP))
			return;
		TeletypeBlockEntity teletype = teletype();
		List<Component> tip = new ArrayList<>();
		int paperLines = teletype.getPaperLines();
		tip.add(paperLines > 0 ? Component.translatable("screen.radiocraft.teletype.paper", paperLines) : Component.translatable("screen.radiocraft.teletype.paper_out"));
		if(!teletype.getRttyLines().isEmpty())
			tip.add(Component.translatable("screen.radiocraft.teletype.tip.roll").withStyle(net.minecraft.ChatFormatting.GRAY));
		guiGraphics.renderComponentTooltip(font, tip, mouseX, mouseY);
	}
}
