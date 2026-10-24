package com.arrl.radiocraft.mixin;

import com.arrl.radiocraft.common.radio.antenna.LightningRodAntennas;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Antennas draw lightning (see {@link LightningRodAntennas#redirect}): where no lightning rod claimed a strike, it may
 * land on a nearby antenna instead. {@code require = 0}: if an update moves the target, antennas simply stop drawing
 * strikes.
 */
@Mixin(ServerLevel.class)
public abstract class MixinServerLevel {

	@Inject(method = "findLightningTargetAround", at = @At("RETURN"), cancellable = true, require = 0)
	private void radiocraft$antennasDrawLightning(BlockPos pos, CallbackInfoReturnable<BlockPos> cir) {
		ServerLevel level = (ServerLevel)(Object)this;
		BlockPos target = cir.getReturnValue();
		if(target == null || level.getBlockState(target.below()).is(Blocks.LIGHTNING_ROD))
			return; // A lightning rod has it.
		BlockPos antenna = LightningRodAntennas.redirect(level, target, level.random);
		if(antenna != target)
			cir.setReturnValue(antenna);
	}
}
