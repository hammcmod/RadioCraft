package com.arrl.radiocraft.compat.anotherweathermod;

import com.arrl.radiocraft.RadiocraftServerConfig;
import com.arrl.radiocraft.common.init.RadiocraftTags;
import com.itcmd.anotherweathermod.api.event.WindDamageEvent;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Storm winds bring antennas down: when Another Weather Mod's wind picks an antenna part or a wire holder, it may
 * blow it over (dropped as an item, so the antenna has to be put back up). The chance grows from nothing at
 * {@link #FROM} metres per second (a hurricane's outer winds) to {@link #MAX_CHANCE} at {@link #FULL}.
 */
final class AwmWindDamage {

	static final double FROM = 33;
	static final double FULL = 60;
	static final double MAX_CHANCE = 0.3;

	private AwmWindDamage() {
	}

	static void onWindDamage(WindDamageEvent event) {
		BlockState state = event.getState();
		if(!state.is(RadiocraftTags.Blocks.ANTENNA_BLOCKS) && !state.is(RadiocraftTags.Blocks.ANTENNA_WIRE_HOLDERS))
			return;
		event.setCanceled(true); // Ours to decide, whatever tag the block is also in.
		if(!RadiocraftServerConfig.WIND_ANTENNA_DAMAGE_ENABLED.get())
			return;
		double chance = Math.min(1, Math.max(0, (event.getWindSpeed() - FROM) / (FULL - FROM))) * MAX_CHANCE;
		if(event.getLevel().random.nextDouble() < chance)
			event.getLevel().destroyBlock(event.getPos(), true);
	}
}
