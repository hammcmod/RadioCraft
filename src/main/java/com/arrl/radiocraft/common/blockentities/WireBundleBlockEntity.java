package com.arrl.radiocraft.common.blockentities;

import com.arrl.radiocraft.common.blocks.WireBlock;
import com.arrl.radiocraft.common.blocks.WireBundleBlock;
import com.arrl.radiocraft.common.init.RadiocraftBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The cables in a {@link WireBundleBlock}: each strand is kept as the {@link BlockState} it would have as a wire block
 * of its own at this position (its six connections, hugged edge, climb and whether it hangs from the ceiling), so the
 * drawing and the network walk run the ordinary {@link WireBlock} rules on it. Kept in display order (see
 * {@link WireBundleBlock#rank}), which is also the order the strands are drawn side by side in.
 */
public class WireBundleBlockEntity extends BlockEntity {

	private final List<BlockState> strands = new ArrayList<>();
	/**
	 * Whether the strands are laid out the other way round across a north-south run ({@code flipX}) or an east-west
	 * one ({@code flipZ}): set along each run of bundles by {@link com.arrl.radiocraft.common.blocks.BundleOrientation}
	 * so the strands keep their order round every bend instead of crossing.
	 */
	private boolean flipX = false, flipZ = false;

	public WireBundleBlockEntity(BlockPos pos, BlockState state) {
		super(RadiocraftBlockEntities.WIRE_BUNDLE.get(), pos, state);
	}

	/** Where a strand {@code offset} sixteenths along the bundle's layout sits, turned as this bundle is. */
	public com.arrl.radiocraft.common.blocks.WireLook.Lateral lateral(double offset) {
		return new com.arrl.radiocraft.common.blocks.WireLook.Lateral(flipX ? -offset : offset, flipZ ? -offset : offset);
	}

	public boolean flipX() {
		return flipX;
	}

	public boolean flipZ() {
		return flipZ;
	}

	/** Sets the turns; true if they changed (the caller syncs). */
	public boolean setFlips(boolean x, boolean z) {
		if(x == flipX && z == flipZ)
			return false;
		flipX = x;
		flipZ = z;
		setChanged();
		return true;
	}

	/**
	 * Puts the strands in the order of {@code kinds} (the run's order, chosen by
	 * {@link com.arrl.radiocraft.common.blocks.BundleOrientation} so strands turning off cross as few others as they
	 * can; kinds not listed go last). Returns whether the order changed. Server side; the solve syncs.
	 */
	public boolean setOrder(List<WireBlock> kinds) {
		List<BlockState> before = List.copyOf(strands);
		strands.sort(Comparator.comparingInt(s -> {
			int at = kinds.indexOf((WireBlock)s.getBlock());
			return at < 0 ? Integer.MAX_VALUE : at;
		}));
		if(before.equals(strands))
			return false;
		setChanged();
		return true;
	}

	/** Loaded with its chunk: the run's turns are worked out again (bundles laid before they existed, or edited offline). */
	@Override
	public void onLoad() {
		super.onLoad();
		if(level != null && !level.isClientSide)
			com.arrl.radiocraft.common.blocks.BundleOrientation.queueLoaded(level, worldPosition);
	}

	/** The strands, in display order. Read only. */
	public List<BlockState> strands() {
		return List.copyOf(strands);
	}

	/** The strand of exactly {@code kind}, or null. */
	@Nullable
	public BlockState strand(WireBlock kind) {
		for(BlockState strand : strands)
			if(strand.getBlock() == kind)
				return strand;
		return null;
	}

	/** The display index of {@code kind}'s strand (its place from the left), or -1. */
	public int indexOf(WireBlock kind) {
		for(int i = 0; i < strands.size(); i++)
			if(strands.get(i).getBlock() == kind)
				return i;
		return -1;
	}

	/** Replaces the whole list (the caller syncs and tells the neighbours). */
	public void setStrands(List<BlockState> states) {
		strands.clear();
		for(BlockState state : states)
			if(state.getBlock() instanceof WireBlock)
				strands.add(state);
		strands.sort(Comparator.comparingInt(s -> WireBundleBlock.rank((WireBlock)s.getBlock())));
		setChanged();
	}

	/** Puts {@code fresh} in place of the strand of its kind (or adds it). Returns true if anything changed. */
	public boolean put(BlockState fresh) {
		for(int i = 0; i < strands.size(); i++)
			if(strands.get(i).getBlock() == fresh.getBlock()) {
				if(strands.get(i) == fresh)
					return false;
				strands.set(i, fresh);
				setChanged();
				return true;
			}
		strands.add(fresh);
		strands.sort(Comparator.comparingInt(s -> WireBundleBlock.rank((WireBlock)s.getBlock())));
		setChanged();
		return true;
	}

	/** Takes the strand of {@code kind} out; returns its state, or null if there was none. */
	@Nullable
	public BlockState remove(WireBlock kind) {
		for(int i = 0; i < strands.size(); i++)
			if(strands.get(i).getBlock() == kind) {
				BlockState out = strands.remove(i);
				setChanged();
				return out;
			}
		return null;
	}

	// -------------------- SAVE AND SYNC --------------------

	@Override
	protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
		super.saveAdditional(tag, registries);
		ListTag list = new ListTag();
		for(BlockState strand : strands)
			list.add(NbtUtils.writeBlockState(strand));
		tag.put("strands", list);
		tag.putBoolean("flipX", flipX);
		tag.putBoolean("flipZ", flipZ);
	}

	@Override
	protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
		super.loadAdditional(tag, registries);
		List<BlockState> loaded = new ArrayList<>();
		for(Tag entry : tag.getList("strands", Tag.TAG_COMPOUND))
			loaded.add(NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), (CompoundTag)entry));
		// In the order saved: the run's solve may have chosen it (see #setOrder), not just the colours' rank.
		strands.clear();
		for(BlockState state : loaded)
			if(state.getBlock() instanceof WireBlock)
				strands.add(state);
		flipX = tag.getBoolean("flipX");
		flipZ = tag.getBoolean("flipZ");
	}

	@Override
	public @NotNull CompoundTag getUpdateTag(HolderLookup.@NotNull Provider registries) {
		return saveWithoutMetadata(registries);
	}

	@Nullable
	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public void onDataPacket(@NotNull Connection net, @NotNull ClientboundBlockEntityDataPacket pkt, HolderLookup.@NotNull Provider registries) {
		super.onDataPacket(net, pkt, registries);
		redraw();
	}

	@Override
	public void handleUpdateTag(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
		super.handleUpdateTag(tag, registries);
		redraw();
	}

	/** The look is worked out from the strands while the chunk is drawn: have it drawn again. */
	private void redraw() {
		if(level != null && level.isClientSide) {
			requestModelDataUpdate();
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
		}
	}
}
