package com.arrl.radiocraft.gametest;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.be_networks.network_objects.RadioNetworkObject;
import com.arrl.radiocraft.common.blocks.TowerBeaconControllerBlock;
import com.arrl.radiocraft.common.blocks.TowerBeamBlock;
import com.arrl.radiocraft.common.blocks.TowerFootholdBlock;
import com.arrl.radiocraft.common.blocks.WireBlock;
import com.arrl.radiocraft.common.blocks.WireLook;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/**
 * Tower beams (2026-10-28): they bear on the blocks around them like iron bars, footholds hang on them, and coax runs
 * up a tower leg to the antenna on top.
 */
@GameTestHolder(Radiocraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class TowerBeamGameTests {

    private static int arms(GameTestHelper helper, BlockPos pos) {
        return TowerBeamBlock.arms(helper.getLevel(), helper.absolutePos(pos));
    }

    private static String bits(int mask) {
        return Integer.toBinaryString(mask);
    }

    /** A beam reaches down to the ground and out to a wall beside it, as iron bars do, and still meets the beam above. */
    @GameTest(template = GameTestUtil.EMPTY)
    public static void beamsBearOnTheGroundAndWalls(GameTestHelper helper) {
        BlockPos foot = new BlockPos(4, 2, 4);
        helper.setBlock(foot.below(), Blocks.STONE); // The ground.
        helper.setBlock(foot, RadiocraftBlocks.TOWER_BEAM.get());
        helper.setBlock(foot.above(), RadiocraftBlocks.TOWER_BEAM.get());
        helper.setBlock(foot.east(), Blocks.STONE);
        int up = TowerBeamBlock.bit(Direction.UP), down = TowerBeamBlock.bit(Direction.DOWN), east = TowerBeamBlock.bit(Direction.EAST);
        int mask = arms(helper, foot);
        helper.assertTrue(mask == (up | down | east), "Foot of the leg: expected up, down and east, got " + bits(mask));
        // The beam above meets only the beam below: nothing solid around it, and no post either, since it has a beam.
        helper.assertTrue(arms(helper, foot.above()) == down, "Upper beam: expected down only, got " + bits(arms(helper, foot.above())));
        // The top of a column reaches up into a redstone torch on it (a beacon light), though a torch is no solid face.
        BlockPos top = new BlockPos(12, 3, 12);
        helper.setBlock(top.below(), RadiocraftBlocks.TOWER_BEAM_THIN.get());
        helper.setBlock(top, RadiocraftBlocks.TOWER_BEACON_CONTROLLER.get());
        helper.setBlock(top.above(), Blocks.REDSTONE_TORCH);
        helper.assertTrue((arms(helper, top) & up) != 0, "The beam does not reach up into the torch: " + bits(arms(helper, top)));
        // A lone beam in the air is still a post, as in the hand.
        BlockPos lone = new BlockPos(10, 6, 10);
        helper.setBlock(lone, RadiocraftBlocks.TOWER_BEAM_THIN.get());
        helper.assertTrue(arms(helper, lone) == TowerBeamBlock.POST, "A lone beam is not a post: " + bits(arms(helper, lone)));
        helper.succeed();
    }

    /** The beacon controller blinks a redstone signal with the world's clock, 2 s on and 2 s off, and lights a lamp beside it. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 200)
    public static void beaconControllerBlinks(GameTestHelper helper) {
        BlockPos controller = new BlockPos(4, 2, 4), lamp = controller.east();
        helper.setBlock(controller.below(), Blocks.STONE);
        helper.setBlock(controller, RadiocraftBlocks.TOWER_BEACON_CONTROLLER.get());
        helper.setBlock(lamp, Blocks.REDSTONE_LAMP);
        helper.assertTrue(helper.getBlockState(controller).getBlock() instanceof TowerBeamBlock, "The controller is not a tower beam");
        boolean[] seen = new boolean[2]; // Off, on.
        long placed = helper.getLevel().getGameTime();
        helper.onEachTick(() -> {
            long now = helper.getLevel().getGameTime();
            // Settling: the controller's first tick, and a redstone lamp takes 4 ticks to go out after a switch.
            if(now - placed < 3 || Math.floorMod(now, (long)TowerBeaconControllerBlock.HALF_PERIOD) < 6)
                return;
            boolean powered = helper.getBlockState(controller).getValue(TowerBeaconControllerBlock.POWERED);
            helper.assertTrue(powered == TowerBeaconControllerBlock.onAt(now), "Out of step with the clock at " + now);
            helper.assertTrue(helper.getBlockState(lamp).getValue(net.minecraft.world.level.block.RedstoneLampBlock.LIT) == powered, "The lamp does not follow");
            seen[powered ? 1 : 0] = true;
        });
        helper.succeedWhen(() -> helper.assertTrue(seen[0] && seen[1], "Has not been both on and off yet"));
    }

    /**
     * A player holding a foothold and clicking the front of one with open air behind it (above a lone beam) builds the
     * stack upwards, as scaffolding does, rather than trying to place it out in front with nothing to hang on.
     */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 40)
    public static void clickingAFootholdsFrontBuildsTheStackUp(GameTestHelper helper) {
        // World coordinates throughout: a test plot may be rotated.
        net.minecraft.server.level.ServerLevel level = helper.getLevel();
        BlockPos beam = helper.absolutePos(new BlockPos(4, 3, 4)), bottom = beam.south();
        level.setBlockAndUpdate(beam, RadiocraftBlocks.TOWER_BEAM.get().defaultBlockState());
        level.setBlockAndUpdate(bottom, RadiocraftBlocks.TOWER_FOOTHOLD.get().defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH)
                .setValue(TowerFootholdBlock.MOUNT, TowerFootholdBlock.Mount.BEAM));
        net.minecraft.world.entity.player.Player player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        net.minecraft.world.item.ItemStack stack = new net.minecraft.world.item.ItemStack(com.arrl.radiocraft.common.init.RadiocraftItems.TOWER_FOOTHOLD.get(), 4);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, stack);
        for(int i = 0; i < 2; i++) { // Twice on the bottom one's front: the second goes on top of the first.
            net.minecraft.world.phys.BlockHitResult hit = new net.minecraft.world.phys.BlockHitResult(
                    net.minecraft.world.phys.Vec3.atCenterOf(bottom).add(0.0D, 0.0D, 0.3D), Direction.SOUTH, bottom, false);
            stack.useOn(new net.minecraft.world.item.context.UseOnContext(player, net.minecraft.world.InteractionHand.MAIN_HAND, hit));
        }
        for(BlockPos pos : List.of(bottom.above(), bottom.above(2))) {
            BlockState placed = level.getBlockState(pos);
            helper.assertTrue(placed.is(RadiocraftBlocks.TOWER_FOOTHOLD.get()) && placed.getValue(LadderBlock.FACING) == Direction.SOUTH,
                    "No foothold stacked at " + pos.toShortString() + ": " + placed);
            helper.assertTrue(placed.getValue(TowerFootholdBlock.MOUNT) == TowerFootholdBlock.Mount.BEAM, "The stacked foothold is not in line with the beam");
        }
        helper.assertTrue(level.getBlockState(bottom.south()).isAir(), "A foothold went out in front");
        helper.succeed();
    }

    /** Footholds stack with nothing behind them, in line with the one below; the stack stands as long as its bottom one. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 40)
    public static void footholdsStack(GameTestHelper helper) {
        BlockPos beam = new BlockPos(4, 3, 4), bottom = beam.south(), top = bottom.above(), topper = top.above();
        helper.setBlock(beam, RadiocraftBlocks.TOWER_BEAM.get()); // One beam: nothing behind the upper two.
        BlockState state = RadiocraftBlocks.TOWER_FOOTHOLD.get().defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH)
                .setValue(TowerFootholdBlock.MOUNT, TowerFootholdBlock.Mount.BEAM);
        helper.setBlock(bottom, state);
        helper.assertTrue(state.canSurvive(helper.getLevel(), helper.absolutePos(top)), "A foothold cannot stand on another");
        helper.setBlock(top, state);
        helper.setBlock(topper, state.setValue(TowerFootholdBlock.MOUNT, TowerFootholdBlock.Mount.WALL));
        helper.setBlock(topper.east(), Blocks.STONE); // Nudge the top one to re-check where it sits.
        helper.assertTrue(helper.getBlockState(top).is(RadiocraftBlocks.TOWER_FOOTHOLD.get()), "The stacked foothold fell");
        helper.assertFalse(RadiocraftBlocks.TOWER_FOOTHOLD.get().defaultBlockState().setValue(LadderBlock.FACING, Direction.EAST)
                .canSurvive(helper.getLevel(), helper.absolutePos(top.above(2))), "A foothold stands on thin air");
        helper.setBlock(bottom, Blocks.AIR);
        helper.succeedWhen(() -> {
            helper.assertBlockNotPresent(RadiocraftBlocks.TOWER_FOOTHOLD.get(), top);
            helper.assertBlockNotPresent(RadiocraftBlocks.TOWER_FOOTHOLD.get(), topper);
        });
    }

    /** A tower beacon on a controller lights and goes dark with it, at light 11, and the beam below reaches up into it. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 200)
    public static void towerBeaconBlinksOnItsController(GameTestHelper helper) {
        BlockPos controller = new BlockPos(6, 2, 6), beacon = controller.above();
        helper.setBlock(controller.below(), Blocks.STONE);
        helper.setBlock(controller, RadiocraftBlocks.TOWER_BEACON_CONTROLLER.get());
        helper.setBlock(beacon, RadiocraftBlocks.TOWER_BEACON.get());
        helper.assertTrue((arms(helper, controller) & TowerBeamBlock.bit(Direction.UP)) != 0, "The controller does not reach up into the beacon");
        boolean[] seen = new boolean[2]; // Dark, lit.
        long placed = helper.getLevel().getGameTime();
        helper.onEachTick(() -> {
            long now = helper.getLevel().getGameTime();
            if(now - placed < 3 || Math.floorMod(now, (long)TowerBeaconControllerBlock.HALF_PERIOD) < 2)
                return; // Settling, or just switched.
            BlockState state = helper.getBlockState(beacon);
            boolean lit = state.getValue(com.arrl.radiocraft.common.blocks.TowerBeaconBlock.LIT);
            helper.assertTrue(lit == TowerBeaconControllerBlock.onAt(now), "The beacon is out of step with its controller at " + now);
            helper.assertTrue(state.getLightEmission() == (lit ? com.arrl.radiocraft.common.blocks.TowerBeaconBlock.LIGHT : 0), "Light " + state.getLightEmission());
            seen[lit ? 1 : 0] = true;
        });
        helper.succeedWhen(() -> helper.assertTrue(seen[0] && seen[1], "Has not been both lit and dark yet"));
    }

    /** A diagonal brace needs room: a block of any kind beside the edge or corner it runs through keeps the beams apart. */
    @GameTest(template = GameTestUtil.EMPTY)
    public static void bracesDoNotGoThroughBlocks(GameTestHelper helper) {
        BlockPos beam = new BlockPos(4, 4, 4), edge = beam.north().east(), corner = beam.above().south().west();
        helper.setBlock(beam, RadiocraftBlocks.TOWER_BEAM.get());
        helper.setBlock(edge, RadiocraftBlocks.TOWER_BEAM.get());
        helper.setBlock(corner, RadiocraftBlocks.TOWER_BEAM.get());
        int ne = 0, usw = 0;
        for(int i = 0; i < TowerBeamBlock.OFFSETS.size(); i++) {
            if(TowerBeamBlock.OFFSETS.get(i).name().equals("north_east"))
                ne = 1 << i;
            if(TowerBeamBlock.OFFSETS.get(i).name().equals("up_south_west"))
                usw = 1 << i;
        }
        int open = TowerBeamBlock.connections(helper.getLevel(), helper.absolutePos(beam));
        helper.assertTrue(open == (ne | usw), "With nothing in the way: expected north_east and up_south_west, got " + bits(open));
        helper.setBlock(beam.east(), Blocks.IRON_BARS); // Beside the edge the north_east brace runs through.
        helper.setBlock(beam.above().west(), Blocks.OAK_FENCE); // Beside the corner the up_south_west brace runs through.
        int blocked = TowerBeamBlock.connections(helper.getLevel(), helper.absolutePos(beam));
        helper.assertTrue(blocked == 0, "Braces went through iron bars and a fence: " + bits(blocked));
        helper.setBlock(beam.east(), RadiocraftBlocks.TOWER_BEAM_THIN.get()); // Another beam is no obstacle.
        helper.assertTrue((TowerBeamBlock.connections(helper.getLevel(), helper.absolutePos(beam)) & ne) != 0, "A beam beside the edge blocked the brace");
        helper.succeed();
    }

    /** A foothold hangs on a beam, set back against the steel; it follows a change of what is behind it and falls with it. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 40)
    public static void footholdsHangOnBeams(GameTestHelper helper) {
        BlockPos beam = new BlockPos(4, 3, 4), rung = beam.south();
        helper.setBlock(beam, RadiocraftBlocks.TOWER_BEAM.get());
        BlockState state = RadiocraftBlocks.TOWER_FOOTHOLD.get().defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH)
                .setValue(TowerFootholdBlock.MOUNT, TowerFootholdBlock.Mount.of(helper.getBlockState(beam)));
        helper.assertTrue(state.canSurvive(helper.getLevel(), helper.absolutePos(rung)), "A foothold cannot hang on a beam");
        helper.assertTrue(state.getValue(TowerFootholdBlock.MOUNT) == TowerFootholdBlock.Mount.BEAM, "Mounted on " + state.getValue(TowerFootholdBlock.MOUNT));
        helper.setBlock(rung, state);
        helper.setBlock(beam, RadiocraftBlocks.TOWER_BEAM_THIN.get());
        helper.assertTrue(helper.getBlockState(rung).getValue(TowerFootholdBlock.MOUNT) == TowerFootholdBlock.Mount.THIN_BEAM,
                "A thinner beam behind it did not move the foothold back: " + helper.getBlockState(rung).getValue(TowerFootholdBlock.MOUNT));
        helper.setBlock(beam, Blocks.STONE);
        helper.assertTrue(helper.getBlockState(rung).getValue(TowerFootholdBlock.MOUNT) == TowerFootholdBlock.Mount.WALL, "On stone it is not a wall foothold");
        helper.setBlock(beam, Blocks.AIR);
        helper.succeedWhen(() -> helper.assertBlockNotPresent(RadiocraftBlocks.TOWER_FOOTHOLD.get(), rung));
    }

    /**
     * Coax from a radio on the ground runs up the side of a beam column and climbs onto the balun on top, drawn out
     * against the steel, and the radio finds the dipole.
     *
     * <pre>
     *   z:   3      4       5
     *      radio  coax   [beam]     y = 2 (ground)
     *             coax   [beam]     y = 3, 4
     *             coax   [beam]     y = 5: climbs south onto the balun
     *                    balun      y = 6, dipole east-west
     * </pre>
     */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 200)
    public static void coaxRunsUpABeamToTheAntenna(GameTestHelper helper) {
        BlockPos radio = new BlockPos(8, 2, 3), balun = new BlockPos(8, 6, 5);
        helper.setBlock(radio, RadiocraftBlocks.HF_RADIO_20M.get());
        for(int y = 2; y <= 5; y++)
            helper.setBlock(new BlockPos(8, y, 5), RadiocraftBlocks.TOWER_BEAM.get());
        helper.setBlock(balun, RadiocraftBlocks.BALUN_ONE_TO_ONE.get());
        GameTestUtil.wire(helper, balun, balun.west(5));
        GameTestUtil.wire(helper, balun, balun.east(5));
        for(int y = 2; y <= 5; y++)
            GameTestUtil.placeWire(helper, new BlockPos(8, y, 4), RadiocraftBlocks.COAX_WIRE.get());
        for(int y = 2; y <= 5; y++) // Placed bottom up, so refresh the column now that it is whole.
            ((WireBlock)helper.getBlockState(new BlockPos(8, y, 4)).getBlock()).refresh(helper.getLevel(), helper.absolutePos(new BlockPos(8, y, 4)));

        helper.succeedWhen(() -> {
            BlockState middle = helper.getBlockState(new BlockPos(8, 3, 4)), top = helper.getBlockState(new BlockPos(8, 5, 4));
            helper.assertTrue(middle.getValue(WireBlock.HUG) == WireBlock.Edge.SOUTH, "The run hugs " + middle.getValue(WireBlock.HUG) + ", not the beam");
            helper.assertTrue(top.getValue(WireBlock.CLIMB) == WireBlock.Climb.ONE, "The top coax does not climb onto the balun: " + top.getValue(WireBlock.CLIMB));
            WireLook.Look look = ((WireBlock)top.getBlock()).look(helper.getLevel(), helper.absolutePos(new BlockPos(8, 5, 4)), top);
            helper.assertTrue(look.end() == WireLook.End.CLIMB_DEVICE, "The climb ends at " + look.end());
            helper.assertTrue(look.hugReach() == 5, "Drawn " + look.hugReach() + " px out; a normal beam's steel is 5 px back");
            int antennas = GameTestUtil.networkObject(helper, radio, RadioNetworkObject.class).getAntennas().size();
            helper.assertTrue(antennas == 1, "The radio sees " + antennas + " antennas up the tower");
        });
    }
}
