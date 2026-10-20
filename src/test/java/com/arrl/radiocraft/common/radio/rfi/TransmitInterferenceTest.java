package com.arrl.radiocraft.common.radio.rfi;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TransmitInterferenceTest {

	private static final BlockPos CENTER = new BlockPos(0, 64, 0);
	private static final List<BlockPos> WIRE = List.of(CENTER, CENTER.east(1), CENTER.east(2), CENTER.east(3));

	@Test
	void reachIsADistanceFromAnyWirePoint() {
		assertTrue(TransmitInterference.inReach(CENTER.east(3).south(6), WIRE, 6, false));
		assertFalse(TransmitInterference.inReach(CENTER.east(3).south(7), WIRE, 6, false));
		assertFalse(TransmitInterference.inReach(CENTER, WIRE, 0, false), "zero reach is no reach");
	}

	@Test
	void verticalAntennasOnlyReachSideways() {
		BlockPos aboveClose = CENTER.above(3);
		assertTrue(TransmitInterference.inReach(aboveClose, WIRE, 6, false), "a horizontal antenna reaches 3 up");
		assertFalse(TransmitInterference.inReach(aboveClose, WIRE, 6, true), "a vertical does not");
		assertTrue(TransmitInterference.inReach(CENTER.above(2).south(5), WIRE, 6, true), "two up and sideways is fine");
	}

	@Test
	void rollIsDeterministicAndSpread() {
		long seed = TransmitInterference.seed(CENTER, CENTER.south(3), 1.0D, false);
		assertEquals(TransmitInterference.roll(seed, CENTER.east(2)), TransmitInterference.roll(seed, CENTER.east(2)));
		int below = 0;
		for(int x = -20; x <= 20; x++)
			for(int z = -20; z <= 20; z++) {
				double r = TransmitInterference.roll(seed, CENTER.offset(x, 0, z));
				assertTrue(r >= 0.0D && r < 1.0D);
				if(r < 0.35D)
					below++;
			}
		assertTrue(below > 1681 * 0.25 && below < 1681 * 0.45, "about 35% of positions should be hit, got " + below + " of 1681");
	}

	@Test
	void seedChangesWithStationNotWithTime() {
		long a = TransmitInterference.seed(CENTER, CENTER.south(3), 1.2D, false);
		assertEquals(a, TransmitInterference.seed(CENTER, CENTER.south(3), 1.7D, false), "same SWR bucket, same seed");
		assertNotEquals(a, TransmitInterference.seed(CENTER, CENTER.south(3), 2.5D, false), "a worse match re-picks");
		assertNotEquals(a, TransmitInterference.seed(CENTER, CENTER.south(3), 1.2D, true), "a different rig re-picks");
		assertNotEquals(a, TransmitInterference.seed(CENTER, CENTER.south(4), 1.2D, false), "a moved antenna re-picks");
	}
}
