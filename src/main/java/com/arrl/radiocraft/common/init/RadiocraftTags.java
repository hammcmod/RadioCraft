package com.arrl.radiocraft.common.init;

import com.arrl.radiocraft.Radiocraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public class RadiocraftTags {

	public static class Blocks {

		public static final TagKey<Block> ANTENNA_BLOCKS = TagKey.create(Registries.BLOCK, Radiocraft.location("antenna_blocks"));
		public static final TagKey<Block> ANTENNA_WIRE_HOLDERS = TagKey.create(Registries.BLOCK, Radiocraft.location("antenna_wire_holders"));
		public static final TagKey<Block> COAX_BLOCKS = TagKey.create(Registries.BLOCK, Radiocraft.location("coax_blocks"));
		public static final TagKey<Block> POWER_BLOCKS = TagKey.create(Registries.BLOCK, Radiocraft.location("power_blocks"));
	}

}
