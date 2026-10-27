package com.arrl.radiocraft.common.radio.data;

import com.arrl.radiocraft.common.radio.voice.Afsk;
import com.arrl.radiocraft.common.radio.voice.Sstv;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.component.WritableBookContent;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Documents a radio fax can send: written books and books and quills, paper (named or not), filled maps and enchanted
 * books. The sender keeps the original, but an enchanted book is used up; the receiver prints a copy on its own blank: a
 * book, a sheet of paper or an empty map. Written books copy as vanilla copies them (a copy of a copy cannot be
 * copied). The old digital interface's tones are still here: books and paper as packet tones, a little longer per page;
 * maps as SSTV, the picture itself. The fax itself paces its packets by {@link FaxRates}.
 */
public final class FileTransfer {

	private FileTransfer() {}

	/** Map pictures are 128 by 128. */
	public static final int MAP_SIZE = 128;
	/** Packet tones for paper and the first page of a book, then each further page. */
	private static final double BASE_SECONDS = 2.0D;
	private static final double SECONDS_PER_PAGE = 0.5D;
	private static final double MAX_SECONDS = 30.0D;

	/** True if {@code stack} can be sent: a file, and (for a written book) one that may still be copied. */
	public static boolean canSend(ItemStack stack) {
		if(stack.is(Items.WRITTEN_BOOK)) {
			WrittenBookContent content = stack.get(DataComponents.WRITTEN_BOOK_CONTENT);
			return content == null || content.tryCraftCopy() != null;
		}
		return stack.is(Items.WRITABLE_BOOK) || stack.is(Items.PAPER) || stack.is(Items.FILLED_MAP) || stack.is(Items.ENCHANTED_BOOK);
	}

	/** True if sending {@code stack} uses it up: an enchanted book goes, it is not copied. */
	public static boolean consumed(ItemStack stack) {
		return stack.is(Items.ENCHANTED_BOOK);
	}

	/** The blank a receiver prints {@code stack} on: {@link Items#BOOK}, {@link Items#PAPER} or {@link Items#MAP}. */
	public static ItemStack blankFor(ItemStack stack) {
		if(stack.is(Items.WRITTEN_BOOK) || stack.is(Items.WRITABLE_BOOK) || stack.is(Items.ENCHANTED_BOOK))
			return new ItemStack(Items.BOOK);
		if(stack.is(Items.FILLED_MAP))
			return new ItemStack(Items.MAP);
		return new ItemStack(Items.PAPER);
	}

	/** The copy a receiver prints, or null if it may not be copied. */
	@Nullable
	public static ItemStack copy(ItemStack stack) {
		if(!canSend(stack))
			return null;
		ItemStack copy = stack.copyWithCount(1);
		WrittenBookContent content = stack.get(DataComponents.WRITTEN_BOOK_CONTENT);
		if(content != null)
			copy.set(DataComponents.WRITTEN_BOOK_CONTENT, content.tryCraftCopy());
		return copy;
	}

	/** What the screens call the file: its kind and its title or name. */
	public static String describe(ItemStack stack) {
		String kind = stack.is(Items.FILLED_MAP) ? "MAP" : stack.is(Items.PAPER) ? "PAPER" : stack.is(Items.ENCHANTED_BOOK) ? "ENCH BOOK" : "BOOK";
		WrittenBookContent content = stack.get(DataComponents.WRITTEN_BOOK_CONTENT);
		String name = content != null ? content.title().raw() : stack.has(DataComponents.CUSTOM_NAME) ? stack.getHoverName().getString() : "";
		return name.isEmpty() ? kind : kind + " " + name;
	}

	/** Pages in a book, 1 for anything else. */
	public static int pages(ItemStack stack) {
		WrittenBookContent written = stack.get(DataComponents.WRITTEN_BOOK_CONTENT);
		if(written != null)
			return written.pages().size();
		WritableBookContent writable = stack.get(DataComponents.WRITABLE_BOOK_CONTENT);
		return writable != null ? writable.pages().size() : 1;
	}

	/** Bytes of packet tones a book or paper goes out as. */
	private static int packetBytes(ItemStack stack) {
		double seconds = Math.min(MAX_SECONDS, BASE_SECONDS + SECONDS_PER_PAGE * Math.max(0, pages(stack) - 1));
		return Math.max(1, (int)(seconds * Afsk.BAUD / 8.0D) - Afsk.PREAMBLE_FLAGS - Afsk.TAIL_FLAGS);
	}

	/** How many 20 ms frames of tones the file takes. The same at both ends, so receivers can show progress. */
	public static int frameCount(ItemStack stack) {
		if(stack.is(Items.FILLED_MAP))
			return Sstv.frameCount(MAP_SIZE, MAP_SIZE);
		return Afsk.frameCount(packetBytes(stack));
	}

	/** The tones of the file: SSTV of the map's picture (as {@code level} knows it), packet tones for the rest. */
	public static List<short[]> audio(ItemStack stack, @Nullable Level level, long seed) {
		if(!stack.is(Items.FILLED_MAP))
			return Afsk.burst(packetBytes(stack), seed);
		double[][] rows = new double[MAP_SIZE][MAP_SIZE];
		MapItemSavedData data = level == null ? null : MapItem.getSavedData(stack, level);
		for(int y = 0; y < MAP_SIZE; y++) {
			for(int x = 0; x < MAP_SIZE; x++) {
				int colour = data == null ? 0 : MapColor.getColorFromPackedId(data.colors[x + y * MAP_SIZE]);
				rows[y][x] = (((colour >> 16) & 0xFF) + ((colour >> 8) & 0xFF) + (colour & 0xFF)) / (3.0D * 255.0D);
			}
		}
		return Sstv.frames(rows);
	}
}
