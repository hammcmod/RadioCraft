package com.arrl.radiocraft.common.radio.tape;

import com.arrl.radiocraft.common.radio.voice.RadioVoiceProcessor;
import com.jcraft.jogg.Packet;
import com.jcraft.jogg.Page;
import com.jcraft.jogg.StreamState;
import com.jcraft.jogg.SyncState;
import com.jcraft.jorbis.Block;
import com.jcraft.jorbis.Comment;
import com.jcraft.jorbis.DspState;
import com.jcraft.jorbis.Info;

import java.io.IOException;
import java.io.InputStream;

/**
 * Decodes an Ogg Vorbis file into the radio's audio: mono, 16 bit, at {@link RadioVoiceProcessor#SAMPLE_RATE}. Uses
 * JOrbis (pure Java, so it works on a dedicated server without Minecraft's sound library). Stereo is mixed down, other
 * sample rates are resampled, and anything past {@link #MAX_SECONDS} is cut off.
 */
public final class OggDecoder {

	/** The longest a tape plays, seconds. */
	public static final int MAX_SECONDS = 300;
	private static final int CHUNK = 4096;

	private OggDecoder() {
	}

	public static short[] decode(InputStream in) throws IOException {
		SyncState sync = new SyncState();
		StreamState stream = new StreamState();
		Page page = new Page();
		Packet packet = new Packet();
		Info info = new Info();
		Comment comment = new Comment();
		DspState dsp = new DspState();
		Block block = new Block(dsp);
		sync.init();
		try {
			if(!fill(sync, in) || sync.pageout(page) != 1)
				throw new IOException("not an Ogg stream");
			stream.init(page.serialno());
			info.init();
			comment.init();
			if(stream.pagein(page) < 0 || stream.packetout(packet) != 1 || info.synthesis_headerin(comment, packet) < 0)
				throw new IOException("not Vorbis audio");
			int headers = 0; // Two more header packets: the comments and the codebooks.
			while(headers < 2) {
				int result = sync.pageout(page);
				if(result == 0) {
					if(!fill(sync, in))
						throw new IOException("the file ends inside its headers");
					continue;
				}
				if(result < 0)
					continue;
				stream.pagein(page);
				while(headers < 2 && stream.packetout(packet) == 1) {
					info.synthesis_headerin(comment, packet);
					headers++;
				}
			}
			dsp.synthesis_init(info);
			block.init(dsp);

			int channels = info.channels;
			int maxSamples = MAX_SECONDS * info.rate;
			float[] mono = new float[info.rate * 10];
			int length = 0;
			float[][][] pcm = new float[1][][];
			int[] index = new int[channels];
			boolean end = false;
			long granule = -1; // The stream's own count of its samples, from its last page: the end is padded past it.
			while(!end && length < maxSamples) {
				int result = sync.pageout(page);
				if(result == 0) {
					if(!fill(sync, in))
						break;
					continue;
				}
				if(result < 0)
					continue; // A gap in the stream: skip it.
				stream.pagein(page);
				if(page.granulepos() > 0)
					granule = page.granulepos();
				while(stream.packetout(packet) == 1) {
					if(block.synthesis(packet) == 0)
						dsp.synthesis_blockin(block);
					int samples;
					while((samples = dsp.synthesis_pcmout(pcm, index)) > 0) {
						int take = Math.min(samples, maxSamples - length);
						if(length + take > mono.length)
							mono = java.util.Arrays.copyOf(mono, Math.max(length + take, mono.length * 2));
						for(int i = 0; i < take; i++) {
							float sum = 0;
							for(int c = 0; c < channels; c++)
								sum += pcm[0][c][index[c] + i];
							mono[length + i] = sum / channels;
						}
						length += take;
						dsp.synthesis_read(samples);
					}
				}
				end = page.eos() != 0;
			}
			if(granule > 0 && granule < length)
				length = (int)granule;
			return resample(mono, length, info.rate);
		}
		finally {
			stream.clear();
			block.clear();
			dsp.clear();
			info.clear();
			sync.clear();
		}
	}

	/** Reads the next chunk of the file into {@code sync}; false at the end of the file. */
	private static boolean fill(SyncState sync, InputStream in) throws IOException {
		int at = sync.buffer(CHUNK);
		int read = in.read(sync.data, at, CHUNK);
		if(read <= 0)
			return false;
		sync.wrote(read);
		return true;
	}

	/** {@code length} samples at {@code rate} to 16 bit at the radio's rate, by linear interpolation. */
	static short[] resample(float[] samples, int length, int rate) {
		int outRate = (int)RadioVoiceProcessor.SAMPLE_RATE;
		int outLength = (int)((long)length * outRate / Math.max(1, rate));
		short[] out = new short[outLength];
		double step = (double)rate / outRate;
		for(int i = 0; i < outLength; i++) {
			double at = i * step;
			int a = (int)at;
			int b = Math.min(length - 1, a + 1);
			double f = at - a;
			double v = samples[a] * (1 - f) + samples[b] * f;
			out[i] = (short)Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, Math.round(v * 32767.0D)));
		}
		return out;
	}
}
