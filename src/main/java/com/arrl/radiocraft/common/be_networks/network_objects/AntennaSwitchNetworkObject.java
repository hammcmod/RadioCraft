package com.arrl.radiocraft.common.be_networks.network_objects;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.api.benetworks.BENetworkObject;
import com.arrl.radiocraft.common.be_networks.ICoaxPassThrough;
import com.arrl.radiocraft.common.blocks.antennas.AntennaSwitchBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The antenna switch on the coax: one radio, three antennas, one at a time. The coax walk passes through it only
 * between its input (the side facing the radio's coax) and the output its knob is turned to
 * ({@link #passesSide}); coax on the other two outputs ends at it, so their antennas are on runs with no radio.
 */
public class AntennaSwitchNetworkObject extends BENetworkObject implements ICoaxPassThrough {

	public static final ResourceLocation TYPE = Radiocraft.id("antenna_switch");

	public AntennaSwitchNetworkObject(Level level, BlockPos pos) {
		super(level, pos);
	}

	/** The coax only: power wire does not run through it. */
	@Override
	public boolean passesThrough(boolean power) {
		return !power;
	}

	@Override
	public boolean passesSide(boolean power, Direction side) {
		BlockState state = level.getBlockState(pos);
		if(power || !(state.getBlock() instanceof AntennaSwitchBlock))
			return false;
		return side == AntennaSwitchBlock.input(state) || side == AntennaSwitchBlock.output(state);
	}

	@Override
	public ResourceLocation getType() {
		return TYPE;
	}
}
