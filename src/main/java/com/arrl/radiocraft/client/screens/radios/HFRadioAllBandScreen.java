package com.arrl.radiocraft.client.screens.radios;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.client.screens.widgets.*;
import com.arrl.radiocraft.common.menus.HFRadioAllBandMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

/*
 * TODO This class was copied from the 10m screen and is almost certainly wrong all over.
 */
public class HFRadioAllBandScreen extends HFRadioScreen<HFRadioAllBandMenu> {

    public HFRadioAllBandScreen(HFRadioAllBandMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, Radiocraft.id("textures/gui/hf_radio_all_band.png"), Radiocraft.id("textures/gui/hf_radio_all_band_widgets.png"));

        this.imageWidth = 252;
        this.imageHeight = 142;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(new StaticToggleButton(false, leftPos + 86, topPos + 57, 32, 17, 30, 5, widgetsTexture, 256, 256, (btn) -> onPressCW(null)));
        addRenderableWidget(new StaticToggleButton(false, leftPos + 86, topPos + 77, 32, 17, 68, 5, widgetsTexture, 256, 256, (btn) -> onPressSSB(null)));
        addRenderableWidget(new StaticToggleButton(false, leftPos + 37, topPos + 106, 49, 17, 0, 29, widgetsTexture, 256, 256, (btn) -> { onPressPTT(null); onReleasePTT(null); }));
        addRenderableWidget(new StaticToggleButton(false, leftPos + 89, topPos + 101, 24, 24, 0, 0, widgetsTexture, 256, 256, (btn) -> {}));
        addRenderableWidget(new StaticToggleButton(false, leftPos + 222, topPos + 80, 20, 20, 0, 49, widgetsTexture, 256, 256, (btn) -> {}));
        addRenderableWidget(new StaticToggleButton(false, leftPos + 222, topPos + 59, 20, 20, 22, 49, widgetsTexture, 256, 256, (btn) -> {}));
        addRenderableWidget(new CustomToggleButton(false, leftPos + 35, topPos + 82, 16, 12, -1, -1, 165, 6, 111, 7, 111, 7, widgetsTexture, 256, 256, (btn) -> {}));
        addRenderableWidget(new CustomToggleButton(false, leftPos + 52, topPos + 82, 16, 12, -1, -1, 182, 6, 127, 7, 127, 7, widgetsTexture, 256, 256, (btn) -> {}));
        addRenderableWidget(new CustomToggleButton(false, leftPos + 69, topPos + 82, 16, 12, -1, -1, 199, 6, 144, 7, 144, 7, widgetsTexture, 256, 256, (btn) -> {}));
        addRenderableWidget(new CustomToggleButton(false, leftPos + 196, topPos + 15, 16, 12, -1, -1, 199, 6, 144, 7, 144, 7, widgetsTexture, 256, 256, (btn) -> {}));
        addRenderableWidget(new ImageButton(leftPos + 129, topPos + 93, 25, 17, 0, 148, widgetsTexture, 256, 256, this::onFrequencyButtonUp));
        addRenderableWidget(new ImageButton(leftPos + 154, topPos + 93, 25, 17, 0, 182, widgetsTexture, 256, 256, this::onFrequencyButtonDown));
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