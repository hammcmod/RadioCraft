package com.arrl.radiocraft.common.radio.data;

import com.arrl.radiocraft.RadiocraftServerConfig;
import com.arrl.radiocraft.common.blockentities.radio.RadioBlockEntity;
import com.arrl.radiocraft.common.radio.Band;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

/**
 * How long a radio fax takes, and how often its packets are lost. Every document goes as {@link #PACKETS} packets,
 * each acknowledged before the next; the time is set by what is sent (on VHF a map takes 20 seconds, a book 10 a page,
 * an enchanted book 40) and the band (slower down the HF bands, ten times faster over a microwave link), all from the
 * server config. Packets over a weak signal are lost now and then and sent again.
 */
public final class FaxRates {

	private FaxRates() {}

	/** Packets every document is sent as. */
	public static final int PACKETS = 30;
	/**
	 * Ticks of each packet's turnaround over the radio that are not picture tones: the ack's four ticks of beep, a
	 * tick for each packet to go out and one in the queue. With the shortest tones (four ticks, see
	 * {@code FaxAudio.MIN_FRAMES}) a packet takes about eleven ticks, so no fax is quicker than about 16 seconds over
	 * a radio, whatever the document.
	 */
	public static final int ACK_OVERHEAD_TICKS = 7;
	/** Ticks of picture tones in the shortest packet. */
	public static final int MIN_PACKET_TICKS = 4;

	/** Seconds the document takes on VHF. */
	public static double seconds(ItemStack stack) {
		if(stack.is(Items.FILLED_MAP))
			return RadiocraftServerConfig.FAX_SECONDS_MAP.get();
		if(stack.is(Items.ENCHANTED_BOOK))
			return RadiocraftServerConfig.FAX_SECONDS_ENCHANTED.get();
		if(stack.is(Items.WRITTEN_BOOK) || stack.is(Items.WRITABLE_BOOK))
			return RadiocraftServerConfig.FAX_SECONDS_PER_PAGE.get() * Math.max(1, FileTransfer.pages(stack));
		return RadiocraftServerConfig.FAX_SECONDS_PAPER.get();
	}

	/** How many times slower than VHF a band is; 1 for VHF and anything unknown. */
	public static double slower(@Nullable Band band) {
		if(band == null)
			return 1.0D;
		return switch(band.name()) {
			case "10m" -> RadiocraftServerConfig.FAX_SLOWER_10M.get();
			case "20m" -> RadiocraftServerConfig.FAX_SLOWER_20M.get();
			case "40m" -> RadiocraftServerConfig.FAX_SLOWER_40M.get();
			case "80m" -> RadiocraftServerConfig.FAX_SLOWER_80M.get();
			default -> 1.0D;
		};
	}

	/** Ticks the whole document takes over {@code band}. */
	public static int totalTicks(ItemStack stack, @Nullable Band band) {
		return Math.max(PACKETS, (int)Math.round(seconds(stack) * slower(band) * 20.0D));
	}

	/** Ticks of picture tones in each packet over {@code band}: the document's time shared out, less the acks. */
	public static int packetTicks(ItemStack stack, @Nullable Band band) {
		return Math.max(MIN_PACKET_TICKS, totalTicks(stack, band) / PACKETS - ACK_OVERHEAD_TICKS);
	}

	/** Ticks between packets over a microwave link, which has no tones and no acks. */
	public static int linkPacketTicks(ItemStack stack) {
		double ticks = seconds(stack) * 20.0D / Math.max(0.01D, RadiocraftServerConfig.FAX_MICROWAVE_FASTER.get());
		return Math.max(1, (int)Math.round(ticks / PACKETS));
	}

	/**
	 * The chance a fax packet heard at {@code strength} (0 to 1) over {@code noise} is lost anyway: none at
	 * {@code fax_solid_signal} and above, up to {@code fax_packet_loss} at the weakest signal that still decodes.
	 */
	public static double lossChance(double strength, double noise) {
		double floor = RadioBlockEntity.copyThreshold(noise);
		double solid = Math.max(floor + 0.01D, RadiocraftServerConfig.FAX_SOLID_SIGNAL.get());
		if(strength >= solid)
			return 0.0D;
		double weak = Math.max(0.0D, Math.min(1.0D, (solid - strength) / (solid - floor)));
		return RadiocraftServerConfig.FAX_PACKET_LOSS.get() * weak;
	}
}
