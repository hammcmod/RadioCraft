package com.arrl.radiocraft.client.screens;

import com.arrl.radiocraft.common.menus.CommercialRadioModuleMenu;
import com.arrl.radiocraft.common.network.serverbound.SStationPacket;
import com.arrl.radiocraft.common.radio.voice.StationProgram;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/** Creative only: sets a commercial radio module's program and frequency (MHz), for building structures. */
public class CommercialRadioModuleScreen extends AbstractContainerScreen<CommercialRadioModuleMenu> {

	private StationProgram.Kind program;
	private EditBox frequency;
	private Button programButton;

	public CommercialRadioModuleScreen(CommercialRadioModuleMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
		imageWidth = 200;
		imageHeight = 96;
		StationProgram.Kind kind = menu.blockEntity.getKind();
		program = kind == null ? StationProgram.Kind.DF_BEEPER : kind;
	}

	@Override
	protected void init() {
		super.init();
		programButton = addRenderableWidget(Button.builder(Component.empty(), b -> {
			StationProgram.Kind[] kinds = StationProgram.Kind.values();
			program = kinds[(program.ordinal() + 1) % kinds.length];
		}).bounds(leftPos + 10, topPos + 22, 180, 20).build());
		frequency = addRenderableWidget(new EditBox(font, leftPos + 10, topPos + 48, 120, 18, Component.literal("MHz")));
		frequency.setValue(menu.blockEntity.getFrequency() > 0 ? String.format(Locale.ROOT, "%.4f", menu.blockEntity.getFrequency() / 1e6) : "146.5200");
		addRenderableWidget(Button.builder(Component.literal("SET"), b -> apply()).bounds(leftPos + 136, topPos + 47, 54, 20).build());
	}

	private void apply() {
		try {
			CompoundTag tag = new CompoundTag();
			tag.putString("program", program.name());
			tag.putFloat("frequency", (float)(Double.parseDouble(frequency.getValue().trim()) * 1e6));
			SStationPacket.send(menu.getBlockPos(), "configure", tag);
			onClose();
		}
		catch(NumberFormatException e) {
			frequency.setTextColor(0xFFFF5555);
		}
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if(frequency.isFocused() && keyCode != org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
			if(keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER || keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER) {
				apply();
				return true;
			}
			frequency.setTextColor(0xFFE0E0E0);
			return frequency.keyPressed(keyCode, scanCode, modifiers) || true; // Keep E from closing the screen.
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		programButton.setMessage(Component.literal("PROGRAM: " + program.name().replace('_', ' ')));
		super.render(graphics, mouseX, mouseY, partialTick);
	}

	@Override
	protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
		graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF3A4048);
		graphics.fill(leftPos + 2, topPos + 2, leftPos + imageWidth - 2, topPos + imageHeight - 2, 0xFF5C6670);
	}

	@Override
	protected void renderLabels(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
		graphics.drawString(font, title, 8, 7, 0xFFF0F0F0, false);
		graphics.drawString(font, "MHz, any band (80m, 40m, 20m, 10m, 2m)", 10, 72, 0xFFD0D8E0, false);
		graphics.drawString(font, "Creative only. Players find it sealed.", 10, 83, 0xFFB0B8C0, false);
	}
}
