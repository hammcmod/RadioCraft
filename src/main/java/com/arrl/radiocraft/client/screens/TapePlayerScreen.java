package com.arrl.radiocraft.client.screens;

import com.arrl.radiocraft.common.blockentities.TapePlayerBlockEntity;
import com.arrl.radiocraft.common.items.TapeItem;
import com.arrl.radiocraft.common.menus.TapePlayerMenu;
import com.arrl.radiocraft.common.network.serverbound.SStationPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * The tape player's controls: the tape in it and where it is, PLAY, STOP, RECORD and EJECT, and when it plays by
 * itself (only when told, over and over, every so many minutes, or on a redstone pulse). Drawn plainly until it has
 * its own panel art.
 */
public class TapePlayerScreen extends AbstractContainerScreen<TapePlayerMenu> {

	private static final int WALNUT = 0xFF4A2E1C, WALNUT_DARK = 0xFF2E1C10, DECK = 0xFF8C8A84, WINDOW = 0xFF101410, AMBER = 0xFFE8B048;
	private static final String[] MODES = { "MANUAL", "LOOP", "INTERVAL", "REDSTONE" };

	private Button mode, minus, plus;

	public TapePlayerScreen(TapePlayerMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
		imageWidth = 210;
		imageHeight = 112;
	}

	@Override
	protected void init() {
		super.init();
		int x = leftPos + 10, y = topPos + 56;
		addRenderableWidget(Button.builder(Component.literal("PLAY"), b -> send("play")).bounds(x, y, 44, 18).build());
		addRenderableWidget(Button.builder(Component.literal("STOP"), b -> send("stop")).bounds(x + 48, y, 44, 18).build());
		addRenderableWidget(Button.builder(Component.literal("REC"), b -> send("record")).bounds(x + 96, y, 44, 18).build());
		addRenderableWidget(Button.builder(Component.literal("EJECT"), b -> send("eject")).bounds(x + 144, y, 46, 18).build());
		mode = addRenderableWidget(Button.builder(Component.empty(), b -> {
			CompoundTag tag = new CompoundTag();
			tag.putInt("mode", menu.get(TapePlayerBlockEntity.DATA_MODE) + 1);
			SStationPacket.send(menu.getBlockPos(), "mode", tag);
		}).bounds(x, y + 24, 92, 18).build());
		minus = addRenderableWidget(Button.builder(Component.literal("-"), b -> interval(-1)).bounds(x + 144, y + 24, 20, 18).build());
		plus = addRenderableWidget(Button.builder(Component.literal("+"), b -> interval(1)).bounds(x + 170, y + 24, 20, 18).build());
	}

	private void send(String action) {
		SStationPacket.send(menu.getBlockPos(), action);
	}

	private void interval(int step) {
		CompoundTag tag = new CompoundTag();
		int minutes = menu.get(TapePlayerBlockEntity.DATA_INTERVAL);
		tag.putInt("minutes", minutes + step * (hasShiftDown() ? 10 : 1));
		SStationPacket.send(menu.getBlockPos(), "interval", tag);
	}

	@Override
	public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		int modeIndex = Math.floorMod(menu.get(TapePlayerBlockEntity.DATA_MODE), MODES.length);
		mode.setMessage(Component.literal("MODE: " + MODES[modeIndex]));
		boolean interval = modeIndex == TapePlayerBlockEntity.Mode.INTERVAL.ordinal();
		minus.visible = plus.visible = interval;
		super.render(graphics, mouseX, mouseY, partialTick);
		renderTooltip(graphics, mouseX, mouseY);
	}

	@Override
	protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
		graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, WALNUT_DARK);
		graphics.fill(leftPos + 2, topPos + 2, leftPos + imageWidth - 2, topPos + imageHeight - 2, WALNUT);
		graphics.fill(leftPos + 8, topPos + 18, leftPos + imageWidth - 8, topPos + 50, DECK);
		graphics.fill(leftPos + 10, topPos + 20, leftPos + imageWidth - 10, topPos + 48, WINDOW);
	}

	@Override
	protected void renderLabels(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
		graphics.drawString(font, title, 8, 6, 0xFFE8DCC8, false);
		graphics.drawString(font, tapeLine(), 14, 24, AMBER, false);
		int seconds = menu.get(TapePlayerBlockEntity.DATA_SECONDS);
		String time = String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
		String state = switch(menu.get(TapePlayerBlockEntity.DATA_STATE)) {
			case TapePlayerBlockEntity.STATE_PLAYING -> "PLAYING  " + time;
			case TapePlayerBlockEntity.STATE_RECORDING -> "● REC  " + time + " / 1:00";
			case TapePlayerBlockEntity.STATE_LOADING -> "THREADING TAPE...";
			default -> "STOPPED";
		};
		graphics.drawString(font, state, 14, 36, AMBER, false);
		if(menu.get(TapePlayerBlockEntity.DATA_MODE) == TapePlayerBlockEntity.Mode.INTERVAL.ordinal())
			graphics.drawString(font, "EVERY " + menu.get(TapePlayerBlockEntity.DATA_INTERVAL) + " MIN", 108, 85, 0xFFE8DCC8, false);
	}

	private String tapeLine() {
		return switch(menu.get(TapePlayerBlockEntity.DATA_TAPE)) {
			case 0 -> "NO TAPE";
			case 1 -> "BLANK TAPE";
			case 2 -> "\"" + TapeItem.label(TapeItem.contents(menu.blockEntity.getTape()).track()).toUpperCase(Locale.ROOT) + "\"";
			default -> "RECORDED TAPE";
		};
	}
}
