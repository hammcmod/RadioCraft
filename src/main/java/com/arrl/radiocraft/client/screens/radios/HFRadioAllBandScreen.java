package com.arrl.radiocraft.client.screens.radios;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.client.screens.widgets.*;
import com.arrl.radiocraft.common.menus.HFRadioAllBandMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

public class HFRadioAllBandScreen extends HFRadioScreen<HFRadioAllBandMenu> {

    public HFRadioAllBandScreen(HFRadioAllBandMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, Radiocraft.id("textures/gui/hf_radio_all_band.png"), Radiocraft.id("textures/gui/hf_radio_all_band_widgets.png"));

        this.imageWidth = 252;
        this.imageHeight = 142;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(new ValueButton(leftPos + 86, topPos + 57, 34, 19, 84, 49, widgetsTexture, 256, 256, () -> menu.blockEntity.getCWEnabled(), this::onPressCW));
        addRenderableWidget(new ValueButton(leftPos + 86, topPos + 77, 34, 19, 0, 0, widgetsTexture, 256, 256, () -> menu.blockEntity.getSSBEnabled(), this::onPressSSB)); // SSB button
        addRenderableWidget(new HoldButton(leftPos + 35, topPos + 104, 51, 19, 0, 192, widgetsTexture, 256, 256, this::onPressPTT, this::onReleasePTT));
        addRenderableWidget(new ToggleButton(false, leftPos + 88, topPos + 100, 26, 26, 0, 138, widgetsTexture, 256, 256, (btn) -> {})); // FM button
        addRenderableWidget(new ToggleButton(false, leftPos + 221, topPos + 79, 22, 22, 0, 47, widgetsTexture, 256, 256, (btn) -> {})); // MIC
        addRenderableWidget(new ToggleButton(false, leftPos + 221, topPos + 58, 22, 22, 0, 91, widgetsTexture, 256, 256, (btn) -> {})); // SPK
        addRenderableWidget(new ToggleButton(false, leftPos + 35, topPos + 82, 16, 11, 115, 2, widgetsTexture, 256, 256, (btn) -> {}));
        addRenderableWidget(new ToggleButton(false, leftPos + 52, topPos + 82, 16, 11, 115, 2, widgetsTexture, 256, 256, (btn) -> {}));
        addRenderableWidget(new ToggleButton(false, leftPos + 69, topPos + 82, 16, 11, 149, 2, widgetsTexture, 256, 256, (btn) -> {}));
        addRenderableWidget(new ToggleButton(false, leftPos + 196, topPos + 15, 16, 11, 149, 2, widgetsTexture, 256, 256, (btn) -> {}));
        addRenderableWidget(new HoldButton(leftPos + 194, topPos + 94, 18, 17, 46, 49, widgetsTexture, 256, 256, (btn) -> onFrequencyButtonUp(null), (btn) -> {}));
        addRenderableWidget(new HoldButton(leftPos + 194, topPos + 112, 18, 17, 46, 83, widgetsTexture, 256, 256, (btn) -> onFrequencyButtonDown(null), (btn) -> {}));
        addRenderableWidget(new Dial(leftPos + 125, topPos + 62, 62, 68, 108, 87, widgetsTexture, 256, 256, this::onFrequencyDialUp, this::onFrequencyDialDown)); // Frequency dial
        addRenderableWidget(new Dial(leftPos + 194, topPos + 62, 28, 31, 52, 125, widgetsTexture, 256, 256, (dial) -> {}, (dial) -> {}));
        addRenderableWidget(new Dial(leftPos + 217, topPos + 101, 28, 31, 52, 125, widgetsTexture, 256, 256, (dial) -> {}, (dial) -> {}));
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
		/*
		if(isHovering(33, 18, 25, 11, pMouseX, pMouseY))
			renderTooltip(poseStack, Component.translatable(Radiocraft.translationKey("screen", "radio.tx")), pMouseX, pMouseY);
		if(isHovering(62, 18, 25, 11, pMouseX, pMouseY))
			renderTooltip(poseStack, Component.translatable(Radiocraft.translationKey("screen", "radio.rx")), pMouseX, pMouseY);

		if(menu.isPowered()) {
			poseStack.pushPose(); // Push/pop allows you to add a set of transformations to the stack. Pushing starts a new set and popping reverts to the previous set.

			poseStack.scale(1.5F, 1.5F, 1.5F);
			float freqMhz = menu.getFrequency() / 1000.0F; // Frequency is in kHz, divide by 1000 to get MHz
			font.draw(poseStack, String.format("%.3f", freqMhz) + "MHz", (leftPos + 24) / 1.5F, (topPos + 50) / 1.5F, 0xFFFFFF); // Divide the positions rendered at by 1.5F as the entire pose was scaled by 1.5F.

			poseStack.popPose(); // Reset pose stack. Will cause a memory leak if you push without popping.
		}*/
    }

    @Override
    protected void renderAdditionalBackground(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        if(menu.isPowered()) {
//			if(menu.blockEntity.isTransmitting())
//				blit(poseStack, leftPos + 30, topPos + 15, 1, 148, 29, 15);
//			if(menu.blockEntity.isReceiving())
//				blit(poseStack, leftPos + 59, topPos + 15, 30, 148, 29, 15);
        }
    }

}