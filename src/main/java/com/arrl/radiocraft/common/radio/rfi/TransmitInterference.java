package com.arrl.radiocraft.common.radio.rfi;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.RadiocraftServerConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.CommandBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.RedstoneTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Redstone interference, transmit side: RF from a transmitting station gets into redstone near its antenna. Dust in
 * reach reads an induced level ({@link #BASE_LEVEL}, higher with a bad SWR) for as long as the station transmits, and
 * levers, buttons and torches in reach get flipped when the transmission starts.
 * <p>
 * Reach, as agreed with the maintainer: {@link #QRP_ANTENNA_REACH} blocks of the antenna for QRP rigs,
 * {@link #FULL_ANTENNA_REACH} for full-power rigs. A badly matched antenna (SWR at or above the config threshold) also
 * puts RF on the feedline and in the shack: {@link #BAD_SWR_RADIO_REACH} blocks of the radio and
 * {@link #BAD_SWR_FEEDLINE_REACH} of the coax. Vertical antennas are strongly horizontally polarised and only reach
 * {@link #VERTICAL_HEIGHT_REACH} blocks above or below their wire.
 * <p>
 * Which components are hit is decided by a hash of the station and the position, not a dice roll: the same door
 * misbehaves every time you key up until you fix the antenna or move the contraption.
 */
public class TransmitInterference {

	public static final int BASE_LEVEL = 4;
	public static final int QRP_ANTENNA_REACH = 2;
	public static final int FULL_ANTENNA_REACH = 6;
	public static final int BAD_SWR_RADIO_REACH = 10;
	public static final int BAD_SWR_FEEDLINE_REACH = 5;
	public static final int VERTICAL_HEIGHT_REACH = 2;
	/** Safety cap on the blocks scanned when a transmission starts. */
	private static final int MAX_SCAN = 40_000;

	/** Antenna types that radiate sideways only, for the height rule. */
	public static final Set<ResourceLocation> VERTICAL_ANTENNAS = Set.of(
			Radiocraft.id("quarter_wave_vertical"), Radiocraft.id("j_pole"), Radiocraft.id("slim_jim"), Radiocraft.id("ringo"),
			Radiocraft.id("folded_dipole"));

	/** What one transmission hits: dust positions with their induced level, and switches to flip at key-up. */
	public record Targets(Map<BlockPos, Integer> dust, List<BlockPos> switches) {
		public static final Targets EMPTY = new Targets(Map.of(), List.of());

		public boolean isEmpty() {
			return dust.isEmpty() && switches.isEmpty();
		}
	}

	/** Reach around the antenna wire for this rig. */
	public static int antennaReach(boolean qrp) {
		return qrp ? QRP_ANTENNA_REACH : FULL_ANTENNA_REACH;
	}

	public static boolean isBadSwr(double swr) {
		return swr >= RadiocraftServerConfig.REDSTONE_INTERFERENCE_BAD_SWR.get();
	}

	/** Induced dust level: {@link #BASE_LEVEL}, rising two per unit of SWR once the match is bad, capped at 15. */
	public static int signalLevel(double swr) {
		if(!isBadSwr(swr))
			return BASE_LEVEL;
		return Math.clamp(BASE_LEVEL + (int)Math.round((swr - 1.0D) * 2.0D), BASE_LEVEL, 15);
	}

	/** True if {@code pos} is within {@code radius} of any of {@code points}; {@code sidewaysOnly} limits the height. */
	public static boolean inReach(BlockPos pos, List<BlockPos> points, int radius, boolean sidewaysOnly) {
		if(radius <= 0)
			return false;
		double radiusSq = (double)radius * radius;
		for(BlockPos p : points) {
			if(sidewaysOnly && Math.abs(pos.getY() - p.getY()) > VERTICAL_HEIGHT_REACH)
				continue;
			if(pos.distSqr(p) <= radiusSq)
				return true;
		}
		return false;
	}

	/** A number in [0, 1) that depends only on the station seed and the position. */
	public static double roll(long seed, BlockPos pos) {
		long z = seed ^ (pos.asLong() * 0x9E3779B97F4A7C15L);
		z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
		z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
		z ^= z >>> 31;
		return (z >>> 11) * 0x1.0p-53;
	}

	/** The station seed: changes when the antenna moves, the match changes bucket or the rig changes class. */
	public static long seed(BlockPos radioPos, BlockPos antennaPos, double swr, boolean qrp) {
		return radioPos.asLong() * 31L + antennaPos.asLong() * 17L + (long)Math.floor(swr) * 7L + (qrp ? 1L : 0L);
	}

	/**
	 * Decides what a transmission from the station described by {@code zone} hits. Scans the reachable box once and
	 * only does distance maths for redstone components, so an empty field costs a block read per position.
	 */
	public static Targets pick(Level level, PickupZone zone, boolean qrp, boolean vertical, double swr) {
		if(!RadiocraftServerConfig.REDSTONE_INTERFERENCE_ENABLED.get() || !RadiocraftServerConfig.REDSTONE_INTERFERENCE_TRANSMIT_ENABLED.get())
			return Targets.EMPTY;
		boolean badSwr = isBadSwr(swr);
		if(RadiocraftServerConfig.REDSTONE_INTERFERENCE_ONLY_BAD_SWR.get() && !badSwr)
			return Targets.EMPTY;

		int antennaReach = antennaReach(qrp);
		int radioReach = badSwr ? BAD_SWR_RADIO_REACH : 0;
		int feedReach = badSwr ? BAD_SWR_FEEDLINE_REACH : 0;
		List<BlockPos> radio = List.of(zone.radioPos());

		BlockPos.MutableBlockPos min = new BlockPos.MutableBlockPos(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
		BlockPos.MutableBlockPos max = new BlockPos.MutableBlockPos(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);
		grow(min, max, zone.antenna(), antennaReach, vertical ? Math.min(antennaReach, VERTICAL_HEIGHT_REACH) : antennaReach);
		grow(min, max, zone.feedline(), feedReach, feedReach);
		grow(min, max, radio, radioReach, radioReach);

		long volume = (long)(max.getX() - min.getX() + 1) * (max.getY() - min.getY() + 1) * (max.getZ() - min.getZ() + 1);
		if(volume <= 0 || volume > MAX_SCAN)
			return Targets.EMPTY;

		double chance = RadiocraftServerConfig.REDSTONE_INTERFERENCE_TRANSMIT_CHANCE.get();
		long seed = seed(zone.radioPos(), zone.antenna().isEmpty() ? zone.radioPos() : zone.antenna().getFirst(), swr, qrp);
		int dustLevel = signalLevel(swr);
		Map<BlockPos, Integer> dust = new HashMap<>();
		List<BlockPos> switches = new ArrayList<>();

		// Only loaded chunks: reading an unloaded one would load it synchronously from inside a block entity tick.
		int minY = Math.max(min.getY(), level.getMinBuildHeight());
		int maxY = Math.min(max.getY(), level.getMaxBuildHeight() - 1);
		for(int cx = min.getX() >> 4; cx <= max.getX() >> 4; cx++) {
			for(int cz = min.getZ() >> 4; cz <= max.getZ() >> 4; cz++) {
				if(!level.hasChunk(cx, cz))
					continue;
				int x0 = Math.max(min.getX(), cx << 4), x1 = Math.min(max.getX(), (cx << 4) + 15);
				int z0 = Math.max(min.getZ(), cz << 4), z1 = Math.min(max.getZ(), (cz << 4) + 15);
				for(BlockPos pos : BlockPos.betweenClosed(x0, minY, z0, x1, maxY, z1)) {
					BlockState state = level.getBlockState(pos);
					boolean isDust = state.is(Blocks.REDSTONE_WIRE);
					if(!isDust && !isSwitch(state))
						continue;
					if(!isDust && isOnCommandBlock(level, pos, state))
						continue; // RF never fires an admin's command block (also what the GameTest framework bolts onto every test).
					if(!inReach(pos, zone.antenna(), antennaReach, vertical) && !inReach(pos, zone.feedline(), feedReach, false) && !inReach(pos, radio, radioReach, false))
						continue;
					if(roll(seed, pos) >= chance)
						continue;
					if(isDust)
						dust.put(pos.immutable(), dustLevel);
					else
						switches.add(pos.immutable());
				}
			}
		}
		return new Targets(dust, switches);
	}

	private static void grow(BlockPos.MutableBlockPos min, BlockPos.MutableBlockPos max, List<BlockPos> points, int reach, int reachY) {
		if(reach <= 0)
			return;
		for(BlockPos p : points) {
			min.set(Math.min(min.getX(), p.getX() - reach), Math.min(min.getY(), p.getY() - reachY), Math.min(min.getZ(), p.getZ() - reach));
			max.set(Math.max(max.getX(), p.getX() + reach), Math.max(max.getY(), p.getY() + reachY), Math.max(max.getZ(), p.getZ() + reach));
		}
	}

	/** Components a transmission can flip: levers, buttons and redstone torches (wall torches included). */
	public static boolean isSwitch(BlockState state) {
		Block block = state.getBlock();
		return block instanceof LeverBlock || block instanceof ButtonBlock || block instanceof RedstoneTorchBlock;
	}

	/** True if the switch at {@code pos} is mounted on a command block (any kind). */
	public static boolean isOnCommandBlock(Level level, BlockPos pos, BlockState state) {
		Direction support;
		if(state.hasProperty(BlockStateProperties.ATTACH_FACE)) {
			support = switch(state.getValue(BlockStateProperties.ATTACH_FACE)) {
				case FLOOR -> Direction.DOWN;
				case CEILING -> Direction.UP;
				case WALL -> state.getValue(HorizontalDirectionalBlock.FACING).getOpposite();
			};
		}
		else if(state.hasProperty(HorizontalDirectionalBlock.FACING))
			support = state.getValue(HorizontalDirectionalBlock.FACING).getOpposite(); // Wall torch.
		else
			support = Direction.DOWN; // Standing torch.
		return level.getBlockState(pos.relative(support)).getBlock() instanceof CommandBlock;
	}

	/** Transmission started: induce the dust levels and flip the switches. */
	public static void start(ServerLevel level, Targets targets) {
		RfSignals.apply(level, targets.dust());
		for(BlockPos pos : targets.switches()) {
			if(!level.isLoaded(pos))
				continue;
			BlockState state = level.getBlockState(pos);
			if(state.getBlock() instanceof LeverBlock lever)
				lever.pull(state, level, pos, null);
			else if(state.getBlock() instanceof ButtonBlock button) {
				if(!state.getValue(BlockStateProperties.POWERED))
					button.press(state, level, pos, null);
			}
			else if(state.getBlock() instanceof RedstoneTorchBlock torch) {
				// Flip it and let the torch's own tick put it right again: a two-tick flicker, like RF in a logic gate.
				level.setBlock(pos, state.cycle(BlockStateProperties.LIT), Block.UPDATE_ALL);
				level.scheduleTick(pos, torch, 2);
			}
		}
	}

	/** Transmission ended: dust goes back to whatever really drives it. Switches stay where they were flipped to. */
	public static void stop(ServerLevel level, Targets targets) {
		RfSignals.remove(level, targets.dust().keySet());
	}
}
