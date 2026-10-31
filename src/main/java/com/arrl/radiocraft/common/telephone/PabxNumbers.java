package com.arrl.radiocraft.common.telephone;

import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * How phone PABX numbers are read (maintainer, 2026-10-31), with no game state: a dialed number is an extension on the
 * caller's own PABX, or another PABX's master number followed by one of its extensions. Every master number on a
 * server is the same length ({@code pabx_master_digits}), or empty for a PABX only its own phones can call.
 */
public final class PabxNumbers {

	/** Longest PABX name, characters. */
	public static final int MAX_NAME = 10;
	/** Longest extension, digits. */
	public static final int MAX_EXTENSION_DIGITS = 5;

	private PabxNumbers() {}

	/** True if {@code text} is all ASCII digits (and not empty). */
	public static boolean digits(String text) {
		if(text.isEmpty())
			return false;
		for(int i = 0; i < text.length(); i++)
			if(text.charAt(i) < '0' || text.charAt(i) > '9')
				return false;
		return true;
	}

	/** True if {@code master} can be saved: empty, or exactly {@code length} digits. */
	public static boolean validMaster(String master, int length) {
		return master.isEmpty() || (master.length() == length && digits(master));
	}

	/** True if {@code c} may be in a PABX name: a letter, a digit, a space or a dash. */
	public static boolean nameChar(char c) {
		return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == ' ' || c == '-';
	}

	/** {@code name} as saved: only the characters a name may have, upper case, trimmed, at most {@link #MAX_NAME}. */
	public static String cleanName(String name) {
		StringBuilder out = new StringBuilder();
		for(int i = 0; i < name.length() && out.length() < MAX_NAME; i++)
			if(nameChar(name.charAt(i)))
				out.append(name.charAt(i));
		return out.toString().toUpperCase(Locale.ROOT).trim();
	}

	/**
	 * The extension {@code dialed} asks for at the PABX with master number {@code master}: what follows the master
	 * number, or null if {@code dialed} does not start with it (or has nothing after it, or the master is empty).
	 */
	@Nullable
	public static String extensionAfter(String dialed, String master) {
		if(master.isEmpty() || dialed.length() <= master.length() || !dialed.startsWith(master))
			return null;
		String rest = dialed.substring(master.length());
		return digits(rest) ? rest : null;
	}

	/** True if {@code dialed} is exactly extension {@code extension}. */
	public static boolean isExtension(String dialed, int extension) {
		return dialed.equals(Integer.toString(extension));
	}

	/** How a phone's screen names a PABX: "PABX 603: OFFICE", or "PABX 603" with no name. */
	public static String label(String master, String name) {
		return name.isEmpty() ? "PABX " + master : "PABX " + master + ": " + name;
	}
}
