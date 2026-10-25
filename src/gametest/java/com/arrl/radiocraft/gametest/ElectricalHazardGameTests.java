package com.arrl.radiocraft.gametest;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.RadiocraftServerConfig;
import com.arrl.radiocraft.api.benetworks.BENetworkObject;
import com.arrl.radiocraft.common.be_networks.BatteryFires;
import com.arrl.radiocraft.common.be_networks.WireFaults;
import com.arrl.radiocraft.common.be_networks.network_objects.AntennaNetworkObject;
import com.arrl.radiocraft.common.be_networks.network_objects.BatteryNetworkObject;
import com.arrl.radiocraft.common.capabilities.BENetworksCapability;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.radio.Band;
import com.arrl.radiocraft.common.radio.antenna.LightningRodAntennas;
import com.arrl.radiocraft.common.radio.rfi.AntennaStrikes;
import com.arrl.radiocraft.common.blocks.radios.RadioBlock;
import com.arrl.radiocraft.common.radio.antenna.types.vhf.LightningRodAntennaType;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/**
 * Electrical hazards (maintainer, 2026-10-24): shorts, wet wire, sparking ends in the rain, burning and exploding
 * batteries, lightning on wire, and lightning rods as antennas. Hazards are off for the rest of the GameTest run (see
 * {@link GameTestDefaults}), so each test switches what it needs on in a batch of its own and off again.
 */
@GameTestHolder(Radiocraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ElectricalHazardGameTests {

    private static final BlockPos BATTERY = new BlockPos(1, 2, 1);
    private static final int CHARGE = 1_000_000;

    private static BatteryNetworkObject battery(GameTestHelper helper, BlockPos pos) {
        return GameTestUtil.networkObject(helper, pos, BatteryNetworkObject.class);
    }

    /** Rain tests build this far up: something in the test plot stands over the ground and keeps the rain off. */
    private static final int UP = 12;

    /** Stone under each position, for builds floated {@link #UP}. */
    private static void floor(GameTestHelper helper, BlockPos... on) {
        for(BlockPos pos : on)
            helper.setBlock(pos.below(), Blocks.STONE);
    }

    private static void wire(GameTestHelper helper, BlockPos... at) {
        for(BlockPos pos : at)
            GameTestUtil.placeWire(helper, pos, RadiocraftBlocks.WIRE.get());
    }

    /** A loop of power wire on a charged battery smokes, burns through, and drains the battery hard. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 200, batch = "hazard_short")
    public static void shortBurnsThrough(GameTestHelper helper) {
        RadiocraftServerConfig.WIRE_HAZARDS_ENABLED.set(true);
        BlockPos closing = new BlockPos(3, 2, 2);
        helper.setBlock(BATTERY, RadiocraftBlocks.LARGE_BATTERY.get());
        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> {
                    battery(helper, BATTERY).getStorage().setEnergy(CHARGE);
                    wire(helper, new BlockPos(2, 2, 1), new BlockPos(3, 2, 1), new BlockPos(2, 2, 2), closing);
                    helper.assertTrue(WireFaults.closesLoop(helper.getLevel(), helper.absolutePos(closing)), "The square of wire is not a loop");
                    helper.assertTrue(WireFaults.faultAt(helper.getLevel(), helper.absolutePos(closing)) == WireFaults.Kind.SHORT, "No short found");
                })
                .thenWaitUntil(() -> helper.assertFalse(helper.getBlockState(closing).is(RadiocraftBlocks.WIRE.get()), "The short has not burned through"))
                .thenExecute(() -> {
                    int left = battery(helper, BATTERY).getStorage().getEnergyStored();
                    RadiocraftServerConfig.WIRE_HAZARDS_ENABLED.set(false);
                    helper.assertTrue(left < CHARGE * 0.7D, "The short barely drained the battery: " + left);
                })
                .thenSucceed();
    }

    /** Plain wire into water electrocutes a pig swimming in it. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 200, batch = "hazard_wet")
    public static void wetWireElectrocutes(GameTestHelper helper) {
        RadiocraftServerConfig.WIRE_HAZARDS_ENABLED.set(true);
        BlockPos pool = new BlockPos(4, 2, 1);
        helper.setBlock(BATTERY, RadiocraftBlocks.LARGE_BATTERY.get());
        for(BlockPos glass : List.of(pool.east(), pool.north(), pool.south(), pool.east().above(), pool.north().above(), pool.south().above()))
            helper.setBlock(glass, Blocks.GLASS);
        helper.setBlock(pool, Blocks.WATER);
        Pig pig = helper.spawn(EntityType.PIG, pool);
        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> {
                    battery(helper, BATTERY).getStorage().setEnergy(CHARGE);
                    wire(helper, new BlockPos(2, 2, 1), new BlockPos(3, 2, 1));
                })
                .thenWaitUntil(() -> helper.assertTrue(pig.getHealth() < pig.getMaxHealth(), "The pig in the water was not hurt; fault: "
                        + WireFaults.faultAt(helper.getLevel(), helper.absolutePos(new BlockPos(3, 2, 1)))))
                .thenExecute(() -> {
                    int left = battery(helper, BATTERY).getStorage().getEnergyStored();
                    RadiocraftServerConfig.WIRE_HAZARDS_ENABLED.set(false);
                    helper.assertTrue(left < CHARGE, "The wet wire did not drain the battery");
                })
                .thenSucceed();
    }

    /** In the rain, an open wire end sparks and zaps a pig standing on it, drawing hard from the battery. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 300, batch = "hazard_spark")
    public static void openEndSparksInRain(GameTestHelper helper) {
        RadiocraftServerConfig.WIRE_HAZARDS_ENABLED.set(true);
        BlockPos battery = BATTERY.above(UP), end = new BlockPos(3, 2 + UP, 1);
        helper.getLevel().setWeatherParameters(0, 6000, true, false);
        floor(helper, battery, battery.east(), end);
        helper.setBlock(battery, RadiocraftBlocks.LARGE_BATTERY.get());
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(helper.getLevel().isRainingAt(helper.absolutePos(end)), "Not raining yet"))
                .thenExecute(() -> {
                    battery(helper, battery).getStorage().setEnergy(CHARGE);
                    wire(helper, battery.east(), end);
                    helper.assertTrue(WireFaults.faultAt(helper.getLevel(), helper.absolutePos(end)) == WireFaults.Kind.SPARK, "The open end is not sparking: "
                            + WireFaults.faultAt(helper.getLevel(), helper.absolutePos(end)));
                    helper.spawn(EntityType.PIG, end);
                })
                .thenWaitUntil(() -> helper.assertTrue(helper.getLevel().getEntitiesOfClass(Pig.class, new net.minecraft.world.phys.AABB(helper.absolutePos(end)).inflate(1.0D))
                        .stream().anyMatch(pig -> pig.getHealth() < pig.getMaxHealth()), "The pig on the sparking end was not zapped"))
                .thenExecute(() -> {
                    int left = battery(helper, battery).getStorage().getEnergyStored();
                    helper.getLevel().setWeatherParameters(6000, 0, false, false);
                    RadiocraftServerConfig.WIRE_HAZARDS_ENABLED.set(false);
                    helper.assertTrue(CHARGE - left > WireFaults.sparkDraw() * 20, "The spark and zap drew too little: " + (CHARGE - left));
                })
                .thenSucceed();
    }

    /** Coax from a VHF base station to a lightning rod: a poor (SWR 6) antenna. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 100)
    public static void lightningRodIsAnAntenna(GameTestHelper helper) {
        BlockPos radio = new BlockPos(4, 2, 4), rod = radio.south(2);
        helper.setBlock(radio, RadiocraftBlocks.VHF_BASE_STATION.get());
        GameTestUtil.placeWire(helper, radio.south(), RadiocraftBlocks.COAX_WIRE.get());
        helper.setBlock(rod, Blocks.LIGHTNING_ROD);
        helper.succeedWhen(() -> {
            BENetworkObject object = BENetworksCapability.get(helper.getLevel()).getObject(helper.absolutePos(rod));
            helper.assertTrue(object instanceof AntennaNetworkObject antenna && antenna.getAntenna() != null
                    && antenna.getAntenna().type instanceof LightningRodAntennaType, "The lightning rod is not an antenna: " + object);
            AntennaNetworkObject antenna = (AntennaNetworkObject)object;
            helper.assertTrue(antenna.getRadios().size() == 1, "The rod is not on the radio's coax");
            double swr = antenna.getSWR(Band.getBand("2m").minFrequency());
            helper.assertTrue(swr == LightningRodAntennaType.VHF_SWR, "2m SWR was " + swr);
        });
    }

    /** With the chance at 1, a strike near an antenna in the rain lands on the antenna. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 300, batch = "antenna_lightning")
    public static void antennasDrawLightning(GameTestHelper helper) {
        double chance = RadiocraftServerConfig.ANTENNA_LIGHTNING_CHANCE.get();
        RadiocraftServerConfig.ANTENNA_LIGHTNING_CHANCE.set(1.0D);
        BlockPos radio = new BlockPos(4, 2 + UP, 4), rod = radio.south(2);
        helper.getLevel().setWeatherParameters(0, 6000, true, false);
        floor(helper, radio, radio.south(), rod);
        helper.setBlock(radio, RadiocraftBlocks.VHF_BASE_STATION.get());
        GameTestUtil.placeWire(helper, radio.south(), RadiocraftBlocks.COAX_WIRE.get());
        helper.setBlock(rod, Blocks.LIGHTNING_ROD);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(helper.getLevel().isRainingAt(helper.absolutePos(rod).above()), "Not raining yet"))
                .thenExecute(() -> {
                    BlockPos target = helper.absolutePos(new BlockPos(12, 2, 12));
                    BlockPos landed = LightningRodAntennas.redirect(helper.getLevel(), target, helper.getLevel().random);
                    helper.getLevel().setWeatherParameters(6000, 0, false, false);
                    RadiocraftServerConfig.ANTENNA_LIGHTNING_CHANCE.set(chance);
                    helper.assertTrue(landed.equals(helper.absolutePos(rod).above()), "The strike landed at " + landed + ", not on the antenna");
                })
                .thenSucceed();
    }

    /** A strike on a J-pole blows the radio on its coax, burns the coax at the antenna, and (chance 1 here) lights the battery. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 200, batch = "antenna_strike")
    public static void strikeOnAntennaBlowsRadio(GameTestHelper helper) {
        double ignite = RadiocraftServerConfig.LIGHTNING_BATTERY_IGNITE_CHANCE.get();
        RadiocraftServerConfig.LIGHTNING_BATTERY_IGNITE_CHANCE.set(1.0D);
        RadiocraftServerConfig.BATTERY_FIRES_ENABLED.set(true);
        BlockPos radio = new BlockPos(4, 2, 4), coax = radio.south(), jPole = radio.south(2), battery = radio.north(2);
        GameTestUtil.radioWithJPole(helper, radio);
        helper.setBlock(battery, RadiocraftBlocks.LARGE_BATTERY.get());
        wire(helper, radio.north());
        helper.startSequence()
                .thenWaitUntil(() -> {
                    AntennaNetworkObject antenna = GameTestUtil.networkObject(helper, jPole, AntennaNetworkObject.class);
                    helper.assertTrue(antenna.getAntenna() != null && antenna.getRadios().size() == 1, "The J-pole is not on the radio yet");
                    helper.assertTrue(GameTestUtil.networkObject(helper, battery, BatteryNetworkObject.class).getNetworks().values().stream()
                            .anyMatch(n -> n.getNetworkObjects().size() > 1), "The battery is not wired to the radio yet");
                })
                .thenExecute(() -> {
                    GameTestUtil.networkObject(helper, battery, BatteryNetworkObject.class).getStorage().setEnergy(CHARGE); // An empty one would not catch.
                    AntennaStrikes.strike(helper.getLevel(), net.minecraft.world.phys.Vec3.atBottomCenterOf(helper.absolutePos(jPole.above(2))));
                    RadiocraftServerConfig.LIGHTNING_BATTERY_IGNITE_CHANCE.set(ignite);
                    helper.assertTrue(helper.getBlockState(radio).getValue(RadioBlock.BLOWN), "The radio did not blow");
                    helper.assertFalse(helper.getBlockState(coax).is(RadiocraftBlocks.COAX_WIRE.get()), "The coax at the antenna did not burn out");
                    helper.assertTrue(BatteryFires.isBurning(helper.getLevel(), helper.absolutePos(battery)), "The battery did not catch");
                    RadiocraftServerConfig.BATTERY_FIRES_ENABLED.set(false);
                })
                .thenSucceed();
    }

    /** A lone battery set alight burns away; water on it holds the flames but it is consumed all the same. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 200, batch = "battery_fire")
    public static void batteryBurnsAwayEvenUnderWater(GameTestHelper helper) {
        int seconds = RadiocraftServerConfig.BATTERY_BURN_SECONDS.get();
        RadiocraftServerConfig.BATTERY_FIRES_ENABLED.set(true);
        RadiocraftServerConfig.BATTERY_BURN_SECONDS.set(2);
        BlockPos dry = new BlockPos(1, 2, 1), wet = new BlockPos(8, 2, 8);
        helper.setBlock(dry, RadiocraftBlocks.LARGE_BATTERY.get());
        helper.setBlock(wet, RadiocraftBlocks.LARGE_BATTERY.get());
        helper.setBlock(wet.above(), Blocks.WATER);
        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> {
                    helper.assertTrue(BatteryFires.ignite(helper.getLevel(), helper.absolutePos(dry)), "The dry battery did not catch");
                    helper.assertTrue(BatteryFires.ignite(helper.getLevel(), helper.absolutePos(wet)), "The wet battery did not catch");
                })
                .thenIdle(20)
                .thenExecute(() -> helper.assertTrue(BatteryFires.isBurning(helper.getLevel(), helper.absolutePos(wet)), "Water put the battery out"))
                .thenWaitUntil(() -> {
                    helper.assertFalse(helper.getBlockState(dry).is(RadiocraftBlocks.LARGE_BATTERY.get()), "The dry battery is not consumed yet");
                    helper.assertFalse(helper.getBlockState(wet).is(RadiocraftBlocks.LARGE_BATTERY.get()), "The wet battery is not consumed yet");
                })
                .thenExecute(() -> {
                    RadiocraftServerConfig.BATTERY_BURN_SECONDS.set(seconds);
                    RadiocraftServerConfig.BATTERY_FIRES_ENABLED.set(false);
                })
                .thenSucceed();
    }

    /**
     * An empty battery bank does nothing: a short on it with a radio on the run (whose own buffer is not charge) does
     * not burn through, and lightning on its wire does not light it.
     */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 200, batch = "hazard_empty")
    public static void emptyBatteryDoesNothing(GameTestHelper helper) {
        RadiocraftServerConfig.WIRE_HAZARDS_ENABLED.set(true);
        RadiocraftServerConfig.BATTERY_FIRES_ENABLED.set(true);
        BlockPos closing = new BlockPos(3, 2, 2), radio = new BlockPos(1, 2, 2);
        helper.setBlock(BATTERY, RadiocraftBlocks.LARGE_BATTERY.get());
        helper.setBlock(radio, RadiocraftBlocks.VHF_BASE_STATION.get());
        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> {
                    battery(helper, BATTERY).getStorage().setEnergy(0);
                    wire(helper, new BlockPos(2, 2, 1), new BlockPos(3, 2, 1), new BlockPos(2, 2, 2), closing);
                    helper.assertTrue(WireFaults.faultAt(helper.getLevel(), helper.absolutePos(closing)) == WireFaults.Kind.SHORT, "No short found");
                })
                .thenIdle(RadiocraftServerConfig.WIRE_SHORT_BURN_SECONDS.get() * 20 + 40)
                .thenExecute(() -> {
                    helper.assertTrue(helper.getBlockState(closing).is(RadiocraftBlocks.WIRE.get()), "A short on an empty battery burned through");
                    LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(helper.getLevel());
                    bolt.moveTo(net.minecraft.world.phys.Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(3, 3, 1))));
                    bolt.setVisualOnly(true);
                    helper.getLevel().addFreshEntity(bolt);
                    boolean burning = BatteryFires.isBurning(helper.getLevel(), helper.absolutePos(BATTERY));
                    RadiocraftServerConfig.WIRE_HAZARDS_ENABLED.set(false);
                    RadiocraftServerConfig.BATTERY_FIRES_ENABLED.set(false);
                    helper.assertFalse(burning, "Lightning lit an empty battery");
                })
                .thenSucceed();
    }

    /** Lightning on power wire sets the battery that wire runs to alight; a big enough group then explodes. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 300, batch = "battery_lightning")
    public static void lightningOnWireIgnitesBatteryGroup(GameTestHelper helper) {
        int seconds = RadiocraftServerConfig.BATTERY_BURN_SECONDS.get();
        double power = RadiocraftServerConfig.BATTERY_EXPLOSION_POWER_PER_BATTERY.get();
        RadiocraftServerConfig.BATTERY_FIRES_ENABLED.set(true);
        RadiocraftServerConfig.BATTERY_BURN_SECONDS.set(2);
        RadiocraftServerConfig.BATTERY_EXPLOSION_POWER_PER_BATTERY.set(0.5D); // Small: keep the test plot together.
        List<BlockPos> group = List.of(new BlockPos(4, 2, 4), new BlockPos(5, 2, 4), new BlockPos(4, 2, 5), new BlockPos(5, 2, 5));
        for(BlockPos pos : group)
            helper.setBlock(pos, RadiocraftBlocks.LARGE_BATTERY.get());
        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> {
                    wire(helper, new BlockPos(3, 2, 4), new BlockPos(2, 2, 4), new BlockPos(1, 2, 4));
                    helper.assertTrue(BatteryFires.groupSize(helper.getLevel(), helper.absolutePos(group.getFirst())) == 4, "The batteries are not one group");
                    for(BlockPos pos : group)
                        battery(helper, pos).getStorage().setEnergy(CHARGE);
                    LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(helper.getLevel());
                    bolt.moveTo(net.minecraft.world.phys.Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 3, 4))));
                    bolt.setVisualOnly(true);
                    helper.getLevel().addFreshEntity(bolt);
                    helper.assertTrue(BatteryFires.isBurning(helper.getLevel(), helper.absolutePos(group.getFirst())), "Lightning on the wire did not set the battery alight");
                })
                .thenWaitUntil(() -> {
                    for(BlockPos pos : group)
                        helper.assertFalse(helper.getBlockState(pos).is(RadiocraftBlocks.LARGE_BATTERY.get()), "A battery of the group is still there");
                })
                .thenExecute(() -> {
                    RadiocraftServerConfig.BATTERY_BURN_SECONDS.set(seconds);
                    RadiocraftServerConfig.BATTERY_EXPLOSION_POWER_PER_BATTERY.set(power);
                    RadiocraftServerConfig.BATTERY_FIRES_ENABLED.set(false);
                })
                .thenSucceed();
    }
}
