package com.arrl.radiocraft.gametest.sites;

import com.arrl.radiocraft.common.worldgen.SitePiece;
import com.arrl.radiocraft.common.worldgen.SiteProcessor;
import com.arrl.radiocraft.common.worldgen.SiteStructure;
import com.arrl.radiocraft.common.worldgen.SiteVariant;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Saves a built site as a structure template in the mod's resources, and puts one back down from there. Plain air is
 * left out (cave air is kept: see {@link SiteBuilder#carve}), and every block entity's tag is cleaned of what belongs
 * to the world it was built in: positions of linked blocks, game times, what it has heard, a microwave radio's claimed
 * identity. A commercial radio module keeps nothing, so each copy picks its own program; repeaters, radio interfaces
 * and processors get a callsign made up for each copy.
 */
public final class SiteTemplates {

	/** Where the templates live, from the run directory ({@code runs/server} or {@code runs/client}). */
	public static final Path DIR = Path.of("..", "..", "src", "main", "resources", "data", "radiocraft", "structure", "sites");

	/** Keys no site keeps, on any block entity. */
	private static final Set<String> DROP = Set.of(
			"master", "modules", "linked", "dishes", "wiredSpeakers", "wiredSpeakerKinds", "micHolder", "micId", "subscribers",
			"entries", "rssiHistory", "beacons", "messages", "mapSets", "lastBeacon", "lastOpened", "newContact", "lastBulletin",
			"alertedLocally", "alertedOnAir", "repeatLampUntil", "bootPending", "checkCooldown", "pttDown", "signal", "tuneFail",
			"alertsSent", "bulletinsSent", "pending", "lastEvent", "statuses", "locked", "bearing", "meter", "sigma", "hearing",
			"callsignSetBy", "unread", "nextMessageNumber", "inventory", "rssi", "lastSignal", "printing", "ready", "pixels", "lastAlertAt",
			"printStart", "requestUntil", "solarHeardAt", "solarFrom", "solarLines", "LootTableSeed");
	/** And on a microwave radio: the identity it claimed in the world it was built in, and its link. */
	private static final Set<String> MICROWAVE_DROP = Set.of("callsign", "hexId", "masterName", "candidateName", "alignName",
			"alignReason", "phase", "problem", "searchTicks", "sweep", "interference", "searchLength");

	/** Block entities that go out under a callsign of their own: each copy of a site makes one up ({@link SiteProcessor}). */
	private static final Set<String> AUTO_CALLSIGN = Set.of("radiocraft:commercial_repeater", "radiocraft:vhf_repeater",
			"radiocraft:radio_interface", "radiocraft:ionospheric_processor");

	/** The charge a powered site's batteries are found with, before the processor takes up to half of it off. */
	public static final int CHARGE = 200_000;

	private SiteTemplates() {}

	public static Path file(String name) {
		return DIR.resolve(name + ".nbt");
	}

	/**
	 * Saves the box from {@code from}, {@code size} big, as site {@code name}; returns the keys left on each kind of
	 * block entity, for a look over.
	 */
	public static Map<String, Set<String>> save(ServerLevel level, BlockPos from, Vec3i size, String name, boolean switchOn) throws IOException {
		StructureTemplate template = new StructureTemplate();
		template.fillFromWorld(level, from, size, false, Blocks.AIR);
		template.setAuthor("RadioCraft");
		CompoundTag tag = template.save(new CompoundTag());
		Map<String, Set<String>> kept = new TreeMap<>();
		ListTag blocks = tag.getList("blocks", Tag.TAG_COMPOUND);
		for(int i = 0; i < blocks.size(); i++) {
			CompoundTag block = blocks.getCompound(i);
			if(!block.contains("nbt", Tag.TAG_COMPOUND))
				continue;
			CompoundTag nbt = block.getCompound("nbt");
			clean(nbt, switchOn);
			kept.computeIfAbsent(nbt.getString("id"), id -> new TreeSet<>()).addAll(nbt.getAllKeys());
		}
		Files.createDirectories(DIR);
		NbtIo.writeCompressed(tag, file(name));
		return kept;
	}

	/** Cleans {@code nbt}; with {@code switchOn}, every radio and finder in it is left switched on, ready on load. */
	private static void clean(CompoundTag nbt, boolean switchOn) {
		String id = nbt.getString("id");
		if(id.equals("radiocraft:commercial_radio_module")) {
			for(String key : Set.copyOf(nbt.getAllKeys()))
				if(!key.equals("id"))
					nbt.remove(key);
			return;
		}
		DROP.forEach(nbt::remove);
		if(AUTO_CALLSIGN.contains(id))
			nbt.putString("callsign", SiteProcessor.AUTO);
		if(id.equals("radiocraft:microwave_radio"))
			MICROWAVE_DROP.forEach(nbt::remove);
		if(switchOn) {
			if(nbt.contains("energy"))
				nbt.putInt("energy", CHARGE);
			for(String flag : new String[] { "wasPowered", "switchedOn", "on" })
				if(nbt.contains(flag))
					nbt.putBoolean(flag, true);
		}
	}

	/** Reads site {@code name} from the resources (not the copy the game loaded), or null if there is none. */
	public static StructureTemplate load(ServerLevel level, String name) throws IOException {
		Path path = file(name);
		if(!Files.exists(path))
			return null;
		StructureTemplate template = new StructureTemplate();
		template.load(level.holderLookup(Registries.BLOCK), NbtIo.readCompressed(path, NbtAccounter.unlimitedHeap()));
		return template;
	}

	/** Puts site {@code name} down at {@code at} exactly as saved: no processor (to add to it and save it again). */
	public static boolean placeRaw(ServerLevel level, String name, BlockPos at) throws IOException {
		StructureTemplate template = load(level, name);
		if(template == null)
			return false;
		template.placeInWorld(level, at, at, new StructurePlaceSettings(), level.getRandom(), Block.UPDATE_ALL);
		return true;
	}

	/**
	 * Puts site {@code name} down at {@code at} as the world generator would (with the site processor), with all its
	 * parts kept or, with {@code variants}, the ones a site generated there would keep.
	 */
	public static boolean place(ServerLevel level, String name, BlockPos at, boolean variants) throws IOException {
		StructureTemplate template = load(level, name);
		if(template == null)
			return false;
		template.placeInWorld(level, at, at, SitePiece.settings(variants ? variants(level, name) : List.of()), level.getRandom(), Block.UPDATE_ALL);
		return true;
	}

	/** Site {@code name}'s optional parts, from its worldgen structure (none if it has none). */
	public static List<SiteVariant> variants(ServerLevel level, String name) {
		return level.registryAccess().registryOrThrow(Registries.STRUCTURE).getOptional(com.arrl.radiocraft.Radiocraft.id(name))
				.map(structure -> structure instanceof SiteStructure site ? site.variants() : List.<SiteVariant>of()).orElse(List.of());
	}
}
