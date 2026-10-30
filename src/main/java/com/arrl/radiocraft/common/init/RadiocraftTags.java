package com.arrl.radiocraft.common.init;

import com.arrl.radiocraft.Radiocraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public class RadiocraftTags {

	public static class Blocks {

		public static final TagKey<Block> ANTENNA_BLOCKS = TagKey.create(Registries.BLOCK, Radiocraft.id("antenna_blocks"));
		public static final TagKey<Block> ANTENNA_WIRE_HOLDERS = TagKey.create(Registries.BLOCK, Radiocraft.id("antenna_wire_holders"));
		public static final TagKey<Block> COAX_BLOCKS = TagKey.create(Registries.BLOCK, Radiocraft.id("coax_blocks"));
		public static final TagKey<Block> POWER_BLOCKS = TagKey.create(Registries.BLOCK, Radiocraft.id("power_blocks"));
		/** Blocks data wire runs into (radios always do, see {@code WireBlock#isDataConnection}). */
		public static final TagKey<Block> DATA_BLOCKS = TagKey.create(Registries.BLOCK, Radiocraft.id("data_blocks"));
		/**
		 * Redstone signal sources whose output is a reading, not a switch (weather instruments, the WEFAX receiver's
		 * alarm): their updates make no redstone interference.
		 */
		public static final TagKey<Block> QUIET_SIGNAL_SOURCES = TagKey.create(Registries.BLOCK, Radiocraft.id("quiet_signal_sources"));
	}

}
