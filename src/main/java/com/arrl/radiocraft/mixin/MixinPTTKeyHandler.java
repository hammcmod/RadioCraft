package com.arrl.radiocraft.mixin;

import com.arrl.radiocraft.client.RadiocraftClientValues;
import de.maxhenkel.voicechat.voice.client.PTTKeyHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/*
TODO: Fix the mixin. This is required to be able to use the PTT functionality without requiring an actual key press
This is used in the GUI pane for the radios; a PTT button on the GUI screen can be clicked in place of using the PTT key.

The source code from the simple voice chat mod:
https://github.com/henkelmax/simple-voice-chat/blob/1.21.3/common/src/main/java/de/maxhenkel/voicechat/voice/client/PTTKeyHandler.java

Reference the old Gradle here for how mixin was used:
https://github.com/hammcmod/RadioCraft/blob/dev/build.gradle
*/


@Mixin(PTTKeyHandler.class)
public class MixinPTTKeyHandler {

	@Inject(method="isPTTDown", at=@At("HEAD"), cancellable = true, remap = false)
	private void isPTTDown(CallbackInfoReturnable<Boolean> cir) {
		if(RadiocraftClientValues.SCREEN_PTT_PRESSED && RadiocraftClientValues.SCREEN_VOICE_ENABLED)
			cir.setReturnValue(true);
	}

	@Inject(method="isAnyDown", at=@At("HEAD"), cancellable = true, remap = false)
	private void isAnyDown(CallbackInfoReturnable<Boolean> cir) {
		if(RadiocraftClientValues.SCREEN_PTT_PRESSED && RadiocraftClientValues.SCREEN_VOICE_ENABLED)
			cir.setReturnValue(true);
	}

}