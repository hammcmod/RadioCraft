package com.arrl.radiocraft.client.screens.radios;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.RadiocraftServerConfig;
import com.arrl.radiocraft.client.screens.widgets.Dial;
import com.arrl.radiocraft.client.screens.widgets.HoldButton;
import com.arrl.radiocraft.client.screens.widgets.ToggleButton;
import com.arrl.radiocraft.client.screens.widgets.ValueButton;
import com.arrl.radiocraft.common.menus.QRPRadio40mMenu;
import com.arrl.radiocraft.common.radio.Band;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

public class QRPRadio40mScreen extends HFRadioScreen<QRPRadio40mMenu> {

    public QRPRadio40mScreen(QRPRadio40mMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, Radiocraft.id("textures/gui/qrp_radio_40m.png"), Radiocraft.id("textures/gui/qrp_radio_40m_widgets.png"));

        this.imageWidth = 252;
        this.imageHeight = 130;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(new ToggleButton(menu.isPowered(), leftPos + 16, topPos + 9, 20, 21, 0, 0, widgetsTexture, 256, 256, this::onPressPower)); // Power button
        addRenderableWidget(new ValueButton(leftPos + 195, topPos + 48, 34, 19, 0, 42, widgetsTexture, 256, 256, () -> menu.blockEntity.getCWEnabled(), this::onPressCW)); // CW Button
        addRenderableWidget(new ValueButton(leftPos + 195, topPos + 68, 34, 19, 0, 80, widgetsTexture, 256, 256, () -> menu.blockEntity.getSSBEnabled(), this::onPressSSB)); // SSB button
        addRenderableWidget(new HoldButton(leftPos + 178, topPos + 97, 51, 19, 0, 118, widgetsTexture, 256, 256, this::onPressPTT, this::onReleasePTT)); // PTT button
        addRenderableWidget(new Dial(leftPos + 48, topPos + 47, 42, 42, 121, 0, widgetsTexture, 256, 256, this::onFrequencyDialUp, this::onFrequencyDialDown)); // Frequency dial
        addRenderableWidget(new Dial(leftPos + 31, topPos + 36, 15, 15, 121, 85, widgetsTexture, 256, 256, this::doNothing, this::doNothing)); // Gain dial
        addRenderableWidget(new Dial(leftPos + 31, topPos + 62, 15, 15, 121, 85, widgetsTexture, 256, 256, this::doNothing, this::doNothing)); // Mic gain dial
        addRenderableWidget(new Dial(leftPos + 29, topPos + 94, 28, 28, 122, 115, widgetsTexture, 256, 256, this::onFrequencyDialUp, this::onFrequencyDialDown)); // Frequency dial
        addRenderableWidget(new Dial(leftPos + 81, topPos + 94, 28, 28, 122, 115, widgetsTexture, 256, 256, this::onFrequencyDialUp, this::onFrequencyDialDown)); // Frequency dial
        addRenderableWidget(new Dial(leftPos + 132, topPos + 94, 28, 28, 122, 115, widgetsTexture, 256, 256, this::onFrequencyDialUp, this::onFrequencyDialDown)); // Frequency dial

    }

    @Override
    protected void renderBg(@NotNull GuiGraphics pGuiGraphics, float pPartialTick, int pMouseX, int pMouseY) {

    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Don't render the default title and inventory labels
    }


    @Override
    protected void renderAdditionalBackground(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        float freq = menu.getFrequency();
        Band band = menu.getBand();
        int step = RadiocraftServerConfig.HF_FREQUENCY_STEP.get();
        float min = band.minFrequency();
        float max = (band.maxFrequency() - band.minFrequency()) / step * step + min;

        if(menu.isPowered()) {
            if(freq >= max || freq <= min) {
                //blit(poseStack, leftPos + 92, topPos + 63, 1, 162, 13, 13);
            }
        }
    }
}
