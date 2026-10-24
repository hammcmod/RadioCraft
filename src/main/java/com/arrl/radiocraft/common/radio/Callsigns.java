package com.arrl.radiocraft.common.radio;

import com.arrl.radiocraft.RadiocraftServerConfig;
import com.arrl.radiocraft.api.capabilities.LicenseClass;
import com.arrl.radiocraft.api.capabilities.PlayerCallsignData;
import com.arrl.radiocraft.common.data.BlockEntityCallsignSavedData;
import com.arrl.radiocraft.common.data.PlayerCallsignSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Callsign rules from the proposal (maintainer, 2026-10-24).
 * <ul>
 *     <li>Players choose their own, shaped like a real amateur callsign: a prefix from {@code callsign_prefixes} (K, W,
 *     AA, VE, G, 2E, ...), a digit (from {@code callsign_digits}) and one to three letters (K1ABC, VE3XY, 2E0AB). Or the
 *     server chooses one for them ({@link #generatePlayer}): a prefix from the list, the zone digit where they stand
 *     (as for repeaters) and three letters. No two players share one. {@code callsign_self_assign} turns choosing off,
 *     leaving callsigns to operators.</li>
 *     <li>Repeaters take the form {@code X#XX}: a one-letter prefix (K, N, W, G, M, F, I), then a digit that says how
 *     far from spawn the repeater stands, then two letters; the digit is 0 near spawn up to 9 near the edge of the world ({@code callsign_zone_radius}, or the world border if
 *     nearer). A repeater can instead carry its owner's own callsign; {@code repeater_callsign_mode} allows either or
 *     locks it to one.</li>
 * </ul>
 */
public final class Callsigns {

	/** What follows the prefix: the digit and one to three letters. */
	private static final Pattern AFTER_PREFIX = Pattern.compile("[0-9][A-Z]{1,3}");

	/** Real amateur prefixes (maintainer, 2026-10-24: callsigns must be valid ham callsigns). */
	public static final java.util.List<String> DEFAULT_PREFIXES = defaultPrefixes();

	private static java.util.List<String> defaultPrefixes() {
		java.util.List<String> out = new java.util.ArrayList<>(java.util.List.of("K", "N", "W"));
		for(char c = 'A'; c <= 'L'; c++)
			out.add("A" + c); // AA to AL: United States.
		for(char c = 'A'; c <= 'Z'; c++) {
			out.add("K" + c);
			out.add("N" + c);
			out.add("W" + c);
		}
		out.addAll(java.util.List.of(
				"VE", "VA", "VO", "VY", // Canada
				"G", "M", "2E", "2M", "GW", "GM", "GI", "MW", "MM", // United Kingdom
				"EI", "DL", "DA", "DB", "DC", "DD", "DJ", "DK", "F", "I", "IK", "IZ", "EA", "EB", "EC", "CT", "ON", "PA", "PD",
				"OH", "SM", "SA", "LA", "OZ", "OE", "HB", "OK", "OM", "SP", "SQ", "HA", "YO", "LZ", "SV", "9A", "S5", "YU", "UR", "UA",
				"JA", "JE", "JF", "JG", "JH", "JI", "JJ", "JK", "JL", "JR", "HL", "BV", "BY", "DU", "YB", "HS", "9V", "VU", "4X", "TA",
				"VK", "ZL", "ZS", "5Z", "CN", "SU", "XE", "TI", "HK", "YV", "PY", "LU", "CE", "CX", "OA", "HC", "CP", "KH6", "KL7", "KP4"));
		return java.util.List.copyOf(out);
	}

	/** The prefixes players may use, from the config. */
	public static java.util.List<String> prefixes() {
		java.util.List<String> out = new java.util.ArrayList<>();
		for(Object o : RadiocraftServerConfig.CALLSIGN_PREFIXES.get())
			out.add(normalise(String.valueOf(o)));
		return out.isEmpty() ? DEFAULT_PREFIXES : out;
	}

	/** What a repeater's callsign may be. */
	public enum RepeaterMode { EITHER, AUTO, OWN }

	/** Sent by the repeater screen for "make one up" and "use my callsign". */
	public static final String AUTO_TOKEN = "@auto", OWN_TOKEN = "@own";

	private Callsigns() {}

	public static String normalise(String callsign) {
		return callsign == null ? "" : callsign.trim().toUpperCase(Locale.ROOT);
	}

	/** True for a callsign shaped like a real one: a listed prefix, an allowed digit, one to three letters. */
	public static boolean isValidPlayerCallsign(String callsign) {
		String c = normalise(callsign);
		for(String prefix : prefixes()) {
			if(!c.startsWith(prefix))
				continue;
			String rest = c.substring(prefix.length());
			if(AFTER_PREFIX.matcher(rest).matches() && allowedDigits().indexOf(rest.charAt(0)) >= 0)
				return true;
		}
		return false;
	}

	/**
	 * A free callsign for {@code player} ("choose one for me"): a prefix from the list, the zone digit where they stand
	 * (0 near spawn up to 9 far out, as for repeaters; a random allowed digit if that one is not allowed), and three
	 * letters. Null if none could be found free.
	 */
	@Nullable
	public static String generatePlayer(ServerPlayer player) {
		MinecraftServer server = player.getServer();
		if(server == null)
			return null;
		java.util.List<String> prefixes = prefixes();
		String digits = allowedDigits();
		RandomSource random = player.getRandom();
		char zone = (char)('0' + zoneDigit(player.level(), player.blockPosition()));
		for(int attempt = 0; attempt < 200; attempt++) {
			char digit = digits.indexOf(zone) >= 0 ? zone : digits.charAt(random.nextInt(digits.length()));
			String callsign = prefixes.get(random.nextInt(prefixes.size())) + digit + letter(random) + letter(random) + letter(random);
			if(isValidPlayerCallsign(callsign) && !isTaken(server, callsign, player.getUUID()))
				return callsign;
		}
		return null;
	}

	/** Sent by the prompt's "Choose one for me". */
	public static final String CHOOSE_TOKEN = "@choose";

	/** The digits players may use, from the config (all ten if it is empty or nonsense). */
	public static String allowedDigits() {
		String digits = RadiocraftServerConfig.CALLSIGN_DIGITS.get().replaceAll("[^0-9]", "");
		return digits.isEmpty() ? "0123456789" : digits;
	}

	/** The player's callsign, or null. */
	@Nullable
	public static String playerCallsign(MinecraftServer server, UUID player) {
		PlayerCallsignData data = PlayerCallsignSavedData.get(server.overworld()).getCallsignData(player);
		return data == null ? null : data.callsign();
	}

	/** True if a player other than {@code except} or a station already has {@code callsign}. */
	public static boolean isTaken(MinecraftServer server, String callsign, @Nullable UUID except) {
		String c = normalise(callsign);
		PlayerCallsignData owner = PlayerCallsignSavedData.get(server.overworld()).getCallsignData(c);
		if(owner != null && (except == null || !owner.playerUUID().equals(except.toString())))
			return true;
		return BlockEntityCallsignSavedData.get(server.overworld()).getCallsignData(c) != null;
	}

	/** Why a claim failed, or {@code OK}. Translation keys under {@code message.radiocraft.callsign.}. */
	public enum ClaimResult { OK, DISABLED, ALREADY_HAS, INVALID, TAKEN }

	/** A player choosing their own callsign. */
	public static ClaimResult claim(ServerPlayer player, String callsign) {
		MinecraftServer server = player.getServer();
		if(server == null || !RadiocraftServerConfig.CALLSIGN_SELF_ASSIGN.get())
			return ClaimResult.DISABLED;
		if(playerCallsign(server, player.getUUID()) != null)
			return ClaimResult.ALREADY_HAS;
		String c = CHOOSE_TOKEN.equals(callsign) ? generatePlayer(player) : normalise(callsign);
		if(c == null || !isValidPlayerCallsign(c))
			return ClaimResult.INVALID;
		if(isTaken(server, c, player.getUUID()))
			return ClaimResult.TAKEN;
		PlayerCallsignSavedData.get(server.overworld()).setCallsignData(player.getUUID(),
				new PlayerCallsignData(player.getUUID().toString(), player.getGameProfile().getName(), c, LicenseClass.TECHNICIAN));
		return ClaimResult.OK;
	}

	/** The callsign {@code player} has after a claim, for the "your callsign is now" message. */
	public static String claimedName(ServerPlayer player, String requested) {
		String own = player.getServer() == null ? null : playerCallsign(player.getServer(), player.getUUID());
		return own != null ? own : normalise(requested);
	}

	// -------------------- REPEATERS --------------------

	public static RepeaterMode repeaterMode() {
		return RadiocraftServerConfig.REPEATER_CALLSIGN_MODE.get();
	}

	/** The digit for a repeater at {@code pos}: 0 near spawn, 9 near the edge of the world. */
	public static int zoneDigit(Level level, BlockPos pos) {
		BlockPos spawn = level.getSharedSpawnPos();
		double distance = Math.sqrt(Math.pow(pos.getX() - spawn.getX(), 2) + Math.pow(pos.getZ() - spawn.getZ(), 2));
		double radius = Math.min(level.getWorldBorder().getSize() / 2.0D, RadiocraftServerConfig.CALLSIGN_ZONE_RADIUS.get());
		if(radius <= 0.0D)
			return 0;
		return Math.max(0, Math.min(9, (int)Math.floor(distance * 10.0D / radius)));
	}

	/** A made-up repeater callsign for {@code pos}: a letter, the zone digit, two letters. */
	public static String generateRepeater(Level level, BlockPos pos, RandomSource random) {
		String first = repeaterPrefixes();
		return "" + first.charAt(random.nextInt(first.length())) + zoneDigit(level, pos) + letter(random) + letter(random);
	}

	/**
	 * Letters a repeater callsign may start with: the one-letter prefixes in {@code callsign_prefixes} (K, N, W, G, M,
	 * F, I by default), so X#XX is a real callsign shape too. K, N and W if the list has none.
	 */
	public static String repeaterPrefixes() {
		StringBuilder out = new StringBuilder();
		for(String prefix : prefixes()) {
			if(prefix.length() == 1 && Character.isLetter(prefix.charAt(0)) && out.indexOf(prefix) < 0)
				out.append(prefix);
		}
		return out.isEmpty() ? "KNW" : out.toString();
	}

	private static char letter(RandomSource random) {
		return (char)('A' + random.nextInt(26));
	}

	/** True for a repeater callsign in the {@code X#XX} form with the right digit for {@code pos}. */
	public static boolean isValidRepeaterCallsign(Level level, BlockPos pos, String callsign) {
		String c = normalise(callsign);
		return c.length() == 4 && repeaterPrefixes().indexOf(c.charAt(0)) >= 0 && c.charAt(1) == (char)('0' + zoneDigit(level, pos))
				&& Character.isLetter(c.charAt(2)) && Character.isLetter(c.charAt(3)) && c.chars().allMatch(ch -> ch < 128);
	}

	/**
	 * The callsign a repeater at {@code pos} gets when {@code player} asks for {@code requested} (or one of the
	 * {@link #AUTO_TOKEN}/{@link #OWN_TOKEN} tokens, or empty to clear it), or null if the mode does not allow it.
	 */
	@Nullable
	public static String resolveRepeaterCallsign(ServerPlayer player, Level level, BlockPos pos, String requested) {
		RepeaterMode mode = repeaterMode();
		String own = player.getServer() == null ? null : playerCallsign(player.getServer(), player.getUUID());
		if(AUTO_TOKEN.equals(requested))
			return mode == RepeaterMode.OWN ? null : generateRepeater(level, pos, player.getRandom());
		if(OWN_TOKEN.equals(requested))
			return mode == RepeaterMode.AUTO || own == null ? null : own;
		String c = normalise(requested);
		if(c.isEmpty())
			return "";
		boolean auto = isValidRepeaterCallsign(level, pos, c), mine = c.equals(own);
		return switch(mode) {
			case EITHER -> auto || mine ? c : null;
			case AUTO -> auto ? c : null;
			case OWN -> mine ? c : null;
		};
	}
}
