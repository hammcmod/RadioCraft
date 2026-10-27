package com.arrl.radiocraft.gametest;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.api.benetworks.BENetwork;
import com.arrl.radiocraft.api.benetworks.PowerBENetwork;
import com.arrl.radiocraft.common.be_networks.network_objects.BatteryNetworkObject;
import com.arrl.radiocraft.common.be_networks.network_objects.RadioNetworkObject;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Batteries on one wire charge and discharge together, an even share each, as parallel batteries do. */
@GameTestHolder(Radiocraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class BatterySharingGameTests {

    private static final BlockPos BATTERY_A = new BlockPos(2, 2, 4), WIRE = new BlockPos(3, 2, 4), BATTERY_B = new BlockPos(4, 2, 4);
    private static final BlockPos RADIO = new BlockPos(3, 2, 5);

    private static BatteryNetworkObject battery(GameTestHelper helper, BlockPos pos) {
        return GameTestUtil.networkObject(helper, pos, BatteryNetworkObject.class);
    }

    private static PowerBENetwork wire(GameTestHelper helper) {
        for(BENetwork network : battery(helper, BATTERY_A).getNetworks().values())
            if(network instanceof PowerBENetwork power)
                return power;
        throw new AssertionError("The battery is on no power wire");
    }

    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 60)
    public static void batteriesShareChargeAndDischargeEvenly(GameTestHelper helper) {
        helper.setBlock(BATTERY_A, RadiocraftBlocks.LARGE_BATTERY.get());
        helper.setBlock(BATTERY_B, RadiocraftBlocks.LARGE_BATTERY.get());
        helper.setBlock(RADIO, RadiocraftBlocks.HF_RADIO_20M.get());
        GameTestUtil.placeWire(helper, WIRE, RadiocraftBlocks.WIRE.get());
        helper.startSequence()
                .thenIdle(3)
                .thenExecute(() -> {
                    BatteryNetworkObject a = battery(helper, BATTERY_A), b = battery(helper, BATTERY_B);
                    helper.assertTrue(b.getNetworks().containsValue(wire(helper)), "The batteries are not on one wire");
                    // Charging: a controller's power is split between them, not poured into one first.
                    a.getStorage().setEnergy(0);
                    b.getStorage().setEnergy(0);
                    int pushed = wire(helper).pushPower(100, false, true, false);
                    helper.assertTrue(pushed == 100, "Pushed " + pushed + " of 100");
                    helper.assertTrue(a.getStorage().getEnergyStored() == 50 && b.getStorage().getEnergyStored() == 50,
                            "Charged " + a.getStorage().getEnergyStored() + " and " + b.getStorage().getEnergyStored() + ", expected 50 each");
                    // Discharging: the radio's draw comes from both.
                    a.getStorage().setEnergy(10_000);
                    b.getStorage().setEnergy(10_000);
                    GameTestUtil.networkObject(helper, RADIO, RadioNetworkObject.class).getStorage().setEnergy(0);
                    int drawn = wire(helper).dischargeBatteries(Long.MAX_VALUE);
                    int fromA = 10_000 - a.getStorage().getEnergyStored(), fromB = 10_000 - b.getStorage().getEnergyStored();
                    helper.assertTrue(drawn > 1 && fromA + fromB == drawn, "Drew " + drawn + ": " + fromA + " from one, " + fromB + " from the other");
                    helper.assertTrue(Math.abs(fromA - fromB) <= 1, "Uneven discharge: " + fromA + " and " + fromB);
                })
                .thenSucceed();
    }
}
