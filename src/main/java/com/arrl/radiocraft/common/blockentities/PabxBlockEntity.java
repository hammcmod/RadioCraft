package com.arrl.radiocraft.common.blockentities;

import com.arrl.radiocraft.api.benetworks.BENetworkObject;
import com.arrl.radiocraft.api.benetworks.INetworkObjectProvider;
import com.arrl.radiocraft.common.be_networks.DataWire;
import com.arrl.radiocraft.common.be_networks.network_objects.PabxNetworkObject;
import com.arrl.radiocraft.common.blocks.PabxBlock;
import com.arrl.radiocraft.common.init.RadiocraftBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A phone PABX: the exchange for the telephones it serves (touching it, or on its data wire) when no microwave radio
 * serves them, so they can call each other (maintainer, 2026-10-30). A microwave radio is its own PABX: a phone it
 * serves goes through the radio. It needs power ({@link PabxNetworkObject}), and numbers its phones: each new one gets
 * the lowest extension from {@link #FIRST_EXTENSION} up that none of its phones has. A phone's number can still be
 * changed from its screen.
 */
public class PabxBlockEntity extends BlockEntity implements INetworkObjectProvider {

	public static final int FIRST_EXTENSION = 100;

	/** The phones it has numbered, so a number set by hand later is left alone. */
	private final Set<BlockPos> numbered = new HashSet<>();
	private boolean powered = false;

	public PabxBlockEntity(BlockPos pos, BlockState state) {
		super(RadiocraftBlockEntities.PABX.get(), pos, state);
	}

	@Override
	public void onLoad() {
		super.onLoad();
		if(level != null && !level.isClientSide())
			getNetworkObject(level, worldPosition); // Joins the power wire.
	}

	@Override
	public BENetworkObject createNetworkObject() {
		return new PabxNetworkObject(level, worldPosition);
	}

	/** True while the wire feeds it. Server side. */
	public boolean isPowered() {
		return powered;
	}

	/** The telephones it serves: touching it, or on its data wire. */
	public List<TelephoneBlockEntity> phones() {
		return level == null ? List.of() : DataWire.linked(level, worldPosition, TelephoneBlockEntity.class);
	}

	/** The PABXes serving a phone at {@code pos}: touching it, or on its data wire. */
	public static List<PabxBlockEntity> serving(Level level, BlockPos pos) {
		return DataWire.linked(level, pos, PabxBlockEntity.class);
	}

	/** The phones a phone at {@code pos} can call through a powered PABX serving it (itself included). */
	public static List<TelephoneBlockEntity> reachable(Level level, BlockPos pos) {
		List<TelephoneBlockEntity> out = new ArrayList<>();
		for(PabxBlockEntity pabx : serving(level, pos))
			if(pabx.isPowered())
				for(TelephoneBlockEntity phone : pabx.phones())
					if(!out.contains(phone))
						out.add(phone);
		return out;
	}

	public static void tick(Level level, BlockPos pos, BlockState state, BlockEntity blockEntity) {
		if(blockEntity instanceof PabxBlockEntity pabx)
			pabx.serverTick(level, state);
	}

	private void serverTick(Level level, BlockState state) {
		boolean now = getNetworkObject(level, worldPosition) instanceof PabxNetworkObject object && object.isPowered;
		if(now != powered) {
			powered = now;
			setChanged();
		}
		if(state.getValue(PabxBlock.LIT) != powered)
			level.setBlock(worldPosition, state.setValue(PabxBlock.LIT, powered), Block.UPDATE_CLIENTS);
		if(powered && level.getGameTime() % 20 == 0)
			number();
	}

	/** Gives each phone it has not numbered yet the lowest free extension from {@link #FIRST_EXTENSION}. */
	private void number() {
		List<TelephoneBlockEntity> phones = phones();
		for(TelephoneBlockEntity phone : phones) {
			if(numbered.contains(phone.getBlockPos()))
				continue;
			Set<Integer> taken = new HashSet<>();
			for(TelephoneBlockEntity other : phones)
				if(other != phone && numbered.contains(other.getBlockPos()))
					taken.add(other.getExtension());
			int extension = FIRST_EXTENSION;
			while(taken.contains(extension))
				extension++;
			phone.assignExtension(extension);
			numbered.add(phone.getBlockPos().immutable());
			setChanged();
		}
		// Forget phones that are gone, so their numbers free up.
		Set<BlockPos> present = new HashSet<>();
		for(TelephoneBlockEntity phone : phones)
			present.add(phone.getBlockPos());
		if(numbered.retainAll(present))
			setChanged();
	}

	@Override
	protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
		super.saveAdditional(tag, registries);
		net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
		for(BlockPos pos : numbered)
			list.add(LongTag.valueOf(pos.asLong()));
		tag.put("numbered", list);
	}

	@Override
	protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
		super.loadAdditional(tag, registries);
		numbered.clear();
		for(Tag entry : tag.getList("numbered", Tag.TAG_LONG))
			numbered.add(BlockPos.of(((LongTag)entry).getAsLong()));
	}
}
