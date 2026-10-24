package com.arrl.radiocraft.common.radio.voice;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.radio.Callsigns;
import com.arrl.radiocraft.common.radio.data.TncPacket;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.audiochannel.StaticAudioChannel;
import de.maxhenkel.voicechat.api.opus.OpusEncoder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Radio moderation (maintainer, 2026-10-24): {@code /radio moderateall} lets an operator hear every voice that goes out
 * over any radio, clean (no static, no distance), with a line over the hotbar naming who is talking, and read every
 * RTTY and APRS packet sent, in chat. {@code /radio moderate <player>} does the same for one player only.
 * <p>
 * Voice is sent to the moderator on a static audio channel per speaker, encoded once per frame. A speaker heard by
 * several radios at once (a handheld and a desk mic, a repeater passing it on) is forwarded once per voice frame.
 */
@EventBusSubscriber(modid = Radiocraft.MOD_ID)
public final class RadioModeration {

	/** Moderator to the one player they listen to, or {@link #EVERYONE}. */
	private static final Map<UUID, UUID> MODERATORS = new ConcurrentHashMap<>();
	public static final UUID EVERYONE = new UUID(0L, 0L);

	/** Shortest gap between two frames forwarded for one speaker: a voice frame is 20 ms. */
	private static final long FRAME_NANOS = 15_000_000L;
	/** How often the "who is talking" line is refreshed, per speaker. */
	private static final long LABEL_NANOS = 1_000_000_000L;

	private static final Map<UUID, Long> LAST_FRAME = new ConcurrentHashMap<>();
	private static final Map<UUID, Long> LAST_LABEL = new ConcurrentHashMap<>();
	private static final Map<UUID, OpusEncoder> ENCODERS = new ConcurrentHashMap<>();
	/** Per moderator, a channel per speaker. */
	private static final Map<UUID, Map<UUID, StaticAudioChannel>> CHANNELS = new ConcurrentHashMap<>();

	private RadioModeration() {}

	/** Starts {@code moderator} listening to {@code target}, or to everyone ({@link #EVERYONE}). */
	public static void listen(ServerPlayer moderator, UUID target) {
		MODERATORS.put(moderator.getUUID(), target);
	}

	/** Stops; true if they were listening. */
	public static boolean stop(UUID moderator) {
		CHANNELS.remove(moderator);
		return MODERATORS.remove(moderator) != null;
	}

	public static boolean isActive() {
		return !MODERATORS.isEmpty();
	}

	@Nullable
	public static UUID listeningTo(UUID moderator) {
		return MODERATORS.get(moderator);
	}

	private static boolean hears(UUID target, @Nullable UUID speaker) {
		return EVERYONE.equals(target) || target.equals(speaker);
	}

	/**
	 * A voice frame going out over a radio ({@code frequencyHz}) from {@code speaker}. Called on the voice thread by
	 * every transmitter; does nothing while nobody moderates.
	 */
	public static void voice(de.maxhenkel.voicechat.api.ServerLevel level, @Nullable UUID speaker, short[] audio, float frequencyHz) {
		if(MODERATORS.isEmpty() || speaker == null || RadiocraftVoicePlugin.API == null)
			return;
		long now = System.nanoTime();
		Long last = LAST_FRAME.get(speaker);
		if(last != null && now - last < FRAME_NANOS)
			return; // Already forwarded through another radio.
		LAST_FRAME.put(speaker, now);
		byte[] opus = null;
		for(Map.Entry<UUID, UUID> entry : MODERATORS.entrySet()) {
			UUID moderator = entry.getKey();
			if(moderator.equals(speaker) || !hears(entry.getValue(), speaker))
				continue;
			StaticAudioChannel channel = channel(level, moderator, speaker);
			if(channel == null)
				continue;
			if(opus == null)
				opus = ENCODERS.computeIfAbsent(speaker, k -> RadiocraftVoicePlugin.API.createEncoder()).encode(audio);
			channel.send(opus);
		}
		Long labelled = LAST_LABEL.get(speaker);
		if(labelled == null || now - labelled > LABEL_NANOS) {
			LAST_LABEL.put(speaker, now);
			MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
			if(server != null)
				server.execute(() -> label(server, speaker, frequencyHz));
		}
	}

	@Nullable
	private static StaticAudioChannel channel(de.maxhenkel.voicechat.api.ServerLevel level, UUID moderator, UUID speaker) {
		Map<UUID, StaticAudioChannel> channels = CHANNELS.computeIfAbsent(moderator, k -> new ConcurrentHashMap<>());
		StaticAudioChannel channel = channels.get(speaker);
		if(channel != null)
			return channel;
		VoicechatConnection connection = RadiocraftVoicePlugin.API.getConnectionOf(moderator);
		if(connection == null)
			return null;
		UUID id = new UUID(moderator.getMostSignificantBits() ^ speaker.getLeastSignificantBits(), moderator.getLeastSignificantBits() ^ speaker.getMostSignificantBits());
		channel = RadiocraftVoicePlugin.API.createStaticAudioChannel(id, level, connection);
		if(channel != null)
			channels.put(speaker, channel);
		return channel;
	}

	/** "On air: Name (K1ABC) 146.520 MHz" over the moderators' hotbars. Server thread. */
	private static void label(MinecraftServer server, UUID speaker, float frequencyHz) {
		ServerPlayer player = server.getPlayerList().getPlayer(speaker);
		String name = player == null ? speaker.toString().substring(0, 8) : player.getGameProfile().getName();
		String callsign = Callsigns.playerCallsign(server, speaker);
		Component line = Component.translatable("message.radiocraft.moderate.on_air", name + (callsign == null ? "" : " (" + callsign + ")"),
				String.format(Locale.ROOT, "%.3f", frequencyHz / 1_000_000.0D)).withStyle(ChatFormatting.AQUA);
		for(Map.Entry<UUID, UUID> entry : MODERATORS.entrySet()) {
			ServerPlayer moderator = server.getPlayerList().getPlayer(entry.getKey());
			if(moderator != null && !entry.getKey().equals(speaker) && hears(entry.getValue(), speaker))
				moderator.displayClientMessage(line, true);
		}
	}

	/** An RTTY or APRS packet going out on {@code frequencyHz}: shown in the moderators' chat. Server thread. */
	public static void data(ServerLevel level, TncPacket packet, float frequencyHz) {
		if(MODERATORS.isEmpty())
			return;
		MinecraftServer server = level.getServer();
		String mhz = String.format(Locale.ROOT, "%.3f", frequencyHz / 1_000_000.0D);
		String text = switch(packet.kind()) {
			case RTTY -> "[RTTY " + mhz + "] " + packet.from() + ": " + packet.text();
			case MESSAGE -> "[APRS " + mhz + "] " + packet.from() + ">" + (packet.to().isEmpty() ? "ALL" : packet.to()) + ": " + packet.text();
			case BEACON -> "[APRS " + mhz + "] " + packet.from() + (packet.position() == null ? "" : " at " + packet.position().getX() + ", " + packet.position().getZ())
					+ (packet.text().isEmpty() ? "" : ": " + packet.text());
			case FILE -> "[FILE " + mhz + "] " + packet.from() + ">" + packet.to() + ": " + packet.text();
			case FILE_HEADER -> null;
		};
		if(text == null)
			return;
		Component line = Component.literal(text).withStyle(ChatFormatting.GRAY);
		for(Map.Entry<UUID, UUID> entry : MODERATORS.entrySet()) {
			ServerPlayer moderator = server.getPlayerList().getPlayer(entry.getKey());
			if(moderator == null)
				continue;
			UUID target = entry.getValue();
			if(!EVERYONE.equals(target)) { // One player: their station's packets, by callsign.
				String callsign = Callsigns.playerCallsign(server, target);
				if(callsign == null || !callsign.equalsIgnoreCase(packet.from()))
					continue;
			}
			moderator.sendSystemMessage(line);
		}
	}

	@SubscribeEvent
	public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
		UUID uuid = event.getEntity().getUUID();
		stop(uuid);
		ENCODERS.computeIfPresent(uuid, (k, encoder) -> {
			encoder.close();
			return null;
		});
		LAST_FRAME.remove(uuid);
		LAST_LABEL.remove(uuid);
	}

	@SubscribeEvent
	public static void onServerStopped(ServerStoppedEvent event) {
		MODERATORS.clear();
		CHANNELS.clear();
		ENCODERS.values().forEach(OpusEncoder::close);
		ENCODERS.clear();
		LAST_FRAME.clear();
		LAST_LABEL.clear();
	}
}
