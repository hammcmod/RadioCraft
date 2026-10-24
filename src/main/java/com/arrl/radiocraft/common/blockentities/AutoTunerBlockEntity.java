package com.arrl.radiocraft.common.blockentities;

import com.arrl.radiocraft.api.benetworks.BENetworkObject;
import com.arrl.radiocraft.api.benetworks.INetworkObjectProvider;
import com.arrl.radiocraft.api.capabilities.IBENetworks;
import com.arrl.radiocraft.common.be_networks.network_objects.AutoTunerNetworkObject;
import com.arrl.radiocraft.common.init.RadiocraftBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The automatic antenna tuner. The tuning itself runs on its network object ({@link AutoTunerNetworkObject}); this
 * block entity only syncs its state to clients for the display (TUNING, TUNED, FAIL; dark without power).
 */
public class AutoTunerBlockEntity extends BlockEntity implements INetworkObjectProvider {

	private AutoTunerNetworkObject.State shownState = AutoTunerNetworkObject.State.IDLE;
	private boolean shownPowered = false;

	public AutoTunerBlockEntity(BlockPos pos, BlockState state) {
		super(RadiocraftBlockEntities.AUTO_TUNER.get(), pos, state);
	}

	@Override
	public BENetworkObject createNetworkObject() {
		return new AutoTunerNetworkObject(level, worldPosition);
	}

	@Override
	public void onLoad() {
		super.onLoad();
		if(!level.isClientSide())
			getNetworkObject(level, worldPosition); // Forces the network object to be created and wired.
	}

	/** Server side: sends the state to clients when it changes. */
	public static void tick(Level level, BlockPos pos, BlockState state, AutoTunerBlockEntity be) {
		if(level.isClientSide || !(IBENetworks.getObject(level, pos) instanceof AutoTunerNetworkObject tuner))
			return;
		if(tuner.getState() != be.shownState || tuner.isPowered() != be.shownPowered) {
			be.shownState = tuner.getState();
			be.shownPowered = tuner.isPowered();
			level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
		}
	}

	/** What the display shows (client side, synced). */
	public AutoTunerNetworkObject.State getShownState() {
		return shownState;
	}

	public boolean isShownPowered() {
		return shownPowered;
	}

	@Override
	public @NotNull CompoundTag getUpdateTag(HolderLookup.@NotNull Provider registries) {
		CompoundTag tag = super.getUpdateTag(registries);
		tag.putString("state", shownState.name());
		tag.putBoolean("powered", shownPowered);
		return tag;
	}

	@Override
	protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
		super.loadAdditional(tag, registries);
		if(tag.contains("state")) {
			try {
				shownState = AutoTunerNetworkObject.State.valueOf(tag.getString("state"));
			}
			catch(IllegalArgumentException e) {
				shownState = AutoTunerNetworkObject.State.IDLE;
			}
			shownPowered = tag.getBoolean("powered");
		}
	}

	@Nullable
	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}
}
