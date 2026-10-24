package com.arrl.radiocraft.client.screens;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.client.screens.widgets.ScrewButton;
import com.arrl.radiocraft.common.menus.DuplexerMenu;
import com.arrl.radiocraft.common.network.serverbound.SDuplexerScrewPacket;
import com.arrl.radiocraft.common.radio.DuplexerMath;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * The duplexer's six cavity screws: the three on the left tune the transmit cavities, the three on the right the
 * receive cavities. Dragging a screw (or scrolling over it) moves its cavity {@link DuplexerMath#SCREW_STEP_HZ} per
 * {@link ScrewButton#DEGREES_PER_STEP}, and its frequency is written above it.
 */
public class DuplexerScreen extends AbstractContainerScreen<DuplexerMenu> {

	private static final ResourceLocation TEXTURE = Radiocraft.id("textures/gui/duplexer.png");

	/** Each screw's left edge (texture border included), its top and size. */
	private static final int[] SCREW_X = {9, 51, 94, 137, 179, 221};
	private static final int SCREW_Y = 32;
	private static final int SCREW_SIZE = 19;
	private static final int LABEL_Y = 22;
	private static final float LABEL_SCALE = 0.75F;
	private static final int LABEL_COLOR = 0xFFE8D8A0;
	private static final int SIDE_LABEL_Y = 64;
	private static final int SIDE_LABEL_COLOR = 0xFF9A9A9A;

	private final ScrewButton[] screws = new ScrewButton[DuplexerMath.SCREWS];
	/** A screw turned here and not yet confirmed by the server, 0 for none: shown instead of the synced frequency. */
	private final int[] pending = new int[DuplexerMath.SCREWS];

	public DuplexerScreen(DuplexerMenu menu, Inventory playerInventory, Component title) {
		super(menu, playerInventory, title);
		this.imageWidth = 250;
		this.imageHeight = 76;
	}

	@Override
	protected void init() {
		super.init();

		// Screw widgets: texture normal at (2,78), hover at (2+width, 78); the size includes the 1 px border.
		for(int i = 0; i < DuplexerMath.SCREWS; i++) {
			int screw = i;
			screws[i] = addRenderableWidget(new ScrewButton(leftPos + SCREW_X[i], topPos + SCREW_Y, SCREW_SIZE, SCREW_SIZE, 2, 78, TEXTURE, 250, 120)
					.onTurn(steps -> turn(screw, steps)));
		}
	}

	/** The frequency a screw shows: what the player just turned it to, or the server's; 0 while untuned. */
	private int screwHz(int index) {
		if(pending[index] > 0 && pending[index] == menu.getScrewHz(index))
			pending[index] = 0;
		return pending[index] > 0 ? pending[index] : menu.getScrewHz(index);
	}

	private void turn(int index, int steps) {
		int hz = screwHz(index);
		if(hz <= 0)
			return; // Never tuned: nothing to turn from until it meets its repeater.
		int next = hz + steps * DuplexerMath.SCREW_STEP_HZ;
		pending[index] = next;
		SDuplexerScrewPacket.send(menu.blockEntity.getBlockPos(), index, next);
	}

	@Override
	public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		for(int i = 0; i < DuplexerMath.SCREWS; i++) // The slot faces the frequency: one step is a fixed turn.
			screws[i].setRotation((screwHz(i) / (float)DuplexerMath.SCREW_STEP_HZ) * ScrewButton.DEGREES_PER_STEP);
		super.render(guiGraphics, mouseX, mouseY, partialTick);
		renderTooltip(guiGraphics, mouseX, mouseY);
	}

	@Override
	protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.setShaderTexture(0, TEXTURE);

		int edgeSpacingX = (this.width - this.imageWidth) / 2;
		int edgeSpacingY = (this.height - this.imageHeight) / 2;
		guiGraphics.blit(TEXTURE, edgeSpacingX, edgeSpacingY, 0, 0, this.imageWidth, this.imageHeight, 250, 120);
	}

	/** Each screw's frequency above it ("---.---" until tuned), and which side is which under each group. */
	@Override
	protected void renderLabels(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
		for(int i = 0; i < DuplexerMath.SCREWS; i++) {
			int hz = screwHz(i);
			String text = hz <= 0 ? "---.---" : String.format(Locale.ROOT, "%.3f", hz / 1_000_000.0D);
			float centre = SCREW_X[i] + SCREW_SIZE / 2.0F;
			guiGraphics.pose().pushPose();
			guiGraphics.pose().translate(centre - font.width(text) * LABEL_SCALE / 2.0F, LABEL_Y, 0.0F);
			guiGraphics.pose().scale(LABEL_SCALE, LABEL_SCALE, 1.0F);
			guiGraphics.drawString(font, text, 0, 0, LABEL_COLOR, true);
			guiGraphics.pose().popPose();
		}
		sideLabel(guiGraphics, "TX", DuplexerMath.TX_FIRST);
		sideLabel(guiGraphics, "RX", DuplexerMath.RX_FIRST);
	}

	private void sideLabel(GuiGraphics guiGraphics, String text, int first) {
		float centre = (SCREW_X[first] + SCREW_X[first + 2] + SCREW_SIZE) / 2.0F;
		guiGraphics.drawString(font, text, Math.round(centre - font.width(text) / 2.0F), SIDE_LABEL_Y, SIDE_LABEL_COLOR, false);
	}
}
