package com.arrl.radiocraft.mixin;

import com.arrl.radiocraft.client.RadiocraftClientValues;
import de.maxhenkel.voicechat.voice.client.PTTKeyHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PTTKeyHandler.class)
public class MixinPTTKeyHandler {

	@Inject(method="isPTTDown", at=@At("HEAD"), cancellable = true, remap = false)
	private void isPTTDown(CallbackInfoReturnable<Boolean> cir) {
		if(RadiocraftClientValues.SCREEN_PTT_PRESSED)
			cir.setReturnValue(true);
	}

	@Inject(method="isAnyDown", at=@At("HEAD"), cancellable = true, remap = false)
	private void isAnyDown(CallbackInfoReturnable<Boolean> cir) {
		if(RadiocraftClientValues.SCREEN_PTT_PRESSED)
			cir.setReturnValue(true);
	}

}
