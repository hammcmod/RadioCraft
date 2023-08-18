package com.arrl.radiocraft.client.screens;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.client.screens.widgets.Dial;
import com.arrl.radiocraft.client.screens.widgets.HoldButton;
import com.arrl.radiocraft.client.screens.widgets.ToggleButton;
import com.arrl.radiocraft.common.menus.AbstractHFRadioMenu;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class HFRadio80mScreen extends AbstractHFRadioScreen {

    public HFRadio80mScreen(AbstractHFRadioMenu container, Inventory playerInventory, Component title) {
        super(container, playerInventory, title, Radiocraft.location("textures/gui/hf_radio_80m.png"), Radiocraft.location("textures/gui/hf_radio_80m_widgets.png"));

        this.imageWidth = 212;
        this.imageHeight = 211;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(new ToggleButton(container.blockEntity.getSSBEnabled(), leftPos + 90, topPos + 54, 34, 19, 0, 38, widgetsTexture, 256, 256, this::onPressSSB)); // SSB Button
        addRenderableWidget(new ToggleButton(container.blockEntity.getCWEnabled(), leftPos + 90, topPos + 74, 34, 19, 0, 0, widgetsTexture, 256, 256, this::onPressCW)); // CW Button

        addRenderableWidget(new HoldButton(leftPos + 139, topPos + 163, 51, 19, 0, 76, widgetsTexture, 256, 256, this::onPressPTT, this::onReleasePTT)); // PTT button

        addRenderableWidget(new Dial(leftPos + 42, topPos + 156, 28, 33, 144, 0, widgetsTexture, 256, 256, this::onFrequencyUp, this::onFrequencyDown));
        addRenderableWidget(new Dial(leftPos + 121, topPos + 186, 17, 17, 94, 0, widgetsTexture, 256, 256, this::doNothing, this::doNothing)); // Mic Gain dial
        addRenderableWidget(new Dial(leftPos + 159, topPos + 186, 17, 17, 94, 0, widgetsTexture, 256, 256, this::doNothing, this::doNothing)); // Gain dial
    }

    @Override
    protected void renderAdditionalTooltips(PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
        if(isHovering(33, 18, 25, 11, mouseX, mouseY))
            renderTooltip(poseStack, Component.translatable(Radiocraft.translationKey("screen", "radio.tx")), mouseX, mouseY);
        if(isHovering(62, 18, 25, 11, mouseX, mouseY))
            renderTooltip(poseStack, Component.translatable(Radiocraft.translationKey("screen", "radio.rx")), mouseX, mouseY);

        if(container.isPowered()) {
            poseStack.pushPose(); // Push/pop allows you to add a set of transformations to the stack. Pushing starts a new set and popping reverts to the previous set.

            poseStack.scale(1.5F, 1.5F, 1.5F);
            float freqMhz = container.getFrequency() / 1000.0F; // Frequency is in kHz, divide by 1000 to get MHz
            font.draw(poseStack, String.format("%.3f", freqMhz) + "MHz", (leftPos + 24) / 1.5F, (topPos + 50) / 1.5F, 0xFFFFFF); // Divide the positions rendered at by 1.5F as the entire pose was scaled by 1.5F.

            poseStack.popPose(); // Reset pose stack. Will cause a memory leak if you push without popping.
        }
    }

    @Override
    protected void renderAdditionalBg(PoseStack poseStack, float partialTicks, int x, int y) {
        if(container.isPowered()) {
            if(container.isTransmitting())
                blit(poseStack, leftPos + 30, topPos + 15, 1, 148, 29, 15);
            if(container.isReceiving())
                blit(poseStack, leftPos + 59, topPos + 15, 30, 148, 29, 15);
        }
    }

    @Override
    protected void renderLabels(PoseStack poseStack, int x, int y) {}

    protected boolean isHovering(int x, int y, int width, int height, double mouseX, double mouseY) {
        mouseX -= leftPos;
        mouseY -= topPos;
        return mouseX >= (x - 1) && mouseX < (x + width + 1) && mouseY >= (y - 1) && mouseY < (y + height + 1);
    }

}
