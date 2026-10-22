package com.arrl.radiocraft.gametest;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.RadiocraftServerConfig;
import com.arrl.radiocraft.common.be_networks.network_objects.BatteryNetworkObject;
import com.arrl.radiocraft.common.be_networks.network_objects.RadioNetworkObject;
import com.arrl.radiocraft.api.capabilities.IBENetworks;
import com.arrl.radiocraft.common.blockentities.LargeBatteryBlockEntity;
import com.arrl.radiocraft.common.blockentities.radio.HFRadioBlockEntity;
import com.arrl.radiocraft.common.blockentities.radio.RadioBlockEntity;
import com.arrl.radiocraft.common.capabilities.BENetworksCapability;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/**
 * Block radios drawing power. One sequential test, in its own batch, because it turns {@code radios_need_power} on
 * (every other test runs with it off, see {@link GameTestDefaults}) and puts it back at the end.
 */
@GameTestHolder(Radiocraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class PowerGameTests {

    private static final BlockPos RADIO = new BlockPos(8, 2, 2);
    private static final BlockPos WIRE = new BlockPos(8, 2, 3);
    private static final BlockPos BATTERY = new BlockPos(8, 2, 4);

    private static RadioNetworkObject radio(GameTestHelper helper) {
        return GameTestUtil.networkObject(helper, RADIO, RadioNetworkObject.class);
    }

    private static void assertPhase(GameTestHelper helper, int phase, String when) {
        helper.assertTrue(radio(helper).getDisplayPhase() == phase, "Display phase " + when + " was " + radio(helper).getDisplayPhase() + ", expected " + phase);
    }

    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 600, batch = "power")
    public static void radioBrownsOutWithoutPowerAndRunsOnABattery(GameTestHelper helper) {
        RadiocraftServerConfig.RADIO_POWER_REQUIRED.set(true);
        helper.setBlock(RADIO, RadiocraftBlocks.HF_RADIO_20M.get());
        HFRadioBlockEntity be = GameTestUtil.blockEntity(helper, RADIO, HFRadioBlockEntity.class);
        int receiveUse = radio(helper).getReceiveUse();

        helper.startSequence()
                .thenIdle(2)
                // Switched on with no battery on the wire at all: dark and silent, panel stays dark (no PWR cycle).
                .thenExecute(() -> {
                    be.toggle();
                    RadioNetworkObject net = radio(helper);
                    helper.assertTrue(net.wantsOn, "Switch should be on");
                    helper.assertTrue(!net.isPowered, "No power source, so the radio must not run");
                    helper.assertTrue(net.isBrownedOut(), "Should report a brown-out");
                    helper.assertTrue(!net.hasBattery(), "No battery yet");
                    helper.assertTrue(!be.getVoiceReceiver().isReceiving(), "A browned-out radio hears nothing");
                })
                .thenExecuteAfter(5, () -> assertPhase(helper, RadioNetworkObject.PHASE_OFF, "with no battery, early"))
                .thenExecuteAfter(20, () -> assertPhase(helper, RadioNetworkObject.PHASE_OFF, "with no battery, later"))
                // An empty battery on the wire: now the panel cycles on / PWR / dark. Switch off and on to start the cycle.
                .thenExecute(() -> {
                    helper.setBlock(BATTERY, RadiocraftBlocks.LARGE_BATTERY.get());
                    GameTestUtil.placeWire(helper, WIRE, RadiocraftBlocks.WIRE.get());
                })
                .thenIdle(2)
                .thenExecute(() -> {
                    helper.assertTrue(radio(helper).hasBattery(), "Radio should see the battery on its wire");
                    be.toggle();
                    be.toggle();
                    helper.assertTrue(radio(helper).wantsOn && !radio(helper).isPowered, "Empty battery: switch on, not running");
                })
                .thenExecuteAfter(5, () -> assertPhase(helper, RadioNetworkObject.PHASE_ON, "in the first second"))
                .thenExecuteAfter(20, () -> assertPhase(helper, RadioNetworkObject.PHASE_PWR, "in the second second"))
                .thenExecuteAfter(20, () -> assertPhase(helper, RadioNetworkObject.PHASE_OFF, "in the third second"))
                .thenExecuteAfter(20, () -> assertPhase(helper, RadioNetworkObject.PHASE_ON, "at the start of the next cycle"))
                // Give the battery 30 ticks of receive draw: the radio runs, drains it, then browns out again.
                .thenExecute(() -> GameTestUtil.networkObject(helper, BATTERY, BatteryNetworkObject.class).getStorage().setEnergy(receiveUse * 30))
                .thenWaitUntil(() -> helper.assertTrue(radio(helper).isPowered, "Radio should run once the battery feeds it"))
                .thenExecuteAfter(5, () -> {
                    helper.assertTrue(be.wasPowered(), "Block entity should see the radio powered");
                    assertPhase(helper, RadioNetworkObject.PHASE_ON, "while running");
                    int left = GameTestUtil.networkObject(helper, BATTERY, BatteryNetworkObject.class).getStorage().getEnergyStored();
                    helper.assertTrue(left < receiveUse * 30, "Battery should be draining, has " + left);
                })
                .thenWaitUntil(() -> helper.assertTrue(!radio(helper).isPowered, "Radio should brown out when the battery is drained"))
                .thenExecute(() -> {
                    helper.assertTrue(radio(helper).wantsOn, "The switch stays on through a brown-out");
                    int left = GameTestUtil.networkObject(helper, BATTERY, BatteryNetworkObject.class).getStorage().getEnergyStored();
                    helper.assertTrue(left < receiveUse, "Battery should be empty, has " + left);
                })
                // Charge it again: the next retry brings the radio back.
                .thenExecute(() -> GameTestUtil.networkObject(helper, BATTERY, BatteryNetworkObject.class).getStorage().setEnergy(receiveUse * 200))
                .thenWaitUntil(() -> helper.assertTrue(radio(helper).isPowered, "Radio should come back once there is power again"))
                // The charge survives a reload: the block entity saves it and hands it to the fresh network object.
                .thenExecute(() -> GameTestUtil.networkObject(helper, BATTERY, BatteryNetworkObject.class).getStorage().setEnergy(123_456))
                .thenIdle(3) // The object hands its charge to the block entity on its next tick (the radio draws a little meanwhile).
                .thenExecute(() -> {
                    BatteryNetworkObject before = GameTestUtil.networkObject(helper, BATTERY, BatteryNetworkObject.class);
                    int live = before.getStorage().getEnergyStored();
                    LargeBatteryBlockEntity be2 = GameTestUtil.blockEntity(helper, BATTERY, LargeBatteryBlockEntity.class);
                    CompoundTag saved = be2.saveWithoutMetadata(helper.getLevel().registryAccess());
                    helper.assertTrue(live > 100_000 && Math.abs(saved.getInt("energy") - live) <= receiveUse * 3,
                            "Saved tag should carry the charge: live " + live + ", saved " + saved.getInt("energy"));
                    BlockPos abs = helper.absolutePos(BATTERY);
                    IBENetworks.removeObject(helper.getLevel(), abs); // Simulate the object being gone after a restart.
                    be2.loadWithComponents(saved, helper.getLevel().registryAccess());
                    BatteryNetworkObject after = GameTestUtil.networkObject(helper, BATTERY, BatteryNetworkObject.class);
                    helper.assertTrue(after != before, "A fresh object should have been created");
                    helper.assertTrue(after.getStorage().getEnergyStored() == saved.getInt("energy"),
                            "Fresh object should start with the saved charge, has " + after.getStorage().getEnergyStored());
                })
                .thenExecute(() -> RadiocraftServerConfig.RADIO_POWER_REQUIRED.set(false))
                .thenSucceed();
    }

    /**
     * Every kind of radio runs on a charged battery. The VHF base station passed its draws the wrong way round, so its
     * buffer was smaller than its draw and it browned out on a full battery (maintainer's playtest, 2026-10-21).
     */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 200, batch = "power_every_radio")
    public static void everyRadioRunsOnABattery(GameTestHelper helper) {
        RadiocraftServerConfig.RADIO_POWER_REQUIRED.set(true);
        List<Block> radios = List.of(RadiocraftBlocks.HF_RADIO_10M.get(), RadiocraftBlocks.HF_RADIO_20M.get(), RadiocraftBlocks.HF_RADIO_40M.get(),
                RadiocraftBlocks.HF_RADIO_80M.get(), RadiocraftBlocks.ALL_BAND_RADIO.get(), RadiocraftBlocks.QRP_RADIO_20M.get(),
                RadiocraftBlocks.QRP_RADIO_40M.get(), RadiocraftBlocks.HF_RECEIVER.get(), RadiocraftBlocks.VHF_BASE_STATION.get(),
                RadiocraftBlocks.VHF_RECEIVER.get(), RadiocraftBlocks.VHF_REPEATER.get());
        List<BlockPos> at = new java.util.ArrayList<>();
        for(int i = 0; i < radios.size(); i++) // Two rows, every other block, so nothing touches its neighbour.
            at.add(new BlockPos(1 + (i % 7) * 2, 2, i < 7 ? 1 : 8));
        helper.startSequence()
                .thenExecute(() -> {
                    for(int i = 0; i < radios.size(); i++) {
                        helper.setBlock(at.get(i), radios.get(i));
                        GameTestUtil.placeWire(helper, at.get(i).south(), RadiocraftBlocks.WIRE.get());
                        helper.setBlock(at.get(i).south(2), RadiocraftBlocks.LARGE_BATTERY.get());
                    }
                })
                .thenIdle(2)
                .thenExecute(() -> {
                    for(BlockPos pos : at) {
                        GameTestUtil.networkObject(helper, pos.south(2), BatteryNetworkObject.class).getStorage().setEnergy(1_000_000);
                        GameTestUtil.blockEntity(helper, pos, RadioBlockEntity.class).toggle();
                    }
                })
                // Switched on before the battery has topped its buffer up, a radio browns out once and retries at the
                // start of the next brown-out cycle; one that can never run never comes on.
                .thenWaitUntil(() -> {
                    for(int i = 0; i < radios.size(); i++) {
                        RadioNetworkObject net = GameTestUtil.networkObject(helper, at.get(i), RadioNetworkObject.class);
                        helper.assertTrue(net.isPowered, radios.get(i).getDescriptionId() + " browned out on a full battery (draws "
                                + net.getReceiveUse() + "/" + net.getTransmitUse() + ")");
                        helper.assertTrue(net.getTransmitUse() >= net.getReceiveUse(), radios.get(i).getDescriptionId() + " draws more receiving than transmitting");
                    }
                })
                .thenExecute(() -> RadiocraftServerConfig.RADIO_POWER_REQUIRED.set(false))
                .thenSucceed();
    }

    /** A base station saved with the draws swapped (before the fix) is put right when it loads. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 20, batch = "power_every_radio")
    public static void swappedDrawsAreRepairedOnLoad(GameTestHelper helper) {
        RadioNetworkObject broken = new RadioNetworkObject(helper.getLevel(), helper.absolutePos(RADIO), 63, 188);
        CompoundTag saved = new CompoundTag();
        broken.save(saved);
        RadioNetworkObject loaded = new RadioNetworkObject(helper.getLevel(), helper.absolutePos(RADIO), 0, 0);
        loaded.load(BENetworksCapability.get(helper.getLevel()), saved);
        helper.assertTrue(loaded.getTransmitUse() == 188 && loaded.getReceiveUse() == 63,
                "Loaded draws " + loaded.getReceiveUse() + "/" + loaded.getTransmitUse());
        helper.assertTrue(loaded.getStorage().getMaxEnergyStored() >= 188, "Buffer still holds only " + loaded.getStorage().getMaxEnergyStored());
        helper.succeed();
    }
}
