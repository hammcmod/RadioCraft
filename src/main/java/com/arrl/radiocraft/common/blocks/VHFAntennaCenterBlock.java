package com.arrl.radiocraft.common.blocks;

import com.arrl.radiocraft.common.blockentities.AntennaBlockEntity;
import com.arrl.radiocraft.common.radio.antenna.networks.AntennaNetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Use for any blocks which contain an AntennaBlockEntity (for example baluns)
 */
public class VHFAntennaCenterBlock extends AntennaCenterBlock {

	public VHFAntennaCenterBlock(Properties properties) {
		super(properties);
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
		return new AntennaBlockEntity(pos, state, AntennaNetworkManager.VHF_ID);
	}

}
