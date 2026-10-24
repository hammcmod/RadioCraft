package com.arrl.radiocraft.gametest;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.RadiocraftServerConfig;
import com.arrl.radiocraft.api.benetworks.BENetwork;
import com.arrl.radiocraft.api.benetworks.BENetworkObject;
import com.arrl.radiocraft.common.be_networks.network_objects.AntennaNetworkObject;
import com.arrl.radiocraft.common.be_networks.network_objects.BatteryNetworkObject;
import com.arrl.radiocraft.common.be_networks.network_objects.RadioNetworkObject;
import com.arrl.radiocraft.common.blockentities.radio.RadioBlockEntity;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import com.arrl.radiocraft.common.blocks.antennas.DoubleVHFAntennaBlock;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The builds the guide book shows, built block for block as its pages draw them, so a page cannot show a station that
 * does not work. "Your First Station" (getting_started/first_station.json): three solar panels, a charge controller,
 * wire, a large battery, wire, the VHF base station, two coax and a J-pole. In the page's pattern each string is a
 * row along X and each character a step along Z; here row {@code i}, character {@code j} is block (2 + i, 2, 2 + j).
 */
@GameTestHolder(Radiocraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class GuideBuildGameTests {

    private static BlockPos at(int row, int column) {
        return new BlockPos(2 + row, 2, 2 + column);
    }

    private static final BlockPos CONTROLLER = at(3, 0), BATTERY = at(5, 0), RADIO = at(7, 0), J_POLE = at(7, 3);

    private static boolean sharePowerNetwork(BENetworkObject a, BENetworkObject b) {
        for(BENetwork network : a.getNetworks().values())
            if(network.getNetworkObjects().contains(b))
                return true;
        return false;
    }

    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 1200, batch = "guide_first_station")
    public static void firstStationFromTheGuideWorks(GameTestHelper helper) {
        RadiocraftServerConfig.RADIO_POWER_REQUIRED.set(true);
        helper.getLevel().setDayTime(6000); // Noon, for the panels.
        for(int row = 0; row < 3; row++)
            helper.setBlock(at(row, 0), RadiocraftBlocks.SOLAR_PANEL.get());
        helper.setBlock(CONTROLLER, RadiocraftBlocks.CHARGE_CONTROLLER.get().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
        GameTestUtil.placeWire(helper, at(4, 0), RadiocraftBlocks.WIRE.get());
        helper.setBlock(BATTERY, RadiocraftBlocks.LARGE_BATTERY.get().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
        GameTestUtil.placeWire(helper, at(6, 0), RadiocraftBlocks.WIRE.get());
        helper.setBlock(RADIO, RadiocraftBlocks.VHF_BASE_STATION.get().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
        GameTestUtil.placeWire(helper, at(7, 1), RadiocraftBlocks.COAX_WIRE.get());
        GameTestUtil.placeWire(helper, at(7, 2), RadiocraftBlocks.COAX_WIRE.get());
        helper.setBlock(J_POLE, RadiocraftBlocks.J_POLE_ANTENNA.get().defaultBlockState().setValue(DoubleVHFAntennaBlock.HALF, DoubleBlockHalf.LOWER));
        helper.setBlock(J_POLE.above(), RadiocraftBlocks.J_POLE_ANTENNA.get().defaultBlockState().setValue(DoubleVHFAntennaBlock.HALF, DoubleBlockHalf.UPPER));
        helper.startSequence()
                .thenWaitUntil(() -> {
                    RadioNetworkObject radio = GameTestUtil.networkObject(helper, RADIO, RadioNetworkObject.class);
                    BENetworkObject controller = GameTestUtil.networkObject(helper, CONTROLLER, BENetworkObject.class);
                    BatteryNetworkObject battery = GameTestUtil.networkObject(helper, BATTERY, BatteryNetworkObject.class);
                    helper.assertTrue(sharePowerNetwork(controller, battery), "The controller is not wired to the battery");
                    helper.assertTrue(sharePowerNetwork(battery, radio), "The battery is not wired to the radio");
                    helper.assertTrue(radio.getAntennas().size() == 1 && radio.getAntennas().getFirst().getAntenna() != null, "The radio does not see its J-pole yet");
                    helper.assertTrue(GameTestUtil.networkObject(helper, J_POLE, AntennaNetworkObject.class).getAntenna() != null, "The J-pole is not recognized yet");
                })
                .thenWaitUntil(() -> helper.assertTrue(GameTestUtil.networkObject(helper, BATTERY, BatteryNetworkObject.class).getStorage().getEnergyStored() > 0,
                        "The panels are not charging the battery"))
                .thenExecute(() -> GameTestUtil.blockEntity(helper, RADIO, RadioBlockEntity.class).toggle())
                .thenWaitUntil(() -> helper.assertTrue(GameTestUtil.networkObject(helper, RADIO, RadioNetworkObject.class).isPowered, "The radio does not run on the solar power"))
                .thenExecute(() -> {
                    double swr = GameTestUtil.networkObject(helper, RADIO, RadioNetworkObject.class).getAntennas().getFirst().getSWR(146_000_000.0F);
                    helper.assertTrue(swr == 1.0D, "The J-pole is not matched on 2m: SWR " + swr);
                })
                .thenExecute(() -> RadiocraftServerConfig.RADIO_POWER_REQUIRED.set(false))
                .thenSucceed();
    }
}
