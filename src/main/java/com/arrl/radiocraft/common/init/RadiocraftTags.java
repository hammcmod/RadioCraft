package com.arrl.radiocraft.common.init;

import com.arrl.radiocraft.Radiocraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;

public class RadiocraftTags {

	public static class Blocks {

		public static final TagKey<Block> ANTENNA_BLOCKS = TagKey.create(Registries.BLOCK, Radiocraft.location("antenna_blocks"));
		public static final TagKey<Block> POWER_WIRES = TagKey.create(Registries.BLOCK, Radiocraft.location("power_wires"));
		public static final TagKey<Block> COAX_WIRES = TagKey.create(Registries.BLOCK, Radiocraft.location("coax_wires"));
		public static final TagKey<Block> POWER_BLOCKS = TagKey.create(Registries.BLOCK, Radiocraft.location("power_blocks"));
		public static final TagKey<Block> COAX_BLOCKS = TagKey.create(Registries.BLOCK, Radiocraft.location("coax_blocks"));

	}


	public static boolean isAntennaBlock(Block block) {
		return ForgeRegistries.BLOCKS.tags().getTag(Blocks.ANTENNA_BLOCKS).contains(block);
	}

	public static boolean isPowerWire(Block block) {
		return ForgeRegistries.BLOCKS.tags().getTag(Blocks.POWER_WIRES).contains(block);
	}

	public static boolean isCoaxWire(Block block) {
		return ForgeRegistries.BLOCKS.tags().getTag(Blocks.COAX_WIRES).contains(block);
	}

	public static boolean isPowerBlock(Block block) {
		return ForgeRegistries.BLOCKS.tags().getTag(Blocks.POWER_BLOCKS).contains(block);
	}

	public static boolean isCoaxBlock(Block block) {
		return ForgeRegistries.BLOCKS.tags().getTag(Blocks.COAX_BLOCKS).contains(block);
	}

}
