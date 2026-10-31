package com.arrl.radiocraft.client.screens;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.blockentities.PabxBlockEntity;
import com.arrl.radiocraft.common.menus.PabxMenu;
import com.arrl.radiocraft.common.network.serverbound.SStationPacket;
import com.arrl.radiocraft.common.telephone.PabxNumbers;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * The phone PABX's settings (maintainer, 2026-10-31): a plain panel at the normal GUI size, so JEI sits beside it.
 * Its master number (exactly {@code pabx_master_digits} digits, or none), its first extension and its name, saved
 * together with Save (or ENTER). The line under Save says what came of it. Texture from tools/pabx_gui.py.
 */
public class PabxScreen extends AbstractContainerScreen<PabxMenu> {

	private static final ResourceLocation TEXTURE = Radiocraft.id("textures/gui/pabx.png");
	private static final int WIDTH = 176, HEIGHT = 166;
	private static final int BOX_X = 8, BOX_WIDTH = 160, MASTER_Y = 30, FIRST_Y = 62, NAME_Y = 94, SAVE_Y = 118, STATUS_Y = 144;
	private static final int INK = 0xFF404040, POWER_ON = 0xFF2E7D32, POWER_OFF = 0xFFAA2222, STATUS_INK = 0xFF303030;

	@Nullable
	private EditBox master, first, name;
	/** A problem found before sending (wrong length); null shows the server's answer instead. */
	@Nullable
	private Component localStatus = null;

	public PabxScreen(PabxMenu menu, Inventory playerInventory, Component title) {
		super(menu, playerInventory, title);
		this.imageWidth = WIDTH;
		this.imageHeight = HEIGHT;
	}

	private PabxBlockEntity pabx() {
		return menu.blockEntity;
	}

	private static Component tr(String key, Object... args) {
		return Component.translatable(Radiocraft.translationKey("screen", "pabx." + key), args);
	}

	@Override
	protected void init() {
		super.init();
		PabxBlockEntity pabx = pabx();
		// Keep what was typed across a resize.
		String masterText = master == null ? pabx.getMaster() : master.getValue();
		String firstText = first == null ? Integer.toString(pabx.getFirstExtension()) : first.getValue();
		String nameText = name == null ? pabx.getName() : name.getValue();

		master = box(MASTER_Y, tr("master"), pabx.getMasterDigits(), s -> s.isEmpty() || PabxNumbers.digits(s), masterText);
		first = box(FIRST_Y, tr("first"), PabxNumbers.MAX_EXTENSION_DIGITS, s -> s.isEmpty() || PabxNumbers.digits(s), firstText);
		name = box(NAME_Y, tr("name"), PabxNumbers.MAX_NAME, s -> s.chars().allMatch(c -> PabxNumbers.nameChar((char)c)), nameText);
		addRenderableWidget(Button.builder(tr("save"), b -> save()).bounds(leftPos + (WIDTH - 60) / 2, topPos + SAVE_Y, 60, 20).build());
		setInitialFocus(master);
	}

	private EditBox box(int y, Component label, int maxLength, java.util.function.Predicate<String> filter, String value) {
		EditBox box = addRenderableWidget(new EditBox(font, leftPos + BOX_X, topPos + y, BOX_WIDTH, 16, label));
		box.setMaxLength(maxLength);
		box.setFilter(filter);
		box.setValue(value.length() > maxLength ? value.substring(0, maxLength) : value);
		return box;
	}

	/** Checks the master number's length here, then sends all three; the server checks the rest. */
	private void save() {
		if(master == null || first == null || name == null)
			return;
		int digits = pabx().getMasterDigits();
		if(!PabxNumbers.validMaster(master.getValue(), digits)) {
			localStatus = tr("master_length", digits);
			return;
		}
		if(first.getValue().isEmpty()) {
			localStatus = tr("bad_extension");
			return;
		}
		localStatus = null;
		CompoundTag data = new CompoundTag();
		data.putString("master", master.getValue());
		data.putString("first", first.getValue());
		data.putString("name", PabxNumbers.cleanName(name.getValue()));
		SStationPacket.send(pabx().getBlockPos(), "save", data);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if(keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
			save();
			return true;
		}
		if(keyCode != GLFW.GLFW_KEY_ESCAPE && getFocused() instanceof EditBox box && box.isFocused()) // Typing 'e' must not close it.
			return box.keyPressed(keyCode, scanCode, modifiers) || box.canConsumeInput();
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean charTyped(char codePoint, int modifiers) {
		if(name != null && name.isFocused() && codePoint >= 'a' && codePoint <= 'z')
			return name.charTyped(Character.toUpperCase(codePoint), modifiers); // Names are upper case, like the phone's LCD.
		return super.charTyped(codePoint, modifiers);
	}

	@Override
	public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		super.render(guiGraphics, mouseX, mouseY, partialTick);
		renderTooltip(guiGraphics, mouseX, mouseY);
	}

	@Override
	protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
		guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
	}

	@Override
	protected void renderLabels(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
		PabxBlockEntity pabx = pabx();
		guiGraphics.drawString(font, title, 8, 6, INK, false);
		Component power = tr(pabx.isPowered() ? "powered" : "no_power");
		guiGraphics.drawString(font, power, WIDTH - 8 - font.width(power), 6, pabx.isPowered() ? POWER_ON : POWER_OFF, false);
		guiGraphics.drawString(font, tr("master_label", pabx.getMasterDigits()), BOX_X, MASTER_Y - 10, INK, false);
		guiGraphics.drawString(font, tr("first_label"), BOX_X, FIRST_Y - 10, INK, false);
		guiGraphics.drawString(font, tr("name_label", PabxNumbers.MAX_NAME), BOX_X, NAME_Y - 10, INK, false);

		Component status = localStatus;
		if(status == null && !pabx.getStatusKey().isEmpty())
			status = Component.translatable(pabx.getStatusKey(), pabx.getStatusArg());
		if(status != null) {
			int y = STATUS_Y;
			for(FormattedCharSequence line : font.split(status, WIDTH - 16)) {
				guiGraphics.drawString(font, line, (WIDTH - font.width(line)) / 2, y, STATUS_INK, false);
				y += 10;
			}
		}
	}
}
