package com.arrl.radiocraft.mixin;

import com.arrl.radiocraft.client.RadiocraftClientValues;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/*

TODO: Not sure why we needed a mixin for this. I think that there may be better ways to handle this.

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
*/