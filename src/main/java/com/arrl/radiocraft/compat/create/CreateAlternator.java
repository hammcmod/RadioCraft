package com.arrl.radiocraft.compat.create;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.RadiocraftServerConfig;
import com.arrl.radiocraft.common.init.RadiocraftTabs;
import com.simibubi.create.api.stress.BlockStressValues;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registers the alternator and everything it needs: block, item, block entity, its FE capability and its stress on
 * Create's networks. Only loaded when {@link CreateCompat#isActive()}.
 */
public final class CreateAlternator {

	private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Radiocraft.MOD_ID);
	private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Radiocraft.MOD_ID);
	private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Radiocraft.MOD_ID);

	public static final DeferredBlock<AlternatorBlock> BLOCK = BLOCKS.register("alternator", () -> new AlternatorBlock(
			BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(2.0F, 6.0F).sound(SoundType.METAL)
					.requiresCorrectToolForDrops().noOcclusion()));
	public static final DeferredItem<BlockItem> ITEM = ITEMS.register("alternator", () -> new BlockItem(BLOCK.get(), new Item.Properties()));
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AlternatorBlockEntity>> BLOCK_ENTITY = BLOCK_ENTITIES.register("alternator",
			() -> BlockEntityType.Builder.of((pos, state) -> new AlternatorBlockEntity(CreateAlternator.BLOCK_ENTITY.get(), pos, state), BLOCK.get()).build(null));

	private CreateAlternator() {
	}

	static void register(IEventBus modBus) {
		BLOCKS.register(modBus);
		ITEMS.register(modBus);
		BLOCK_ENTITIES.register(modBus);
		modBus.addListener(CreateAlternator::registerCapabilities);
		modBus.addListener(CreateAlternator::addToTab);
		modBus.addListener(FMLCommonSetupEvent.class, event -> event.enqueueWork(() ->
				BlockStressValues.IMPACTS.register(BLOCK.get(), CreateAlternator::stressImpact)));
	}

	private static void registerCapabilities(RegisterCapabilitiesEvent event) {
		event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, BLOCK_ENTITY.get(), (alternator, side) -> alternator.energy);
	}

	private static void addToTab(BuildCreativeModeTabContentsEvent event) {
		if(event.getTab() == RadiocraftTabs.RADIOCRAFT_TAB.get())
			event.accept(ITEM.get());
	}

	/** Stress per RPM, as Create counts it, from {@code alternator_stress_at_256_rpm}; the default before a world loads. */
	static double stressImpact() {
		return serverValue(RadiocraftServerConfig.ALTERNATOR_STRESS_AT_256_RPM.getDefault(), true) / 256.0;
	}

	/** FE a tick at 256 RPM, from {@code alternator_fe_at_256_rpm}. */
	static int feAt256Rpm() {
		return serverValue(RadiocraftServerConfig.ALTERNATOR_FE_AT_256_RPM.getDefault(), false);
	}

	/** A server config value, or {@code fallback} where no world's config is loaded (item tooltips at the title screen). */
	private static int serverValue(int fallback, boolean stress) {
		try {
			return stress ? RadiocraftServerConfig.ALTERNATOR_STRESS_AT_256_RPM.get() : RadiocraftServerConfig.ALTERNATOR_FE_AT_256_RPM.get();
		}
		catch(IllegalStateException notLoaded) {
			return fallback;
		}
	}
}
