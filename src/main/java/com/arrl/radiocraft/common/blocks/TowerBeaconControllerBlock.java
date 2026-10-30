package com.arrl.radiocraft.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * The flasher for a tower's obstruction lights: a thin tower beam (it joins the tower like one, and coax runs up it)
 * that puts out a redstone signal two seconds on, two seconds off, for the lamps beside it. Timed by the world's clock,
 * so every controller blinks together. Not craftable: creative and structures only.
 */
public class TowerBeaconControllerBlock extends TowerBeamBlock {

	public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
	/** Ticks the signal stays on, then off: two seconds each. */
	public static final int HALF_PERIOD = 40;

	@SuppressWarnings("this-escape")
	public TowerBeaconControllerBlock(Properties properties) {
		super(properties, 4);
		registerDefaultState(stateDefinition.any().setValue(POWERED, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.@NotNull Builder<Block, BlockState> builder) {
		builder.add(POWERED);
	}

	/** Whether the signal is on at {@code gameTime}: the first half of every four seconds. */
	public static boolean onAt(long gameTime) {
		return Math.floorMod(gameTime, 2L * HALF_PERIOD) < HALF_PERIOD;
	}

	/** Ticks from {@code gameTime} to the next switch. */
	private static int untilSwitch(long gameTime) {
		return HALF_PERIOD - (int)Math.floorMod(gameTime, (long)HALF_PERIOD);
	}

	@Override
	protected void onPlace(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState oldState, boolean movedByPiston) {
		super.onPlace(state, level, pos, oldState, movedByPiston);
		if(!level.isClientSide && !oldState.is(this))
			level.scheduleTick(pos, this, 1); // Picks up the phase straight away.
	}

	/**
	 * Restarts the blink if it is not running: a controller a structure put down at world generation never had
	 * {@link #onPlace}, so it stood stuck on (or off) in sites already generated (seen 2026-10-30). New sites start it in
	 * {@link com.arrl.radiocraft.common.worldgen.SitePiece#postProcess}.
	 */
	@Override
	protected boolean isRandomlyTicking(@NotNull BlockState state) {
		return true;
	}

	@Override
	protected void randomTick(@NotNull BlockState state, @NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull RandomSource random) {
		if(!level.getBlockTicks().hasScheduledTick(pos, this))
			level.scheduleTick(pos, this, 1);
	}

	@Override
	protected void tick(@NotNull BlockState state, @NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull RandomSource random) {
		long now = level.getGameTime();
		boolean on = onAt(now);
		if(state.getValue(POWERED) != on)
			level.setBlock(pos, state.setValue(POWERED, on), Block.UPDATE_ALL);
		level.scheduleTick(pos, this, untilSwitch(now));
	}

	@Override
	protected boolean isSignalSource(@NotNull BlockState state) {
		return true;
	}

	@Override
	protected int getSignal(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull Direction direction) {
		return state.getValue(POWERED) ? 15 : 0;
	}

	@Override
	public void appendHoverText(@NotNull ItemStack stack, Item.@NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
		super.appendHoverText(stack, context, tooltip, flag);
		tooltip.add(Component.translatable("tooltip.radiocraft.tower_beacon_controller"));
	}
}
