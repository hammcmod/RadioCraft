package com.arrl.radiocraft.common.items;

import com.arrl.radiocraft.common.datacomponents.TapeContents;
import com.arrl.radiocraft.common.init.RadiocraftDataComponent;
import com.arrl.radiocraft.common.radio.tape.TapeLibrary;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/**
 * A reel of tape for the tape player. Blank (crafted), it can be recorded on; found in structures, it carries one of
 * the server's tapes; recorded, a player's voice. Sixty colourways: twelve reel colours by five label colours, drawn by
 * tinting the item's layers ({@link #reelColour}, {@link #labelColour}).
 */
public class TapeItem extends Item {

	private static final int[] REELS = {
			0x2B2B2B, 0xE8E4D8, 0xB03A2E, 0x2E5FA8, 0x3E8A47, 0xD9A33A,
			0x7A4A9C, 0xD67A2C, 0x3BA6A6, 0x8A6A4A, 0xC85A8A, 0x707A80 };
	private static final int[] LABELS = { 0xF2EFE4, 0xF0D96A, 0xE89A9A, 0x9AC4E8, 0xB8E0A0 };

	public TapeItem(Properties properties) {
		super(properties.stacksTo(1));
	}

	public static TapeContents contents(ItemStack stack) {
		return stack.getOrDefault(RadiocraftDataComponent.TAPE_CONTENTS.get(), TapeContents.BLANK);
	}

	public static int reelColour(ItemStack stack) {
		return 0xFF000000 | REELS[contents(stack).colorway() / TapeContents.LABEL_COLOURS % REELS.length];
	}

	public static int labelColour(ItemStack stack) {
		return 0xFF000000 | LABELS[contents(stack).colorway() % TapeContents.LABEL_COLOURS];
	}

	/** A tape as found in a structure: one of the server's tapes (from {@code subfolder}, "" for any) in a random colourway; blank if the server has none. */
	public static ItemStack found(ItemStack stack, @Nullable MinecraftServer server, RandomSource random, String subfolder) {
		List<String> tracks = server == null ? List.of() : TapeLibrary.tracks(server, subfolder);
		String track = tracks.isEmpty() ? "" : tracks.get(random.nextInt(tracks.size()));
		stack.set(RadiocraftDataComponent.TAPE_CONTENTS.get(), new TapeContents(random.nextInt(TapeContents.COLORWAYS), track, "", 0));
		return stack;
	}

	/** "numbers/station_a.ogg" as a label: "Station A". */
	public static String label(String track) {
		String name = track.substring(track.lastIndexOf('/') + 1);
		if(name.toLowerCase(Locale.ROOT).endsWith(".ogg"))
			name = name.substring(0, name.length() - 4);
		StringBuilder out = new StringBuilder();
		for(String word : name.replace('-', '_').split("_")) {
			if(word.isEmpty())
				continue;
			if(!out.isEmpty())
				out.append(' ');
			out.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
		}
		return out.toString();
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, context, tooltip, flag);
		TapeContents contents = contents(stack);
		if(contents.isFound())
			tooltip.add(Component.literal("\"" + label(contents.track()) + "\"").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
		else if(contents.isRecorded())
			tooltip.add(Component.translatable("item.radiocraft.tape_reel.recorded", contents.seconds() / 60, String.format(Locale.ROOT, "%02d", contents.seconds() % 60)).withStyle(ChatFormatting.GRAY));
		else
			tooltip.add(Component.translatable("item.radiocraft.tape_reel.blank").withStyle(ChatFormatting.GRAY));
	}
}
