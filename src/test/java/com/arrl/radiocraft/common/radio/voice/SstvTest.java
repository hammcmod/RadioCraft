package com.arrl.radiocraft.common.radio.voice;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SstvTest {

	@Test
	void mapTakesAboutTwentySixSeconds() {
		double[][] rows = new double[128][128];
		int frames = Sstv.frames(rows).size();
		assertEquals(Sstv.frameCount(128, 128), frames);
		assertEquals(26.0D, frames * 0.02D, 1.0D);
	}
}
