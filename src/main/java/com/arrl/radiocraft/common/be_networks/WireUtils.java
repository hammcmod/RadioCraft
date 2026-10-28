package com.arrl.radiocraft.common.be_networks;

import com.arrl.radiocraft.common.blocks.Strand;
import com.arrl.radiocraft.common.blocks.StackedBlocks;
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
import com.arrl.radiocraft.common.radio.antenna.LightningRodAntennas;
import com.arrl.radiocraft.common.blocks.radios.RadioBlock;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.init.RadiocraftTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * The walk along wires that joins devices into networks. It moves between {@link Strand}s (a kind of cable at a
 * position: a wire block, or one cable of a bundle), entering the next block only along the strands the current one
 * joins ({@link WireBlock#joins}), so dyed coax stays apart from other colours whether it runs as blocks of its own or
 * inside a bundle. Positions already walked are kept as strands too, a device as a strand with no kind.
 */
public class WireUtils {

	/** The wire blocks a power network can run through. */
	public static WireBlock[] powerWires() {
		java.util.List<WireBlock> wires = new java.util.ArrayList<>(List.of(RadiocraftBlocks.WIRE.get(), (WireBlock)RadiocraftBlocks.WATERPROOF_WIRE.get(), RadiocraftBlocks.PASSTHROUGH.get()));
		RadiocraftBlocks.COLOURED_WIRE.values().forEach(wire -> wires.add(wire.get())); // Which colours join is WireBlock#joins.
		return wires.toArray(WireBlock[]::new);
	}

	/** The wire blocks a coax network can run through. */
	public static WireBlock[] coaxWires() {
		java.util.List<WireBlock> wires = new java.util.ArrayList<>(List.of(RadiocraftBlocks.COAX_WIRE.get(), RadiocraftBlocks.PASSTHROUGH.get()));
		RadiocraftBlocks.COLOURED_COAX.values().forEach(coax -> wires.add(coax.get())); // Which colours join is WireBlock#joins.
		return wires.toArray(WireBlock[]::new);
	}

	/** True for coax of any colour (not a passthrough). */
	public static boolean isCoax(BlockState state) {
		return state.getBlock() instanceof WireBlock wire && !wire.isPower && !wire.conductsAll();
	}

	/** True if there is coax of any colour at {@code pos}: a coax block, or a bundle with a coax strand. */
	public static boolean hasCoax(BlockGetter level, BlockPos pos) {
		for(Strand strand : Strand.at(level, pos))
			if(strand.kind() != null && !strand.kind().isPower && !strand.kind().conductsAll())
				return true;
		return false;
	}

	/** True if the walk through {@code wires} is the power one (they include the power wire) rather than the coax one. */
	public static boolean isPowerWalk(Set<WireBlock> wires) {
		return wires.contains(RadiocraftBlocks.WIRE.get());
	}

	/**
	 * Merges all connections to a strand into a single network, or creates a new one.
	 * @param level The {@link Level} to check.
	 * @param strand The strand to merge from.
	 * @param connection {@link Predicate} determining what counts as a valid connection.
	 * @param wires List of valid {@link WireBlock}s
	 */
	public static void mergeNetworks(Level level, Strand strand, Predicate<BENetworkObject> connection, Supplier<BENetwork> fallbackSupplier, WireBlock... wires) {
		Map<BENetworkObject, Direction> connections = getConnections(new HashMap<>(), new HashSet<>(), level, strand, connection, Set.of(wires));
		if(!connections.isEmpty())
			joinAll(level, connections, fallbackSupplier);
	}

	/** {@link #mergeNetworks(Level, Strand, Predicate, Supplier, WireBlock...)} for every strand at {@code pos} of a kind in {@code wires}. */
	public static void mergeNetworks(Level level, BlockPos pos, Predicate<BENetworkObject> connection, Supplier<BENetwork> fallbackSupplier, WireBlock... wires) {
		Set<WireBlock> wireSet = Set.of(wires);
		for(Strand strand : Strand.at(level, pos))
			if(wireSet.contains(strand.kind()))
				mergeNetworks(level, strand, connection, fallbackSupplier, wires);
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
	 * Splits what a strand used to join into separate networks: the blocks in {@code links} are walked (without passing
	 * through any strand in {@code gone}) and every group that is still connected to each other gets a network of its
	 * own.
	 *
	 * @param level The {@link Level} to check.
	 * @param from The strand being split: the walk out of each link follows the strands its kind joins.
	 * @param gone The strands that have been removed (none if {@code from} is still there but lost a link, a climb
	 *             that ended, in which case the walk may still pass through it).
	 * @param links The blocks the strand was linked to, with the side of each it touched ({@link WireBlock#getLinks}).
	 * @param connection A {@link Predicate} determining if a {@link BENetworkObject} is a valid connection or not.
	 * @param wires List of valid {@link WireBlock}s.
	 */
	public static void splitNetworks(Level level, Strand from, Set<Strand> gone, Map<BlockPos, Direction> links, Predicate<BENetworkObject> connection, WireBlock... wires) {
		Map<BlockPos, Map<BENetworkObject, Direction>> connectionsPerSide = new LinkedHashMap<>();
		Set<WireBlock> wireSet = Set.of(wires);
		Set<Strand> blacklist = new HashSet<>(gone); // Sharing a set means sides connected to each other actually won't get double processed.

		for(Map.Entry<BlockPos, Direction> link : links.entrySet()) {
			BlockPos checkPos = linkTarget(level, link.getKey(), link.getValue());

			BENetworkObject networkObject = IBENetworks.getObject(level, checkPos);
			if(blacklist.contains(Strand.device(checkPos)))
				continue; // Already reached through another side (a loop), leave it with that side.
			if(networkObject != null && connection.test(networkObject)) {
				Map<BENetworkObject, Direction> side = new HashMap<>();
				addConnection(side, blacklist, level, checkPos, networkObject, link.getValue(), connection, wireSet); // Keeps what lies beyond a tuner with it.
				connectionsPerSide.put(checkPos, side);
			}
			else {
				Map<BENetworkObject, Direction> side = enter(new HashMap<>(), blacklist, level, checkPos, from.kind(), connection, wireSet);
				if(!side.isEmpty())
					connectionsPerSide.put(checkPos, side);
			}
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
		boolean power = isPowerWalk(wireSet);

		// A pass-through device (antenna tuner) is one node for all its sides: everything reachable from any side goes
		// on one network, otherwise the radio on one side and the antenna on the other would never meet.
		boolean passThrough = networkObject instanceof IPassThroughNetworkObject through && through.passesThrough(power);
		Set<Strand> sharedBlacklist = new HashSet<>();
		sharedBlacklist.add(Strand.device(pos));
		Map<BENetworkObject, Direction> allConnections = new HashMap<>();
		Direction firstSide = null;

		// The wires touching this device: its neighbours and any climber coming up the block below-diagonal.
		for(Map.Entry<BlockPos, Direction> link : wireLinksOf(level, pos, wireSet).entrySet()) {
			BlockPos checkPos = link.getKey();
			Direction dir = link.getValue();

			if(passThrough) {
				enter(allConnections, sharedBlacklist, level, checkPos, null, validConnection, wireSet);
				if(firstSide == null)
					firstSide = dir;
				continue;
			}

			Set<Strand> blacklist = new HashSet<>();
			blacklist.add(Strand.device(pos)); // Don't walk back into ourselves through the wire.
			Map<BENetworkObject, Direction> connections = enter(new HashMap<>(), blacklist, level, checkPos, null, validConnection, wireSet);
			if(connections.isEmpty())
				continue; // Nothing on the other end yet; whatever loads there later will connect to us.

			connections.put(networkObject, dir);
			joinAll(level, connections, fallbackSupplier);
		}

		// Devices that link by touching (solar panels to each other and to a charge controller): no wire needed.
		for(Direction dir : Direction.values()) {
			BlockPos checkPos = pos.relative(dir);
			BENetworkObject other = IBENetworks.getObject(level, checkPos);
			if(other == null || !validConnection.test(other) || !linksDirectly(networkObject, other, power) || sharedBlacklist.contains(Strand.device(checkPos)))
				continue;
			if(passThrough) {
				addConnection(allConnections, sharedBlacklist, level, checkPos, other, dir.getOpposite(), validConnection, wireSet);
				if(firstSide == null)
					firstSide = dir;
				continue;
			}
			Set<Strand> blacklist = new HashSet<>();
			blacklist.add(Strand.device(pos));
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
	 * Get all connections found along the cables at {@code pos}: the walk starts down every strand there of a kind in
	 * {@code wires}.
	 *
	 * @param level The {@link Level} to check in.
	 * @param pos The {@link BlockPos} of the starting wire.
	 * @param connection A {@link Predicate} determining if a {@link BENetworkObject} is a valid connection or not.
	 * @param wires List of valid {@link WireBlock}s
	 */
	public static Map<BENetworkObject, Direction> getConnections(Level level, BlockPos pos, Predicate<BENetworkObject> connection, WireBlock... wires) {
		return enter(new HashMap<>(), new HashSet<>(), level, pos, null, connection, Set.of(wires));
	}

	/**
	 * Walks into the cables at {@code pos}: down every strand there whose kind is in {@code wires} and that
	 * {@code from} (the kind of the strand we come from, null for a device, which joins any) joins, unless already walked.
	 */
	public static Map<BENetworkObject, Direction> enter(Map<BENetworkObject, Direction> connections, Set<Strand> blackList, Level level, BlockPos pos,
			@Nullable WireBlock from, Predicate<BENetworkObject> connection, Set<WireBlock> wires) {
		for(Strand strand : Strand.at(level, pos)) {
			WireBlock kind = strand.kind();
			if(kind == null || !wires.contains(kind) || (from != null && !from.joins(kind)) || blackList.contains(strand))
				continue;
			getConnections(connections, blackList, level, strand, connection, wires);
		}
		return connections;
	}

	/** Get all connections found along {@code strand} and the cables it links to. */
	public static Map<BENetworkObject, Direction> getConnections(Map<BENetworkObject, Direction> connections, Set<Strand> blackList, Level level, Strand strand, Predicate<BENetworkObject> connection, Set<WireBlock> wires) {
		blackList.add(strand);
		WireBlock wire = strand.kind();
		BlockState state = strand.state(level);
		if(wire == null || state == null)
			return connections;

		for(Map.Entry<BlockPos, Direction> link : wire.getLinks(level, strand.pos(), state).entrySet()) {
			BlockPos checkPos = linkTarget(level, link.getKey(), link.getValue());
			if(blackList.contains(Strand.device(checkPos)))
				continue;

			BENetworkObject networkObject = IBENetworks.getObject(level, checkPos);
			if(networkObject == null)
				networkObject = LightningRodAntennas.ensure(level, checkPos); // A rod has no block entity to make it.
			if(networkObject != null && connection.test(networkObject))
				addConnection(connections, blackList, level, checkPos, networkObject, link.getValue(), connection, wires);
			else
				enter(connections, blackList, level, checkPos, wire, connection, wires);
		}
		return connections;
	}

	/**
	 * Records a device the walk reached and, if it is an {@link IPassThroughNetworkObject} for this kind of network,
	 * keeps walking out of its other sides, so devices beyond it join the same connection set.
	 */
	private static void addConnection(Map<BENetworkObject, Direction> connections, Set<Strand> blackList, Level level, BlockPos pos, BENetworkObject networkObject, Direction side, Predicate<BENetworkObject> connection, Set<WireBlock> wires) {
		connections.put(networkObject, side);
		boolean power = isPowerWalk(wires);
		if(!(networkObject instanceof IPassThroughNetworkObject through) || !through.passesThrough(power) || !blackList.add(Strand.device(pos)))
			return;

		for(BlockPos checkPos : wireLinksOf(level, pos, wires).keySet())
			enter(connections, blackList, level, checkPos, null, connection, wires);
		// ...and into devices it links to by touching (the next solar panel in a row, the charge controller at its end).
		for(Direction dir : Direction.values()) {
			BlockPos checkPos = pos.relative(dir);
			BENetworkObject other = IBENetworks.getObject(level, checkPos);
			if(other != null && connection.test(other) && linksDirectly(networkObject, other, power) && !blackList.contains(Strand.device(checkPos)))
				addConnection(connections, blackList, level, checkPos, other, dir.getOpposite(), connection, wires);
		}
	}

	/**
	 * Where a wire's link into {@code target} (touching its {@code side}) really goes. Coax laid straight behind a
	 * repeater with a duplexer on top goes up the duplexer's cable, so it reaches the duplexer, not the repeater. A wire
	 * touching an upper cell of a stacked block (the commercial repeater's rack) reaches the block entity in its bottom
	 * cell.
	 */
	public static BlockPos linkTarget(BlockGetter level, BlockPos target, Direction side) {
		BlockState state = level.getBlockState(target);
		if(state.hasProperty(StackedBlocks.PART) && state.getValue(StackedBlocks.PART) > 0)
			return StackedBlocks.bottom(target, state, StackedBlocks.PART);
		return duplexerOnTop(level, target) && side == repeaterBack(state) ? target.above() : target;
	}

	/**
	 * The wires a device at {@code pos} is linked to ({@link WireBlock#wireLinksAround}), with the duplexer's cable:
	 * a repeater with a duplexer on top does not take the coax behind it, the duplexer does. A stacked block (the
	 * commercial repeater's rack) takes wire touching any of its cells.
	 */
	public static Map<BlockPos, Direction> wireLinksOf(BlockGetter level, BlockPos pos, Set<WireBlock> wires) {
		Map<BlockPos, Direction> links = WireBlock.wireLinksAround(level, pos, wires);
		BlockState state = level.getBlockState(pos);
		if(state.hasProperty(StackedBlocks.PART) && state.getValue(StackedBlocks.PART) == 0) {
			for(int cell = 1; cell < StackedBlocks.HEIGHT; cell++) {
				BlockPos upper = pos.above(cell);
				BlockState upperState = level.getBlockState(upper);
				if(!upperState.is(state.getBlock()) || upperState.getValue(StackedBlocks.PART) != cell)
					break;
				for(Map.Entry<BlockPos, Direction> link : WireBlock.wireLinksAround(level, upper, wires).entrySet())
					if(!link.getKey().equals(pos) && !Strand.at(level, link.getKey()).isEmpty() && !isCellOf(level, link.getKey(), pos))
						links.putIfAbsent(link.getKey(), link.getValue());
			}
		}
		if(duplexerOnTop(level, pos)) {
			Direction back = repeaterBack(state);
			links.remove(pos.relative(back));
		}
		BlockPos below = pos.below();
		if(state.getBlock() instanceof DuplexerBlock && duplexerOnTop(level, below)) {
			Direction back = repeaterBack(level.getBlockState(below));
			BlockPos behind = below.relative(back);
			if(!Strand.at(level, behind).isEmpty())
				links.put(behind, back);
		}
		return links;
	}

	/** True if {@code pos} is a cell of the stacked block whose bottom is {@code bottom}. */
	private static boolean isCellOf(BlockGetter level, BlockPos pos, BlockPos bottom) {
		BlockState state = level.getBlockState(pos);
		return state.hasProperty(StackedBlocks.PART) && StackedBlocks.bottom(pos, state, StackedBlocks.PART).equals(bottom);
	}

	private static boolean duplexerOnTop(BlockGetter level, BlockPos pos) {
		return level.getBlockState(pos).getBlock() instanceof VHFRepeaterBlock && level.getBlockState(pos.above()).getBlock() instanceof DuplexerBlock;
	}

	private static Direction repeaterBack(BlockState repeater) {
		return repeater.getValue(VHFRepeaterBlock.HORIZONTAL_FACING).getOpposite();
	}

	/** True if either object says it joins the other by touching on this kind of network, see {@link BENetworkObject#linksDirectlyTo}. */
	private static boolean linksDirectly(BENetworkObject a, BENetworkObject b, boolean power) {
		return a.linksDirectlyTo(b, power) || b.linksDirectlyTo(a, power);
	}
}
