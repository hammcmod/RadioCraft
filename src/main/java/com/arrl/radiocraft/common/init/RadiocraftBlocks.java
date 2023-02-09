package com.arrl.radiocraft.common.init;

import com.arrl.radiocraft.Radiocraft;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registry class for Radiocraft's blocks
 */
public class RadiocraftBlocks {

	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Radiocraft.MOD_ID);

	public static final RegistryObject<Block> WIRE = simpleBlock("wire", Properties.copy(Blocks.REDSTONE_WIRE));
	public static final RegistryObject<Block> WATERPROOF_WIRE = simpleBlock("waterproof_wire", Properties.copy(Blocks.REDSTONE_WIRE));

	public static RegistryObject<Block> simpleBlock(String name, Properties properties) {
		return BLOCKS.register(name, () -> new Block(properties));
	}
}
