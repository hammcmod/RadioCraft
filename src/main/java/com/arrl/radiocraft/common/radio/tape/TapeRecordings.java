package com.arrl.radiocraft.common.radio.tape;

import com.arrl.radiocraft.Radiocraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * What players record on custom tapes, kept with the world in radiocraft/recordings, one file per tape named by the
 * tape's id. Stored as telephone-quality audio, 8 bit mu-law at 16 kHz (a minute is under a megabyte): it only ever
 * goes out over a radio, which cuts it to that anyway. Read and written off the server thread.
 */
public final class TapeRecordings {

	/** The longest recording on a tape, seconds. */
	public static final int MAX_SECONDS = 60;
	private static final int DOWN = 3; // 48 kHz to 16 kHz.

	private static final Map<UUID, short[]> CACHE = new ConcurrentHashMap<>();
	private static final Map<UUID, CompletableFuture<short[]>> LOADING = new ConcurrentHashMap<>();

	private TapeRecordings() {
	}

	private static Path file(MinecraftServer server, UUID id) {
		return server.getWorldPath(LevelResource.ROOT).resolve(Radiocraft.MOD_ID).resolve("recordings").resolve(id + ".ulaw");
	}

	/** Saves {@code audio} (48 kHz) as the recording of tape {@code id}. */
	public static void save(MinecraftServer server, UUID id, short[] audio) {
		CACHE.put(id, expand(compress(audio))); // As it will sound played back.
		byte[] data = compress(audio);
		Path file = file(server, id);
		CompletableFuture.runAsync(() -> {
			try {
				Files.createDirectories(file.getParent());
				Files.write(file, data);
			}
			catch(IOException e) {
				Radiocraft.LOGGER.warn("Could not save the tape recording {}", file, e);
			}
		});
	}

	/** The recording of tape {@code id} at 48 kHz, or null while it is read in the background (empty if there is none). */
	@Nullable
	public static short[] audio(MinecraftServer server, UUID id) {
		short[] ready = CACHE.get(id);
		if(ready != null)
			return ready;
		Path file = file(server, id);
		LOADING.computeIfAbsent(id, key -> CompletableFuture.supplyAsync(() -> {
			try {
				return Files.isRegularFile(file) ? expand(Files.readAllBytes(file)) : new short[0];
			}
			catch(IOException e) {
				return new short[0];
			}
		}).whenComplete((audio, error) -> {
			if(audio != null)
				CACHE.put(id, audio);
			LOADING.remove(id);
		}));
		return null;
	}

	/** 48 kHz 16 bit to 16 kHz mu-law, averaging each three samples. */
	static byte[] compress(short[] audio) {
		byte[] out = new byte[audio.length / DOWN];
		for(int i = 0; i < out.length; i++) {
			int sum = 0;
			for(int j = 0; j < DOWN; j++)
				sum += audio[i * DOWN + j];
			out[i] = linearToMuLaw((short)(sum / DOWN));
		}
		return out;
	}

	/** Back to 48 kHz 16 bit, by linear interpolation. */
	static short[] expand(byte[] data) {
		short[] out = new short[data.length * DOWN];
		for(int i = 0; i < data.length; i++) {
			int a = muLawToLinear(data[i]);
			int b = i + 1 < data.length ? muLawToLinear(data[i + 1]) : a;
			for(int j = 0; j < DOWN; j++)
				out[i * DOWN + j] = (short)(a + (b - a) * j / DOWN);
		}
		return out;
	}

	private static final int BIAS = 0x84;
	private static final int CLIP = 32635;

	/** G.711 mu-law. */
	static byte linearToMuLaw(short sample) {
		int pcm = sample;
		int sign = (pcm >> 8) & 0x80;
		if(sign != 0)
			pcm = -pcm;
		pcm = Math.min(pcm, CLIP) + BIAS;
		int exponent = 7;
		for(int mask = 0x4000; (pcm & mask) == 0 && exponent > 0; mask >>= 1)
			exponent--;
		int mantissa = (pcm >> (exponent + 3)) & 0x0F;
		return (byte)~(sign | (exponent << 4) | mantissa);
	}

	static int muLawToLinear(byte value) {
		int mu = ~value & 0xFF;
		int sign = mu & 0x80;
		int exponent = (mu >> 4) & 0x07;
		int mantissa = mu & 0x0F;
		int sample = ((mantissa << 3) + BIAS) << exponent;
		sample -= BIAS;
		return sign != 0 ? -sample : sample;
	}
}
