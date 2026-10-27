package com.arrl.radiocraft.common.radio.microwave;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Which microwave radio holds each callsign and ID pair, for the whole server (saved with the overworld), so no two
 * radios share one even while one of them is unloaded.
 */
public class MicrowaveIdentities extends SavedData {

	public static final String SAVE_NAME = "radiocraft_microwave_ids";

	private final Map<String, GlobalPos> owners = new HashMap<>();

	public static Factory<MicrowaveIdentities> factory() {
		return new Factory<>(MicrowaveIdentities::new, MicrowaveIdentities::load, null);
	}

	public static MicrowaveIdentities get(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(factory(), SAVE_NAME);
	}

	public static String key(String callsign, int id) {
		return callsign + "-" + MicrowaveMath.hex(id);
	}

	/** Who holds {@code key}, null if nobody. */
	@Nullable
	public GlobalPos owner(String key) {
		return owners.get(key);
	}

	/**
	 * Gives {@code key} to the radio at {@code at}, letting go of {@code previous} (its old pair, null for none).
	 * False, changing nothing, if another radio holds it.
	 */
	public boolean claim(String key, @Nullable String previous, GlobalPos at) {
		GlobalPos holder = owners.get(key);
		if(holder != null && !holder.equals(at))
			return false;
		if(previous != null && at.equals(owners.get(previous)))
			owners.remove(previous);
		owners.put(key, at);
		setDirty();
		return true;
	}

	/** The radio at {@code at} is gone: its pair is free again. */
	public void release(String key, GlobalPos at) {
		if(at.equals(owners.get(key))) {
			owners.remove(key);
			setDirty();
		}
	}

	@Override
	public @NotNull CompoundTag save(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
		CompoundTag list = new CompoundTag();
		owners.forEach((key, pos) -> GlobalPos.CODEC.encodeStart(NbtOps.INSTANCE, pos).result().ifPresent(encoded -> list.put(key, encoded)));
		tag.put("owners", list);
		return tag;
	}

	public static MicrowaveIdentities load(CompoundTag tag, HolderLookup.Provider registries) {
		MicrowaveIdentities out = new MicrowaveIdentities();
		CompoundTag list = tag.getCompound("owners");
		for(String key : list.getAllKeys())
			GlobalPos.CODEC.parse(NbtOps.INSTANCE, list.get(key)).result().ifPresent(pos -> out.owners.put(key, pos));
		return out;
	}

	/** For tests: a fresh position key. */
	public static GlobalPos at(net.minecraft.world.level.Level level, BlockPos pos) {
		return GlobalPos.of(level.dimension(), pos.immutable());
	}
}
