package com.arrl.radiocraft.client.screens.radios;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.client.screens.widgets.*;
import com.arrl.radiocraft.common.be_networks.network_objects.RadioNetworkObject;
import com.arrl.radiocraft.common.blockentities.radio.HFRadioAllBandBlockEntity;
import com.arrl.radiocraft.common.menus.HFRadioAllBandMenu;
import com.arrl.radiocraft.common.network.serverbound.SBlockRadioControlPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

/**
 * The all-band transceiver's panel: power on the square button, the band knob (80, 40, 20, 10, 2m), SSB / CW / FM, and
 * four lamps: power by the bulb, then RX, antenna OK and TX under the brand plate.
 */
public class HFRadioAllBandScreen extends HFRadioScreen<HFRadioAllBandMenu> {

    /** The band knob's sprite for each position (W, NW, N, NE, E), lined up on the knob painted on the panel. */
    private static final BandKnob.Sprite[] BAND_KNOB = {
            new BandKnob.Sprite(162, 24, 22, 23, 0, 1),  // 3.5 MHz (80m)
            new BandKnob.Sprite(86, 25, 20, 24, 2, 1),   // 7 MHz (40m)
            new BandKnob.Sprite(236, 25, 20, 23, 2, 2),  // 14 MHz (20m)
            new BandKnob.Sprite(110, 24, 22, 24, 2, 1),  // 28 MHz (10m)
            new BandKnob.Sprite(135, 24, 22, 23, 2, 1)   // 144 MHz (2m)
    };
    private static final int KNOB_X = 204;
    private static final int KNOB_Y = 34;

    /** Lamps: 16 x 11 cells, dark (off) on top and lit (on) below. */
    private static final int LAMP_GREEN_U = 115;
    private static final int LAMP_AMBER_U = 149;
    private static final int LAMP_OFF_V = 2;
    private static final int LAMP_ON_V = 13;
    /** Highest SWR at which the antenna lamp still counts the match as good. */
    private static final double GOOD_SWR = 2.0D;

    public HFRadioAllBandScreen(HFRadioAllBandMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, Radiocraft.id("textures/gui/hf_radio_all_band.png"), Radiocraft.id("textures/gui/hf_radio_all_band_widgets.png"));

        this.imageWidth = 252;
        this.imageHeight = 142;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(new PanelButton(leftPos + 11, topPos + 72, 18, 19, () -> onPressPower(null))); // Power button
        addRenderableWidget(new BandKnob(leftPos + KNOB_X, topPos + KNOB_Y, 26, 26, widgetsTexture, 256, 256, BAND_KNOB,
                () -> menu.blockEntity.getBandIndex(), this::onTurnBand)); // Band switch
        addRenderableWidget(new ValueButton(leftPos + 86, topPos + 57, 34, 19, 84, 49, widgetsTexture, 256, 256, () -> menu.blockEntity.getCWEnabled(), this::onPressCW));
        addRenderableWidget(new ValueButton(leftPos + 86, topPos + 77, 34, 19, 0, 0, widgetsTexture, 256, 256, this::isSSBSelected, this::onPressSSB)); // SSB button
        addRenderableWidget(new ValueButton(leftPos + 88, topPos + 100, 26, 26, 0, 138, widgetsTexture, 256, 256, () -> menu.blockEntity.getFMEnabled(), this::onPressFM)); // FM button
        addRenderableWidget(new HoldButton(leftPos + 35, topPos + 104, 51, 19, 0, 192, widgetsTexture, 256, 256, this::onPressPTT, this::onReleasePTT));
        addRenderableWidget(new ToggleButton(false, leftPos + 221, topPos + 79, 22, 22, 0, 47, widgetsTexture, 256, 256, (btn) -> {})); // MIC
        addRenderableWidget(new ToggleButton(false, leftPos + 221, topPos + 58, 22, 22, 0, 91, widgetsTexture, 256, 256, (btn) -> {})); // SPK
        addRenderableWidget(new HoldButton(leftPos + 194, topPos + 94, 18, 17, 46, 49, widgetsTexture, 256, 256, (btn) -> onFrequencyButtonUp(null), (btn) -> {}));
        addRenderableWidget(new HoldButton(leftPos + 194, topPos + 112, 18, 17, 46, 83, widgetsTexture, 256, 256, (btn) -> onFrequencyButtonDown(null), (btn) -> {}));
        addRenderableWidget(new Dial(leftPos + 125, topPos + 62, 62, 68, 108, 87, widgetsTexture, 256, 256, this::onFrequencyDialUp, this::onFrequencyDialDown)); // Frequency dial
        speakerGainDial = addRenderableWidget(new Dial(leftPos + 194, topPos + 62, 28, 31, 52, 125, widgetsTexture, 256, 256, this::onGainUp, this::onGainDown)); // Gain dial
        micGainDial = addRenderableWidget(new Dial(leftPos + 217, topPos + 101, 28, 31, 52, 125, widgetsTexture, 256, 256, this::onMicGainUp, this::onMicGainDown)); // Mic gain dial
    }

    private void onTurnBand(int index) {
        SBlockRadioControlPacket.updateServer(menu.blockEntity.getBlockPos(), SBlockRadioControlPacket.Action.SET_BAND, index);
        menu.blockEntity.setBandIndex(index); // Optimistic; the server re-syncs.
    }

    private void onPressFM(ValueButton button) {
        if(menu.blockEntity.getFMEnabled())
            return; // Mode buttons select: pressing the active one does nothing.
        SBlockRadioControlPacket.updateServer(menu.blockEntity.getBlockPos(), SBlockRadioControlPacket.Action.SET_FM, true);
        menu.blockEntity.setFMEnabled(true);
    }

    @Override
    protected boolean isSSBSelected() {
        return menu.blockEntity.isSSBSelected();
    }

    @Override
    protected String modeLabel() {
        if(menu.blockEntity.getFMEnabled())
            return "FM";
        return super.modeLabel();
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics pGuiGraphics, float pPartialTick, int pMouseX, int pMouseY) {

    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Don't render the default title and inventory labels
    }

    @Override
    protected void renderAdditionalTooltips(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        renderDisplay(pGuiGraphics, 110, 17, 67, 28, 1.0F, 0xFFFFFF); // Display window on the texture
    }

    @Override
    protected void renderAdditionalBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        boolean on = menu.getDisplayPhase() == RadioNetworkObject.PHASE_ON;
        HFRadioAllBandBlockEntity radio = menu.blockEntity;
        double swr = radio.getAntennaSWR();
        lamp(graphics, 196, 15, LAMP_AMBER_U, menu.getDisplayPhase() != RadioNetworkObject.PHASE_OFF); // Power, by the bulb.
        lamp(graphics, 35, 82, LAMP_GREEN_U, on && radio.getSyncedSignal() > 0.0D); // RX: a signal is coming in.
        lamp(graphics, 52, 82, LAMP_GREEN_U, on && swr > 0.0D && swr <= GOOD_SWR); // Antenna matched.
        lamp(graphics, 69, 82, LAMP_AMBER_U, on && isTransmitting()); // TX.
    }

    private void lamp(GuiGraphics graphics, int x, int y, int u, boolean lit) {
        graphics.blit(widgetsTexture, leftPos + x, topPos + y, u, lit ? LAMP_ON_V : LAMP_OFF_V, 16, 11, 256, 256);
    }

}
