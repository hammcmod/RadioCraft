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
 *     <li>Players choose their own, in the form {@code !@#!@@}: a required letter, an optional letter, a digit (from
 *     {@code callsign_digits}), a required letter and up to two more (K1ABC, W4X). No two players share one.
 *     {@code callsign_self_assign} turns choosing off, leaving callsigns to operators.</li>
 *     <li>Repeaters take the form {@code X#XX}: three letters around a digit that says how far from spawn the repeater
 *     stands, 0 near spawn up to 9 near the edge of the world ({@code callsign_zone_radius}, or the world border if
 *     nearer). A repeater can instead carry its owner's own callsign; {@code repeater_callsign_mode} allows either or
 *     locks it to one.</li>
 * </ul>
 */
public final class Callsigns {

	private static final Pattern PLAYER = Pattern.compile("[A-Z][A-Z]?[0-9][A-Z][A-Z]{0,2}");

	/** What a repeater's callsign may be. */
	public enum RepeaterMode { EITHER, AUTO, OWN }

	/** Sent by the repeater screen for "make one up" and "use my callsign". */
	public static final String AUTO_TOKEN = "@auto", OWN_TOKEN = "@own";

	private Callsigns() {}

	public static String normalise(String callsign) {
		return callsign == null ? "" : callsign.trim().toUpperCase(Locale.ROOT);
	}

	/** True for a callsign in the player form whose digit is one the server allows. */
	public static boolean isValidPlayerCallsign(String callsign) {
		String c = normalise(callsign);
		if(!PLAYER.matcher(c).matches())
			return false;
		for(char ch : c.toCharArray()) {
			if(Character.isDigit(ch))
				return allowedDigits().indexOf(ch) >= 0;
		}
		return false;
	}

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
		String c = normalise(callsign);
		if(!isValidPlayerCallsign(c))
			return ClaimResult.INVALID;
		if(isTaken(server, c, player.getUUID()))
			return ClaimResult.TAKEN;
		PlayerCallsignSavedData.get(server.overworld()).setCallsignData(player.getUUID(),
				new PlayerCallsignData(player.getUUID().toString(), player.getGameProfile().getName(), c, LicenseClass.TECHNICIAN));
		return ClaimResult.OK;
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
		return "" + letter(random) + zoneDigit(level, pos) + letter(random) + letter(random);
	}

	private static char letter(RandomSource random) {
		return (char)('A' + random.nextInt(26));
	}

	/** True for a repeater callsign in the {@code X#XX} form with the right digit for {@code pos}. */
	public static boolean isValidRepeaterCallsign(Level level, BlockPos pos, String callsign) {
		String c = normalise(callsign);
		return c.length() == 4 && Character.isLetter(c.charAt(0)) && c.charAt(1) == (char)('0' + zoneDigit(level, pos))
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
