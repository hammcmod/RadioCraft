package com.arrl.radiocraft.common.init;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.worldgen.SitePiece;
import com.arrl.radiocraft.common.worldgen.SiteProcessor;
import com.arrl.radiocraft.common.worldgen.SiteStructure;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The radio sites found in the world: one structure type ({@link SiteStructure}), its piece and its processor. The
 * sites themselves are data: {@code data/radiocraft/worldgen/structure}, {@code worldgen/structure_set/radio_sites}
 * and the templates in {@code data/radiocraft/structure/sites}.
 */
public final class RadiocraftStructures {

	public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, Radiocraft.MOD_ID);
	public static final DeferredRegister<StructurePieceType> PIECE_TYPES = DeferredRegister.create(Registries.STRUCTURE_PIECE, Radiocraft.MOD_ID);
	public static final DeferredRegister<StructureProcessorType<?>> PROCESSORS = DeferredRegister.create(Registries.STRUCTURE_PROCESSOR, Radiocraft.MOD_ID);

	public static final DeferredHolder<StructureType<?>, StructureType<SiteStructure>> SITE = STRUCTURE_TYPES.register("site", () -> () -> SiteStructure.CODEC);
	public static final DeferredHolder<StructurePieceType, StructurePieceType> SITE_PIECE = PIECE_TYPES.register("site",
			() -> (StructurePieceType.StructureTemplateType)SitePiece::new);
	public static final DeferredHolder<StructureProcessorType<?>, StructureProcessorType<SiteProcessor>> SITE_PROCESSOR = PROCESSORS.register("site",
			() -> () -> SiteProcessor.CODEC);

	private RadiocraftStructures() {}

	public static void register(IEventBus modEventBus) {
		STRUCTURE_TYPES.register(modEventBus);
		PIECE_TYPES.register(modEventBus);
		PROCESSORS.register(modEventBus);
	}
}
