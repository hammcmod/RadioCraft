package com.arrl.radiocraft.gametest;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.be_networks.network_objects.BatteryNetworkObject;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.radio.solar.SolarEvent.SolarEventInstance;
import com.arrl.radiocraft.common.radio.solar.SolarEventManager;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * World-level systems: solar weather and the solar power network.
 */
@GameTestHolder(Radiocraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class WorldSystemsGameTests {

    @GameTest(template = GameTestUtil.EMPTY)
    public static void solarWeatherEventIsActive(GameTestHelper helper) {
        helper.succeedWhen(() -> {
            SolarEventInstance event = SolarEventManager.getEvent(helper.getLevel());
            helper.assertTrue(event != null, "No solar event for this level");
            helper.assertTrue(event.getEvent().getNoise() >= 0, "Negative noise floor");
        });
    }

    /**
     * Solar panel (open sky, daytime) -> wire -> charge controller -> wire -> large battery. Panels only feed
     * "direct" consumers (the controller), and the controller feeds batteries, so the controller is required.
     */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 200)
    public static void solarPanelChargesBatteryInDaylight(GameTestHelper helper) {
        helper.getLevel().setDayTime(6000);
        helper.getLevel().setWeatherParameters(6000, 0, false, false);
        BlockPos panel = new BlockPos(8, 1, 6);
        BlockPos controller = new BlockPos(8, 1, 8);
        BlockPos battery = new BlockPos(8, 1, 10);
        helper.setBlock(panel, RadiocraftBlocks.SOLAR_PANEL.get());
        helper.setBlock(controller, RadiocraftBlocks.CHARGE_CONTROLLER.get());
        helper.setBlock(battery, RadiocraftBlocks.LARGE_BATTERY.get());

        GameTestUtil.placeWire(helper, panel.south(), RadiocraftBlocks.WIRE.get());
        GameTestUtil.placeWire(helper, controller.south(), RadiocraftBlocks.WIRE.get());

        helper.succeedWhen(() -> {
            BatteryNetworkObject net = GameTestUtil.networkObject(helper, battery, BatteryNetworkObject.class);
            helper.assertTrue(net.getStorage().getEnergyStored() > 0, "Battery is still empty");
        });
    }

    /**
     * The same chain at night: nothing should charge. Runs in its own batch because the day time is a property of the
     * whole level: tests in one batch run at the same time, and this one would turn the lights off on the daylight test.
     */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 60, batch = "night")
    public static void solarPanelDoesNotChargeAtNight(GameTestHelper helper) {
        helper.getLevel().setDayTime(18000);
        BlockPos panel = new BlockPos(8, 1, 6);
        BlockPos controller = new BlockPos(8, 1, 8);
        BlockPos battery = new BlockPos(8, 1, 10);
        helper.setBlock(panel, RadiocraftBlocks.SOLAR_PANEL.get());
        helper.setBlock(controller, RadiocraftBlocks.CHARGE_CONTROLLER.get());
        helper.setBlock(battery, RadiocraftBlocks.LARGE_BATTERY.get());
        GameTestUtil.placeWire(helper, panel.south(), RadiocraftBlocks.WIRE.get());
        GameTestUtil.placeWire(helper, controller.south(), RadiocraftBlocks.WIRE.get());

        helper.runAfterDelay(40, () -> {
            BatteryNetworkObject net = GameTestUtil.networkObject(helper, battery, BatteryNetworkObject.class);
            helper.assertTrue(net.getStorage().getEnergyStored() == 0, "Battery charged at night: " + net.getStorage().getEnergyStored());
            helper.succeed();
        });
    }
}
