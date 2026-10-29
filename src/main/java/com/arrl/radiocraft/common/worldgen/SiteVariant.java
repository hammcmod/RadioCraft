package com.arrl.radiocraft.common.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.ArrayList;
import java.util.List;

/**
 * Optional parts of a radio site: of {@code parts} (each one or more boxes of the template, in its own coordinates)
 * each copy of the site keeps {@code keep} of them, picked at random, and the rest are left out (cave air where they
 * would be). Four horns with {@code keep} 1 to 4, say, or a commercial radio module kept a third of the time.
 * Which are kept only depends on the world seed and where the site is ({@link SiteProcessor#siteRandom}), so the same
 * copy comes out the same whenever it is worked out.
 */
public record SiteVariant(List<List<BoundingBox>> parts, IntProvider keep) {

	public static final Codec<SiteVariant> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			BoundingBox.CODEC.listOf().listOf().fieldOf("parts").forGetter(SiteVariant::parts),
			IntProvider.CODEC.fieldOf("keep").forGetter(SiteVariant::keep)
	).apply(instance, SiteVariant::new));

	/** Salt for the variants' picks, the {@code i}th variant adding {@code i}. */
	private static final long SALT = 0x5173_7661_7269_616EL;

	/** The boxes, in template coordinates, that the copy of a site at {@code origin} leaves out. */
	public static List<BoundingBox> removed(List<SiteVariant> variants, long seed, BlockPos origin) {
		List<BoundingBox> out = new ArrayList<>();
		for(int i = 0; i < variants.size(); i++) {
			SiteVariant variant = variants.get(i);
			RandomSource random = SiteProcessor.siteRandom(seed, origin, SALT + i);
			int keep = Mth.clamp(variant.keep().sample(random), 0, variant.parts().size());
			List<Integer> order = new ArrayList<>();
			for(int part = 0; part < variant.parts().size(); part++)
				order.add(part);
			Util.shuffle(order, random);
			for(int k = keep; k < order.size(); k++)
				out.addAll(variant.parts().get(order.get(k)));
		}
		return out;
	}
}
