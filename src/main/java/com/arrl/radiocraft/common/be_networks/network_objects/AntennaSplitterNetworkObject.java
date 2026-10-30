package com.arrl.radiocraft.common.be_networks.network_objects;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.api.benetworks.BENetworkObject;
import com.arrl.radiocraft.common.be_networks.ICoaxPassThrough;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/**
 * The receive-only antenna splitter (maintainer, 2026-10-27): one antenna feeding several receivers. The coax walk
 * passes through it ({@link ICoaxPassThrough}), so the antenna and every radio on its other sides share one coax run,
 * and {@link AntennaNetworkObject} then hands what the antenna hears to all of them (each a share of it) instead of
 * calling the radios overdrawn. Nothing transmits through it.
 */
public class AntennaSplitterNetworkObject extends BENetworkObject implements ICoaxPassThrough {

	public static final ResourceLocation TYPE = Radiocraft.id("antenna_splitter");

	public AntennaSplitterNetworkObject(Level level, BlockPos pos) {
		super(level, pos);
	}

	/** The coax only: power wire does not run through it. */
	@Override
	public boolean passesThrough(boolean power) {
		return !power;
	}

	@Override
	public ResourceLocation getType() {
		return TYPE;
	}
}
