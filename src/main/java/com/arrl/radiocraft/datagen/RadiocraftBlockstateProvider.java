package com.arrl.radiocraft.datagen;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class RadiocraftBlockstateProvider extends BlockStateProvider {

	public RadiocraftBlockstateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
		super(output, Radiocraft.MOD_ID, exFileHelper);
	}

	@Override
	protected void registerStatesAndModels() {
		complexHorizontalBlockWithItem(RadiocraftBlocks.HF_RADIO_10M.get());
		complexHorizontalBlockWithItem(RadiocraftBlocks.HF_RADIO_20M.get());
		complexHorizontalBlockWithItem(RadiocraftBlocks.HF_RADIO_40M.get());
		complexHorizontalBlockWithItem(RadiocraftBlocks.HF_RADIO_80M.get());
		complexHorizontalBlockWithItem(RadiocraftBlocks.LARGE_BATTERY.get());
		complexHorizontalBlockWithItem(RadiocraftBlocks.ANTENNA_TUNER.get());
		complexHorizontalBlockWithItem(RadiocraftBlocks.SOLAR_WEATHER_STATION.get());
		complexHorizontalBlockWithItem(RadiocraftBlocks.DIGITAL_INTERFACE.get());
        complexHorizontalBlockWithItem(RadiocraftBlocks.FM_RADIO.get());
		complexHorizontalBlockWithItem(RadiocraftBlocks.DUPLEXER.get());
		complexHorizontalBlockWithItem(RadiocraftBlocks.MICROPHONE.get());
		complexBlockWithItem(RadiocraftBlocks.SOLAR_PANEL.get());
		complexBlockWithItem(RadiocraftBlocks.ANTENNA_POLE.get());
		complexHorizontalBlockWithItem(RadiocraftBlocks.QRP_RADIO_40M.get());
		complexHorizontalBlockWithItem(RadiocraftBlocks.QRP_RADIO_20M.get());
		complexHorizontalBlockWithItem(RadiocraftBlocks.VHF_BASE_STATION.get());
		complexHorizontalBlockWithItem(RadiocraftBlocks.VHF_RECEIVER.get());
		complexHorizontalBlockWithItem(RadiocraftBlocks.HF_RECEIVER.get());
		complexHorizontalBlockWithItem(RadiocraftBlocks.VHF_REPEATER.get());
		complexHorizontalBlockWithItem(RadiocraftBlocks.ALL_BAND_RADIO.get());
	}

	private void simpleBlockWithItem(Block block) {
		simpleBlock(block);
		simpleBlockItem(block, models().getExistingFile(modLoc("block/" + key(block).getPath())));
	}

	private void complexBlockWithItem(Block block) {
		ModelFile model = models().getExistingFile(modLoc("block/" + key(block).getPath()));
		simpleBlock(block, model);
		simpleBlockItem(block, model);
	}

	private void complexHorizontalBlock(Block block) {
		ModelFile model = models().getExistingFile(modLoc("block/" + key(block).getPath()));
		horizontalBlock(block, model);
	}

	private void complexHorizontalBlockWithItem(Block block) {
		ModelFile model = models().getExistingFile(modLoc("block/" + key(block).getPath()));
		horizontalBlock(block, model);
		simpleBlockItem(block, model);
	}

	private ResourceLocation key(Block block) {
		return RadiocraftBlocks.BLOCKS.getRegistry().get().getKey(block);
	}

}
