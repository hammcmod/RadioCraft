package com.arrl.radiocraft.common.worldgen;

import com.arrl.radiocraft.common.blockentities.CommercialRadioModuleBlockEntity;
import com.arrl.radiocraft.common.blocks.TowerFootholdBlock;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.init.RadiocraftStructures;
import com.arrl.radiocraft.common.radio.Callsigns;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Makes each copy of a radio site its own as it goes down:
 * <ul>
 * <li>its optional parts ({@link SiteVariant}) are kept or left out;</li>
 * <li>a {@code callsign} of {@value #AUTO} in a block entity becomes a made-up station callsign for where the site
 * stands ({@link Callsigns#generateRepeater}), so two copies of a site never beacon under one name;</li>
 * <li>a commercial radio module with no program set gets one;</li>
 * <li>an old tower foothold is bent already {@link #BROKEN_CHANCE} of the time;</li>
 * <li>a charged battery keeps half to all of its charge.</li>
 * </ul>
 * What parts are kept and a module's program only depend on the world seed and where the site is, so
 * {@link SitePrediction} can tell what a site not generated yet will have.
 */
public class SiteProcessor extends StructureProcessor {

	public static final SiteProcessor INSTANCE = new SiteProcessor(List.of());
	public static final MapCodec<SiteProcessor> CODEC = SiteVariant.CODEC.listOf().optionalFieldOf("variants", List.of())
			.xmap(SiteProcessor::new, processor -> processor.variants);

	/** A callsign to make up on placement. */
	public static final String AUTO = "@auto";
	/** Share of a site's old footholds that are found bent. */
	public static final float BROKEN_CHANCE = 0.08F;
	/** Salt for a module's program, plus where it is in the template. */
	private static final long MODULE_SALT = 0x6D6F_6475_6C65L;

	private final List<SiteVariant> variants;
	/** The boxes the last copy placed leaves out, for its seed and origin. */
	@Nullable
	private volatile Removed removed;

	private record Removed(long seed, BlockPos origin, List<BoundingBox> boxes) {}

	public SiteProcessor(List<SiteVariant> variants) {
		this.variants = List.copyOf(variants);
	}

	/**
	 * A random source for the copy of a site at {@code origin}: the same for one world seed and one spot on the map
	 * (its height does not count), whenever and however often it is made.
	 */
	public static RandomSource siteRandom(long seed, BlockPos origin, long salt) {
		return RandomSource.create(seed ^ (origin.getX() * 341_873_128_712L + origin.getZ() * 132_897_987_541L) ^ salt * 0x9E37_79B9_7F4A_7C15L);
	}

	private static long seed(LevelReader level) {
		return level instanceof WorldGenLevel world ? world.getSeed() : 0L;
	}

	private List<BoundingBox> removed(long seed, BlockPos origin) {
		Removed last = removed;
		if(last == null || last.seed() != seed || !last.origin().equals(origin)) {
			last = new Removed(seed, origin.immutable(), SiteVariant.removed(variants, seed, origin));
			removed = last;
		}
		return last.boxes();
	}

	@Override
	public @Nullable StructureTemplate.StructureBlockInfo processBlock(@NotNull LevelReader level, @NotNull BlockPos offset, @NotNull BlockPos pos,
																	  @NotNull StructureTemplate.StructureBlockInfo original, @NotNull StructureTemplate.StructureBlockInfo info,
																	  @NotNull StructurePlaceSettings settings) {
		long seed = seed(level);
		if(!variants.isEmpty())
			for(BoundingBox box : removed(seed, offset))
				if(box.isInside(original.pos()))
					return new StructureTemplate.StructureBlockInfo(info.pos(), Blocks.CAVE_AIR.defaultBlockState(), null);
		RandomSource random = settings.getRandom(info.pos());
		BlockState state = info.state();
		if(state.is(RadiocraftBlocks.OLD_TOWER_FOOTHOLD.get()) && random.nextFloat() < BROKEN_CHANCE)
			state = state.setValue(TowerFootholdBlock.BROKEN, true);
		CompoundTag nbt = info.nbt();
		if(nbt != null) {
			nbt = nbt.copy();
			ServerLevel server = level instanceof ServerLevelAccessor accessor ? accessor.getLevel() : null;
			if(AUTO.equals(nbt.getString("callsign")))
				nbt.putString("callsign", server == null ? "" : Callsigns.generateRepeater(server, info.pos(), random));
			if(nbt.getString("id").equals(CommercialRadioModuleBlockEntity.ID) && !nbt.contains("program") && server != null)
				CommercialRadioModuleBlockEntity.preset(nbt, server, info.pos(), siteRandom(seed, offset, MODULE_SALT + original.pos().asLong()));
			if(nbt.contains("energy", Tag.TAG_INT) && nbt.getInt("energy") > 0)
				nbt.putInt("energy", (int)(nbt.getInt("energy") * (0.5F + 0.5F * random.nextFloat())));
		}
		return new StructureTemplate.StructureBlockInfo(info.pos(), state, nbt);
	}

	@Override
	protected @NotNull StructureProcessorType<?> getType() {
		return RadiocraftStructures.SITE_PROCESSOR.get();
	}
}
