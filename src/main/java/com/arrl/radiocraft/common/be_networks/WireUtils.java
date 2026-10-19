package com.arrl.radiocraft.common.be_networks;

import com.arrl.radiocraft.api.benetworks.BENetwork;
import com.arrl.radiocraft.api.benetworks.BENetworkObject;
import com.arrl.radiocraft.api.benetworks.BENetworkRegistry;
import com.arrl.radiocraft.api.benetworks.PowerBENetwork;
import com.arrl.radiocraft.api.benetworks.PowerNetworkObject;
import com.arrl.radiocraft.api.capabilities.IBENetworks;
import com.arrl.radiocraft.common.blocks.WireBlock;
import com.arrl.radiocraft.common.blocks.radios.RadioBlock;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.init.RadiocraftTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class WireUtils {

	/** The wire blocks a power network can run through. */
	public static WireBlock[] powerWires() {
		return new WireBlock[] { RadiocraftBlocks.WIRE.get(), (WireBlock)RadiocraftBlocks.WATERPROOF_WIRE.get() };
	}

	/** The wire blocks a coax network can run through. */
	public static WireBlock[] coaxWires() {
		return new WireBlock[] { RadiocraftBlocks.COAX_WIRE.get() };
	}

	/**
	 * Merges all connections to a wire into a single network, or creates a new one.
	 * @param level The {@link Level} to check.
	 * @param pos The {@link BlockPos} to merge from.
	 * @param connection {@link Predicate} determining what counts as a valid connection.
	 * @param wires List of valid {@link WireBlock}s
	 */
	public static void mergeNetworks(Level level, BlockPos pos, Predicate<BENetworkObject> connection, Supplier<BENetwork> fallbackSupplier, WireBlock... wires) {
		Map<BENetworkObject, Direction> connections = getConnections(level, pos, connection, wires);
		if(!connections.isEmpty())
			joinAll(level, connections, fallbackSupplier);
	}

	/**
	 * Puts every object in {@code connections} on one shared network: the networks they already have (on the given
	 * sides) are merged into a new one, and objects without a network on that side are added to it. Objects that are
	 * all already on the same single network are left alone.
	 *
	 * @param level The {@link Level} the objects are in.
	 * @param connections Objects to join, each with the side of that object the wire run touches.
	 * @param fallbackSupplier Creates the new network if none of the objects had one.
	 * @return The network every object is now on.
	 */
	public static BENetwork joinAll(Level level, Map<BENetworkObject, Direction> connections, Supplier<BENetwork> fallbackSupplier) {
		Set<BENetwork> existingNetworks = new HashSet<>();
		Set<BENetworkObject> nullItems = new HashSet<>(); // These network objects don't have a network on that side.

		for(BENetworkObject networkObject : connections.keySet()) {
			BENetwork network = networkObject.getNetwork(connections.get(networkObject));

			if(network == null)
				nullItems.add(networkObject);
			else
				existingNetworks.add(network);
		}

		if(existingNetworks.size() == 1 && nullItems.isEmpty())
			return existingNetworks.iterator().next(); // Already one network, nothing to merge.

		BENetwork newNetwork = BENetwork.merge(existingNetworks, fallbackSupplier, level);
		for(BENetworkObject networkObject : nullItems) {
			networkObject.setNetwork(connections.get(networkObject), newNetwork);
			newNetwork.add(networkObject);
		}
		return newNetwork;
	}

	/**
	 * Splits connections on each side of a wire into separate networks.
	 *
	 * @param level The {@link Level} to check.
	 * @param pos The {@link BlockPos} of the wire being split.
	 * @param connection A {@link Predicate} determining if a {@link BENetworkObject} is a valid connection or not.
	 * @param wires List of valid {@link WireBlock}s.
	 */
	public static void splitNetworks(Level level, BlockPos pos, Predicate<BENetworkObject> connection, WireBlock... wires) {
		Map<Direction, Map<BENetworkObject, Direction>> connectionsPerSide = new HashMap<>();
		Set<WireBlock> wireSet = Set.of(wires);
		Set<BlockPos> blacklist = new HashSet<>(); // Sharing a set means sides connected to each other actually won't get double processed.
		blacklist.add(pos);

		for(Direction dir : Direction.values()) {
			BlockPos checkPos = pos.relative(dir);

			BENetworkObject networkObject = IBENetworks.getObject(level, checkPos);
			if(networkObject != null && connection.test(networkObject))
				connectionsPerSide.put(dir, Map.of(networkObject, dir.getOpposite()));
			else if(wireSet.contains(level.getBlockState(checkPos).getBlock()))
				connectionsPerSide.put(dir, getConnections(new HashMap<>(), blacklist, level, checkPos, connection, wireSet));
		}

		for(Direction dir : connectionsPerSide.keySet()) {
			Map<BENetworkObject, Direction> connections = connectionsPerSide.get(dir);

			BENetwork networkToReplace = null; // Search for a network on the wire first. Just in case there isn't one for some reason.
			for(BENetworkObject networkObject : connections.keySet()) {
				networkToReplace = networkObject.getNetwork(connections.get(networkObject));
				if(networkToReplace != null)
					break;
			}

			if(networkToReplace != null) {
				BENetwork newNetwork = BENetworkRegistry.createNetwork(networkToReplace.getType(), UUID.randomUUID(), level);

				for(BENetworkObject networkObject : connections.keySet()) {
					networkToReplace.remove(networkObject, true);

					networkObject.setNetwork(connections.get(networkObject), newNetwork);
					newNetwork.add(networkObject);

					IBENetworks.get(level).removeNetwork(networkToReplace);
					IBENetworks.get(level).addNetwork(newNetwork);
				}
			}
		}
	}

	/**
	 * Connects the {@link BENetworkObject} at {@code pos} to every wire run touching it, on both the power and coax
	 * wire types its block supports. Called when a network object is created (see
	 * {@link com.arrl.radiocraft.api.benetworks.INetworkObjectProvider}), which covers placing a block next to
	 * existing wire as well as re-linking after a chunk or world reload, since networks are not persisted.
	 *
	 * @param level The {@link Level} to check in.
	 * @param pos The {@link BlockPos} of the object to connect.
	 */
	public static void connectToWires(Level level, BlockPos pos) {
		if(level.isClientSide)
			return;
		BlockState state = level.getBlockState(pos);
		if(state.is(RadiocraftTags.Blocks.POWER_BLOCKS))
			tryConnect(level, pos, no -> no instanceof PowerNetworkObject, PowerBENetwork::new, powerWires());
		if(state.is(RadiocraftTags.Blocks.COAX_BLOCKS) || state.getBlock() instanceof RadioBlock)
			tryConnect(level, pos, no -> no instanceof ICoaxNetworkObject, BENetwork::new, coaxWires());
	}

	/**
	 * Attempts to connect a {@link BENetworkObject} to all valid networks around it. For each adjacent wire, every
	 * object reachable through that wire run is collected and all of them (plus this object) are put on one network.
	 *
	 * @param level The {@link Level} to check in.
	 * @param pos The {@link BlockPos} to check around.
	 * @param validConnection {@link Predicate} determining if a {@link BENetworkObject} is a valid connection.
	 * @param fallbackSupplier A {@link Supplier} providing a new instance of {@link BENetwork} if no connections had one.
	 * @param wires A list of valid {@link WireBlock}s.
	 */
	public static void tryConnect(Level level, BlockPos pos, Predicate<BENetworkObject> validConnection, Supplier<BENetwork> fallbackSupplier, WireBlock... wires) {
		BENetworkObject networkObject = IBENetworks.getObject(level, pos);

		if(networkObject == null || !validConnection.test(networkObject))
			return;

		Set<WireBlock> wireSet = Set.of(wires);
		for(Direction dir : Direction.values()) {
			BlockPos checkPos = pos.relative(dir);
			if(!wireSet.contains(level.getBlockState(checkPos).getBlock()))
				continue;

			Set<BlockPos> blacklist = new HashSet<>();
			blacklist.add(pos); // Don't walk back into ourselves through the wire.
			Map<BENetworkObject, Direction> connections = getConnections(new HashMap<>(), blacklist, level, checkPos, validConnection, wireSet);
			if(connections.isEmpty())
				continue; // Nothing on the other end yet; whatever loads there later will connect to us.

			connections.put(networkObject, dir);
			joinAll(level, connections, fallbackSupplier);
		}
	}


	/**
	 * Get all connections found on a set of wires.
	 *
	 * @param level The {@link Level} to check in.
	 * @param pos The {@link BlockPos} of the starting wire.
	 * @param connection A {@link Predicate} determining if a {@link BENetworkObject} is a valid connection or not.
	 * @param wires List of valid {@link WireBlock}s
	 */
	public static Map<BENetworkObject, Direction> getConnections(Level level, BlockPos pos, Predicate<BENetworkObject> connection, WireBlock... wires) {
		return getConnections(new HashMap<>(), new HashSet<>(), level, pos, connection, Set.of(wires));
	}

	public static Map<BENetworkObject, Direction> getConnections(Map<BENetworkObject, Direction> connections, Set<BlockPos> blackList, Level level, BlockPos pos, Predicate<BENetworkObject> connection, Set<WireBlock> wires) {
		blackList.add(pos);
		BlockState state = level.getBlockState(pos);

		if(wires.contains(state.getBlock())) {
			WireBlock wire = (WireBlock)state.getBlock();

			for(Direction dir : wire.getConnections(level, pos)) {
				BlockPos checkPos = pos.relative(dir);
				if(blackList.contains(checkPos))
					continue;

				BENetworkObject networkObject = IBENetworks.getObject(level, checkPos);
				if(networkObject != null && connection.test(networkObject))
					connections.put(networkObject, dir.getOpposite());
				else
					getConnections(connections, blackList, level, checkPos, connection, wires);
			}
		}
		return connections;
	}

	/**
	 * Get the first connection found on a set of wires. First meaning the first the algorithm finds.
	 *
	 * @param level The {@link Level} to check in.
	 * @param pos The {@link BlockPos} of the starting wire.
	 * @param connection A {@link Predicate} determining if a {@link BENetworkObject} is a valid connection or not.
	 * @param wires List of valid {@link WireBlock}s
	 */
	public static Pair<BENetworkObject, Direction> getFirstConnection(Level level, BlockPos pos, Predicate<BENetworkObject> connection, WireBlock... wires) {
		return getFirstConnection(new HashSet<>(), level, pos, connection, Set.of(wires));
	}

	public static Pair<BENetworkObject, Direction> getFirstConnection(Set<BlockPos> blackList, Level level, BlockPos pos, Predicate<BENetworkObject> connection, Set<WireBlock> wires) {
		blackList.add(pos); // Add self to blacklist, stops infinite loops.
		BlockState state = level.getBlockState(pos);

		if(wires.contains(state.getBlock())) { // This call is fine, IntelliJ just dislikes the type mismatch.
			WireBlock wire = (WireBlock)state.getBlock();

			for(Direction dir : wire.getConnections(level, pos)) {
				BlockPos checkPos = pos.relative(dir);
				if(blackList.contains(checkPos))
					continue;

				BENetworkObject networkObject = IBENetworks.getObject(level, checkPos);
				if(networkObject != null && connection.test(networkObject))
					return Pair.of(networkObject, dir.getOpposite());
				else {
					Pair<BENetworkObject, Direction> next = getFirstConnection(blackList, level, checkPos, connection, wires);
					if(next != null)
						return next;
				}
			}
		}
		return null;
	}

}
