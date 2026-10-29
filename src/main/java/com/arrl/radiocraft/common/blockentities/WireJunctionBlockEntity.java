package com.arrl.radiocraft.common.blockentities;

import com.arrl.radiocraft.api.benetworks.BENetworkObject;
import com.arrl.radiocraft.api.benetworks.INetworkObjectProvider;
import com.arrl.radiocraft.common.be_networks.network_objects.WireJunctionNetworkObject;
import com.arrl.radiocraft.common.init.RadiocraftBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** The wire junction's block entity: it only puts the junction on the wire ({@link WireJunctionNetworkObject}). */
public class WireJunctionBlockEntity extends BlockEntity implements INetworkObjectProvider {

	public WireJunctionBlockEntity(BlockPos pos, BlockState state) {
		super(RadiocraftBlockEntities.WIRE_JUNCTION.get(), pos, state);
	}

	@Override
	public BENetworkObject createNetworkObject() {
		return new WireJunctionNetworkObject(level, worldPosition);
	}

	@Override
	public void onLoad() {
		super.onLoad();
		if(level != null && !level.isClientSide())
			getNetworkObject(level, worldPosition); // Makes the object and joins the wires around it.
	}
}
