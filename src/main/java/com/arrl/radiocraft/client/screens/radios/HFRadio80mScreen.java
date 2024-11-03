package com.arrl.radiocraft.client.screens.radios;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.client.screens.widgets.Dial;
import com.arrl.radiocraft.client.screens.widgets.HoldButton;
import com.arrl.radiocraft.client.screens.widgets.ToggleButton;
import com.arrl.radiocraft.client.screens.widgets.ValueButton;
import com.arrl.radiocraft.common.menus.HFRadio80mMenu;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

public class HFRadio80mScreen extends HFRadioScreen<HFRadio80mMenu> {

    public HFRadio80mScreen(HFRadio80mMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, Radiocraft.id("textures/gui/hf_radio_80m.png"), Radiocraft.id("textures/gui/hf_radio_80m_widgets.png"));

        this.imageWidth = 212;
        this.imageHeight = 211;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(new ToggleButton(menu.isPowered(), leftPos + 10, topPos + 188, 14, 17, 0, 203, widgetsTexture, 256, 256, this::onPressPower)); // Power button (temporary)
        addRenderableWidget(new ValueButton(leftPos + 90, topPos + 74, 34, 19, -1, 0, widgetsTexture, 256, 256, () -> menu.blockEntity.getCWEnabled(), this::onPressCW)); // CW Button
        addRenderableWidget(new ValueButton(leftPos + 90, topPos + 54, 34, 19, -1, 38, widgetsTexture, 256, 256, () -> menu.blockEntity.getSSBEnabled(), this::onPressSSB)); // SSB Button
        addRenderableWidget(new HoldButton(leftPos + 139, topPos + 163, 51, 19, -1, 76, widgetsTexture, 256, 256, this::onPressPTT, this::onReleasePTT)); // PTT button
        addRenderableWidget(new Dial(leftPos + 42, topPos + 156, 28, 33, 143, 0, widgetsTexture, 256, 256, this::onFrequencyDialUp, this::onFrequencyDialDown)); // Frequency Dial
        addRenderableWidget(new Dial(leftPos + 122, topPos + 186, 15, 17, 94, 0, widgetsTexture, 256, 256, this::doNothing, this::doNothing)); // Mic Gain dial
        addRenderableWidget(new Dial(leftPos + 160, topPos + 186, 15, 17, 94, 0, widgetsTexture, 256, 256, this::doNothing, this::doNothing)); // Gain dial
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics pGuiGraphics, float pPartialTick, int pMouseX, int pMouseY) {

    }


    @Override
    protected void renderAdditionalTooltips(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        /*
        if(menu.isPowered()) {
            poseStack.pushPose(); // Push/pop allows you to add a set of transformations to the stack. Pushing starts a new set and popping reverts to the previous set.
            poseStack.scale(0.8F, 0.8F, 0.8F);
            float freqMhz = menu.getFrequency() / 1000.0F; // Frequency is in kHz, divide by 1000 to get MHz
            font.draw(poseStack, String.format("%.3f", freqMhz) + "MHz", (leftPos + 19) / 0.8F, (topPos + 22) / 0.8F, 0xFFFFFF);
            poseStack.popPose(); // Reset pose stack. Will cause a memory leak if you push without popping.
        }*/
    }


}
