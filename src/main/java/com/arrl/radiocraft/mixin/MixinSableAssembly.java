package com.arrl.radiocraft.mixin;

import com.arrl.radiocraft.compat.sable.SableAssembly;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Sable (optional): once Sable has moved the blocks of a new sub-level into its plot, carry the antenna wires strung
 * between them too ({@link SableAssembly#carryWires}). {@link RadiocraftMixinPlugin} lists this mixin only when Sable
 * is installed; {@code require = 0}: if a Sable update moves the method, wires are simply left behind.
 */
@Pseudo
@Mixin(targets = "dev.ryanhcode.sable.api.SubLevelAssemblyHelper", remap = false)
public abstract class MixinSableAssembly {

	@Inject(method = "moveBlocks", at = @At("TAIL"), require = 0, remap = false)
	private static void radiocraft$carryAntennaWires(ServerLevel level, SubLevelAssemblyHelper.AssemblyTransform transform, Iterable<BlockPos> blocks, CallbackInfo ci) {
		SableAssembly.carryWires(level, transform::apply, blocks);
	}
}
