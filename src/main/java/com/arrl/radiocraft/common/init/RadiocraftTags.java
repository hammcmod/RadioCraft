package com.arrl.radiocraft.common.init;

import com.arrl.radiocraft.Radiocraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;

public class RadiocraftTags {

	public static class Blocks {

		public static final TagKey<Block> ANTENNA_BLOCKS = TagKey.create(Registries.BLOCK, Radiocraft.location("antenna_blocks"));
		public static final TagKey<Block> ANTENNA_WIRE_HOLDERS = TagKey.create(Registries.BLOCK, Radiocraft.location("antenna_wire_holders"));

	}

	public static boolean isAntennaWireHolder(Block block) {
		return ForgeRegistries.BLOCKS.tags().getTag(Blocks.ANTENNA_WIRE_HOLDERS).contains(block);
	}

	public static boolean isAntennaBlock(Block block) {
		return ForgeRegistries.BLOCKS.tags().getTag(Blocks.ANTENNA_BLOCKS).contains(block);
	}

}
