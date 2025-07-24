package com.arrl.radiocraft.client.screens.radios;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.api.capabilities.IVHFHandheldCapability;
import com.arrl.radiocraft.client.RadiocraftClientValues;
import com.arrl.radiocraft.client.screens.widgets.*;
import com.arrl.radiocraft.common.capabilities.RadiocraftCapabilities;
import com.arrl.radiocraft.common.network.Serverbound.SHandheldRadioUpdatePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

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

    LedIndicator TX_LED, RX_LED, DATA_LED;

    MeterNeedleIndicator POWER_METER;

    public VHFHandheldScreen(int index) {
        super(Component.translatable(Radiocraft.translationKey("screen", "vhf_handheld")));
        this.index = index;
        this.item = Minecraft.getInstance().player.getInventory().getItem(index);
        this.cap = item.getCapability(RadiocraftCapabilities.VHF_HANDHELDS);

        if(this.cap == null) // IntelliJ is lying, this is not always true.
            onClose();

        RadiocraftClientValues.SCREEN_VOICE_ENABLED = true;
    }

    @Override
    protected void init() {
        super.init();
        leftPos = (width - imageWidth) / 2;
        topPos = (height - imageHeight) / 2;

        TX_LED = new LedIndicator(Component.literal("TX LED"), leftPos+ 32, topPos + 107, 10, 13, 157, 0, TEXTURE, 256, 256);
        RX_LED = new LedIndicator(Component.literal("RX LED"), leftPos + 44, topPos + 107, 10, 13, 167, 0, TEXTURE, 256, 256);
        DATA_LED = new LedIndicator(Component.literal("Data LED"), leftPos + 56, topPos + 107, 10, 13, 167, 0, TEXTURE, 256, 256);

        //     public MeterNeedleIndicator(Component name, MeterNeedleType mnt, int meterDimension, int x, int y, int width, int height, int u, int v, ResourceLocation texture, int textureWidth, int textureHeight) {
        POWER_METER = new MeterNeedleIndicator(Component.literal("Power"), MeterNeedleIndicator.MeterNeedleType.METER_HORIZONTAL, 33, leftPos + 33, topPos + 126, 2, 20, 232, 0, WIDGETS_TEXTURE, 256, 256);

        addRenderableWidget(new ToggleButton(cap.isPowered(), leftPos + 1, topPos + 37, 18, 38, 0, 0, WIDGETS_TEXTURE, 256, 256, this::onPressPower)); // Power
        addRenderableWidget(new HoldButton(leftPos - 1, topPos + 80, 20, 101, 36, 0, WIDGETS_TEXTURE, 256, 256, this::onPressPTT, this::onReleasePTT)); // PTT
        addRenderableWidget(new Dial(leftPos + 66, topPos - 1, 37, 21, 76, 0, WIDGETS_TEXTURE, 256, 256, this::doNothing, this::doNothing)); // Mic gain
        addRenderableWidget(new Dial(leftPos + 111, topPos - 1, 37, 21, 76, 42, WIDGETS_TEXTURE, 256, 256, this::doNothing, this::doNothing)); // Gain
        addRenderableWidget(new HoverableImageButton(leftPos + 105, topPos + 168, 18, 14, 94, 84, 76, 84, WIDGETS_TEXTURE, 256, 256, this::onFrequencyButtonUp)); // Frequency up button
        addRenderableWidget(new HoverableImageButton(leftPos + 125, topPos + 168, 18, 14, 94, 98, 76, 98, WIDGETS_TEXTURE, 256, 256, this::onFrequencyButtonDown)); // Frequency down button
        addRenderableWidget(TX_LED);
        addRenderableWidget(RX_LED);
        addRenderableWidget(DATA_LED);
        addRenderableWidget(POWER_METER);
    }

    @Override
    public void render(@NotNull GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        // I removed the .super() call because for some reason it renders the backgrounds before buttons but after the other rendering of the actual menu.
        // I guess I could just add these items to the renderables list, but then you can't influence the powered state.
        renderBackground(pGuiGraphics, pMouseX, pMouseY, pPartialTick);

        int edgeSpacingX = (width - imageWidth) / 2;
        int edgeSpacingY = (height - imageHeight) / 2;
        pGuiGraphics.blit(TEXTURE, edgeSpacingX, edgeSpacingY, 0, 0, imageWidth, imageHeight);

        /*

        * Power meter shows transmitted power (based on user's voice amplitude?)
        * Data light turns on if there's a data transmission
        * RX light turns on if there's any signal being received (maybe we add squelch?)
        * Power meter shows receive strength if there's any signal being received (ignoring squelch)

         */

        if (cap.isPowered() && cap.isPTTDown()) {
            POWER_METER.setValue(1.0);
        } else if (cap.isPowered()) {
            POWER_METER.setValue(Math.random() / 10.0);
        } else {
            POWER_METER.setValue(0.0);
        }

        if (cap.isPowered()) {
            pGuiGraphics.drawString(this.font, cap.getFrequencyKiloHertz() / 1000.0 + " MHz", leftPos + 80,  topPos + 126, 0xFFFFFF);
        }

        TX_LED.setIsOn(cap.isPowered() && cap.isPTTDown());
        RX_LED.setIsOn(cap.isPowered() && cap.getReceiveIndicator());
        DATA_LED.setIsOn(false); // TBI

        cap.getFrequencyKiloHertz();

        for (Renderable renderable : this.renderables) {
            renderable.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        }
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
        if(cap.isPowered());
            //RadiocraftPackets.sendToServer(new SHandheldFrequencyPacket(index, 1)); // Frequency is sync'd back from server as client doesn't know steps.
        //TODO frequency stepping send to server
    }

    /**
     * Callback for frequency down buttons.
     */
    protected void onFrequencyButtonDown(Button button) {
        if(cap.isPowered());
            //RadiocraftPackets.sendToServer(new SHandheldFrequencyPacket(index, -1)); // Frequency is sync'd back from server as client doesn't know steps.
        //TODO frequency stepping send to server
    }

    /**
     * Callback for pressing a PTT button.
     */
    protected void onPressPTT(HoldButton button) {
        //RadiocraftPackets.sendToServer(new SHandheldPTTPacket(index, true));
        cap.setPTTDown(true);
        RadiocraftClientValues.SCREEN_PTT_PRESSED = true;
        updateServer();
    }

    /**
     * Callback for releasing a PTT button.
     */
    protected void onReleasePTT(HoldButton button) {
        //RadiocraftPackets.sendToServer(new SHandheldPTTPacket(index, false));
        cap.setPTTDown(false);
        RadiocraftClientValues.SCREEN_PTT_PRESSED = false;
        updateServer();
    }

    /**
     * Callback to toggle power on a device.
     */
    protected void onPressPower(ToggleButton button) {
        //RadiocraftPackets.sendToServer(new SHandheldPowerPacket(index, !cap.isPowered()));
        cap.setPowered(!cap.isPowered());
        updateServer();
    }

    protected void updateServer(){
        PacketDistributor.sendToServer(new SHandheldRadioUpdatePacket(index, cap.isPowered(), cap.isPTTDown(), cap.getFrequencyKiloHertz())); //TODO frequency stepping is server side, change to indicate increments
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
