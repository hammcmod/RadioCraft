package com.arrl.radiocraft.client.compat;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.client.screens.AprsTerminalScreen;
import com.arrl.radiocraft.client.screens.StationPanelScreen;
import com.arrl.radiocraft.client.screens.RadioFaxScreen;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.gui.handlers.IGuiProperties;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.List;

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
		registration.addGuiScreenHandler(com.arrl.radiocraft.client.screens.IonosphericProcessorScreen.class, RadiocraftJeiPlugin::properties); // Was missing (2026-10-27): JEI covered it.
		// Every scaled panel, listed above or not, also reports where it really is, so JEI moves its list off it or
		// hides what would land on it (2026-10-31: JEI still overlapped some).
		registration.addGenericGuiContainerHandler(StationPanelScreen.class, new PanelArea());
	}

	/** The panel's rectangle on the window, scaled up as it is drawn. */
	private static final class PanelArea implements IGuiContainerHandler<StationPanelScreen<?>> {
		@Override
		public @NotNull List<Rect2i> getGuiExtraAreas(@NotNull StationPanelScreen<?> screen) {
			float scale = screen.getScale();
			return List.of(new Rect2i((int)Math.floor(screen.getGuiLeft() * scale), (int)Math.floor(screen.getGuiTop() * scale),
					(int)Math.ceil(screen.getXSize() * scale), (int)Math.ceil(screen.getYSize() * scale)));
		}
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
