package com.arrl.radiocraft.mixin;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.client.RadiocraftClientValues;
import com.arrl.radiocraft.client.VoxStateEnum;
import de.maxhenkel.voicechat.VoicechatClient;
import de.maxhenkel.voicechat.voice.client.MicActivator;
import de.maxhenkel.voicechat.voice.common.Utils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Consumer;

/**
 * Mixin to override the voice activation system in Simple Voice Chat.
 * This allows RadioCraft to control when the microphone should be active based on
 * the radio's VOX mode setting.
 */
@Mixin(value = MicActivator.class, remap = false)
public class MixinVoiceActivation {

    // Shadow the private fields from MicActivator
    @Shadow private boolean activating;
    @Shadow private int deactivationDelay;
    @Shadow private short[] lastBuff;
    @Shadow public void stopActivating() {}

    @Inject(method = "push", at = @At("RETURN"), cancellable = true)
    private void push(short[] audio, Consumer<short[]> audioConsumer, CallbackInfoReturnable<Boolean> cir) {
        boolean consumedAudio = false;
        boolean aboveThreshold = Utils.isAboveThreshold(audio, (Double) VoicechatClient.CLIENT_CONFIG.voiceActivationThreshold.get());
        if (this.activating) {
            if (!aboveThreshold) {
                if (this.deactivationDelay >= (Integer)VoicechatClient.CLIENT_CONFIG.deactivationDelay.get()) {
                    this.stopActivating();
                    RadiocraftClientValues.RADIO_VOX_MODE = VoxStateEnum.INACTIVE;
                    System.out.println(RadiocraftClientValues.RADIO_VOX_MODE);
                } else {
                    audioConsumer.accept(audio);
                    consumedAudio = true;
                    ++this.deactivationDelay;
                }
            } else {
                audioConsumer.accept(audio);
                consumedAudio = true;
            }
        } else if (aboveThreshold) {
            if (this.lastBuff != null) {
                audioConsumer.accept(this.lastBuff);
            }

            audioConsumer.accept(audio);
            consumedAudio = true;
            this.activating = true;
            RadiocraftClientValues.RADIO_VOX_MODE = VoxStateEnum.ACTIVE;
            System.out.println(RadiocraftClientValues.RADIO_VOX_MODE);
        }

        this.lastBuff = consumedAudio ? null : audio;
        cir.setReturnValue(consumedAudio);
    }
}