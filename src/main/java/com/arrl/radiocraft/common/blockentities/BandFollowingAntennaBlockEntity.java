package com.arrl.radiocraft.common.blockentities;

import com.arrl.radiocraft.api.antenna.AntennaTypes;
import com.arrl.radiocraft.api.benetworks.BENetworkObject;
import com.arrl.radiocraft.api.capabilities.IBENetworks;
import com.arrl.radiocraft.common.be_networks.network_objects.BandFollowingAntennaNetworkObject;
import com.arrl.radiocraft.common.radio.antenna.StaticAntenna;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * An antenna block whose network object follows its radio's band (see {@link BandFollowingAntennaNetworkObject}), for
 * antennas that work on HF and VHF alike, well or badly: the ionosonde and the 5-BTV. A radio on either band sees how
 * it matches there.
 */
public class BandFollowingAntennaBlockEntity extends AntennaBlockEntity {

	public BandFollowingAntennaBlockEntity(BlockPos pos, BlockState state) {
		super(pos, state);
	}

	public static <T extends BlockEntity> void tick(Level level, BlockPos pos, BlockState state, T t) {
		AntennaBlockEntity.tick(level, pos, state, t);
		if(level.getGameTime() % 10 == 0 && IBENetworks.getObject(level, pos) instanceof BandFollowingAntennaNetworkObject antenna)
			antenna.followRadioBand(); // An all-band radio can change band at any time.
	}

	@Override
	public BENetworkObject createNetworkObject() {
		BandFollowingAntennaNetworkObject object = new BandFollowingAntennaNetworkObject(level, worldPosition);
		if(level != null && AntennaTypes.match(level, worldPosition) instanceof StaticAntenna<?> antenna)
			object.setAntenna(antenna);
		return object;
	}
}
