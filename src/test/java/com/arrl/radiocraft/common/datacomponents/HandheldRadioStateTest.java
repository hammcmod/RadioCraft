package com.arrl.radiocraft.common.datacomponents;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HandheldRadioStateTest {

	private static final HandheldRadioState STATE = new HandheldRadioState(true, false, 146_940_000.0F, 1.0F, 1.0F, 0.0F, 100.0F, -600_000, 123.0F);

	@Test
	void txAndRxTonesSurviveSaving() {
		JsonElement saved = HandheldRadioState.CODEC.encodeStart(JsonOps.INSTANCE, STATE).getOrThrow();
		HandheldRadioState loaded = HandheldRadioState.CODEC.parse(JsonOps.INSTANCE, saved).getOrThrow();
		assertEquals(100.0F, loaded.ctcssTone());
		assertEquals(123.0F, loaded.rxTone());
		assertEquals(-600_000, loaded.offsetHz());
	}

	@Test
	void anOldSaveHasItsToneAsTheTxToneAndNoRxTone() {
		JsonObject old = new JsonObject();
		old.addProperty("power", true);
		old.addProperty("ptt", false);
		old.addProperty("frequency", 146_520_000.0F);
		old.addProperty("gain", 1.0F);
		old.addProperty("micGain", 1.0F);
		old.addProperty("ctcssTone", 100.0F);
		HandheldRadioState loaded = HandheldRadioState.CODEC.parse(JsonOps.INSTANCE, old).getOrThrow();
		assertEquals(100.0F, loaded.ctcssTone());
		assertEquals(0.0F, loaded.rxTone());
	}

	@Test
	void txAndRxTonesSurviveTheNetwork() {
		ByteBuf buf = Unpooled.buffer();
		HandheldRadioState.STREAM_CODEC.encode(buf, STATE);
		assertEquals(STATE, HandheldRadioState.STREAM_CODEC.decode(buf));
	}

	@Test
	void settingOneToneKeepsTheOther() {
		assertEquals(100.0F, STATE.withRxTone(0.0F).ctcssTone());
		assertEquals(123.0F, STATE.withToneAndOffset(0.0F, 0).rxTone());
		HandheldRadioState retuned = new HandheldRadioState(true, false, 147_000_000.0F, 1.0F, 1.0F, 0.0F).withTonesOf(STATE);
		assertEquals(123.0F, retuned.rxTone());
		assertEquals(-600_000, retuned.offsetHz());
	}
}
