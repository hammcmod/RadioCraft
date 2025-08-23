package com.arrl.radiocraft.client.screens.radios;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.RadiocraftServerConfig;
import com.arrl.radiocraft.api.capabilities.IVHFHandheldCapability;
import com.arrl.radiocraft.client.RadiocraftClientValues;
import com.arrl.radiocraft.client.screens.widgets.*;
import com.arrl.radiocraft.common.capabilities.RadiocraftCapabilities;
import com.arrl.radiocraft.common.network.serverbound.SHandheldRadioUpdatePacket;
import com.arrl.radiocraft.common.radio.Band;
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
import org.jetbrains.annotations.NotNull;

public class VHFHandheldScreen extends Screen {

    public static final ResourceLocation TEXTURE = Radiocraft.id("textures/gui/vhf_handheld.png");
    public static final ResourceLocation WIDGETS_TEXTURE = Radiocraft.id("textures/gui/vhf_handheld_widgets.png");

    protected static final int imageWidth = 157;
    protected static final int imageHeight = 248;
    protected int leftPos;
    protected int topPos;

    protected final int index;
    protected ItemStack item;
    protected IVHFHandheldCapability cap;

    protected static int FREQUENCY_ENTERING_TIMEOUT = 7000; //in millis

    protected int enteredFrequency=0;
    protected int curDigit=0;
    protected long millisOfLastFrequency=0;

    protected MenuState menuState = MenuState.DEFAULT;

    LedIndicator TX_LED, RX_LED, DATA_LED;

    MeterNeedleIndicator POWER_METER;

    public VHFHandheldScreen(int index) {
        super(Component.translatable(Radiocraft.translationKey("screen", "vhf_handheld")));
        this.index = index;
        updateCap();

        if(this.cap == null) // IntelliJ is lying, this is not always true.
            onClose();

        RadiocraftClientValues.SCREEN_VOICE_ENABLED = true;
    }

    protected void updateCap(){

        if(Minecraft.getInstance().player == null) return; // just to get rid of IDE warning, if this happens we have bigger fish to fry

        this.item = Minecraft.getInstance().player.getInventory().getItem(index);
        this.cap = item.getCapability(RadiocraftCapabilities.VHF_HANDHELDS);

        if(cap == null) onClose();
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
        //number buttons
        addRenderableWidget(new HoverableImageButton(leftPos + 54, topPos + 225, 20, 14, 172, 0, 152, 0, WIDGETS_TEXTURE, 256, 256, (b) -> this.onNum(0)));
        for(int i=1; i<10; i++) {
            final int num = i;
            addRenderableWidget(new HoverableImageButton(leftPos + 29 + ((i-1)%3)*25, topPos + 168 + ((i-1)/3)*19, 20, 14, 172, i*14, 152, i*14, WIDGETS_TEXTURE, 256, 256, (b) -> this.onNum(num)));
        }

        addRenderableWidget(new HoverableImageButton(leftPos + 103, topPos + 225, 42, 14, 192, 112, 192, 42, WIDGETS_TEXTURE, 256, 256, this::onPressEnter));

        addRenderableWidget(TX_LED);
        addRenderableWidget(RX_LED);
        addRenderableWidget(DATA_LED);
        addRenderableWidget(POWER_METER);
    }

    protected enum MenuState{
        DEFAULT,
        SET_FREQ
    }

    @Override
    public void render(@NotNull GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        // I removed the .super() call because for some reason it renders the backgrounds before buttons but after the other rendering of the actual menu.
        // I guess I could just add these items to the renderables list, but then you can't influence the powered state.

        updateCap();

        if(cap == null) return;

        if(menuState == MenuState.SET_FREQ && System.currentTimeMillis() - millisOfLastFrequency > FREQUENCY_ENTERING_TIMEOUT) {
            curDigit = 0;
            enteredFrequency = 0;
            millisOfLastFrequency = 0;
            menuState = MenuState.DEFAULT;
        }

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
            if(cap.getReceiveStrength() <= 0f) {
                POWER_METER.setValue(Math.random() / 10.0);
            } else {
                POWER_METER.setValue(Math.log10(cap.getReceiveStrength() / 5f));
                System.out.println(cap.getReceiveStrength() + " " + Math.log10(cap.getReceiveStrength() / 5f));
            }
        } else {
            POWER_METER.setValue(0.0);
        }

        if (cap.isPowered()) {
            switch (menuState) {
                case DEFAULT:
                    pGuiGraphics.drawString(this.font, String.format("%03.3f MHz", cap.getFrequencyKiloHertz() / 1000.0f), leftPos + 80, topPos + 119, 0xFFFFFF);

                    break;
                case SET_FREQ:
                    pGuiGraphics.drawString(this.font, "Set Freq", leftPos + 80, topPos + 119, 0xFFFFFF);
                    pGuiGraphics.drawString(this.font, String.format("%03.3f MHz", enteredFrequency / 1000.0f), leftPos + 80, topPos + 133, 0xFFFFFF);
                    break;
            }
        }

        TX_LED.setIsOn(cap.isPowered() && cap.isPTTDown());
        RX_LED.setIsOn(cap.isPowered() && cap.getReceiveStrength() > 0);
        DATA_LED.setIsOn(false); // TBI

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
        if(cap.isPTTDown()) {
            cap.setPTTDown(false);
            updateServer();
        }
    }

    /**
     * Callback to do nothing, for readability.
     */
    protected void doNothing(AbstractWidget button) {}

    private final int[] powLookup= {
            100_000,
            10_000,
            1_000,
            100,
            10,
            1
    };


    /**
     * callback for number buttons
     * @param num - which digit was pressed
     */
    protected void onNum(int num){
        if(!cap.isPowered()) return;
        switch (menuState) {
            case DEFAULT:
                menuState = MenuState.SET_FREQ;
            case SET_FREQ:
                if (curDigit >= 6) break;
                millisOfLastFrequency = System.currentTimeMillis();
                enteredFrequency += powLookup[curDigit++] * num;
                break;
            default:
                break;
        }
    }

    /**
     * Callback for pressing enter
     */
    private void onPressEnter(Button button) {
        if(!cap.isPowered()) return;
        if(menuState == MenuState.SET_FREQ) {
            if(!cap.isPowered()) return;
            if(enteredFrequency >= Band.getBand(2).minFrequency() && enteredFrequency <= Band.getBand(2).maxFrequency()) {
                cap.setFrequencyKiloHertz(
                        enteredFrequency
                );
            }
            updateServer();
            curDigit = 0;
            enteredFrequency = 0;
            millisOfLastFrequency = 0;
            menuState = MenuState.DEFAULT;
        }
    }

    /**
     * Callback for frequency up buttons.
     */
    protected void onFrequencyButtonUp(Button button) {
        if(!cap.isPowered()) return;
        cap.setFrequencyKiloHertz(
                Math.min( //ServerConfig is synced on game join, so no further checking is necessary
                        cap.getFrequencyKiloHertz() + RadiocraftServerConfig.VHF_FREQUENCY_STEP.get(),
                        Band.getBand(2).maxFrequency()
                )
        );
        updateServer();
    }

    /**
     * Callback for frequency down buttons.
     */
    protected void onFrequencyButtonDown(Button button) {
        if(!cap.isPowered()) return;
        cap.setFrequencyKiloHertz(
                Math.max(  //ServerConfig is synced on game join, so no further checking is necessary
                        cap.getFrequencyKiloHertz() - RadiocraftServerConfig.VHF_FREQUENCY_STEP.get(),
                        Band.getBand(2).minFrequency()
                )
        );
        updateServer();
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
        SHandheldRadioUpdatePacket.updateServer(index, cap);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
