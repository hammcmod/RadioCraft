package com.arrl.radiocraft.common.radio.data;

import com.arrl.radiocraft.common.radio.voice.Rtty;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * What a digital interface (TNC) puts on the air: an APRS position beacon or message to a callsign, which travels
 * whole and either decodes at the far end or does not (the real thing has a checksum); a few characters of RTTY,
 * which print garbled when the signal is weak (see {@link Rtty}); or a file (book, paper or map, see
 * {@link FileTransfer}), announced by a header as it starts so receivers can show its progress; or one leg of a radio
 * fax (see {@code RadioFaxBlockEntity}): the call, its answer, a numbered packet of the picture (the last one carrying
 * the document) or a packet's ack.
 *
 * @param id       Random number for the packet, kept by digipeaters so each repeats a packet once.
 * @param kind     What the packet carries.
 * @param from     Callsign of the station that sent it.
 * @param to       Callsign a message or file is for; empty on a beacon (and on a file for everyone).
 * @param text     The beacon's comment, the message, the RTTY characters (a newline ends a line), what a file is, or
 *                 the message number an ack answers. A message wanting an ack ends with its number after a '{', as in
 *                 real APRS ("HELLO{42"); see {@link #messageBody} and {@link #messageNumber}.
 * @param position Where the station is, on a beacon; null otherwise.
 * @param icon     The {@link AprsIcon} name the station shows on maps (beacons).
 * @param hops     Digipeaters the packet has been through on its way here.
 * @param item     The file a file packet (and its header) carries; empty on anything else. Never saved.
 */
public record TncPacket(long id, Kind kind, String from, String to, String text, @Nullable BlockPos position, String icon, int hops, ItemStack item) {

	public enum Kind { BEACON, MESSAGE, RTTY, FILE_HEADER, FILE, ACK, FAX_CALL, FAX_ANSWER, FAX_PACKET, FAX_ACK }

	/** A message to this callsign is for every station that hears it, as a bulletin. */
	public static final String EVERYONE = "ALL";
	public static final int CALLSIGN_MAX = 12;
	public static final int BEACON_TEXT_MAX = 30;
	/** Characters of weather data a weather beacon starts with (a full report is 38). */
	public static final int WEATHER_TEXT_MAX = 48;
	public static final int MESSAGE_TEXT_MAX = 100;
	/** Digits in a message number (real APRS allows up to five characters). */
	public static final int MESSAGE_NUMBER_MAX = 5;
	/** Characters of RTTY in one packet: what was typed since the last one went out. */
	public static final int RTTY_TEXT_MAX = 64;

	public static TncPacket beacon(long id, String from, BlockPos position, String comment, String icon) {
		return new TncPacket(id, Kind.BEACON, sanitizeCallsign(from), "", sanitizeText(comment, BEACON_TEXT_MAX), position.immutable(), AprsIcon.byName(icon).name(), 0, ItemStack.EMPTY);
	}

	/**
	 * A beacon from a station with a weather station: the APRS weather data {@code weather} (see {@link AprsWeather}),
	 * then the station's comment after a space.
	 */
	public static TncPacket weatherBeacon(long id, String from, BlockPos position, String weather, String comment) {
		String clean = sanitizeText(comment, BEACON_TEXT_MAX);
		String text = sanitizeText(weather, WEATHER_TEXT_MAX).replace(' ', '.') + (clean.isEmpty() ? "" : " " + clean);
		return new TncPacket(id, Kind.BEACON, sanitizeCallsign(from), "", text, position.immutable(), AprsIcon.WX.name(), 0, ItemStack.EMPTY);
	}

	public static TncPacket message(long id, String from, String to, String text) {
		return new TncPacket(id, Kind.MESSAGE, sanitizeCallsign(from), sanitizeCallsign(to), sanitizeText(text, MESSAGE_TEXT_MAX), null, AprsIcon.HOUSE.name(), 0, ItemStack.EMPTY);
	}

	/** A message that wants an ack: {@code text} with its message {@code number} after a '{'. */
	public static TncPacket message(long id, String from, String to, String text, int number) {
		String body = sanitizeText(text, MESSAGE_TEXT_MAX).replace('{', '(');
		return new TncPacket(id, Kind.MESSAGE, sanitizeCallsign(from), sanitizeCallsign(to), body + "{" + number, null, AprsIcon.HOUSE.name(), 0, ItemStack.EMPTY);
	}

	/** {@code from} telling {@code to} its message {@code number} arrived. */
	public static TncPacket ack(long id, String from, String to, String number) {
		return new TncPacket(id, Kind.ACK, sanitizeCallsign(from), sanitizeCallsign(to), sanitizeText(number, MESSAGE_NUMBER_MAX), null, AprsIcon.HOUSE.name(), 0, ItemStack.EMPTY);
	}

	/** A message's text without its message number. */
	public String messageBody() {
		int brace = numberBrace();
		return brace < 0 ? text : text.substring(0, brace);
	}

	/** A message's number (what its ack answers), or empty if it wants no ack. */
	public String messageNumber() {
		int brace = numberBrace();
		return brace < 0 ? "" : text.substring(brace + 1);
	}

	/** Where a message's number starts ('{' then 1 to 5 digits at the end), or -1. */
	private int numberBrace() {
		if(kind != Kind.MESSAGE)
			return -1;
		int brace = text.lastIndexOf('{');
		if(brace < 0 || text.length() - brace - 1 < 1 || text.length() - brace - 1 > MESSAGE_NUMBER_MAX)
			return -1;
		for(int i = brace + 1; i < text.length(); i++)
			if(!Character.isDigit(text.charAt(i)))
				return -1;
		return brace;
	}

	public static TncPacket rtty(long id, String from, String text) {
		return new TncPacket(id, Kind.RTTY, sanitizeCallsign(from), "", Rtty.sanitize(text, RTTY_TEXT_MAX), null, AprsIcon.HOUSE.name(), 0, ItemStack.EMPTY);
	}

	/** A file for {@code to} (empty for everyone): one of {@code item}, described for the screens. */
	public static TncPacket file(long id, String from, String to, ItemStack item) {
		return new TncPacket(id, Kind.FILE, sanitizeCallsign(from), sanitizeCallsign(to), sanitizeText(FileTransfer.describe(item), MESSAGE_TEXT_MAX),
				null, AprsIcon.HOUSE.name(), 0, item.copyWithCount(1));
	}

	/**
	 * One leg of a radio fax from {@code from} to {@code to}: the call ("MAP WORLD|8": the document and the tone ticks a
	 * packet takes), its answer ("OK" or why not), a packet ("12/30|8", the last one carrying the document) or an ack
	 * ("12").
	 */
	public static TncPacket fax(long id, Kind kind, String from, String to, String text, ItemStack item) {
		return new TncPacket(id, kind, sanitizeCallsign(from), sanitizeCallsign(to), sanitizeText(text, MESSAGE_TEXT_MAX), null, AprsIcon.HOUSE.name(), 0,
				item.isEmpty() ? ItemStack.EMPTY : item.copyWithCount(1));
	}

	/** True for any leg of a radio fax. */
	public boolean isFax() {
		return kind == Kind.FAX_CALL || kind == Kind.FAX_ANSWER || kind == Kind.FAX_PACKET || kind == Kind.FAX_ACK;
	}

	/** The tone ticks a fax packet takes, after the '|' in a call or packet; 0 if there is none. */
	public int faxTicks() {
		int bar = text.lastIndexOf('|');
		return bar < 0 ? 0 : digits(text.substring(bar + 1));
	}

	/** A fax packet's number ("12/30|8" is 12) or the number an ack answers; 0 if there is none. */
	public int faxSeq() {
		int end = text.indexOf('/');
		return digits(end < 0 ? text : text.substring(0, end));
	}

	/** What a fax call says is coming: its text before the '|'. */
	public String faxLabel() {
		int bar = text.lastIndexOf('|');
		return bar < 0 ? text : text.substring(0, bar);
	}

	private static int digits(String s) {
		int value = 0;
		for(int i = 0; i < s.length() && i < 9; i++) {
			char c = s.charAt(i);
			if(c < '0' || c > '9')
				break;
			value = value * 10 + (c - '0');
		}
		return value;
	}

	/** The header a file goes out with, as its tones start. */
	public TncPacket header() {
		return new TncPacket(id ^ 0x5DEECE66DL, Kind.FILE_HEADER, from, to, text, position, icon, hops, item);
	}

	/** The same packet under a new id (a message sent again: digipeaters repeat it again). */
	public TncPacket withId(long newId) {
		return new TncPacket(newId, kind, from, to, text, position, icon, hops, item);
	}

	/** The same packet with other text (an RTTY packet as a weak signal prints it). */
	public TncPacket withText(String newText) {
		return new TncPacket(id, kind, from, to, newText, position, icon, hops, item);
	}

	/** The same packet starting its hops over, as when it comes off a microwave link into another APRS network. */
	public TncPacket hopsReset() {
		return new TncPacket(id, kind, from, to, text, position, icon, 0, item);
	}

	/** The same packet one digipeater further along. */
	public TncPacket digipeated() {
		return new TncPacket(id, kind, from, to, text, position, icon, hops + 1, item);
	}

	/** About how many bytes the packet would be in AX.25: addresses and control (about 16 bytes) plus the information field. */
	public int airBytes() {
		int info = text.length() + (position != null ? 20 : 0) + (to.isEmpty() ? 0 : 11);
		return 16 + info + 2; // And the checksum.
	}

	/** True if a message or file to {@code to} is for every station: no callsign, or {@link #EVERYONE}. */
	public static boolean isForEveryone(String to) {
		return to.isEmpty() || to.equals(EVERYONE);
	}

	/** Upper case, letters, digits, '/' and '-' only (W1BTR-9), at most {@link #CALLSIGN_MAX} characters. */
	public static String sanitizeCallsign(String callsign) {
		StringBuilder out = new StringBuilder();
		for(char c : callsign.toUpperCase(Locale.ROOT).toCharArray()) {
			if((c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '/' || c == '-')
				out.append(c);
			if(out.length() >= CALLSIGN_MAX)
				break;
		}
		return out.toString();
	}

	/** Printable ASCII only, at most {@code max} characters: what an APRS packet can carry. */
	public static String sanitizeText(String text, int max) {
		StringBuilder out = new StringBuilder();
		for(char c : text.toCharArray()) {
			if(c >= 32 && c < 127)
				out.append(c);
			if(out.length() >= max)
				break;
		}
		return out.toString().strip();
	}

	/** Saves everything but the item (files are never stored as packets). */
	public CompoundTag save() {
		CompoundTag tag = new CompoundTag();
		tag.putLong("id", id);
		tag.putString("kind", kind.name());
		tag.putString("from", from);
		tag.putString("to", to);
		tag.putString("text", text);
		if(position != null)
			tag.putLong("position", position.asLong());
		tag.putString("icon", icon);
		tag.putInt("hops", hops);
		return tag;
	}

	public static TncPacket load(CompoundTag tag) {
		Kind kind;
		try {
			kind = Kind.valueOf(tag.getString("kind"));
		}
		catch(IllegalArgumentException e) {
			kind = Kind.BEACON;
		}
		return new TncPacket(tag.getLong("id"), kind, sanitizeCallsign(tag.getString("from")), sanitizeCallsign(tag.getString("to")),
				kind == Kind.RTTY ? Rtty.sanitize(tag.getString("text"), RTTY_TEXT_MAX) : sanitizeText(tag.getString("text"), MESSAGE_TEXT_MAX + 1 + MESSAGE_NUMBER_MAX), tag.contains("position") ? BlockPos.of(tag.getLong("position")) : null,
				AprsIcon.byName(tag.getString("icon")).name(), tag.getInt("hops"), ItemStack.EMPTY);
	}
}
