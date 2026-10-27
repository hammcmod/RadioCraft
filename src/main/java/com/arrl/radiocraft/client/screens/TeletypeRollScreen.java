package com.arrl.radiocraft.client.screens;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.blockentities.TeletypeBlockEntity;
import com.arrl.radiocraft.common.datacomponents.TeletypeRollContents;
import com.arrl.radiocraft.common.items.TeletypeRollItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * Reading a roll torn off a teletype ({@link TeletypeRollItem}): the strip of paper as the machine printed it, drawn
 * with the teletype screen's paper art, scrolled with the wheel or the keys (Page Up and Down, Home, End).
 */
public class TeletypeRollScreen extends Screen {

	private static final ResourceLocation PAPER = Radiocraft.id("textures/gui/teletype_machine_paper.png");
	private static final int TEXTURE_SIZE = 256;
	// The paper art, as TeletypeScreen draws it.
	private static final int[] TORN_EDGE = {11, 83, 106, 4};
	private static final int CLEAN_V = 115, CREASED_V = 145, PAGE_HEIGHT = 30, CREASE_EVERY = 5;
	private static final int PAPER_WIDTH = 106;
	private static final int LINE_HEIGHT = 5;
	private static final int COLUMNS = TeletypeBlockEntity.RTTY_LINE_MAX;
	private static final int TEXT_LEFT = 11, TEXT_WIDTH = PAPER_WIDTH - 22;
	private static final float TEXT_SCALE = TEXT_WIDTH / (COLUMNS * 6.0F);
	private static final int INK = 0xFF2B2118;
	private static final int ROLL_START = 12;
	/** The strip is drawn this many times larger than the GUI scale. */
	private static final float SCALE = 2.0F;

	private final TeletypeRollContents contents;
	private float scroll = 0.0F; // Texels from the top of the roll at the top of the window.

	public TeletypeRollScreen(ItemStack stack) {
		super(Component.translatable("item.radiocraft.teletype_roll"));
		this.contents = TeletypeRollItem.contents(stack);
	}

	public static void open(ItemStack stack) {
		Minecraft.getInstance().setScreen(new TeletypeRollScreen(stack));
	}

	/** How tall the strip is, in texels: the torn edge, the paper fed before the first line, the lines and a tail. */
	private int stripHeight() {
		return TORN_EDGE[3] + ROLL_START + (Math.max(contents.fedLines(), contents.lines().size()) + 2) * LINE_HEIGHT;
	}

	private int viewHeight() {
		return (int)(height / SCALE);
	}

	private void clampScroll() {
		scroll = Mth.clamp(scroll, 0.0F, Math.max(0.0F, stripHeight() - viewHeight() + 8));
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		scroll -= (float)scrollY * LINE_HEIGHT * 2;
		clampScroll();
		return true;
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		switch(keyCode) {
			case GLFW.GLFW_KEY_PAGE_UP -> scroll -= viewHeight() - LINE_HEIGHT * 2;
			case GLFW.GLFW_KEY_PAGE_DOWN -> scroll += viewHeight() - LINE_HEIGHT * 2;
			case GLFW.GLFW_KEY_HOME -> scroll = 0.0F;
			case GLFW.GLFW_KEY_END -> scroll = Float.MAX_VALUE;
			case GLFW.GLFW_KEY_UP -> scroll -= LINE_HEIGHT;
			case GLFW.GLFW_KEY_DOWN -> scroll += LINE_HEIGHT;
			default -> {
				return super.keyPressed(keyCode, scanCode, modifiers);
			}
		}
		clampScroll();
		return true;
	}

	@Override
	protected void init() {
		super.init();
		scroll = Float.MAX_VALUE; // Opens at the end, where the last lines are.
		clampScroll();
	}

	@Override
	public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		super.render(guiGraphics, mouseX, mouseY, partialTick);
		guiGraphics.pose().pushPose();
		guiGraphics.pose().scale(SCALE, SCALE, 1.0F);
		int left = (int)((width / SCALE - PAPER_WIDTH) / 2);
		int top = (int)(8 - scroll);
		int bottom = viewHeight();
		guiGraphics.blit(PAPER, left, top, TORN_EDGE[0], TORN_EDGE[1], TORN_EDGE[2], TORN_EDGE[3], TEXTURE_SIZE, TEXTURE_SIZE);
		int pages = (stripHeight() - TORN_EDGE[3] + PAGE_HEIGHT - 1) / PAGE_HEIGHT;
		for(int page = 0; page < pages; page++) {
			int pageY = top + TORN_EDGE[3] + page * PAGE_HEIGHT;
			if(pageY + PAGE_HEIGHT < 0 || pageY > bottom)
				continue;
			int v = page % CREASE_EVERY == CREASE_EVERY - 2 ? CREASED_V : CLEAN_V;
			guiGraphics.blit(PAPER, left, pageY, TORN_EDGE[0], v, PAPER_WIDTH, PAGE_HEIGHT, TEXTURE_SIZE, TEXTURE_SIZE);
		}
		// The lines, oldest at the top, under the paper fed before them.
		List<String> lines = contents.lines();
		int fed = Math.max(contents.fedLines(), lines.size());
		float firstBottom = top + TORN_EDGE[3] + ROLL_START + (fed - lines.size() + 1) * LINE_HEIGHT;
		Matrix4f pose = new Matrix4f(guiGraphics.pose().last().pose()).translate(left + TEXT_LEFT, 0.0F, 0.0F).scale(TEXT_SCALE, TEXT_SCALE, 1.0F);
		for(int i = 0; i < lines.size(); i++) {
			float lineBottom = firstBottom + i * LINE_HEIGHT;
			if(lineBottom < 0 || lineBottom - LINE_HEIGHT > bottom)
				continue;
			drawMono(lines.get(i), (lineBottom - LINE_HEIGHT + 1.0F) / TEXT_SCALE, pose, guiGraphics);
		}
		guiGraphics.flush();
		guiGraphics.pose().popPose();
	}

	/** {@code text} one character to a cell, as the type bars printed it. */
	private void drawMono(String text, float y, Matrix4f pose, GuiGraphics guiGraphics) {
		for(int i = 0; i < text.length() && i < COLUMNS; i++) {
			String c = String.valueOf(text.charAt(i));
			if(c.equals(" "))
				continue;
			font.drawInBatch(c, i * 6.0F + (6 - font.width(c) + 1) / 2.0F, y, INK, false, pose, guiGraphics.bufferSource(), Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
