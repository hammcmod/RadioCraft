package com.arrl.radiocraft.common.blockentities;

import com.arrl.radiocraft.api.antenna.AntennaTypes;
import com.arrl.radiocraft.api.benetworks.BENetworkObject;
import com.arrl.radiocraft.api.capabilities.IBENetworks;
import com.arrl.radiocraft.common.be_networks.network_objects.DummyLoadNetworkObject;
import com.arrl.radiocraft.common.radio.antenna.StaticAntenna;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** A dummy load: an antenna block whose network object follows its radio's band (see {@link DummyLoadNetworkObject}). */
public class DummyLoadBlockEntity extends AntennaBlockEntity {

	public DummyLoadBlockEntity(BlockPos pos, BlockState state) {
		super(pos, state);
	}

	public static <T extends BlockEntity> void tick(Level level, BlockPos pos, BlockState state, T t) {
		AntennaBlockEntity.tick(level, pos, state, t);
		if(level.getGameTime() % 10 == 0 && IBENetworks.getObject(level, pos) instanceof DummyLoadNetworkObject load)
			load.followRadioBand(); // An all-band radio can change band at any time.
	}

	@Override
	public BENetworkObject createNetworkObject() {
		DummyLoadNetworkObject load = new DummyLoadNetworkObject(level, worldPosition);
		if(level != null && AntennaTypes.match(level, worldPosition) instanceof StaticAntenna<?> antenna)
			load.setAntenna(antenna);
		return load;
	}
}
