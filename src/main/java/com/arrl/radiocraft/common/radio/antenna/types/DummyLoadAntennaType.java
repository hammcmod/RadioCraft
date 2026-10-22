package com.arrl.radiocraft.common.radio.antenna.types;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.RadiocraftServerConfig;
import com.arrl.radiocraft.api.antenna.IAntennaPacket;
import com.arrl.radiocraft.api.antenna.IAntennaType;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.radio.antenna.StaticAntenna;
import com.arrl.radiocraft.common.radio.antenna.data.EmptyAntennaData;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/**
 * A dummy load: a resistor that soaks up the transmitter's power instead of radiating it. It is a perfect match (SWR
 * 1.0) on every frequency, so a radio can be keyed into it safely, but only a trickle gets out: strength
 * {@link RadiocraftServerConfig#DUMMY_LOAD_STRENGTH} beside it, falling to nothing at
 * {@link RadiocraftServerConfig#DUMMY_LOAD_RANGE} blocks, both ways. No skip, no solar weather, no aurora.
 */
public class DummyLoadAntennaType implements IAntennaType<EmptyAntennaData> {

	private static final ResourceLocation ID = Radiocraft.id("dummy_load");

	@Override
	public ResourceLocation getId() {
		return ID;
	}

	@Override
	public StaticAntenna<EmptyAntennaData> match(Level level, BlockPos pos) {
		return level.getBlockState(pos).is(RadiocraftBlocks.DUMMY_LOAD.get()) ? new StaticAntenna<>(this, pos, level) : null;
	}

	/** Share of the signal left after {@code distance} blocks: 1 at the load, 0 at its range. */
	public static double falloff(double distance) {
		double range = Math.max(1.0D, RadiocraftServerConfig.DUMMY_LOAD_RANGE.get());
		return Math.max(0.0D, 1.0D - distance / range);
	}

	private static double distance(IAntennaPacket packet, BlockPos other) {
		if(packet.getSource() == null || packet.getSource().getAntennaPos() == null || packet.getSource().getAntennaPos().level() != packet.getLevel())
			return Double.MAX_VALUE;
		return Math.sqrt(packet.getSource().getAntennaPos().position().distSqr(other));
	}

	@Override
	public double getTransmitEfficiency(IAntennaPacket packet, EmptyAntennaData data, BlockPos destination, boolean isCW) {
		return RadiocraftServerConfig.DUMMY_LOAD_STRENGTH.get() * falloff(distance(packet, destination));
	}

	/** It hears only what is within its range; the sender's own antenna already decided how strong that is. */
	@Override
	public double getReceiveEfficiency(IAntennaPacket packet, EmptyAntennaData data, BlockPos pos) {
		return falloff(distance(packet, pos)) > 0.0D ? packet.getStrength() : 0.0D;
	}

	@Override
	public double getSWR(EmptyAntennaData data, float frequencyHertz) {
		return 1.0D;
	}

	@Override
	public EmptyAntennaData getDefaultData() {
		return new EmptyAntennaData();
	}
}
