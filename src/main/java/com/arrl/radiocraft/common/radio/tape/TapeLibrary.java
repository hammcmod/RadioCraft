package com.arrl.radiocraft.common.radio.tape;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.RadiocraftServerConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.fml.loading.FMLPaths;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

/**
 * The server's tapes: .ogg recordings an admin puts in a folder (config/radiocraft/tapes, or the world's
 * radiocraft/tapes with {@code tapes_in_world}). A found tape names one of them by its path in the folder
 * ("old_broadcast.ogg", "numbers/station_a.ogg"). Files are decoded off the server thread and kept decoded while they
 * fit in {@link #CACHE_SAMPLES}; {@link #audio} hands back null until a file is ready (and asks for it).
 */
public final class TapeLibrary {

	/** Subfolder whose recordings numbers stations read out. */
	public static final String NUMBERS = "numbers";
	/** Decoded audio kept at once: about 40 MB. */
	private static final long CACHE_SAMPLES = 20_000_000L;

	private static final ExecutorService DECODER = Executors.newSingleThreadExecutor(runnable -> {
		Thread thread = new Thread(runnable, "RadioCraft tape decoder");
		thread.setDaemon(true);
		return thread;
	});
	private static final Map<String, short[]> CACHE = Collections.synchronizedMap(new LinkedHashMap<>(16, 0.75F, true));
	private static final Map<String, CompletableFuture<short[]>> LOADING = new java.util.concurrent.ConcurrentHashMap<>();

	private TapeLibrary() {
	}

	/** The folder the server's tapes are in (made if missing). */
	public static Path folder(MinecraftServer server) {
		Path folder = RadiocraftServerConfig.TAPES_IN_WORLD.get()
				? server.getWorldPath(LevelResource.ROOT).resolve(Radiocraft.MOD_ID).resolve("tapes")
				: FMLPaths.CONFIGDIR.get().resolve(Radiocraft.MOD_ID).resolve("tapes");
		try {
			Files.createDirectories(folder.resolve(NUMBERS));
		}
		catch(IOException e) {
			Radiocraft.LOGGER.warn("Could not make the tape folder {}", folder, e);
		}
		return folder;
	}

	/** The tapes in {@code subfolder} ("" for the top), as paths in the folder with forward slashes, sorted. */
	public static List<String> tracks(MinecraftServer server, String subfolder) {
		Path root = folder(server);
		Path where = subfolder.isEmpty() ? root : root.resolve(subfolder);
		List<String> out = new ArrayList<>();
		if(!Files.isDirectory(where))
			return out;
		try(Stream<Path> files = Files.list(where)) {
			files.filter(path -> Files.isRegularFile(path) && path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".ogg"))
					.forEach(path -> out.add(root.relativize(path).toString().replace('\\', '/')));
		}
		catch(IOException e) {
			Radiocraft.LOGGER.warn("Could not list the tapes in {}", where, e);
		}
		out.sort(null);
		return out;
	}

	/** The file for {@code track}, or null if the name would reach outside the folder. */
	@Nullable
	static Path file(MinecraftServer server, String track) {
		Path root = folder(server).toAbsolutePath().normalize();
		Path file = root.resolve(track).toAbsolutePath().normalize();
		return file.startsWith(root) && !file.equals(root) ? file : null;
	}

	/**
	 * The decoded audio of {@code track}: at once if it is ready, otherwise null while it is decoded in the background
	 * (and an empty array for good if the file is missing or not Vorbis).
	 */
	@Nullable
	public static short[] audio(MinecraftServer server, String track) {
		short[] ready = CACHE.get(track);
		if(ready != null)
			return ready;
		Path file = file(server, track);
		if(file == null)
			return new short[0];
		LOADING.computeIfAbsent(track, key -> CompletableFuture.supplyAsync(() -> decode(file), DECODER).whenComplete((audio, error) -> {
			if(audio != null)
				keep(track, audio);
			LOADING.remove(track);
		}));
		return null;
	}

	private static short[] decode(Path file) {
		if(!Files.isRegularFile(file))
			return new short[0];
		try(InputStream in = Files.newInputStream(file)) {
			return OggDecoder.decode(new java.io.BufferedInputStream(in));
		}
		catch(IOException | RuntimeException e) {
			Radiocraft.LOGGER.warn("Could not play the tape {}: {}", file.getFileName(), e.getMessage());
			return new short[0];
		}
	}

	private static void keep(String track, short[] audio) {
		synchronized(CACHE) {
			CACHE.put(track, audio);
			long total = 0;
			for(short[] kept : CACHE.values())
				total += kept.length;
			var oldest = CACHE.entrySet().iterator();
			while(total > CACHE_SAMPLES && oldest.hasNext()) {
				var entry = oldest.next();
				if(entry.getKey().equals(track))
					continue;
				total -= entry.getValue().length;
				oldest.remove();
			}
		}
	}

	/** Forgets every decoded tape: the admin changed the files. */
	public static void clearCache() {
		CACHE.clear();
	}
}
