package com.arrl.radiocraft.common.blocks.radios;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.blockentities.radio.RadioBlockEntity;
import com.arrl.radiocraft.common.init.RadiocraftItems;
import com.arrl.radiocraft.common.radio.crystal.CrystalQuality;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import com.arrl.radiocraft.common.items.CordedMicrophoneItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import com.arrl.radiocraft.common.blocks.power.AbstractPowerNetworkBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A radio: it faces the player who placed it, its comparator output is its S-meter, and placed against the underside
 * of a block it hangs from it the right way up ({@link #CEILING}), like a rig on an under-shelf mount.
 */
public abstract class RadioBlock extends AbstractPowerNetworkBlock {

	public static final DirectionProperty HORIZONTAL_FACING = BlockStateProperties.HORIZONTAL_FACING;
	/** True for a radio hanging from the block above: drawn and shaped raised so its top meets that block. */
	public static final BooleanProperty CEILING = BooleanProperty.create("ceiling");
	/**
	 * True once transmitting into a bad SWR has burnt out the transmitter (see {@link RadioBlockEntity}): it still
	 * receives, keying it only gives static, and broken it gives back its parts. Drawn the same as a working radio.
	 */
	public static final BooleanProperty BLOWN = BooleanProperty.create("blown");
	private static final Map<VoxelShape, VoxelShape> RAISED = new ConcurrentHashMap<>();

	/** {@code floor}, the radio's shape standing on the floor, raised to meet the ceiling if {@code state} hangs. */
	protected static VoxelShape hanging(BlockState state, VoxelShape floor) {
		if(!state.getValue(CEILING) || floor.isEmpty())
			return floor;
		return RAISED.computeIfAbsent(floor, shape -> shape.move(0.0D, 1.0D - shape.max(Direction.Axis.Y), 0.0D));
	}

	private final CrystalQuality crystal;

	public RadioBlock(Properties properties) {
		this(properties, CrystalQuality.GOOD);
	}

	/** A radio built around a crystal of {@code crystal} quality (each quality is its own block). */
	public RadioBlock(Properties properties, CrystalQuality crystal) {
		super(properties);
		this.crystal = crystal;
		registerDefaultState(defaultBlockState().setValue(CEILING, false).setValue(BLOWN, false));
	}

	public CrystalQuality getCrystalQuality() {
		return crystal;
	}

	@Override
	public void appendHoverText(@NotNull ItemStack stack, Item.@NotNull TooltipContext context, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag tooltipFlag) {
		super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
		if(crystal != CrystalQuality.GOOD)
			tooltipComponents.add(Component.translatable(Radiocraft.translationKey("tooltip", "crystal_" + crystal.getId())).withStyle(ChatFormatting.GRAY));
	}


	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
		return level.isClientSide ? RadioBlockEntity::clientTick : RadioBlockEntity::tick;
	}

	@Override
	protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
		builder.add(HORIZONTAL_FACING, CEILING, BLOWN);
	}

	// A comparator against any radio reads its S-meter: 0 with nothing coming in, up to 15 for a full-strength signal.

	@Override
	protected boolean hasAnalogOutputSignal(@NotNull BlockState state) {
		return true;
	}

	@Override
	protected int getAnalogOutputSignal(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos) {
		return level.getBlockEntity(pos) instanceof RadioBlockEntity radio ? radio.getMeterReading() : 0;
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(HORIZONTAL_FACING, context.getHorizontalDirection().getOpposite())
				.setValue(CEILING, context.getClickedFace() == Direction.DOWN);
	}

	/** Sneak-right-click with an empty hand takes the radio's hand mic on its cord; otherwise the radio's screen opens. */
	@Override
	protected @NotNull InteractionResult useWithoutItem(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull BlockHitResult hitResult) {
		if(player.isShiftKeyDown() && level.getBlockEntity(pos) instanceof RadioBlockEntity radio && radio.canLendHandMic()) {
			if(player instanceof ServerPlayer serverPlayer)
				radio.takeHandMic(serverPlayer);
			return InteractionResult.sidedSuccess(level.isClientSide);
		}
		return super.useWithoutItem(state, level, pos, player, hitResult);
	}

	/** Right-clicking the radio with its own hand mic hangs it back up. */
	@Override
	protected @NotNull ItemInteractionResult useItemOn(@NotNull ItemStack stack, @NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull InteractionHand hand, @NotNull BlockHitResult hitResult) {
		if(level.getBlockEntity(pos) instanceof RadioBlockEntity radio && CordedMicrophoneItem.isMicOf(stack, radio)) {
			if(!level.isClientSide)
				radio.hangUpHandMic(player);
			return ItemInteractionResult.sidedSuccess(level.isClientSide);
		}
		return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
	}

	/** A blown radio drops what it was crafted from instead of itself; see {@link #salvage}. */
	@Override
	protected @NotNull List<ItemStack> getDrops(@NotNull BlockState state, LootParams.@NotNull Builder params) {
		if(!state.getValue(BLOWN))
			return super.getDrops(state, params);
		return salvage(params.getLevel(), asItem());
	}

	/**
	 * The parts of a blown radio: one of each ingredient of the shaped recipe that crafts {@code radio}, read from the
	 * recipes the server has loaded (so a datapack recipe is followed), except the crystal, which the blowout cracked.
	 * A tag ingredient (planks) gives its first item.
	 */
	public static List<ItemStack> salvage(ServerLevel level, Item radio) {
		ShapedRecipe recipe = level.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING).stream()
				.map(RecipeHolder::value)
				.filter(r -> r instanceof ShapedRecipe && r.getResultItem(level.registryAccess()).is(radio))
				.map(r -> (ShapedRecipe)r)
				.findFirst().orElse(null);
		List<ItemStack> parts = new ArrayList<>();
		if(recipe == null)
			return parts;
		Set<Item> crystals = crystals();
		for(Ingredient ingredient : recipe.getIngredients()) {
			ItemStack[] items = ingredient.getItems();
			if(items.length > 0 && !crystals.contains(items[0].getItem()))
				parts.add(items[0].copyWithCount(1));
		}
		return parts;
	}

	/** Everything that is the crystal in a radio recipe: crystals, encased crystals and the all-band board built from them. */
	private static Set<Item> crystals() {
		return Stream.concat(Stream.of(RadiocraftItems.CRYSTALS, RadiocraftItems.ENCASED_CRYSTALS)
						.flatMap(map -> map.values().stream())
						.flatMap(byQuality -> byQuality.values().stream()),
						Stream.of(RadiocraftItems.ALL_BAND_CIRCUIT_BOARD))
				.map(Supplier::get)
				.collect(Collectors.toSet());
	}

}
