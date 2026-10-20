package com.arrl.radiocraft.common.blockentities;

import com.arrl.radiocraft.common.blockentities.radio.RadioBlockEntity;
import com.arrl.radiocraft.common.blocks.radios.ParrotControllerBlock;
import com.arrl.radiocraft.common.blocks.radios.ParrotControllerBlock.LinkSide;
import com.arrl.radiocraft.common.init.RadiocraftBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Drives the radio a {@link ParrotControllerBlock} is linked to: keeps the radio's parrot delay set to this
 * controller's delay while linked, clears it when the link goes away, and mirrors the radio's receive and echo
 * activity on the block's two LEDs. The delay is the only saved state; the radio itself forgets it is a parrot on
 * reload and is told again on the next tick.
 */
public class ParrotControllerBlockEntity extends BlockEntity {

	public static final int DEFAULT_DELAY_TICKS = 40;
	/** Delays right clicking cycles through, in seconds. */
	public static final int[] DELAY_STEPS_SECONDS = { 1, 2, 3, 5, 10 };
	/** How long an LED stays lit after the last frame of a signal or echo, so continuous audio reads as steady light. */
	public static final int LED_HOLD_TICKS = 10;

	private int delayTicks = DEFAULT_DELAY_TICKS;
	@Nullable
	private BlockPos controlledRadio; // The radio we last set a parrot delay on, so it can be reset when the link changes.

	public ParrotControllerBlockEntity(BlockPos pos, BlockState state) {
		super(RadiocraftBlockEntities.PARROT_CONTROLLER.get(), pos, state);
	}

	public static void tick(Level level, BlockPos pos, BlockState state, ParrotControllerBlockEntity be) {
		RadioBlockEntity radio = be.getLinkedRadio();
		BlockPos radioPos = radio == null ? null : radio.getBlockPos();
		if(!Objects.equals(radioPos, be.controlledRadio)) {
			be.release();
			be.controlledRadio = radioPos;
		}

		boolean receiving = false;
		boolean transmitting = false;
		if(radio != null) {
			if(radio.getParrotDelay() != be.delayTicks)
				radio.setParrotDelay(be.delayTicks);
			long now = level.getGameTime();
			receiving = radio.getLastSignalGameTime() >= 0 && now - radio.getLastSignalGameTime() <= LED_HOLD_TICKS;
			transmitting = radio.getLastParrotEchoGameTime() >= 0 && now - radio.getLastParrotEchoGameTime() <= LED_HOLD_TICKS;
		}

		if(state.getValue(ParrotControllerBlock.RECEIVING) != receiving || state.getValue(ParrotControllerBlock.TRANSMITTING) != transmitting)
			level.setBlock(pos, state.setValue(ParrotControllerBlock.RECEIVING, receiving).setValue(ParrotControllerBlock.TRANSMITTING, transmitting), Block.UPDATE_CLIENTS);
	}

	/** The radio on the side {@link ParrotControllerBlock#LINK} points at, if there is one. */
	@Nullable
	public RadioBlockEntity getLinkedRadio() {
		if(level == null)
			return null;
		LinkSide side = getBlockState().getValue(ParrotControllerBlock.LINK);
		if(side == LinkSide.NONE)
			return null;
		return level.getBlockEntity(worldPosition.relative(side.direction())) instanceof RadioBlockEntity radio ? radio : null;
	}

	/** Stops the radio this controller was driving from parroting. Called when the link changes or the block is removed. */
	public void release() {
		if(controlledRadio != null && level != null && level.getBlockEntity(controlledRadio) instanceof RadioBlockEntity radio)
			radio.setParrotDelay(0);
		controlledRadio = null;
	}

	public int getDelayTicks() {
		return delayTicks;
	}

	public void setDelayTicks(int delayTicks) {
		this.delayTicks = Math.max(1, delayTicks);
		setChanged(); // The linked radio picks the new value up on the next tick.
		if(level != null)
			level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock()); // Comparators show the delay.
	}

	/** What a comparator reads off the unit: the delay in whole seconds, 1 to 15. */
	public int getComparatorOutput() {
		return Mth.clamp(Math.round(delayTicks / 20.0F), 1, 15);
	}

	/** Moves to the next entry of {@link #DELAY_STEPS_SECONDS} (wrapping), and returns the new delay in ticks. */
	public int cycleDelay() {
		int next = DELAY_STEPS_SECONDS[0] * 20;
		for(int i = 0; i < DELAY_STEPS_SECONDS.length; i++) {
			if(DELAY_STEPS_SECONDS[i] * 20 > delayTicks) {
				next = DELAY_STEPS_SECONDS[i] * 20;
				break;
			}
		}
		setDelayTicks(next);
		return delayTicks;
	}

	@Override
	protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
		super.saveAdditional(tag, registries);
		tag.putInt("delayTicks", delayTicks);
		if(controlledRadio != null)
			tag.put("controlledRadio", NbtUtils.writeBlockPos(controlledRadio));
	}

	@Override
	protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
		super.loadAdditional(tag, registries);
		if(tag.contains("delayTicks"))
			delayTicks = Math.max(1, tag.getInt("delayTicks"));
		controlledRadio = NbtUtils.readBlockPos(tag, "controlledRadio").orElse(null);
	}
}
