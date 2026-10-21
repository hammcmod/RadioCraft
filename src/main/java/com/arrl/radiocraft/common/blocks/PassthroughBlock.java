package com.arrl.radiocraft.common.blocks;

import com.arrl.radiocraft.api.benetworks.BENetwork;
import com.arrl.radiocraft.api.benetworks.PowerBENetwork;
import com.arrl.radiocraft.api.benetworks.PowerNetworkObject;
import com.arrl.radiocraft.common.be_networks.ICoaxNetworkObject;
import com.arrl.radiocraft.common.be_networks.WireUtils;
import com.arrl.radiocraft.common.blockentities.PassthroughBlockEntity;
import com.arrl.radiocraft.common.init.RadiocraftItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A wall block that wire runs through. Made by using the passthrough item on a solid block: that block is replaced
 * by this one, which keeps the old block's look ({@link PassthroughBlockEntity} remembers it and the renderer draws
 * it) but conducts power wire and coax alike between all six of its sides, so a cable can go through a wall without a
 * hole. It is a {@link WireBlock} as far as the network walk is concerned, in both wire sets, and links to every
 * neighbouring wire or device of either kind; it never hugs or climbs, having no cable of its own to show. Breaking
 * it gives back the passthrough item and the block it replaced.
 */
public class PassthroughBlock extends WireBlock implements EntityBlock {

	public PassthroughBlock(Properties properties) {
		super(properties, false, false);
	}

	/** Only the six connections: no cable is drawn, so none of the wire's look properties (keeps the state count tiny). */
	@Override
	protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN);
	}

	/** No climbs to settle: just follow the neighbours. */
	@Override
	public void refresh(Level level, BlockPos pos) {
		if(level.isClientSide)
			return;
		BlockState old = level.getBlockState(pos);
		if(!old.is(this))
			return;
		BlockState fresh = getBlockState(level, pos);
		if(fresh != old)
			level.setBlock(pos, fresh, Block.UPDATE_ALL);
	}

	/** A solid wall block with 64 states: the normal per-state cache is cheap and helps lighting. */
	@Override
	public boolean hasDynamicShape() {
		return false;
	}

	@Override
	public boolean conductsAll() {
		return true;
	}

	/** Anything a wire of either kind links to, or any wire. */
	private static boolean canLink(BlockState state) {
		return state.getBlock() instanceof WireBlock || isValidConnection(state, true) || isValidConnection(state, false);
	}

	@Override
	public BlockState getBlockState(BlockGetter level, BlockPos pos) {
		BlockState state = defaultBlockState();
		for(Direction direction : Direction.values())
			state = state.setValue(PROPERTY_BY_DIRECTION.get(direction), canLink(level.getBlockState(pos.relative(direction))));
		return state;
	}

	@Override
	public Map<BlockPos, Direction> getLinks(BlockGetter level, BlockPos pos, BlockState state) {
		Map<BlockPos, Direction> out = new LinkedHashMap<>();
		for(Direction dir : Direction.values())
			if(state.getValue(PROPERTY_BY_DIRECTION.get(dir)))
				out.put(pos.relative(dir), dir.getOpposite());
		return out;
	}

	@Override
	public boolean canClimb(BlockGetter level, BlockPos pos, Direction direction) {
		return false;
	}

	@Override
	public @NotNull BlockState updateShape(BlockState state, @NotNull Direction direction, @NotNull BlockState neighborState,
										   @NotNull LevelAccessor level, @NotNull BlockPos pos, @NotNull BlockPos neighborPos) {
		return state.setValue(PROPERTY_BY_DIRECTION.get(direction), canLink(neighborState));
	}

	// -------------------- NETWORKS: both kinds --------------------

	@Override
	public void onPlace(@NotNull BlockState state, Level level, @NotNull BlockPos pos, @NotNull BlockState oldState, boolean isMoving) {
		if(level.isClientSide || oldState.is(this))
			return;
		if(checkMultipleSides(level, pos, state)) {
			WireUtils.mergeNetworks(level, pos, no -> no instanceof PowerNetworkObject, PowerBENetwork::new, WireUtils.powerWires());
			WireUtils.mergeNetworks(level, pos, no -> no instanceof ICoaxNetworkObject, BENetwork::new, WireUtils.coaxWires());
		}
	}

	@Override
	public void onRemove(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, BlockState newState, boolean isMoving) {
		if(newState.is(this) || level.isClientSide) {
			if(!newState.is(this))
				super.onRemove(state, level, pos, newState, isMoving);
			return;
		}
		Map<BlockPos, Direction> links = getLinks(level, pos, state);
		if(links.size() >= 2) {
			WireUtils.splitNetworks(level, pos, links, true, no -> no instanceof PowerNetworkObject, WireUtils.powerWires());
			WireUtils.splitNetworks(level, pos, links, true, no -> no instanceof ICoaxNetworkObject, WireUtils.coaxWires());
		}
		// Skip WireBlock#onRemove (it would split one kind again); Block#onRemove drops the block entity.
		if(!state.is(newState.getBlock()))
			level.removeBlockEntity(pos);
	}

	// -------------------- LOOK AND FEEL --------------------

	@Override
	public @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
		return RenderShape.INVISIBLE; // The renderer draws the block this one replaced.
	}

	@Override
	public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
		return Shapes.block();
	}

	@Override
	public @NotNull FluidState getFluidState(@NotNull BlockState state) {
		return net.minecraft.world.level.material.Fluids.EMPTY.defaultFluidState(); // A solid wall block, never waterlogged.
	}

	@Override
	public @NotNull ItemStack getCloneItemStack(@NotNull LevelReader level, @NotNull BlockPos pos, @NotNull BlockState state) {
		return new ItemStack(RadiocraftItems.PASSTHROUGH.get());
	}

	/** Survival players get both the passthrough and the wall block back. */
	@Override
	public @NotNull BlockState playerWillDestroy(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull Player player) {
		if(!level.isClientSide && !player.isCreative()) {
			Block.popResource(level, pos, new ItemStack(RadiocraftItems.PASSTHROUGH.get()));
			if(level.getBlockEntity(pos) instanceof PassthroughBlockEntity be && be.getMimic() != null)
				Block.popResource(level, pos, new ItemStack(be.getMimic().getBlock()));
		}
		return super.playerWillDestroy(level, pos, state, player);
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
		return new PassthroughBlockEntity(pos, state);
	}
}
