package com.arrl.radiocraft.common.radio.rfi;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LightningStaticTest {

	@Test
	void strikeOnTopOfTheStationIsFull() {
		assertEquals(1.0D, LightningStatic.falloff(0.0D, 512.0D), 1e-9);
	}

	@Test
	void fallsLinearlyToNothingAtRange() {
		assertEquals(0.5D, LightningStatic.falloff(256.0D, 512.0D), 1e-9);
		assertEquals(0.0D, LightningStatic.falloff(512.0D, 512.0D), 1e-9);
		assertEquals(0.0D, LightningStatic.falloff(1000.0D, 512.0D), 1e-9);
	}

	@Test
	void closerIsLouder() {
		assertTrue(LightningStatic.falloff(10.0D, 512.0D) > LightningStatic.falloff(100.0D, 512.0D));
	}

	@Test
	void zeroRangeHearsNothing() {
		assertEquals(0.0D, LightningStatic.falloff(0.0D, 0.0D), 1e-9);
	}
}
