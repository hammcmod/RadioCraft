package com.arrl.radiocraft.common.be_networks.network_objects;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.api.benetworks.PowerNetworkObject;
import com.arrl.radiocraft.common.be_networks.IPassThroughNetworkObject;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/**
 * The wire junction: a box where power wire of any colour meets. The power walk passes through it
 * ({@link IPassThroughNetworkObject}), so every power wire touching it is one circuit, whatever its colour. Coax does
 * not join it. It stores, draws and gives nothing.
 */
public class WireJunctionNetworkObject extends PowerNetworkObject implements IPassThroughNetworkObject {

	public static final ResourceLocation TYPE = Radiocraft.id("wire_junction");

	public WireJunctionNetworkObject(Level level, BlockPos pos) {
		super(level, pos, 0, 0, 0);
	}

	@Override
	public boolean isIndirectConsumer() {
		return false;
	}

	@Override
	public boolean isDirectConsumer() {
		return false;
	}

	@Override
	public ResourceLocation getType() {
		return TYPE;
	}
}
