package com.arrl.radiocraft.common.worldgen;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.init.RadiocraftStructures;
import com.arrl.radiocraft.common.radio.CommercialStations;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * The one piece of a {@link SiteStructure}: its template, unturned, through a {@link SiteProcessor} with the site's
 * optional parts (kept with the piece, as it is saved between being planned and being placed).
 */
public class SitePiece extends TemplateStructurePiece {

	private final List<SiteVariant> variants;

	public SitePiece(StructureTemplateManager manager, ResourceLocation template, BlockPos origin, List<SiteVariant> variants) {
		super(RadiocraftStructures.SITE_PIECE.get(), 0, manager, template, template.toString(), settings(variants), origin);
		this.variants = variants;
	}

	public SitePiece(StructureTemplateManager manager, CompoundTag tag) {
		super(RadiocraftStructures.SITE_PIECE.get(), tag, manager, location -> settings(readVariants(tag)));
		this.variants = readVariants(tag);
	}

	private static List<SiteVariant> readVariants(CompoundTag tag) {
		if(!tag.contains("variants", Tag.TAG_LIST))
			return List.of();
		return SiteVariant.CODEC.listOf().parse(NbtOps.INSTANCE, tag.get("variants"))
				.resultOrPartial(error -> Radiocraft.LOGGER.error("A radio site's variants: {}", error)).orElse(List.of());
	}

	@Override
	protected void addAdditionalSaveData(@NotNull StructurePieceSerializationContext context, @NotNull CompoundTag tag) {
		super.addAdditionalSaveData(context, tag);
		SiteVariant.CODEC.listOf().encodeStart(NbtOps.INSTANCE, variants).result().ifPresent(variants -> tag.put("variants", variants));
	}

	/** How a site's template goes down: as built, all its parts kept (a dev placement). */
	public static StructurePlaceSettings settings() {
		return settings(List.of());
	}

	/** How a site's template goes down: as built, with each copy's parts, callsigns and wear its own. */
	public static StructurePlaceSettings settings(List<SiteVariant> variants) {
		return new StructurePlaceSettings().setRotation(Rotation.NONE).setMirror(Mirror.NONE).addProcessor(new SiteProcessor(variants));
	}

	/**
	 * The commercial radio module stations this copy of the site will have once it is placed: its modules that are
	 * kept, with the programs the {@link SiteProcessor} gives them, as predicted stations.
	 */
	public List<CommercialStations.Station> stations(ServerLevel level) {
		List<CommercialStations.Station> out = new ArrayList<>();
		for(StructureTemplate.StructureBlockInfo info : template.filterBlocks(templatePosition, placeSettings, RadiocraftBlocks.COMMERCIAL_RADIO_MODULE.get())) {
			if(info.nbt() == null)
				continue; // Not the bottom cell.
			StructureTemplate.StructureBlockInfo original = new StructureTemplate.StructureBlockInfo(info.pos().subtract(templatePosition), info.state(), info.nbt());
			StructureTemplate.StructureBlockInfo placed = null;
			for(var processor : placeSettings.getProcessors()) {
				placed = processor.process(level, templatePosition, templatePosition, original, placed == null ? info : placed, placeSettings, template);
				if(placed == null)
					break;
			}
			if(placed == null || placed.nbt() == null || !placed.state().is(RadiocraftBlocks.COMMERCIAL_RADIO_MODULE.get()))
				continue;
			CommercialStations.Station station = CommercialStations.Station.fromModuleTag(placed.pos(), placed.nbt(), true);
			if(station != null)
				out.add(station);
		}
		return out;
	}

	@Override
	protected void handleDataMarker(@NotNull String name, @NotNull BlockPos pos, @NotNull ServerLevelAccessor level, @NotNull RandomSource random, @NotNull BoundingBox box) {
	}
}
