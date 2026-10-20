package com.arrl.radiocraft.mixin;

import com.arrl.radiocraft.common.radio.rfi.RfSignals;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RedStoneWireBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Lets a transmitting station induce a redstone level in nearby dust (see {@link RfSignals}). Dust recomputes its
 * power from its neighbours on every update, so this is the one place a level can be added that survives: the dust
 * settles at the larger of what really drives it and what the transmitter induces.
 * <p>
 * {@code require = 0}: if a Minecraft update moves or renames the target, the injection is skipped with a warning
 * instead of refusing to start the game. Dust then ignores transmitters; nothing else is affected.
 */
@Mixin(RedStoneWireBlock.class)
public abstract class MixinRedStoneWireBlock {

	@Inject(method = "calculateTargetStrength", at = @At("RETURN"), cancellable = true, require = 0)
	private void radiocraft$induceRfSignal(Level level, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
		int induced = RfSignals.get(level, pos);
		if(induced > cir.getReturnValueI())
			cir.setReturnValue(induced);
	}
}
