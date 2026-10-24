package com.arrl.radiocraft.common.be_networks;

import net.minecraft.world.level.BlockGetter;
import com.arrl.radiocraft.common.blocks.radios.VHFRepeaterBlock;
import com.arrl.radiocraft.common.blocks.antennas.DuplexerBlock;
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
		return new WireBlock[] { RadiocraftBlocks.WIRE.get(), (WireBlock)RadiocraftBlocks.WATERPROOF_WIRE.get(), RadiocraftBlocks.PASSTHROUGH.get() };
	}

	/** The wire blocks a coax network can run through. */
	public static WireBlock[] coaxWires() {
		return new WireBlock[] { RadiocraftBlocks.COAX_WIRE.get(), RadiocraftBlocks.PASSTHROUGH.get() };
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

		BENetwork newNetwork = existingNetworks.size() == 1 && nullItems.isEmpty()
				? existingNetworks.iterator().next() // Already one network, nothing to merge.
				: BENetwork.merge(existingNetworks, fallbackSupplier, level);

		// Objects without a network on that side join here. So does any object whose side still points at a network
		// it is no longer a member of (a split removes an object from the whole network but only re-links the side
		// the split came from; a pass-through device has several sides), otherwise merge would have skipped it.
		for(BENetworkObject networkObject : connections.keySet()) {
			boolean member = newNetwork.getNetworkObjects().contains(networkObject);
			if(nullItems.contains(networkObject) || !member) {
				networkObject.setNetwork(connections.get(networkObject), newNetwork);
				if(!member)
					newNetwork.add(networkObject);
			}
		}
		return newNetwork;
	}

	/**
	 * Splits what a wire used to join into separate networks: the blocks in {@code links} are walked (without passing
	 * through the wire at {@code pos} if it is gone) and every group that is still connected to each other gets a
	 * network of its own.
	 *
	 * @param level The {@link Level} to check.
	 * @param pos The {@link BlockPos} of the wire being split.
	 * @param links The blocks the wire was linked to, with the side of each it touched ({@link WireBlock#getLinks}).
	 * @param wireGone True if the wire at {@code pos} has been removed; false if it is still there but lost a link
	 *                 (a climb that ended), in which case the walk may still pass through it.
	 * @param connection A {@link Predicate} determining if a {@link BENetworkObject} is a valid connection or not.
	 * @param wires List of valid {@link WireBlock}s.
	 */
	public static void splitNetworks(Level level, BlockPos pos, Map<BlockPos, Direction> links, boolean wireGone, Predicate<BENetworkObject> connection, WireBlock... wires) {
		Map<BlockPos, Map<BENetworkObject, Direction>> connectionsPerSide = new LinkedHashMap<>();
		Set<WireBlock> wireSet = Set.of(wires);
		Set<BlockPos> blacklist = new HashSet<>(); // Sharing a set means sides connected to each other actually won't get double processed.
		if(wireGone)
			blacklist.add(pos);

		for(Map.Entry<BlockPos, Direction> link : links.entrySet()) {
			BlockPos checkPos = linkTarget(level, link.getKey(), link.getValue());

			BENetworkObject networkObject = IBENetworks.getObject(level, checkPos);
			if(blacklist.contains(checkPos))
				continue; // Already reached through another side (a loop), leave it with that side.
			if(networkObject != null && connection.test(networkObject)) {
				Map<BENetworkObject, Direction> side = new HashMap<>();
				addConnection(side, blacklist, level, checkPos, networkObject, link.getValue(), connection, wireSet); // Keeps what lies beyond a tuner with it.
				connectionsPerSide.put(checkPos, side);
			}
			else if(wireSet.contains(level.getBlockState(checkPos).getBlock()))
				connectionsPerSide.put(checkPos, getConnections(new HashMap<>(), blacklist, level, checkPos, connection, wireSet));
		}

		for(BlockPos side : connectionsPerSide.keySet()) {
			Map<BENetworkObject, Direction> connections = connectionsPerSide.get(side);

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

		// A pass-through device (antenna tuner) is one node for all its sides: everything reachable from any side goes
		// on one network, otherwise the radio on one side and the antenna on the other would never meet.
		boolean passThrough = networkObject instanceof IPassThroughNetworkObject;
		Set<BlockPos> sharedBlacklist = new HashSet<>();
		sharedBlacklist.add(pos);
		Map<BENetworkObject, Direction> allConnections = new HashMap<>();
		Direction firstSide = null;

		// The wires touching this device: its neighbours and any climber coming up the block below-diagonal.
		for(Map.Entry<BlockPos, Direction> link : wireLinksOf(level, pos, wireSet).entrySet()) {
			BlockPos checkPos = link.getKey();
			Direction dir = link.getValue();

			if(passThrough) {
				getConnections(allConnections, sharedBlacklist, level, checkPos, validConnection, wireSet);
				if(firstSide == null)
					firstSide = dir;
				continue;
			}

			Set<BlockPos> blacklist = new HashSet<>();
			blacklist.add(pos); // Don't walk back into ourselves through the wire.
			Map<BENetworkObject, Direction> connections = getConnections(new HashMap<>(), blacklist, level, checkPos, validConnection, wireSet);
			if(connections.isEmpty())
				continue; // Nothing on the other end yet; whatever loads there later will connect to us.

			connections.put(networkObject, dir);
			joinAll(level, connections, fallbackSupplier);
		}

		// Devices that link by touching (solar panels to each other and to a charge controller): no wire needed.
		for(Direction dir : Direction.values()) {
			BlockPos checkPos = pos.relative(dir);
			BENetworkObject other = IBENetworks.getObject(level, checkPos);
			if(other == null || !validConnection.test(other) || !linksDirectly(networkObject, other) || sharedBlacklist.contains(checkPos))
				continue;
			if(passThrough) {
				addConnection(allConnections, sharedBlacklist, level, checkPos, other, dir.getOpposite(), validConnection, wireSet);
				if(firstSide == null)
					firstSide = dir;
				continue;
			}
			Set<BlockPos> blacklist = new HashSet<>();
			blacklist.add(pos);
			Map<BENetworkObject, Direction> connections = new HashMap<>();
			addConnection(connections, blacklist, level, checkPos, other, dir.getOpposite(), validConnection, wireSet);
			connections.put(networkObject, dir);
			joinAll(level, connections, fallbackSupplier);
		}

		if(passThrough && !allConnections.isEmpty()) {
			allConnections.put(networkObject, firstSide);
			joinAll(level, allConnections, fallbackSupplier);
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

			for(Map.Entry<BlockPos, Direction> link : wire.getLinks(level, pos, state).entrySet()) {
				BlockPos checkPos = linkTarget(level, link.getKey(), link.getValue());
				if(blackList.contains(checkPos))
					continue;

				BENetworkObject networkObject = IBENetworks.getObject(level, checkPos);
				if(networkObject != null && connection.test(networkObject))
					addConnection(connections, blackList, level, checkPos, networkObject, link.getValue(), connection, wires);
				else
					getConnections(connections, blackList, level, checkPos, connection, wires);
			}
		}
		return connections;
	}

	/**
	 * Records a device the walk reached and, if it is an {@link IPassThroughNetworkObject}, keeps walking out of its other
	 * sides, so devices beyond it join the same connection set.
	 */
	private static void addConnection(Map<BENetworkObject, Direction> connections, Set<BlockPos> blackList, Level level, BlockPos pos, BENetworkObject networkObject, Direction side, Predicate<BENetworkObject> connection, Set<WireBlock> wires) {
		connections.put(networkObject, side);
		if(!(networkObject instanceof IPassThroughNetworkObject) || !blackList.add(pos))
			return;

		for(BlockPos checkPos : wireLinksOf(level, pos, wires).keySet()) {
			if(!blackList.contains(checkPos))
				getConnections(connections, blackList, level, checkPos, connection, wires);
		}
		// ...and into devices it links to by touching (the next solar panel in a row, the charge controller at its end).
		for(Direction dir : Direction.values()) {
			BlockPos checkPos = pos.relative(dir);
			BENetworkObject other = IBENetworks.getObject(level, checkPos);
			if(other != null && connection.test(other) && linksDirectly(networkObject, other) && !blackList.contains(checkPos))
				addConnection(connections, blackList, level, checkPos, other, dir.getOpposite(), connection, wires);
		}
	}

	/**
	 * Where a wire's link into {@code target} (touching its {@code side}) really goes. Coax laid straight behind a
	 * repeater with a duplexer on top goes up the duplexer's cable, so it reaches the duplexer, not the repeater.
	 */
	public static BlockPos linkTarget(BlockGetter level, BlockPos target, Direction side) {
		return duplexerOnTop(level, target) && side == repeaterBack(level.getBlockState(target)) ? target.above() : target;
	}

	/**
	 * The wires a device at {@code pos} is linked to ({@link WireBlock#wireLinksAround}), with the duplexer's cable:
	 * a repeater with a duplexer on top does not take the coax behind it, the duplexer does.
	 */
	public static Map<BlockPos, Direction> wireLinksOf(BlockGetter level, BlockPos pos, Set<WireBlock> wires) {
		Map<BlockPos, Direction> links = WireBlock.wireLinksAround(level, pos, wires);
		if(duplexerOnTop(level, pos)) {
			Direction back = repeaterBack(level.getBlockState(pos));
			links.remove(pos.relative(back));
		}
		BlockPos below = pos.below();
		if(level.getBlockState(pos).getBlock() instanceof DuplexerBlock && duplexerOnTop(level, below)) {
			Direction back = repeaterBack(level.getBlockState(below));
			BlockPos behind = below.relative(back);
			if(wires.contains(level.getBlockState(behind).getBlock()))
				links.put(behind, back);
		}
		return links;
	}

	private static boolean duplexerOnTop(BlockGetter level, BlockPos pos) {
		return level.getBlockState(pos).getBlock() instanceof VHFRepeaterBlock && level.getBlockState(pos.above()).getBlock() instanceof DuplexerBlock;
	}

	private static Direction repeaterBack(BlockState repeater) {
		return repeater.getValue(VHFRepeaterBlock.HORIZONTAL_FACING).getOpposite();
	}

	/** True if either object says it joins the other by touching, see {@link BENetworkObject#linksDirectlyTo}. */
	private static boolean linksDirectly(BENetworkObject a, BENetworkObject b) {
		return a.linksDirectlyTo(b) || b.linksDirectlyTo(a);
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

			for(Map.Entry<BlockPos, Direction> link : wire.getLinks(level, pos, state).entrySet()) {
				BlockPos checkPos = linkTarget(level, link.getKey(), link.getValue());
				if(blackList.contains(checkPos))
					continue;

				BENetworkObject networkObject = IBENetworks.getObject(level, checkPos);
				if(networkObject != null && connection.test(networkObject))
					return Pair.of(networkObject, link.getValue());
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
