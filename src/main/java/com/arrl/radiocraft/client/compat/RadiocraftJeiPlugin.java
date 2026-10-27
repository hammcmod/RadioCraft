package com.arrl.radiocraft.client.compat;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.client.screens.AprsTerminalScreen;
import com.arrl.radiocraft.client.screens.StationPanelScreen;
import com.arrl.radiocraft.client.screens.RadioFaxScreen;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.handlers.IGuiProperties;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/**
 * JEI support, loaded only when JEI is installed. The station terminal screens are laid out small and drawn scaled up
 * (see {@link StationPanelScreen#getScale}), so JEI must be told where it really is on the window, or it lays its
 * item list out over it.
 */
@JeiPlugin
public class RadiocraftJeiPlugin implements IModPlugin {

	@Override
	public @NotNull ResourceLocation getPluginUid() {
		return Radiocraft.id("jei");
	}

	@Override
	public void registerGuiHandlers(IGuiHandlerRegistration registration) {
		registration.addGuiScreenHandler(AprsTerminalScreen.class, RadiocraftJeiPlugin::properties);
		registration.addGuiScreenHandler(RadioFaxScreen.class, RadiocraftJeiPlugin::properties);
		registration.addGuiScreenHandler(com.arrl.radiocraft.client.screens.TeletypeScreen.class, RadiocraftJeiPlugin::properties);
		registration.addGuiScreenHandler(com.arrl.radiocraft.client.screens.DirectionFinderScreen.class, RadiocraftJeiPlugin::properties);
		registration.addGuiScreenHandler(com.arrl.radiocraft.client.screens.MicrowaveRadioScreen.class, RadiocraftJeiPlugin::properties);
		registration.addGuiScreenHandler(com.arrl.radiocraft.client.screens.TelephoneScreen.class, RadiocraftJeiPlugin::properties);
		registration.addGuiScreenHandler(com.arrl.radiocraft.client.screens.WefaxReceiverScreen.class, RadiocraftJeiPlugin::properties);
	}

	private static IGuiProperties properties(StationPanelScreen<?> screen) {
		float scale = screen.getScale();
		return new Properties(screen.getClass(), Math.round(screen.getGuiLeft() * scale), Math.round(screen.getGuiTop() * scale),
				Math.round(screen.getXSize() * scale), Math.round(screen.getYSize() * scale),
				Math.round(screen.width * scale), Math.round(screen.height * scale));
	}

	private record Properties(Class<? extends Screen> screenClass, int guiLeft, int guiTop, int guiXSize, int guiYSize,
	                          int screenWidth, int screenHeight) implements IGuiProperties {}
}
