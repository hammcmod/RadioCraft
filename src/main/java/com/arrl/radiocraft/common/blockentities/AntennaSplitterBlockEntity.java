package com.arrl.radiocraft.common.blockentities;

import com.arrl.radiocraft.api.benetworks.BENetworkObject;
import com.arrl.radiocraft.api.benetworks.INetworkObjectProvider;
import com.arrl.radiocraft.common.be_networks.network_objects.AntennaSplitterNetworkObject;
import com.arrl.radiocraft.common.init.RadiocraftBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** The antenna splitter's block entity: it only puts the splitter on the coax ({@link AntennaSplitterNetworkObject}). */
public class AntennaSplitterBlockEntity extends BlockEntity implements INetworkObjectProvider {

	public AntennaSplitterBlockEntity(BlockPos pos, BlockState state) {
		super(RadiocraftBlockEntities.ANTENNA_SPLITTER.get(), pos, state);
	}

	@Override
	public BENetworkObject createNetworkObject() {
		return new AntennaSplitterNetworkObject(level, worldPosition);
	}

	@Override
	public void onLoad() {
		super.onLoad();
		if(level != null && !level.isClientSide())
			getNetworkObject(level, worldPosition); // Makes the object and joins the coax around it.
	}
}
