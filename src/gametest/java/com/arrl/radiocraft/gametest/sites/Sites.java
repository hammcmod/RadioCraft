package com.arrl.radiocraft.gametest.sites;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.blocks.WireBlock;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.init.RadiocraftItems;
import com.arrl.radiocraft.common.worldgen.SiteProcessor;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * The radio sites, built by {@link SiteBuilder} (dev runs only: {@code /rcsite build <site> <pos>}, then
 * {@code /rcsite save}). Each is laid out from its north-west bottom corner: y 0 is the ground layer the site replaces
 * (floors, foundations, the gravel of a compound), y 1 the first layer above ground.
 */
public final class Sites {

	/**
	 * One site: its template name and size, the margin of open ground round what is built (the builder's corner is
	 * that far in on x and z, so a sign or a button on an outside wall is in the template), whether it is found with
	 * power, and how it is built.
	 */
	public record Site(String name, int sizeX, int sizeY, int sizeZ, int margin, boolean powered, Consumer<SiteBuilder> build) {}

	public static final Map<String, Site> ALL = new LinkedHashMap<>();

	static {
		add(new Site("repeater_site", 14, 14, 10, 0, true, Sites::repeaterSite));
		add(new Site("buried_site", 11, 33, 9, 0, false, Sites::buriedSite));
		add(new Site("ionospheric_observatory", 13, 14, 11, 1, true, Sites::ionosphericObservatory));
		add(new Site("weather_observatory", 11, 10, 9, 1, true, Sites::weatherObservatory));
		add(new Site("direction_finding_station", 13, 8, 11, 1, true, Sites::directionFindingStation));
		add(new Site("long_lines", 19, 42, 13, 0, true, Sites::longLines));
	}

	private Sites() {}

	private static void add(Site site) {
		ALL.put(site.name(), site);
	}

	private static final WireBlock WIRE = RadiocraftBlocks.WIRE.get();
	private static final WireBlock COAX = RadiocraftBlocks.COAX_WIRE.get();

	private static WireBlock coax(DyeColor colour) {
		return RadiocraftBlocks.COLOURED_COAX.get(colour).get();
	}

	// -------------------- shared pieces --------------------

	/** A chest facing {@code facing} filled from {@code radiocraft:chests/<table>} when first opened. */
	private static void chest(SiteBuilder site, int x, int y, int z, Direction facing, String table) {
		site.block(x, y, z, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing));
		ResourceKey<LootTable> key = ResourceKey.create(Registries.LOOT_TABLE, Radiocraft.id("chests/" + table));
		site.with(x, y, z, ChestBlockEntity.class, chest -> chest.setLootTable(key, 0L));
	}

	/** A wall sign on the block behind it, facing {@code facing}, with up to four lines. */
	private static void sign(SiteBuilder site, int x, int y, int z, Direction facing, String... lines) {
		site.block(x, y, z, Blocks.SPRUCE_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING, facing));
		site.with(x, y, z, SignBlockEntity.class, sign -> {
			SignText text = new SignText();
			for(int i = 0; i < lines.length && i < 4; i++)
				text = text.setMessage(i, Component.literal(lines[i]));
			sign.setText(text, true);
			sign.setWaxed(true);
		});
	}

	/** An iron door in a wall, opening onto {@code outside}, with a stone button either side of it. */
	private static void ironDoor(SiteBuilder site, int x, int y, int z, Direction outside, boolean buttonLeft) {
		site.fill(x, y, z, x, y + 1, z, Blocks.AIR);
		site.place(x, y, z, Items.IRON_DOOR, outside.getOpposite());
		Direction along = buttonLeft ? outside.getCounterClockWise() : outside.getClockWise();
		int bx = x + along.getStepX(), bz = z + along.getStepZ();
		site.place(bx + outside.getStepX(), y + 1, bz + outside.getStepZ(), Items.STONE_BUTTON, outside, outside.getOpposite());
		site.place(bx - outside.getStepX(), y + 1, bz - outside.getStepZ(), Items.STONE_BUTTON, outside.getOpposite(), outside);
	}

	/** A column of {@code beam} from {@code y0} to {@code y1}. */
	private static void leg(SiteBuilder site, Block beam, int x, int y0, int y1, int z) {
		site.fill(x, y0, z, x, y1, z, beam);
	}

	/** Old footholds up the face of a beam column, facing {@code facing} (the beam is behind them). */
	private static void footholds(SiteBuilder site, int x, int y0, int y1, int z, Direction facing) {
		for(int y = y0; y <= y1; y++)
			site.place(x, y, z, RadiocraftItems.OLD_TOWER_FOOTHOLD.get(), facing, facing.getOpposite());
	}

	/** A Tower Beacon Controller with its red beacon on top. */
	private static void beacon(SiteBuilder site, int x, int y, int z) {
		site.place(x, y, z, RadiocraftItems.TOWER_BEACON_CONTROLLER.get(), Direction.NORTH);
		site.place(x, y + 1, z, RadiocraftItems.TOWER_BEACON.get(), Direction.NORTH);
	}

	/** Iron bars round the box's edge, two high, with a gap {@code gate} wide in the south side from {@code gateX}. */
	private static void fence(SiteBuilder site, int x0, int z0, int x1, int z1, int gateX, int gate) {
		BlockState bars = Blocks.IRON_BARS.defaultBlockState();
		for(int y = 1; y <= 2; y++) {
			for(int x = x0; x <= x1; x++) {
				site.block(x, y, z0, bars);
				if(x < gateX || x >= gateX + gate)
					site.block(x, y, z1, bars);
			}
			for(int z = z0; z <= z1; z++) {
				site.block(x0, y, z, bars);
				site.block(x1, y, z, bars);
			}
		}
	}

	/** A commercial radio module, its front facing back along {@code look}; whether a copy of the site keeps it is up to the site's variants. */
	private static void module(SiteBuilder site, int x, int y, int z, Direction look) {
		if(!site.level().getBlockState(site.at(x, y, z)).is(RadiocraftBlocks.COMMERCIAL_RADIO_MODULE.get()))
			site.place(x, y, z, RadiocraftItems.COMMERCIAL_RADIO_MODULE.get(), look);
	}

	// -------------------- the sites --------------------

	/**
	 * The AP&P Long Lines site, built by hand by the maintainer (its template captured with {@code /rcsite capture}):
	 * put back down as saved, with a commercial radio module added by the battery room wall.
	 */
	private static void longLines(SiteBuilder site) {
		try {
			SiteTemplates.placeRaw(site.level(), "long_lines", site.at(0, 0, 0));
		}
		catch(java.io.IOException e) {
			throw new java.io.UncheckedIOException(e);
		}
		module(site, 10, 1, 5, Direction.NORTH);
	}

	/**
	 * A fenced VHF repeater site: a stone hut with the commercial repeater and a commercial radio module (each copy's
	 * module picks its own program), twenty solar panels on the roof through a charge controller to two batteries, and
	 * a short lattice tower with the folded dipole on top, fed by coax out through the wall and up a leg.
	 */
	private static void repeaterSite(SiteBuilder site) {
		// Ground: a gravel compound, the hut's slab and the tower's footing.
		site.fill(1, 0, 1, 12, 0, 8, Blocks.GRAVEL);
		site.fill(2, 0, 2, 8, 0, 7, Blocks.LIGHT_GRAY_CONCRETE);
		site.fill(10, 0, 3, 11, 0, 4, Blocks.SMOOTH_STONE);
		fence(site, 0, 0, 13, 9, 3, 3);
		// The hut: stone walls, a flat roof, the inside kept clear.
		site.walls(2, 1, 2, 8, 5, 7, Blocks.SMOOTH_STONE);
		site.fill(2, 6, 2, 8, 6, 7, Blocks.SMOOTH_STONE);
		site.carve(3, 1, 3, 7, 5, 6);
		ironDoor(site, 4, 1, 7, Direction.SOUTH, false);
		site.place(3, 5, 4, Items.LANTERN, Direction.DOWN, Direction.NORTH);
		// The racks: the repeater in the north-east corner, the module in the north-west, fronts into the room.
		site.place(7, 1, 3, RadiocraftItems.COMMERCIAL_REPEATER.get(), Direction.EAST);
		site.place(3, 1, 3, RadiocraftItems.COMMERCIAL_RADIO_MODULE.get(), Direction.WEST);
		chest(site, 4, 1, 3, Direction.SOUTH, "repeater_site");
		// Power: the panels on the roof come down through it to the charge controller, which charges the batteries
		// along the east wall; the repeater runs off those.
		site.fill(3, 7, 3, 7, 7, 6, RadiocraftBlocks.SOLAR_PANEL.get());
		site.passthrough(5, 6, 6);
		site.run(WIRE, false, 5, 5, 6, 5, 2, 6);
		site.place(5, 1, 6, RadiocraftItems.CHARGE_CONTROLLER.get(), Direction.SOUTH);
		site.wire(WIRE, false, 6, 1, 6);
		site.place(7, 1, 6, RadiocraftItems.LARGE_BATTERY.get(), Direction.EAST);
		site.place(7, 1, 5, RadiocraftItems.LARGE_BATTERY.get(), Direction.EAST);
		site.wire(WIRE, false, 7, 2, 5, 7, 2, 6, 7, 1, 4);
		// The tower: a two by two of legs east of the hut, footholds up its south face, the dipole and a beacon on top.
		for(int[] xz : new int[][] { { 10, 3 }, { 11, 3 }, { 10, 4 }, { 11, 4 } })
			leg(site, RadiocraftBlocks.TOWER_BEAM.get(), xz[0], 1, 10, xz[1]);
		footholds(site, 10, 1, 10, 5, Direction.SOUTH);
		site.place(10, 11, 3, RadiocraftItems.FOLDED_DIPOLE_ANTENNA.get(), Direction.SOUTH);
		beacon(site, 11, 11, 4);
		// Coax: off the top of the repeater, through the east wall and up the tower's west leg to the dipole.
		site.wire(COAX, false, 7, 5, 3);
		site.passthrough(8, 5, 3);
		site.run(COAX, false, 9, 5, 3, 9, 11, 3);
		sign(site, 6, 2, 8, Direction.SOUTH, "", "REPEATER SITE", "NO TRESPASSING", "");
		site.refreshCables(0, 0, 0, 13, 13, 9);
	}

	/** Every so often a block of the box goes {@code worn}: cracked bricks in an old wall, say. */
	private static void wear(SiteBuilder site, int x0, int y0, int z0, int x1, int y1, int z1, Block from, Block worn, int every) {
		for(int x = x0; x <= x1; x++)
			for(int y = y0; y <= y1; y++)
				for(int z = z0; z <= z1; z++)
					if(Math.floorMod(x * 7 + y * 13 + z * 5, every) == 0 && site.level().getBlockState(site.at(x, y, z)).is(from))
						site.block(x, y, z, worn);
	}

	/**
	 * A bunker under the desert sand (found without power): a stone room eight blocks down (the template's ground is
	 * at y 8), a ladder shaft up to a hatch, and a thin lattice tower standing on the roof, its top eighteen blocks
	 * above the sand with a 5-BTV and a J-pole. Inside, a 40m HF radio and a VHF base station on their coax up the
	 * tower, a flat battery and a charge controller whose wire comes up the shaft for whoever brings solar panels.
	 */
	private static void buriedSite(SiteBuilder site) {
		// The shell, worn, with the room and the shaft kept clear.
		site.fill(0, 0, 0, 10, 5, 8, Blocks.STONE_BRICKS);
		site.fill(0, 5, 0, 3, 8, 2, Blocks.STONE_BRICKS);
		wear(site, 0, 0, 0, 10, 8, 8, Blocks.STONE_BRICKS, Blocks.CRACKED_STONE_BRICKS, 5);
		site.carve(1, 1, 1, 9, 4, 7);
		site.carve(1, 5, 1, 2, 8, 1);
		site.fill(1, 0, 1, 9, 0, 7, Blocks.POLISHED_ANDESITE);
		// Up the shaft: a ladder to a trapdoor, and the charge controller's wire beside it.
		for(int y = 1; y <= 7; y++)
			site.place(1, y, 1, Items.LADDER, Direction.SOUTH, Direction.NORTH);
		site.place(1, 8, 1, Items.SPRUCE_TRAPDOOR, Direction.DOWN, Direction.SOUTH);
		site.place(3, 1, 1, RadiocraftItems.CHARGE_CONTROLLER.get(), Direction.NORTH);
		site.run(WIRE, false, 2, 1, 1, 2, 8, 1);
		site.wire(WIRE, false, 4, 1, 1);
		site.place(5, 1, 1, RadiocraftItems.LARGE_BATTERY.get(), Direction.NORTH);
		// The radios, a block off the east wall with their power wire behind them.
		site.run(WIRE, false, 6, 1, 1, 9, 1, 1);
		site.run(WIRE, false, 9, 1, 2, 9, 1, 6);
		site.place(8, 1, 3, RadiocraftItems.HF_RADIO_40M.get(), Direction.EAST);
		site.place(8, 1, 5, RadiocraftItems.VHF_BASE_STATION.get(), Direction.EAST);
		site.with(8, 1, 3, com.arrl.radiocraft.common.blockentities.radio.RadioBlockEntity.class, radio -> { radio.setFrequency(7_150_000.0F); radio.updateFrequency(0); });
		site.with(8, 1, 5, com.arrl.radiocraft.common.blockentities.radio.RadioBlockEntity.class, radio -> { radio.setFrequency(146_520_000.0F); radio.updateFrequency(0); });
		// The tower on the roof, footholds up its south face from the sand, the antennas on top.
		for(int[] xz : new int[][] { { 8, 3 }, { 9, 3 }, { 8, 4 }, { 9, 4 } })
			leg(site, RadiocraftBlocks.TOWER_BEAM_THIN.get(), xz[0], 6, 25, xz[1]);
		footholds(site, 8, 8, 25, 5, Direction.SOUTH);
		site.place(8, 26, 3, RadiocraftItems.FIVE_BTV_ANTENNA.get(), Direction.NORTH);
		site.place(8, 26, 4, RadiocraftItems.J_POLE_ANTENNA.get(), Direction.NORTH);
		site.place(9, 26, 3, Items.LIGHTNING_ROD, Direction.NORTH);
		// Coax: black from the HF radio, plain from the VHF one, along the ceiling to one bundle up through the roof
		// beside the tower's leg; the plain strand steps off at the top to the J-pole.
		WireBlock black = coax(DyeColor.BLACK);
		site.run(black, false, 8, 2, 3, 8, 3, 3);
		site.wire(black, true, 8, 4, 3);
		site.run(COAX, false, 8, 2, 5, 8, 3, 5);
		site.wire(COAX, true, 8, 4, 5, 7, 4, 5, 7, 4, 4);
		site.run(black, true, 7, 4, 3, 7, 26, 3);
		site.run(COAX, true, 7, 4, 3, 7, 25, 3);
		site.wire(COAX, false, 7, 25, 4, 7, 26, 4);
		// Left as it was: a bunk, a chest, cobwebs.
		site.place(1, 1, 7, Items.RED_BED, Direction.EAST);
		chest(site, 3, 1, 7, Direction.NORTH, "buried_site");
		site.place(4, 1, 7, Items.BARREL, Direction.UP, Direction.SOUTH);
		site.place(5, 4, 4, Items.SOUL_LANTERN, Direction.DOWN, Direction.NORTH);
		for(int[] web : new int[][] { { 9, 4, 7 }, { 1, 4, 4 }, { 6, 4, 1 }, { 9, 3, 7 } })
			site.block(web[0], web[1], web[2], Blocks.COBWEB);
		site.refreshCables(0, 0, 0, 10, 32, 8);
	}

	/**
	 * An ionospheric observatory: a white building with the Ionospheric Processor, a 20m HF radio touching it (its
	 * timed bulletins on, so it puts the solar weather out over HF) and a WEFAX beside the radio that prints the
	 * reports; on the roof the processor's ionosonde, the radio's 5-BTV and twenty solar panels for the batteries.
	 */
	private static void ionosphericObservatory(SiteBuilder site) {
		site.fill(0, 0, 0, 10, 0, 8, Blocks.POLISHED_DIORITE);
		site.walls(0, 1, 0, 10, 5, 8, Blocks.WHITE_CONCRETE);
		site.fill(0, 6, 0, 10, 6, 8, Blocks.SMOOTH_QUARTZ);
		site.carve(1, 1, 1, 9, 5, 7);
		for(int x = 2; x <= 8; x += 3)
			site.fill(x, 2, 8, x, 3, 8, Blocks.GLASS_PANE);
		ironDoor(site, 0, 1, 4, Direction.WEST, false);
		site.place(5, 5, 4, Items.LANTERN, Direction.DOWN, Direction.NORTH);
		// Along the north wall: the processor, its radio, the WEFAX.
		site.place(5, 1, 1, RadiocraftItems.IONOSPHERIC_PROCESSOR.get(), Direction.NORTH);
		site.place(6, 1, 1, RadiocraftItems.HF_RADIO_20M.get(), Direction.NORTH);
		site.place(7, 1, 1, RadiocraftItems.WEFAX_RECEIVER.get(), Direction.NORTH);
		site.with(6, 1, 1, com.arrl.radiocraft.common.blockentities.radio.RadioBlockEntity.class, radio -> { radio.setFrequency(14_100_000.0F); radio.updateFrequency(0); });
		site.with(5, 1, 1, com.arrl.radiocraft.common.blockentities.IonosphericProcessorBlockEntity.class, processor -> processor.setTimedBulletins(true));
		site.with(7, 1, 1, com.arrl.radiocraft.common.blockentities.WefaxReceiverBlockEntity.class, wefax -> wefax.insertPaper(new net.minecraft.world.item.ItemStack(Items.PAPER, 32)));
		chest(site, 2, 1, 1, Direction.SOUTH, "ionospheric_observatory");
		module(site, 1, 1, 7, Direction.WEST);
		// Coax up the north wall: the processor's to the ionosonde, the radio's to the 5-BTV.
		site.run(COAX, false, 4, 1, 1, 4, 7, 1);
		site.place(4, 7, 2, RadiocraftItems.IONOSONDE.get(), Direction.SOUTH);
		site.run(coax(DyeColor.BLACK), false, 6, 2, 1, 6, 7, 1);
		site.place(7, 7, 1, RadiocraftItems.FIVE_BTV_ANTENNA.get(), Direction.NORTH);
		// Power: panels on the roof, down the south wall to the charge controller, the batteries in the south-east
		// corner, and a wire along the front of the desk to the radio and the WEFAX.
		site.fill(4, 7, 4, 8, 7, 7, RadiocraftBlocks.SOLAR_PANEL.get());
		site.passthrough(8, 6, 7);
		site.run(WIRE, false, 8, 5, 7, 8, 2, 7);
		site.place(8, 1, 7, RadiocraftItems.CHARGE_CONTROLLER.get(), Direction.SOUTH);
		site.wire(WIRE, false, 9, 1, 7);
		site.place(9, 1, 6, RadiocraftItems.LARGE_BATTERY.get(), Direction.EAST);
		site.place(9, 1, 5, RadiocraftItems.LARGE_BATTERY.get(), Direction.EAST);
		site.wire(WIRE, false, 9, 2, 6, 9, 2, 5);
		site.run(WIRE, false, 9, 1, 4, 9, 1, 2);
		site.run(WIRE, false, 8, 1, 2, 6, 1, 2);
		site.place(9, 7, 1, Items.LIGHTNING_ROD, Direction.NORTH);
		sign(site, -1, 3, 3, Direction.WEST, "", "IONOSPHERIC", "OBSERVATORY", "");
		site.refreshCables(0, 0, 0, 10, 13, 8);
	}

	/**
	 * A weather observatory: a timber station with a VHF base station on the APRS frequency, a radio interface and an
	 * H19 that beacons the weather from the weather station on its mast on the roof, a Slim Jim antenna, and solar
	 * panels on the roof for the batteries.
	 */
	private static void weatherObservatory(SiteBuilder site) {
		site.fill(0, 0, 0, 8, 0, 6, Blocks.STONE_BRICKS);
		site.walls(0, 1, 0, 8, 4, 6, Blocks.SPRUCE_PLANKS);
		for(int[] xz : new int[][] { { 0, 0 }, { 8, 0 }, { 0, 6 }, { 8, 6 } })
			site.fill(xz[0], 1, xz[1], xz[0], 4, xz[1], Blocks.STRIPPED_SPRUCE_LOG);
		site.fill(0, 5, 0, 8, 5, 6, Blocks.SPRUCE_PLANKS);
		site.carve(1, 1, 1, 7, 4, 5);
		site.fill(2, 2, 6, 2, 3, 6, Blocks.GLASS_PANE);
		site.fill(6, 2, 6, 6, 3, 6, Blocks.GLASS_PANE);
		ironDoor(site, 4, 1, 6, Direction.SOUTH, true);
		site.place(4, 4, 3, Items.LANTERN, Direction.DOWN, Direction.NORTH);
		// The station along the north wall: radio, interface, H19; the battery beside them.
		site.place(2, 1, 1, RadiocraftItems.VHF_BASE_STATION.get(), Direction.NORTH);
		site.place(3, 1, 1, RadiocraftItems.RADIO_INTERFACE.get(), Direction.NORTH);
		site.place(4, 1, 1, RadiocraftItems.APRS_TERMINAL.get(), Direction.NORTH);
		site.place(6, 1, 1, RadiocraftItems.LARGE_BATTERY.get(), Direction.NORTH);
		site.with(2, 1, 1, com.arrl.radiocraft.common.blockentities.radio.RadioBlockEntity.class, radio -> { radio.setFrequency(144_390_000.0F); radio.updateFrequency(0); });
		site.with(4, 1, 1, com.arrl.radiocraft.common.blockentities.AprsTerminalBlockEntity.class, terminal -> {
			net.minecraft.nbt.CompoundTag settings = new net.minecraft.nbt.CompoundTag();
			settings.putString("comment", "WX OBSERVATORY");
			settings.putBoolean("beaconEnabled", true);
			terminal.applySettings(settings);
		});
		chest(site, 1, 1, 5, Direction.EAST, "weather_observatory");
		site.block(7, 1, 5, Blocks.BOOKSHELF);
		module(site, 7, 1, 4, Direction.EAST);
		// Power: panels on the roof, down the north-east corner to the charge controller, a wire along the desk.
		site.fill(4, 6, 1, 7, 6, 5, RadiocraftBlocks.SOLAR_PANEL.get());
		site.passthrough(7, 5, 1);
		site.run(WIRE, false, 7, 4, 1, 7, 2, 1);
		site.place(7, 1, 1, RadiocraftItems.CHARGE_CONTROLLER.get(), Direction.NORTH);
		site.run(WIRE, false, 7, 1, 2, 2, 1, 2);
		// The Slim Jim on the roof's north-west corner, the weather station on a mast at the south-west.
		site.run(COAX, false, 2, 2, 1, 2, 6, 1);
		site.place(1, 6, 1, RadiocraftItems.SLIM_JIM_ANTENNA.get(), Direction.SOUTH);
		leg(site, RadiocraftBlocks.TOWER_BEAM_THIN.get(), 1, 6, 8, 5);
		site.place(1, 9, 5, RadiocraftItems.WEATHER_STATION.get(), Direction.NORTH);
		sign(site, 5, 2, 7, Direction.SOUTH, "", "WEATHER", "OBSERVATORY", "");
		site.refreshCables(0, 0, 0, 8, 10, 6);
	}

	/**
	 * A radio monitoring station of the kind the FCC runs to track interference down: a concrete building with a
	 * five-piece direction finder (the finder and four modules round it) on 2m, each module on its own colour of coax
	 * up through the roof to a Ringo at the roof's edge, north, east, south and west; solar panels on the roof.
	 */
	private static void directionFindingStation(SiteBuilder site) {
		site.fill(0, 0, 0, 10, 0, 8, Blocks.GRAY_CONCRETE);
		site.walls(0, 1, 0, 10, 4, 8, Blocks.LIGHT_GRAY_CONCRETE);
		site.fill(0, 5, 0, 10, 5, 8, Blocks.GRAY_CONCRETE);
		site.carve(1, 1, 1, 9, 4, 7);
		for(int x = 6; x <= 8; x++)
			site.block(x, 2, 8, Blocks.GLASS_PANE);
		ironDoor(site, 2, 1, 8, Direction.SOUTH, false);
		site.place(5, 4, 6, Items.LANTERN, Direction.DOWN, Direction.NORTH);
		// The finder and its modules in the middle of the room.
		site.place(5, 1, 4, RadiocraftItems.DIRECTION_FINDER.get(), Direction.NORTH);
		int[][] modules = { { 4, 4 }, { 6, 4 }, { 5, 3 }, { 5, 5 } };
		for(int[] xz : modules)
			site.place(xz[0], 1, xz[1], RadiocraftItems.DIRECTION_FINDER_MODULE.get(), Direction.NORTH);
		// Each module's coax straight up through the roof and out to its Ringo.
		DyeColor[] colours = { DyeColor.LIGHT_BLUE, DyeColor.ORANGE, DyeColor.RED, DyeColor.LIME };
		int[][] ringos = { { 1, 4 }, { 9, 4 }, { 5, 1 }, { 5, 7 } };
		for(int i = 0; i < 4; i++) {
			WireBlock kind = coax(colours[i]);
			int x = modules[i][0], z = modules[i][1];
			site.run(kind, false, x, 2, z, x, 6, z);
			int dx = Integer.signum(ringos[i][0] - x), dz = Integer.signum(ringos[i][1] - z);
			site.run(kind, false, x + dx, 6, z + dz, ringos[i][0] - dx, 6, ringos[i][1] - dz);
			site.place(ringos[i][0], 6, ringos[i][1], RadiocraftItems.RINGO_ANTENNA.get(), Direction.NORTH);
		}
		// Power: panels on the roof's north-west corner, down the west wall to the charge controller and the
		// battery, and a wire to the west module, which passes it to the finder.
		site.fill(1, 6, 1, 3, 6, 3, RadiocraftBlocks.SOLAR_PANEL.get());
		site.fill(7, 6, 5, 9, 6, 7, RadiocraftBlocks.SOLAR_PANEL.get());
		site.passthrough(1, 5, 2);
		site.passthrough(9, 5, 6);
		site.run(WIRE, false, 1, 4, 2, 1, 2, 2);
		site.run(WIRE, false, 9, 4, 6, 9, 2, 6);
		site.place(1, 1, 2, RadiocraftItems.CHARGE_CONTROLLER.get(), Direction.WEST);
		site.place(9, 1, 6, RadiocraftItems.CHARGE_CONTROLLER.get(), Direction.EAST);
		site.wire(WIRE, false, 1, 1, 3, 9, 1, 5);
		site.place(1, 1, 4, RadiocraftItems.LARGE_BATTERY.get(), Direction.WEST);
		site.place(9, 1, 4, RadiocraftItems.LARGE_BATTERY.get(), Direction.EAST);
		site.run(WIRE, false, 2, 1, 4, 3, 1, 4);
		site.run(WIRE, false, 8, 1, 4, 7, 1, 4);
		site.with(5, 1, 4, com.arrl.radiocraft.common.blockentities.DirectionFinderBlockEntity.class, finder -> finder.setFrequency(146_520_000.0F));
		chest(site, 9, 1, 1, Direction.WEST, "direction_finding_station");
		site.block(1, 1, 7, Blocks.CARTOGRAPHY_TABLE);
		module(site, 8, 1, 7, Direction.SOUTH); // A seized transmitter.
		sign(site, 4, 2, 9, Direction.SOUTH, "", "RADIO MONITORING", "STATION", "");
		site.refreshCables(0, 0, 0, 10, 7, 8);
	}
}
