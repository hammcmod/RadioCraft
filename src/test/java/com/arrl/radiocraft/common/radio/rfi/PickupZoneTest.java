package com.arrl.radiocraft.common.radio.rfi;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PickupZoneTest {

	private static final BlockPos RADIO = new BlockPos(0, 64, 0);
	/** Far enough down the coax that the antenna radius does not reach back to the radio. */
	private static final BlockPos BALUN = RADIO.south(7);

	/** Radio at the origin, six coax blocks south, a balun, and a 5+5 dipole east-west. */
	private static PickupZone station() {
		List<BlockPos> feed = new ArrayList<>();
		for(int i = 0; i < 7; i++)
			feed.add(RADIO.south(i));
		List<BlockPos> antenna = new ArrayList<>();
		antenna.add(BALUN);
		PickupZone.sampleLine(BALUN, BALUN.east(5), antenna);
		PickupZone.sampleLine(BALUN, BALUN.west(5), antenna);
		return PickupZone.of(RADIO, feed, antenna);
	}

	@Test
	void sampleLineCoversEveryBlockAlongAWire() {
		List<BlockPos> out = new ArrayList<>();
		PickupZone.sampleLine(new BlockPos(0, 0, 0), new BlockPos(4, 0, 2), out);
		assertEquals(5, out.size());
		assertEquals(new BlockPos(0, 0, 0), out.getFirst());
		assertEquals(new BlockPos(4, 0, 2), out.getLast());
	}

	@Test
	void couplingIsFullOnTheWireAndZeroAtTheRadius() {
		PickupZone zone = station();
		assertEquals(1.0D, zone.coupling(RADIO), 1e-9);
		assertEquals(1.0D, zone.coupling(BALUN.east(5)), 1e-9, "end of the dipole arm");
		assertEquals(0.0D, zone.coupling(RADIO.north(PickupZone.FEEDLINE_RADIUS)), 1e-9, "at the feedline radius");
		assertEquals(0.0D, zone.coupling(BALUN.east(5 + PickupZone.ANTENNA_RADIUS)), 1e-9, "at the antenna radius");
		assertEquals(0.0D, zone.coupling(RADIO.east(40)), 1e-9, "far away");
	}

	@Test
	void couplingFadesLinearly() {
		PickupZone zone = station();
		assertEquals(0.5D, zone.coupling(RADIO.north(2)), 1e-9, "half way to the feedline radius");
		assertEquals(0.5D, zone.coupling(BALUN.above(4)), 1e-9, "half way to the antenna radius, above the balun");
	}

	@Test
	void strongerOfFeedlineAndAntennaWins() {
		PickupZone zone = station();
		// 3 east of the coax, 4 blocks short of the balun: 3 from the feedline (0.25), 4 from the east dipole arm (0.5).
		assertEquals(0.5D, zone.coupling(RADIO.south(3).east(3)), 1e-9);
	}

	@Test
	void boundingBoxCoversAllRadii() {
		PickupZone zone = station();
		assertTrue(zone.mayAffect(BALUN.east(5 + PickupZone.ANTENNA_RADIUS - 1)));
		assertFalse(zone.mayAffect(BALUN.east(5 + PickupZone.ANTENNA_RADIUS + 1)));
		assertTrue(zone.mayAffect(RADIO.north(PickupZone.FEEDLINE_RADIUS)));
		assertFalse(zone.mayAffect(RADIO.north(PickupZone.FEEDLINE_RADIUS + 1)));
		assertTrue(zone.chunks().length >= 1);
	}
}
