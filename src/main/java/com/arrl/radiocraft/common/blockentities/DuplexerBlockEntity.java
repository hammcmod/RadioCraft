package com.arrl.radiocraft.common.blockentities;

import com.arrl.radiocraft.api.benetworks.BENetworkObject;
import com.arrl.radiocraft.api.capabilities.IBENetworks;
import com.arrl.radiocraft.common.be_networks.network_objects.DuplexerNetworkObject;
import com.arrl.radiocraft.common.init.RadiocraftBlockEntities;
import com.arrl.radiocraft.common.menus.DuplexerMenu;
import com.arrl.radiocraft.common.radio.DuplexerMath;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The duplexer. Its six cavity screws live on its network object ({@link DuplexerNetworkObject}); this block entity
 * owns the screen, which shows and turns them.
 */
public class DuplexerBlockEntity extends PowerBlockEntity {

	/** Each screw is synced to the screen as kHz above the bottom of 2m (menu data slots carry shorts), -1 untuned. */
	public static final int BAND_BOTTOM_KHZ = 144_000;

	private final ContainerData data = new ContainerData() {
		@Override
		public int get(int index) {
			int hz = getScrewHz(index);
			return hz <= 0 ? -1 : hz / 1000 - BAND_BOTTOM_KHZ;
		}

		@Override
		public void set(int index, int value) {}

		@Override
		public int getCount() {
			return DuplexerMath.SCREWS;
		}
	};

	public DuplexerBlockEntity(BlockPos pos, BlockState state) {
		super(RadiocraftBlockEntities.DUPLEXER.get(), pos, state);
	}

	@Override
	public @NotNull Component getDisplayName() {
		return Component.translatable("container.radiocraft.duplexer");
	}

	@Nullable
	@Override
	public AbstractContainerMenu createMenu(int id, @NotNull Inventory playerInventory, @NotNull Player player) {
		return new DuplexerMenu(id, this, data);
	}

	@Override
	public BENetworkObject createNetworkObject() {
		return new DuplexerNetworkObject(level, worldPosition); // Passes the coax through; see VHFRepeaterBlockEntity.
	}

	@Nullable
	private DuplexerNetworkObject cavities() {
		return level != null && IBENetworks.getObject(level, worldPosition) instanceof DuplexerNetworkObject cavities ? cavities : null;
	}

	/** A screw's frequency in Hz, 0 while untuned (server side). */
	public int getScrewHz(int index) {
		DuplexerNetworkObject cavities = cavities();
		return cavities == null ? 0 : cavities.getScrewHz(index);
	}

	/** Turns a screw (from the screen's packet). */
	public void setScrewHz(int index, int hz) {
		DuplexerNetworkObject cavities = cavities();
		if(cavities != null) {
			cavities.turnScrew(index, hz);
			setChanged();
		}
	}
}
