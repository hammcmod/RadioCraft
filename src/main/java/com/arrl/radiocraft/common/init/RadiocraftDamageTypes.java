package com.arrl.radiocraft.common.init;

import com.arrl.radiocraft.Radiocraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.Level;

/** The mod's damage types; their definitions are data, in {@code data/radiocraft/damage_type}. */
public final class RadiocraftDamageTypes {

	/** Bare power wire: water it touches, or its sparking end in the rain. */
	public static final ResourceKey<DamageType> ELECTROCUTION = ResourceKey.create(Registries.DAMAGE_TYPE, Radiocraft.id("electrocution"));

	private RadiocraftDamageTypes() {}

	public static DamageSource electrocution(Level level) {
		return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(ELECTROCUTION));
	}
}
