package com.arrl.radiocraft.common.radio.morse;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class CWReceiveBuffer {

	private final List<PlaybackEntry> playbackBuffer = new ArrayList<>();

	/**
	 * Adds a {@link CWBuffer} to the playback queue, sorts lowest ID to be first.
	 *
	 * @param buffer The {@link CWBuffer} to be added to playback.
	 * @param strength The strength of the on states in the buffer.
	 */
	public void addToBuffer(CWBuffer buffer, float strength) {
		if(playbackBuffer.isEmpty()) {
			playbackBuffer.add(new PlaybackEntry(buffer, strength));
		}
		else {
			for(int i = 0; i < playbackBuffer.size(); i++) {
				if(playbackBuffer.get(i).buffer.getId() > buffer.getId()) {
					playbackBuffer.add(i, new PlaybackEntry(buffer, strength));
					return;
				}

				if(i == playbackBuffer.size() - 1) {
					playbackBuffer.add(new PlaybackEntry(buffer, strength));
					return;
				}
			}
		}
	}

	/**
	 * @return Grab the strength of the next tick to be played, or 0 if no value is present.
	 */
	public float getNextStrength() {
		if(!playbackBuffer.isEmpty()) {
			PlaybackEntry entry = playbackBuffer.get(0);
			boolean f = entry.buffer.readNext();

			if(entry.buffer.isFinished())
				playbackBuffer.remove(entry);

			if(f)
				return entry.strength;
		}

		return 0.0F;
	}

	public record PlaybackEntry(@Nonnull CWBuffer buffer, float strength) { }

}
