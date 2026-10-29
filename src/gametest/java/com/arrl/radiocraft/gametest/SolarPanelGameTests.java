package com.arrl.radiocraft.gametest;

import com.arrl.radiocraft.common.blockentities.ChargeControllerBlockEntity;
import com.arrl.radiocraft.common.blocks.power.ChargeControllerBlock;
import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.api.benetworks.BENetwork;
import com.arrl.radiocraft.api.benetworks.BENetworkObject;
import com.arrl.radiocraft.api.benetworks.PowerBENetwork;
import com.arrl.radiocraft.common.be_networks.network_objects.BatteryNetworkObject;
import com.arrl.radiocraft.common.be_networks.network_objects.ChargeControllerNetworkObject;
import com.arrl.radiocraft.common.be_networks.network_objects.SolarPanelNetworkObject;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/**
 * Solar panels touching each other, or a charge controller, join without wire: a row of panels is one network with
 * the controller at its end. Batteries still need wire. Own batch because it sets the time of day.
 */
@GameTestHolder(Radiocraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class SolarPanelGameTests {

    private static final BlockPos PANEL_A = new BlockPos(6, 2, 8);
    private static final BlockPos PANEL_B = new BlockPos(7, 2, 8);
    private static final BlockPos PANEL_C = new BlockPos(8, 2, 8);
    private static final BlockPos CONTROLLER = new BlockPos(9, 2, 8);
    private static final BlockPos WIRE = new BlockPos(9, 2, 9);
    private static final BlockPos BATTERY = new BlockPos(9, 2, 10);
    private static final BlockPos LOOSE_BATTERY = new BlockPos(10, 2, 8); // Touching the controller, no wire: must not join.

    private static boolean sharePowerNetwork(BENetworkObject a, BENetworkObject b) {
        for(BENetwork network : a.getNetworks().values())
            if(network instanceof PowerBENetwork && network.getNetworkObjects().contains(b))
                return true;
        return false;
    }

    /** The charge controller's switch: off, the panels' power stops at the controller; on again, the battery charges. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 400, batch = "solar")
    public static void chargeControllerSwitchStopsCharging(GameTestHelper helper) {
        helper.getLevel().setDayTime(6000); // Noon.
        helper.setBlock(CONTROLLER, RadiocraftBlocks.CHARGE_CONTROLLER.get());
        helper.setBlock(PANEL_C, RadiocraftBlocks.SOLAR_PANEL.get());
        helper.setBlock(BATTERY, RadiocraftBlocks.LARGE_BATTERY.get());
        GameTestUtil.placeWire(helper, WIRE, RadiocraftBlocks.WIRE.get());
        int[] charge = new int[1];
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(battery(helper).getStorage().getEnergyStored() > 0, "A new controller does not charge the battery"))
                .thenExecute(() -> {
                    helper.assertTrue(helper.getBlockState(CONTROLLER).getValue(ChargeControllerBlock.POWERED), "A new controller is switched off");
                    GameTestUtil.blockEntity(helper, CONTROLLER, ChargeControllerBlockEntity.class).toggle();
                })
                .thenIdle(2)
                .thenExecute(() -> charge[0] = battery(helper).getStorage().getEnergyStored())
                .thenIdle(20)
                .thenExecute(() -> {
                    int now = battery(helper).getStorage().getEnergyStored();
                    helper.assertTrue(now == charge[0], "Switched off, the battery still charged from " + charge[0] + " to " + now);
                    GameTestUtil.blockEntity(helper, CONTROLLER, ChargeControllerBlockEntity.class).toggle();
                })
                .thenWaitUntil(() -> helper.assertTrue(battery(helper).getStorage().getEnergyStored() > charge[0], "Switched back on, the battery does not charge"))
                .thenSucceed();
    }

    private static BatteryNetworkObject battery(GameTestHelper helper) {
        return GameTestUtil.networkObject(helper, BATTERY, BatteryNetworkObject.class);
    }

    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 400, batch = "solar")
    public static void panelsJoinEachOtherAndTheControllerWithoutWire(GameTestHelper helper) {
        helper.getLevel().setDayTime(6000); // Noon.
        helper.setBlock(CONTROLLER, RadiocraftBlocks.CHARGE_CONTROLLER.get());
        helper.setBlock(PANEL_C, RadiocraftBlocks.SOLAR_PANEL.get());
        helper.setBlock(PANEL_B, RadiocraftBlocks.SOLAR_PANEL.get());
        helper.setBlock(PANEL_A, RadiocraftBlocks.SOLAR_PANEL.get());
        helper.setBlock(BATTERY, RadiocraftBlocks.LARGE_BATTERY.get());
        helper.setBlock(LOOSE_BATTERY, RadiocraftBlocks.LARGE_BATTERY.get());
        GameTestUtil.placeWire(helper, WIRE, RadiocraftBlocks.WIRE.get());

        helper.startSequence()
                .thenIdle(5)
                .thenExecute(() -> {
                    SolarPanelNetworkObject a = GameTestUtil.networkObject(helper, PANEL_A, SolarPanelNetworkObject.class);
                    SolarPanelNetworkObject b = GameTestUtil.networkObject(helper, PANEL_B, SolarPanelNetworkObject.class);
                    SolarPanelNetworkObject c = GameTestUtil.networkObject(helper, PANEL_C, SolarPanelNetworkObject.class);
                    ChargeControllerNetworkObject controller = GameTestUtil.networkObject(helper, CONTROLLER, ChargeControllerNetworkObject.class);
                    BatteryNetworkObject battery = GameTestUtil.networkObject(helper, BATTERY, BatteryNetworkObject.class);
                    BatteryNetworkObject loose = GameTestUtil.networkObject(helper, LOOSE_BATTERY, BatteryNetworkObject.class);
                    helper.assertTrue(sharePowerNetwork(a, b), "Touching panels should share a network");
                    helper.assertTrue(sharePowerNetwork(a, controller), "The far panel should reach the controller through the row");
                    helper.assertTrue(sharePowerNetwork(c, controller), "The panel touching the controller should be on its network");
                    helper.assertTrue(sharePowerNetwork(controller, battery), "Controller and battery are wired together");
                    helper.assertTrue(!sharePowerNetwork(controller, loose), "A battery merely touching the controller must not join");
                })
                .thenExecuteAfter(40, () -> {
                    SolarPanelNetworkObject a = GameTestUtil.networkObject(helper, PANEL_A, SolarPanelNetworkObject.class);
                    helper.assertTrue(a.getLastPowerTick() > 0, "Panel should make power at noon under the sky");
                    int charge = GameTestUtil.networkObject(helper, BATTERY, BatteryNetworkObject.class).getStorage().getEnergyStored();
                    helper.assertTrue(charge > 0, "The wired battery should be charging, has " + charge);
                    int looseCharge = GameTestUtil.networkObject(helper, LOOSE_BATTERY, BatteryNetworkObject.class).getStorage().getEnergyStored();
                    helper.assertTrue(looseCharge == 0, "The loose battery must stay empty, has " + looseCharge);
                    helper.succeed();
                });
    }

    /**
     * Two charge controllers on one row of panels, both wired to the same battery: they split the panels' power
     * between them and nothing goes round in a loop (the battery gains no more than the panels make).
     */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 400, batch = "solar")
    public static void twoControllersShareOneRow(GameTestHelper helper) {
        helper.getLevel().setDayTime(6000); // Noon.
        BlockPos west = PANEL_A.west(), battery = new BlockPos(7, 2, 10);
        helper.setBlock(CONTROLLER, RadiocraftBlocks.CHARGE_CONTROLLER.get());
        helper.setBlock(west, RadiocraftBlocks.CHARGE_CONTROLLER.get());
        for(BlockPos panel : List.of(PANEL_A, PANEL_B, PANEL_C))
            helper.setBlock(panel, RadiocraftBlocks.SOLAR_PANEL.get());
        helper.setBlock(battery, RadiocraftBlocks.LARGE_BATTERY.get());
        for(BlockPos pos : List.of(CONTROLLER.south(), CONTROLLER.south(2), battery.east(), west.south(), west.south(2), battery.west()))
            GameTestUtil.placeWire(helper, pos, RadiocraftBlocks.WIRE.get());
        int[] start = new int[1];
        int span = 40;
        helper.startSequence()
                .thenIdle(10)
                .thenExecute(() -> start[0] = GameTestUtil.networkObject(helper, battery, BatteryNetworkObject.class).getStorage().getEnergyStored())
                .thenIdle(span)
                .thenExecute(() -> {
                    ChargeControllerNetworkObject a = GameTestUtil.networkObject(helper, CONTROLLER, ChargeControllerNetworkObject.class);
                    ChargeControllerNetworkObject b = GameTestUtil.networkObject(helper, west, ChargeControllerNetworkObject.class);
                    helper.assertTrue(a.getLastPowerTick() > 0 && b.getLastPowerTick() > 0,
                            "Both controllers should pass power on: " + a.getLastPowerTick() + " and " + b.getLastPowerTick());
                    int made = 0;
                    for(BlockPos panel : List.of(PANEL_A, PANEL_B, PANEL_C))
                        made += GameTestUtil.networkObject(helper, panel, SolarPanelNetworkObject.class).getLastPowerTick();
                    int gained = GameTestUtil.networkObject(helper, battery, BatteryNetworkObject.class).getStorage().getEnergyStored() - start[0];
                    helper.assertTrue(gained > 0 && gained <= made * (span + 2), "The battery gained " + gained + " in " + span + " ticks from panels making " + made + " a tick");
                })
                .thenSucceed();
    }
}
