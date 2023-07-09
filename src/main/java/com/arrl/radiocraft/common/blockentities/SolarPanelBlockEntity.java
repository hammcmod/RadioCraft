package com.arrl.radiocraft.common.blockentities;

import com.arrl.radiocraft.RadiocraftConfig;
import com.arrl.radiocraft.common.init.RadiocraftBlockEntities;
import com.arrl.radiocraft.common.menus.SolarPanelMenu;
import com.arrl.radiocraft.common.benetworks.power.ConnectionType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class SolarPanelBlockEntity extends AbstractPowerBlockEntity {

	public final double rainMultiplier;
	private int lastPowerTick = 0;

	// Using a ContainerData for one value is awkward, but it changes constantly and needs to be synchronised.
	private final ContainerData fields = new ContainerData() {
		@Override
		public int get(int index) {
			if(index == 0)
				return lastPowerTick;
			return 0;
		}

		@Override
		public void set(int index, int value) {
			if(index == 0)
				lastPowerTick = value;
		}

		@Override
		public int getCount() {
			return 1;
		}
	};

	public SolarPanelBlockEntity(BlockPos pos, BlockState state) {
		super(RadiocraftBlockEntities.SOLAR_PANEL.get(), pos, state, RadiocraftConfig.SOLAR_PANEL_MAX_OUTPUT.get(), RadiocraftConfig.SOLAR_PANEL_MAX_OUTPUT.get());
		rainMultiplier = RadiocraftConfig.SOLAR_PANEL_RAIN_MULTIPLIER.get();
	}

	public static <T extends BlockEntity> void tick(Level level, BlockPos pos, BlockState state, T t) {
		if(t instanceof SolarPanelBlockEntity be) {
			if(!level.isClientSide) { // Serverside only
				if(level.isDay() && level.canSeeSky(pos)) { // Time is day & solar panel has direct skylight
					int powerGenerated = level.isRaining() ? (int)Math.round(be.energyStorage.getMaxReceive() * be.rainMultiplier) : be.energyStorage.getMaxReceive();
					be.energyStorage.receiveEnergy(powerGenerated, false); // Generate power
					be.lastPowerTick = powerGenerated;
				}
				else {
					be.lastPowerTick = 0;
				}
				be.pushToAll(be.energyStorage.getMaxExtract(), true); // Push to connected networks
			}
		}
	}

	@Override
	public ConnectionType getConnectionType() {
		return ConnectionType.PUSH;
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable("container.solar_panel");
	}

	@Nullable
	@Override
	public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
		return new SolarPanelMenu(id, this, fields);
	}
}
