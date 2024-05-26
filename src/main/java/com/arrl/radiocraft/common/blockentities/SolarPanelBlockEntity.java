package com.arrl.radiocraft.common.blockentities;

import com.arrl.radiocraft.api.benetworks.BENetworkObject;
import com.arrl.radiocraft.common.be_networks.network_objects.SolarPanelNetworkObject;
import com.arrl.radiocraft.common.init.RadiocraftBlockEntities;
import com.arrl.radiocraft.common.menus.SolarPanelMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class SolarPanelBlockEntity extends PowerBlockEntity {

	private final DataSlot data = new DataSlot() {

		@Override
		public int get() {
			return ((SolarPanelNetworkObject)getNetworkObject(level, worldPosition)).getLastPowerTick();
		}

		@Override
		public void set(int value) {} // Empty set because the client isn't allowed to set this value.

	};

	public SolarPanelBlockEntity(BlockPos pos, BlockState state) {
		super(RadiocraftBlockEntities.SOLAR_PANEL.get(), pos, state);
	}

	public static <T extends BlockEntity> void tick(Level level, BlockPos pos, BlockState state, T t) {
		if(t instanceof SolarPanelBlockEntity be)
			((SolarPanelNetworkObject)be.getNetworkObject(level, pos)).setCanSeeSky(level.canSeeSky(pos)); // Just update the power gen value in tick.
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable("container.solar_panel");
	}

	@Nullable
	@Override
	public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
		return new SolarPanelMenu(id, this, data);
	}

	@Override
	public BENetworkObject createNetworkObject() {
		return new SolarPanelNetworkObject(level, worldPosition);
	}

}
