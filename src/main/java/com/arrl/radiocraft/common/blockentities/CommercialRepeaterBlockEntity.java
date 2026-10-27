package com.arrl.radiocraft.common.blockentities;

import com.arrl.radiocraft.api.benetworks.BENetworkObject;
import com.arrl.radiocraft.common.be_networks.network_objects.RepeaterNetworkObject;
import com.arrl.radiocraft.common.blocks.StackedBlocks;
import com.arrl.radiocraft.common.blocks.radios.CommercialRepeaterBlock;
import com.arrl.radiocraft.common.init.RadiocraftBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The commercial repeater's works, in the bottom cell of its rack: a VHF repeater ({@link VHFRepeaterBlockEntity}) with
 * its duplexer built in, running off its own batteries and charger (it needs no power wire), and transmitting
 * {@link #POWER} times as strong as the VHF repeater. Coax from it still goes to an antenna. Its lamps are on the rack:
 * power, receive and transmit on the radio (cell 1), and the controller's beacon (cell 3) while it is on.
 */
public class CommercialRepeaterBlockEntity extends VHFRepeaterBlockEntity {

	public static final float POWER = 2.0F;

	public CommercialRepeaterBlockEntity(BlockPos pos, BlockState state) {
		super(RadiocraftBlockEntities.COMMERCIAL_REPEATER.get(), pos, state);
	}

	@Override
	protected boolean hasBuiltInDuplexer() {
		return true;
	}

	@Override
	protected float transmitPower() {
		return POWER;
	}

	@Override
	protected void additionalTick() {
		super.additionalTick();
		if(level == null || level.isClientSide)
			return;
		boolean on = isPowered();
		lamps(worldPosition.above(1), on, on && isInputActive(), on && isTransmitting());
		lamps(worldPosition.above(3), on, false, false);
	}

	private void lamps(BlockPos pos, boolean pwr, boolean rx, boolean tx) {
		BlockState state = level.getBlockState(pos);
		if(!(state.getBlock() instanceof CommercialRepeaterBlock) || state.getValue(StackedBlocks.PART) == 0)
			return;
		BlockState lit = state.setValue(CommercialRepeaterBlock.LAMP_PWR, pwr).setValue(CommercialRepeaterBlock.LAMP_RX, rx).setValue(CommercialRepeaterBlock.LAMP_TX, tx);
		if(lit != state)
			level.setBlock(pos, lit, Block.UPDATE_CLIENTS);
	}

	@Override
	public BENetworkObject createNetworkObject() {
		return new SelfPowered(level, worldPosition);
	}

	/** The repeater on the coax, running whenever it is switched on: its batteries and charger are in the rack. */
	public static class SelfPowered extends RepeaterNetworkObject {

		public static final ResourceLocation TYPE = com.arrl.radiocraft.Radiocraft.id("commercial_repeater");

		public SelfPowered(Level level, BlockPos pos) {
			super(level, pos, 0, 0);
		}

		@Override
		public void resolvePower() {
			isPowered = wantsOn;
		}

		@Override
		public ResourceLocation getType() {
			return TYPE;
		}
	}
}
