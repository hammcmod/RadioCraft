package com.arrl.radiocraft.gametest.sites;

import com.arrl.radiocraft.common.blocks.Strand;
import com.arrl.radiocraft.common.blocks.WireBlock;
import com.arrl.radiocraft.common.blocks.WireBundleBlock;
import com.arrl.radiocraft.common.init.RadiocraftItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

import java.util.function.Consumer;

/**
 * Builds a radio site in a level, in coordinates relative to the site's corner, the way a player would: devices go
 * down through their items ({@link #place}, so multi-block ones put up all their parts and footholds pick their mount),
 * cables through {@link #wire} (a second kind in a block makes a bundle), passthroughs with the passthrough item. Air
 * that the template must keep (a room, a shaft) is {@link #carve}d as cave air: the template is saved without plain
 * air, so the ground a site is set into shows through everywhere else.
 */
public final class SiteBuilder {

	private final ServerLevel level;
	private final BlockPos origin;
	private final FakePlayer player;

	public SiteBuilder(ServerLevel level, BlockPos origin) {
		this.level = level;
		this.origin = origin.immutable();
		this.player = FakePlayerFactory.getMinecraft(level);
		player.setGameMode(GameType.CREATIVE);
	}

	public ServerLevel level() {
		return level;
	}

	public BlockPos at(int x, int y, int z) {
		return origin.offset(x, y, z);
	}

	public void block(int x, int y, int z, BlockState state) {
		level.setBlock(at(x, y, z), state, Block.UPDATE_ALL);
	}

	public void block(int x, int y, int z, Block block) {
		block(x, y, z, block.defaultBlockState());
	}

	public void fill(int x0, int y0, int z0, int x1, int y1, int z1, BlockState state) {
		for(int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++)
			for(int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++)
				for(int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++)
					block(x, y, z, state);
	}

	public void fill(int x0, int y0, int z0, int x1, int y1, int z1, Block block) {
		fill(x0, y0, z0, x1, y1, z1, block.defaultBlockState());
	}

	/** Walls of {@code block} round the box, open top and bottom. */
	public void walls(int x0, int y0, int z0, int x1, int y1, int z1, Block block) {
		fill(x0, y0, z0, x1, y1, z0, block);
		fill(x0, y0, z1, x1, y1, z1, block);
		fill(x0, y0, z0, x0, y1, z1, block);
		fill(x1, y0, z0, x1, y1, z1, block);
	}

	/** Air the template keeps: inside a room or a shaft, where the ground it is set into must not show. */
	public void carve(int x0, int y0, int z0, int x1, int y1, int z1) {
		fill(x0, y0, z0, x1, y1, z1, Blocks.CAVE_AIR);
	}

	/**
	 * Places {@code item} at {@code (x, y, z)} as a player would: clicking {@code face} (the face of the block being
	 * placed against: UP for the floor, a horizontal side for a wall, so a foothold faces it) while looking
	 * {@code look} (the way a radio's front or a horn's mouth ends up facing back, as players face what they place).
	 */
	public void place(int x, int y, int z, ItemLike item, Direction face, Direction look) {
		BlockPos pos = at(x, y, z);
		ItemStack stack = new ItemStack(item);
		player.setItemInHand(InteractionHand.MAIN_HAND, stack);
		player.setYRot(look.toYRot());
		player.setXRot(0.0F);
		if(!(stack.getItem() instanceof BlockItem blockItem))
			throw new IllegalArgumentException(stack.getItem() + " is not a block");
		BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), face, pos, false);
		InteractionResult result = blockItem.place(new BlockPlaceContext(level, player, InteractionHand.MAIN_HAND, stack, hit));
		if(!result.consumesAction())
			throw new IllegalStateException("Could not place " + stack.getItem() + " at " + x + " " + y + " " + z + " (" + result + ")");
	}

	/** {@link #place} on the floor. */
	public void place(int x, int y, int z, ItemLike item, Direction look) {
		place(x, y, z, item, Direction.UP, look);
	}

	/**
	 * A cable of {@code kind} through each of {@code path} in turn (x, y, z triples), on the floor or hanging from the
	 * ceiling. Where a cable already runs, this one joins it in a bundle.
	 */
	public void wire(WireBlock kind, boolean ceiling, int... path) {
		if(path.length % 3 != 0)
			throw new IllegalArgumentException("A path is x, y, z triples");
		for(int i = 0; i < path.length; i += 3) {
			BlockPos pos = at(path[i], path[i + 1], path[i + 2]);
			if(Strand.isCable(level.getBlockState(pos))) {
				if(Strand.stateOf(level, pos, kind) == null)
					WireBundleBlock.add(level, pos, kind);
			}
			else
				level.setBlock(pos, kind.getBlockState(level, pos, ceiling), Block.UPDATE_ALL);
		}
	}

	/** A straight run of {@code kind} from one block to another, both included (along one axis). */
	public void run(WireBlock kind, boolean ceiling, int x0, int y0, int z0, int x1, int y1, int z1) {
		int dx = Integer.signum(x1 - x0), dy = Integer.signum(y1 - y0), dz = Integer.signum(z1 - z0);
		int steps = Math.max(Math.abs(x1 - x0), Math.max(Math.abs(y1 - y0), Math.abs(z1 - z0)));
		for(int i = 0; i <= steps; i++)
			wire(kind, ceiling, x0 + dx * i, y0 + dy * i, z0 + dz * i);
	}

	/** Turns the solid block at {@code (x, y, z)} into a passthrough, with the passthrough item. */
	public void passthrough(int x, int y, int z) {
		BlockPos pos = at(x, y, z);
		ItemStack stack = new ItemStack(RadiocraftItems.PASSTHROUGH.get());
		player.setItemInHand(InteractionHand.MAIN_HAND, stack);
		InteractionResult result = stack.getItem().useOn(new UseOnContext(level, player, InteractionHand.MAIN_HAND, stack,
				new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false)));
		if(!result.consumesAction())
			throw new IllegalStateException("Could not make a passthrough at " + x + " " + y + " " + z);
	}

	/** Does {@code action} to the block entity at {@code (x, y, z)}, which must be a {@code type}. */
	public <T extends BlockEntity> void with(int x, int y, int z, Class<T> type, Consumer<T> action) {
		BlockEntity entity = level.getBlockEntity(at(x, y, z));
		if(!type.isInstance(entity))
			throw new IllegalStateException("No " + type.getSimpleName() + " at " + x + " " + y + " " + z + ": " + entity);
		action.accept(type.cast(entity));
		entity.setChanged();
	}

	/** Every cable in the box worked out again, now that everything round it is down. */
	public void refreshCables(int x0, int y0, int z0, int x1, int y1, int z1) {
		for(BlockPos pos : BlockPos.betweenClosed(at(x0, y0, z0), at(x1, y1, z1))) {
			BlockState state = level.getBlockState(pos);
			if(state.getBlock() instanceof WireBundleBlock bundle)
				bundle.refresh(level, pos);
			else if(state.getBlock() instanceof WireBlock wire)
				wire.refresh(level, pos);
		}
	}
}
