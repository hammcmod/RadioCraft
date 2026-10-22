package com.arrl.radiocraft.common.radio.voice;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class RttyTest {

	@Test
	void sixCharactersTakeAboutASecond() {
		List<short[]> frames = Rtty.frames("CQ CQ ", false, new double[1]);
		assertEquals(Rtty.seconds(6), frames.size() * 0.02D, 0.02D);
		assertEquals(0.99D, Rtty.seconds(6), 0.01D);
	}

	@Test
	void leadInAddsSteadyMark() {
		int without = Rtty.frames("A", false, new double[1]).size();
		int with = Rtty.frames("A", true, new double[1]).size();
		assertEquals(Rtty.LEAD_IN_SECONDS / 0.02D, with - without, 1.0D);
	}

	@Test
	void teleprinterIsUpperCaseOnly() {
		assertEquals("CQ DE W1AW?\n", Rtty.sanitize("cq de w1aw?\r", 64));
		assertEquals("AB", Rtty.sanitize("a{b}~", 64));
	}

	@Test
	void garbleKeepsLengthAndLineBreaks() {
		String text = "THE QUICK BROWN FOX\nJUMPS OVER THE LAZY DOG";
		String garbled = Rtty.garble(text, 0.5D, new Random(1L));
		assertEquals(text.length(), garbled.length());
		assertEquals(text.indexOf('\n'), garbled.indexOf('\n'));
		assertNotEquals(text, garbled);
		assertEquals(text, Rtty.garble(text, 0.0D, new Random(1L)));
	}
}
