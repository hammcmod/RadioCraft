package com.arrl.radiocraft.datagen;

import com.arrl.radiocraft.common.blocks.radios.MicrophoneBlock;
import net.neoforged.neoforge.client.model.generators.MultiPartBlockStateBuilder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.core.Direction;
import com.arrl.radiocraft.common.blocks.RadioLinks;
import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.blocks.antennas.DuplexerBlock;
import com.arrl.radiocraft.common.blocks.radios.RadioBlock;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import net.minecraft.data.PackOutput;
import net.minecraft.server.packs.PackType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;

public class RadiocraftBlockstateProvider extends BlockStateProvider {

	public RadiocraftBlockstateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
		super(output, Radiocraft.MOD_ID, exFileHelper);
	}

	@Override
	protected void registerStatesAndModels() {
		RadiocraftBlocks.HF_RADIO_10M_VARIANTS.values().forEach(this::radio);
		RadiocraftBlocks.HF_RADIO_20M_VARIANTS.values().forEach(this::radio);
		RadiocraftBlocks.HF_RADIO_40M_VARIANTS.values().forEach(this::radio);
		RadiocraftBlocks.HF_RADIO_80M_VARIANTS.values().forEach(this::radio);
		complexHorizontalBlockWithItem(RadiocraftBlocks.LARGE_BATTERY.get());
		complexHorizontalBlockWithItem(RadiocraftBlocks.ANTENNA_TUNER.get());
		complexHorizontalBlockWithItem(RadiocraftBlocks.AUTO_TUNER.get());
		horizontalWithRadioLinks(RadiocraftBlocks.TELETYPE.get(), "solar_weather_station"); // The maintainer's model, from before the rename.
		horizontalWithRadioLinks(RadiocraftBlocks.RADIO_INTERFACE.get());
		horizontalWithRadioLinks(RadiocraftBlocks.TRANSFER_INTERFACE.get());
		duplexer();
		complexHorizontalBlockWithItem(RadiocraftBlocks.DUMMY_LOAD.get());
		deskMicrophone();
		complexHorizontalBlockWithItem(RadiocraftBlocks.WEFAX_RECEIVER.get());
		complexBlockWithItem(RadiocraftBlocks.SOLAR_PANEL.get());
		complexBlockWithItem(RadiocraftBlocks.ANTENNA_POLE.get());
		radio(RadiocraftBlocks.QRP_RADIO_40M.get());
		radio(RadiocraftBlocks.QRP_RADIO_20M.get());
		RadiocraftBlocks.VHF_BASE_STATION_VARIANTS.values().forEach(this::radio);
		radio(RadiocraftBlocks.VHF_RECEIVER.get());
		radio(RadiocraftBlocks.HF_RECEIVER.get());
		radio(RadiocraftBlocks.VHF_REPEATER.get());
		radio(RadiocraftBlocks.ALL_BAND_RADIO.get());
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

	/**
	 * A radio: turned like the other horizontal blocks, and with a second model for hanging from a ceiling, the same
	 * model raised so its top meets the block above (by however much room it leaves under a full block). A blown
	 * radio looks the same, so {@link RadioBlock#BLOWN} is left out of the variants.
	 */
	private void radio(Block block) {
		String name = key(block).getPath();
		ModelFile floor = models().getExistingFile(modLoc("block/" + name));
		float rise = 1.0F - modelTop(modLoc("models/block/" + name + ".json"));
		ModelFile ceiling = models().getBuilder("block/" + name + "_ceiling").parent(floor)
				.rootTransforms().translation(0.0F, rise, 0.0F).end();
		getVariantBuilder(block).forAllStatesExcept(state -> ConfiguredModel.builder()
				.modelFile(state.getValue(RadioBlock.CEILING) ? ceiling : floor)
				.rotationY(((int)state.getValue(RadioBlock.HORIZONTAL_FACING).toYRot() + 180) % 360)
				.build(), RadioBlock.BLOWN);
		simpleBlockItem(block, floor);
	}

	/** How high a model's elements reach, 0 to 1 of a block; 1 if it has none of its own. */
	private float modelTop(ResourceLocation file) {
		try(Reader reader = models().existingFileHelper.getResource(file, PackType.CLIENT_RESOURCES).openAsReader()) {
			JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
			if(!json.has("elements"))
				return 1.0F;
			float top = 0.0F;
			for(JsonElement element : json.getAsJsonArray("elements"))
				top = Math.max(top, element.getAsJsonObject().getAsJsonArray("to").get(1).getAsFloat());
			return Math.min(1.0F, top / 16.0F);
		}
		catch(IOException e) {
			throw new UncheckedIOException("Cannot read " + file, e);
		}
	}

	/**
	 * A radio accessory: turned like the other horizontal blocks, with a floor cable (block/radio_link) out of each side
	 * that has something it works with (see {@link RadioLinks}).
	 */
	private void horizontalWithRadioLinks(Block block) {
		horizontalWithRadioLinks(block, key(block).getPath());
	}

	private void horizontalWithRadioLinks(Block block, String modelName) {
		ModelFile model = models().getExistingFile(modLoc("block/" + modelName));
		ModelFile link = models().getExistingFile(modLoc("block/radio_link"));
		MultiPartBlockStateBuilder builder = getMultipartBuilder(block);
		for(Direction facing : Direction.Plane.HORIZONTAL)
			builder.part().modelFile(model).rotationY(((int)facing.toYRot() + 180) % 360).addModel()
					.condition(BlockStateProperties.HORIZONTAL_FACING, facing).end();
		for(Direction side : Direction.Plane.HORIZONTAL)
			builder.part().modelFile(link).rotationY(((int)side.toYRot() + 180) % 360).addModel()
					.condition(RadioLinks.side(side), true).end();
		simpleBlockItem(block, model);
	}

	/** The desk microphone: the maintainer's left or right model, whichever side its radio is on, turned by facing. */
	private void deskMicrophone() {
		Block block = RadiocraftBlocks.MICROPHONE.get();
		ModelFile left = models().getExistingFile(modLoc("block/desk_mic_left"));
		ModelFile right = models().getExistingFile(modLoc("block/desk_mic_right"));
		getVariantBuilder(block).forAllStates(state -> ConfiguredModel.builder()
				.modelFile(state.getValue(MicrophoneBlock.SIDE) == MicrophoneBlock.Side.LEFT ? left : right)
				.rotationY(((int)state.getValue(MicrophoneBlock.FACING).toYRot() + 180) % 360)
				.build());
		simpleBlockItem(block, left);
	}

	/** The duplexer, turned like the other horizontal blocks, with its coax run down to the repeater when it sits on one. */
	private void duplexer() {
		Block block = RadiocraftBlocks.DUPLEXER.get();
		ModelFile plain = models().getExistingFile(modLoc("block/duplexer"));
		ModelFile onRepeater = models().getExistingFile(modLoc("block/duplexer_on_repeater"));
		getVariantBuilder(block).forAllStates(state -> ConfiguredModel.builder()
				.modelFile(state.getValue(DuplexerBlock.ON_REPEATER) ? onRepeater : plain)
				.rotationY(((int)state.getValue(DuplexerBlock.FACING).toYRot() + 180) % 360)
				.build());
		simpleBlockItem(block, plain);
	}

	private ResourceLocation key(Block block) {
		return RadiocraftBlocks.BLOCKS.getRegistry().get().getKey(block);
	}

}
