package com.arrl.radiocraft.common.blockentities;

import com.arrl.radiocraft.RadiocraftServerConfig;
import com.arrl.radiocraft.api.antenna.AntennaTypes;
import com.arrl.radiocraft.api.antenna.IAntenna;
import com.arrl.radiocraft.api.benetworks.BENetworkObject;
import com.arrl.radiocraft.api.benetworks.INetworkObjectProvider;
import com.arrl.radiocraft.api.capabilities.IBENetworks;
import com.arrl.radiocraft.common.be_networks.network_objects.AntennaNetworkObject;
import com.arrl.radiocraft.common.init.RadiocraftBlockEntities;
import com.arrl.radiocraft.common.radio.antenna.StaticAntenna;
import com.arrl.radiocraft.common.radio.antenna.networks.AntennaNetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Shared {@link BlockEntity} for all blocks which act as an antenna-- used for processing packets/sending them to the
 * network, receiving packets from the network & scheduling antenna update checks.
 */
public class AntennaBlockEntity extends BlockEntity implements INetworkObjectProvider {

	protected ResourceLocation networkId;

	// Cache the results of antenna/radio updates and only update them at delays, cutting down on resource usage.
	private int antennaCheckCooldown = -1;

	public AntennaBlockEntity(BlockPos pos, BlockState state) {
		this(pos, state, AntennaNetworkManager.HF_ID);
	}

	public AntennaBlockEntity(BlockPos pos, BlockState state, ResourceLocation networkId) {
		super(RadiocraftBlockEntities.ANTENNA.get(), pos, state);
		this.networkId = networkId;
	}
	/**
	 * Updates the antenna at this position in the world
	 */
	private void updateAntenna() {
		IAntenna a = AntennaTypes.match(level, worldPosition);
		if(a instanceof StaticAntenna<?> newAntenna)
			((AntennaNetworkObject)IBENetworks.getObject(level, worldPosition)).setAntenna(newAntenna);
	}

	/**
	 * Reset the antenna check cooldown-- used every time a block is placed "on" the antenna.
	 */
	public void markAntennaChanged() {
		antennaCheckCooldown = RadiocraftServerConfig.ANTENNA_UPDATE_DELAY.get() * 20;
	}

	public static <T extends BlockEntity> void tick(Level level, BlockPos pos, BlockState state, T t) {
		if(t instanceof AntennaBlockEntity be) {
			if(be.antennaCheckCooldown-- == 0)
				be.updateAntenna();
		}
	}

	@Override
	protected void saveAdditional(CompoundTag nbt) {
		super.saveAdditional(nbt);
		nbt.putInt("checkCooldown", Math.max(antennaCheckCooldown, -1));
	}

	@Override
	public void load(CompoundTag nbt) {
		super.load(nbt);
		antennaCheckCooldown = nbt.getInt("checkCooldown");
	}

	@Override
	public void onLoad() {
		super.onLoad();
		if(!level.isClientSide())
			getNetworkObject(level, worldPosition); // This forces the network object to get initialised.
	}

	@Override
	public BENetworkObject createNetworkObject() {
		return new AntennaNetworkObject(level, worldPosition, networkId);
	}

}
