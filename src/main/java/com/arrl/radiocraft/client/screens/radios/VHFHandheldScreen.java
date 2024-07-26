package com.arrl.radiocraft.client.screens.radios;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.api.capabilities.IVHFHandheldCapability;
import com.arrl.radiocraft.api.capabilities.RadiocraftCapabilities;
import com.arrl.radiocraft.client.RadiocraftClientValues;
import com.arrl.radiocraft.client.screens.widgets.Dial;
import com.arrl.radiocraft.client.screens.widgets.HoldButton;
import com.arrl.radiocraft.client.screens.widgets.ImageButton;
import com.arrl.radiocraft.client.screens.widgets.ToggleButton;
import com.arrl.radiocraft.common.init.RadiocraftPackets;
import com.arrl.radiocraft.common.network.packets.serverbound.SHandheldFrequencyPacket;
import com.arrl.radiocraft.common.network.packets.serverbound.SHandheldPTTPacket;
import com.arrl.radiocraft.common.network.packets.serverbound.SHandheldPowerPacket;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class VHFHandheldScreen extends Screen {

    public static final ResourceLocation TEXTURE = Radiocraft.id("textures/gui/vhf_handheld.png");
    public static final ResourceLocation WIDGETS_TEXTURE = Radiocraft.id("textures/gui/vhf_handheld_widgets.png");

    protected static final int imageWidth = 157;
    protected static final int imageHeight = 248;
    protected int leftPos;
    protected int topPos;

    protected final int index;
    protected final ItemStack item;
    protected final IVHFHandheldCapability cap;

    // This stores the entered digits on the keypad for frequency selection, confirmed by pressing Enter.
    protected String enterBuffer = "";

    public VHFHandheldScreen(int index) {
        super(Component.translatable(Radiocraft.translationKey("screen", "vhf_handheld")));
        this.index = index;
        this.item = Minecraft.getInstance().player.getInventory().getItem(index);
        this.cap = item.getCapability(RadiocraftCapabilities.VHF_HANDHELDS).orElse(null);

        if(this.cap == null) // IntelliJ is lying, this is not always true.
            onClose();

        RadiocraftClientValues.SCREEN_VOICE_ENABLED = true;
    }

    @Override
    protected void init() {
        super.init();
        leftPos = (width - imageWidth) / 2;
        topPos = (height - imageHeight) / 2;

        addRenderableWidget(new ToggleButton(cap.isPowered(), leftPos - 1, topPos + 37, 18, 38, 0, 0, WIDGETS_TEXTURE, 256, 256, this::onPressPower)); // Power
        addRenderableWidget(new HoldButton(leftPos - 1, topPos + 80, 20, 101, 36, 0, WIDGETS_TEXTURE, 256, 256, this::onPressPTT, this::onReleasePTT)); // PTT
        addRenderableWidget(new Dial(leftPos + 66, topPos - 1, 37, 21, 76, 0, WIDGETS_TEXTURE, 256, 256, this::doNothing, this::doNothing)); // Mic gain
        addRenderableWidget(new Dial(leftPos + 111, topPos - 1, 37, 21, 76, 42, WIDGETS_TEXTURE, 256, 256, this::doNothing, this::doNothing)); // Gain
        addRenderableWidget(new ImageButton(leftPos + 106, topPos + 168, 18, 14, 76, 84, WIDGETS_TEXTURE, 256, 256, this::onFrequencyButtonUp)); // Frequency up button
        addRenderableWidget(new ImageButton(leftPos + 126, topPos + 168, 18, 14, 76, 98, WIDGETS_TEXTURE, 256, 256, this::onFrequencyButtonDown)); // Frequency down button
        addRenderableWidget(new ImageButton(leftPos + 31, topPos + 169, 18, 12, 172, 5, WIDGETS_TEXTURE, 256, 256, this::onPressOne)); // 1
        addRenderableWidget(new ImageButton(leftPos + 56, topPos + 169, 18, 12, 172, 75, WIDGETS_TEXTURE, 256, 256, this::onPressTwo)); // 2
        addRenderableWidget(new ImageButton(leftPos + 80, topPos + 169, 18, 12, 171, 146, WIDGETS_TEXTURE, 256, 256, this::onPressThree)); // 3
        addRenderableWidget(new ImageButton(leftPos + 31, topPos + 188, 18, 12, 172, 24, WIDGETS_TEXTURE, 256, 256, this::onPressFour)); // 4
        addRenderableWidget(new ImageButton(leftPos + 56, topPos + 188, 18, 12, 172, 94, WIDGETS_TEXTURE, 256, 256, this::onPressFive)); // 5
        addRenderableWidget(new ImageButton(leftPos + 80, topPos + 188, 18, 12, 221, 106, WIDGETS_TEXTURE, 256, 256, this::onPressSix)); // 6
        addRenderableWidget(new ImageButton(leftPos + 31, topPos + 207, 18, 12, 172, 43, WIDGETS_TEXTURE, 256, 256, this::onPressSeven)); // 7
        addRenderableWidget(new ImageButton(leftPos + 56, topPos + 207, 18, 12, 172, 113, WIDGETS_TEXTURE, 256, 256, this::onPressEight)); // 8
        addRenderableWidget(new ImageButton(leftPos + 80, topPos + 207, 18, 12, 221, 125, WIDGETS_TEXTURE, 256, 256, this::onPressNine)); // 9
        addRenderableWidget(new ImageButton(leftPos + 31, topPos + 226, 18, 12, 172, 62, WIDGETS_TEXTURE, 256, 256, this::onPressStar)); // *
        addRenderableWidget(new ImageButton(leftPos + 56, topPos + 226, 18, 12, 172, 132, WIDGETS_TEXTURE, 256, 256, this::onPressZero)); // 0
        addRenderableWidget(new ImageButton(leftPos + 80, topPos + 226, 18, 12, 221, 144, WIDGETS_TEXTURE, 256, 256, this::onPressPound)); // #
        addRenderableWidget(new ImageButton(leftPos + 104, topPos + 224, 40, 12, 193, 64, WIDGETS_TEXTURE, 256, 256, this::onPressEnter)); // Enter
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
        renderBackground(poseStack);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);

        int edgeSpacingX = (width - imageWidth) / 2;
        int edgeSpacingY = (height - imageHeight) / 2;
        blit(poseStack, edgeSpacingX, edgeSpacingY, 0, 0, imageWidth, imageHeight);


        if(cap.isPowered()) {
            blit(poseStack, leftPos + 32, topPos + 107, 157, 0, 10, 13, 256, 256); // Render power light.
            if (cap.isPTTDown())
                blit(poseStack, leftPos + 44, topPos + 107, 167, 0, 10, 13, 256, 256); // Render power light.

            if (enterBuffer.isEmpty()) {
                float freqMhz = cap.getFrequency() / 1000.0F; // Frequency is in kHz, divide by 1000 to get MHz
                font.draw(poseStack, String.format("%.3f", freqMhz), (leftPos + 82), (topPos + 125), 0xFFFFFF);
            } else {
                int strlen = Math.min(enterBuffer.length(), 7);
                String buffer = enterBuffer.substring(0, strlen);
                if (buffer.length() < 7) {
                    buffer += "_";
                } else {
                    // Indicate end-of-entry, instead of showing what would be shown immediately after pressing the Enter button. Should prevent some confusion.
                    buffer += "#";
                }
                font.draw(poseStack, buffer, (leftPos + 82), (topPos + 125), 0xFFFFFF);
            }
        }

        super.render(poseStack, mouseX, mouseY, partialTicks);
    }

    public static void open(int index) {
        Minecraft.getInstance().setScreen(new VHFHandheldScreen(index));
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        boolean value = super.mouseReleased(mouseX, mouseY, button);

        for(GuiEventListener listener : children()) { // Janky fix to make the dials detect a mouse release which isn't over them, forge allows for a mouse to go down but not back up.
            if(!listener.isMouseOver(mouseX, mouseY)) {
                if(listener instanceof Dial)
                    listener.mouseReleased(mouseX, mouseY, button);
            }
        }

        return value;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if(keyCode == Minecraft.getInstance().options.keyInventory.getKey().getValue()) { // Manually close when E pressed since this isn't a normal inventory screen.
            onClose();
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        super.onClose();
        RadiocraftClientValues.SCREEN_PTT_PRESSED = false; // Make sure to stop recording player's mic when the UI is closed, in case they didn't let go of PTT
        RadiocraftClientValues.SCREEN_VOICE_ENABLED = false;
        RadiocraftClientValues.SCREEN_CW_ENABLED = false;
    }

    /**
     * Callback to do nothing, for readability.
     */
    protected void doNothing(AbstractWidget button) {}

    /**
     * Callback for frequency up buttons.
     */
    protected void onFrequencyButtonUp(Button button) {
        if(cap.isPowered())
            RadiocraftPackets.sendToServer(new SHandheldFrequencyPacket(index, 1)); // Frequency is sync'd back from server as client doesn't know steps.
    }

    /**
     * Callback for frequency down buttons.
     */
    protected void onFrequencyButtonDown(Button button) {
        if(cap.isPowered())
            RadiocraftPackets.sendToServer(new SHandheldFrequencyPacket(index, -1)); // Frequency is sync'd back from server as client doesn't know steps.
    }

    /**
     * Callback for pressing a PTT button.
     */
    protected void onPressPTT(HoldButton button) {
        RadiocraftPackets.sendToServer(new SHandheldPTTPacket(index, true));
        cap.setPTTDown(true);
        RadiocraftClientValues.SCREEN_PTT_PRESSED = true;
    }

    /**
     * Callback for releasing a PTT button.
     */
    protected void onReleasePTT(HoldButton button) {
        RadiocraftPackets.sendToServer(new SHandheldPTTPacket(index, false));
        cap.setPTTDown(false);
        RadiocraftClientValues.SCREEN_PTT_PRESSED = false;
    }

    /**
     * Callback to toggle power on a device.
     */
    protected void onPressPower(ToggleButton button) {
        RadiocraftPackets.sendToServer(new SHandheldPowerPacket(index, !cap.isPowered()));
        cap.setPowered(!cap.isPowered());
        enterBuffer = ""; // Clear the enter buffer, in case the user entered numbers before powering up device.
    }

    protected void onPressOne(Button button) {
        enterBuffer += "1";
    }
    protected void onPressTwo(Button button) {
        enterBuffer += "2";
    }
    protected void onPressThree(Button button) {
        enterBuffer += "3";
    }
    protected void onPressFour(Button button) {
        enterBuffer += "4";
    }
    protected void onPressFive(Button button) {
        enterBuffer += "5";
    }
    protected void onPressSix(Button button) {
        enterBuffer += "6";
    }
    protected void onPressSeven(Button button) {
        enterBuffer += "7";
    }
    protected void onPressEight(Button button) {
        enterBuffer += "8";
    }
    protected void onPressNine(Button button) {
        enterBuffer += "9";
    }
    protected void onPressZero(Button button) {
        enterBuffer += "0";
    }
    protected void onPressStar(Button button) {
        enterBuffer += ".";
    }
    protected void onPressPound(Button button) {
        enterBuffer += "#";
    }

    /**
     * Callback to enter the frequency selection, after pressing 6 digits plus an optional decimal point
     */
    protected void onPressEnter(Button button) {
        // If the device is not powered, do not set a frequency. The buffer is cleared on power up as well.
        if (!cap.isPowered()) {
            return;
        }
        if (enterBuffer.length() == 6) {
            boolean hasOnlyDigits = true;
            for (int x = 0; x < enterBuffer.length(); x++) {
                if (!Character.isDigit(enterBuffer.charAt(x))) {
                    hasOnlyDigits = false;
                }
            }
            if (hasOnlyDigits) {
                String megahertz = enterBuffer.substring(0, 3);
                String kilohertz = enterBuffer.substring(3, 6);
                float frequency = Float.parseFloat(megahertz + "." + kilohertz);
                cap.setFrequency((int)(frequency * 1000)); // Frequency is stored in kHz, convert from MHz.
            }
        }
        if (enterBuffer.length() == 7) {
            boolean isValidEntry = true;
            for (int x = 0; x < enterBuffer.length(); x++) {
                if (x == 3) {
                    // Check that there's a pound or star (decimal point) contained.
                    if (!(enterBuffer.charAt(x) == '#' || enterBuffer.charAt(x) == '.')) {
                        isValidEntry = false;
                    }
                } else if (!Character.isDigit(enterBuffer.charAt(x))) {
                    isValidEntry = false;
                }
            }
            if (isValidEntry) {
                String megahertz = enterBuffer.substring(0, 3);
                // Skip 4, this is where the decimal point is
                String kilohertz = enterBuffer.substring(4, 7);
                float frequency = Float.parseFloat(megahertz + "." + kilohertz);
                cap.setFrequency((int)(frequency * 1000));
            }
        }
        enterBuffer = ""; // If the user presses enter, it will clear the screen and put it back.
    }

}
