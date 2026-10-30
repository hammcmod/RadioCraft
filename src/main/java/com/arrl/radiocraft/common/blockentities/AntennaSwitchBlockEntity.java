package com.arrl.radiocraft.common.blockentities;

import com.arrl.radiocraft.api.benetworks.BENetwork;
import com.arrl.radiocraft.api.benetworks.BENetworkObject;
import com.arrl.radiocraft.api.benetworks.INetworkObjectProvider;
import com.arrl.radiocraft.api.benetworks.PowerBENetwork;
import com.arrl.radiocraft.api.capabilities.IBENetworks;
import com.arrl.radiocraft.common.be_networks.ICoaxNetworkObject;
import com.arrl.radiocraft.common.be_networks.WireUtils;
import com.arrl.radiocraft.common.be_networks.network_objects.AntennaSwitchNetworkObject;
import com.arrl.radiocraft.common.be_networks.network_objects.DirectionFinderModuleNetworkObject;
import com.arrl.radiocraft.common.be_networks.network_objects.RadioNetworkObject;
import com.arrl.radiocraft.common.blocks.Strand;
import com.arrl.radiocraft.common.blocks.WireBlock;
import com.arrl.radiocraft.common.blocks.antennas.AntennaSwitchBlock;
import com.arrl.radiocraft.common.init.RadiocraftBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * The antenna switch's block entity: puts the switch on the coax ({@link AntennaSwitchNetworkObject}), turns its input
 * to face the coax that leads to a radio (maintainer, 2026-10-30: no setting it up by hand), and rebuilds the coax
 * networks around it when the input or the knob changes.
 */
public class AntennaSwitchBlockEntity extends BlockEntity implements INetworkObjectProvider {

	/** How often it looks for the radio's side, in ticks. */
	public static final int CHECK_TICKS = 20;

	public AntennaSwitchBlockEntity(BlockPos pos, BlockState state) {
		super(RadiocraftBlockEntities.ANTENNA_SWITCH.get(), pos, state);
	}

	@Override
	public BENetworkObject createNetworkObject() {
		return new AntennaSwitchNetworkObject(level, worldPosition);
	}

	@Override
	public void onLoad() {
		super.onLoad();
		if(level != null && !level.isClientSide())
			getNetworkObject(level, worldPosition); // Makes the object and joins the coax around it.
	}

	public static void tick(Level level, BlockPos pos, BlockState state, AntennaSwitchBlockEntity entity) {
		if(level instanceof ServerLevel serverLevel && (level.getGameTime() + pos.asLong()) % CHECK_TICKS == 0L)
			entity.faceTheRadio(serverLevel);
	}

	/** Turns the input to the side whose coax leads to a radio, if that is not the side it already faces. */
	public void faceTheRadio(ServerLevel level) {
		BlockState state = getBlockState();
		if(!(state.getBlock() instanceof AntennaSwitchBlock))
			return;
		Direction input = AntennaSwitchBlock.input(state);
		Direction radio = radioSide(level, worldPosition, input);
		if(radio == input)
			return;
		level.setBlock(worldPosition, state.setValue(AntennaSwitchBlock.FACING, radio), Block.UPDATE_ALL);
		rewire(level, worldPosition);
	}

	/**
	 * The side of the switch at {@code pos} whose coax run (not counting the way through the switch) reaches a radio:
	 * {@code current} if it does, else the first that does, else {@code current}.
	 */
	public static Direction radioSide(Level level, BlockPos pos, Direction current) {
		Set<WireBlock> coax = Set.of(WireUtils.coaxWires());
		Direction found = null;
		for(Map.Entry<BlockPos, Direction> link : WireUtils.wireLinksOf(level, pos, coax).entrySet()) {
			Direction side = link.getValue();
			if(!side.getAxis().isHorizontal())
				continue;
			Set<Strand> blacklist = new HashSet<>();
			blacklist.add(Strand.device(pos)); // Not back through the switch itself.
			Map<BENetworkObject, Direction> reached = WireUtils.enter(new HashMap<>(), blacklist, level, link.getKey(), null,
					object -> object instanceof ICoaxNetworkObject, coax);
			boolean radio = reached.keySet().stream().anyMatch(object -> object instanceof RadioNetworkObject && !(object instanceof DirectionFinderModuleNetworkObject));
			if(!radio)
				continue;
			if(side == current)
				return current;
			if(found == null)
				found = side;
		}
		return found == null ? current : found;
	}

	/**
	 * Rebuilds the coax networks the switch at {@code pos} is part of, after its input or knob changed: everything on
	 * them comes off its coax networks and joins again, so the walk (which passes through the switch only between the
	 * input and the selected output) sorts out which antenna the radio now has.
	 */
	public static void rewire(ServerLevel level, BlockPos pos) {
		BENetworkObject self = IBENetworks.getObject(level, pos);
		if(self == null)
			return;
		Set<BENetworkObject> members = new LinkedHashSet<>();
		members.add(self);
		for(BENetwork network : self.getNetworks().values())
			if(!(network instanceof PowerBENetwork))
				members.addAll(network.getNetworkObjects());
		for(BENetworkObject member : members)
			member.clearNetworks(false);
		for(BENetworkObject member : members)
			WireUtils.connectToWires(level, member.getPos());
	}
}
