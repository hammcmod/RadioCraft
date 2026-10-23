package com.arrl.radiocraft.client.screens.radios;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.client.screens.widgets.Dial;
import com.arrl.radiocraft.client.screens.widgets.HoldButton;
import com.arrl.radiocraft.client.screens.widgets.ToggleButton;
import com.arrl.radiocraft.client.screens.widgets.ValueButton;
import com.arrl.radiocraft.common.menus.HFRadio80mMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

public class HFRadio80mScreen extends HFRadioScreen<HFRadio80mMenu> {

    public HFRadio80mScreen(HFRadio80mMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, guiTexture(menu, "hf_radio_80m", ""), guiTexture(menu, "hf_radio_80m", "_widgets"));

        this.imageWidth = 212;
        this.imageHeight = 211;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(new ToggleButton(menu.isPowered(), leftPos + 10, topPos + 188, 14, 17, 0, 203, widgetsTexture, 256, 256, this::onPressPower)); // Power button
        addRenderableWidget(new ValueButton(leftPos + 90, topPos + 74, 34, 19, -1, 0, widgetsTexture, 256, 256, () -> menu.blockEntity.getCWEnabled(), this::onPressCW)); // CW Button
        addRenderableWidget(new ValueButton(leftPos + 90, topPos + 54, 34, 19, -1, 38, widgetsTexture, 256, 256, () -> menu.blockEntity.getSSBEnabled(), this::onPressSSB)); // SSB Button
        addRenderableWidget(new HoldButton(leftPos + 139, topPos + 163, 51, 19, -1, 76, widgetsTexture, 256, 256, this::onPressPTT, this::onReleasePTT)); // PTT button
        addRenderableWidget(new Dial(leftPos + 87, topPos + 176, 28, 33, 143, 0, widgetsTexture, 256, 256, this::onFrequencyDialUp, this::onFrequencyDialDown)); // Frequency Dial (moved left of Mic Gain)
        addRenderableWidget(new Dial(leftPos + 122, topPos + 183, 15, 17, 94, 0, widgetsTexture, 256, 256, this::doNothing, this::doNothing)); // Mic Gain dial
        addRenderableWidget(new Dial(leftPos + 160, topPos + 183, 15, 17, 94, 0, widgetsTexture, 256, 256, this::doNothing, this::doNothing)); // Gain dial
        addRenderableWidget(new ToggleButton(false, leftPos + 175, topPos + 183, 22, 22, 0, 159, widgetsTexture, 256, 256, (btn) -> {})); // SPK 
        addRenderableWidget(new ToggleButton(false, leftPos + 138, topPos + 183, 22, 22, 0, 114, widgetsTexture, 256, 256, (btn) -> {})); // MIC
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
        renderDisplay(pGuiGraphics, 26, 117, 62, 21, 1.0F, 0x7A1A1A); // The tan panel above the brown box, dark red text (maintainer's choice)
    }


}
