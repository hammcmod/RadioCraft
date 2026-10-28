package com.arrl.radiocraft.gametest;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.RadiocraftServerConfig;
import com.arrl.radiocraft.common.be_networks.network_objects.BatteryNetworkObject;
import com.arrl.radiocraft.common.blockentities.DirectionFinderBlockEntity;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.AfterBatch;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The direction finder draws power: from a wire to it, or to any module touching it. The GameTest server runs with
 * power draw off ({@link GameTestDefaults}); it is on for this batch, and off again however the tests end.
 */
@GameTestHolder(Radiocraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class DirectionFinderPowerGameTests {

	private static final String BATCH = "power_direction_finder";
	private static final BlockPos FINDER = new BlockPos(5, 1, 5);

	@BeforeBatch(batch = BATCH)
	public static void powerRequired(ServerLevel level) {
		RadiocraftServerConfig.RADIO_POWER_REQUIRED.set(true);
	}

	@AfterBatch(batch = BATCH)
	public static void powerFree(ServerLevel level) {
		RadiocraftServerConfig.RADIO_POWER_REQUIRED.set(false);
	}

	private static void modules(GameTestHelper helper) {
		helper.setBlock(FINDER, RadiocraftBlocks.DIRECTION_FINDER.get());
		for(Direction side : Direction.Plane.HORIZONTAL)
			helper.setBlock(FINDER.relative(side), RadiocraftBlocks.DIRECTION_FINDER_MODULE.get());
	}

	/** A charged battery on a wire to {@code at}, one block further out. */
	private static void power(GameTestHelper helper, BlockPos at, Direction out) {
		GameTestUtil.placeWire(helper, at.relative(out), RadiocraftBlocks.WIRE.get());
		helper.setBlock(at.relative(out, 2), RadiocraftBlocks.LARGE_BATTERY.get());
	}

	private static DirectionFinderBlockEntity finder(GameTestHelper helper) {
		return GameTestUtil.blockEntity(helper, FINDER, DirectionFinderBlockEntity.class);
	}

	/** Switched on with nothing on its wire it stays dark; a battery wired straight to it lights it. */
	@GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 80, batch = BATCH)
	public static void directionFinderNeedsPower(GameTestHelper helper) {
		modules(helper);
		helper.startSequence()
				.thenIdle(2)
				.thenExecute(() -> {
					finder(helper).togglePower();
					helper.assertTrue(finder(helper).isSwitchedOn(), "the switch did not go on");
				})
				.thenIdle(3)
				.thenExecute(() -> {
					helper.assertFalse(finder(helper).isOn(), "it runs with no power wire at all");
					power(helper, FINDER, Direction.UP); // The sides are taken by modules.
				})
				.thenIdle(2)
				.thenExecute(() -> GameTestUtil.networkObject(helper, FINDER.above(2), BatteryNetworkObject.class).getStorage().setEnergy(100_000))
				.thenWaitUntil(() -> helper.assertTrue(finder(helper).isOn(), "the battery on its wire does not run it"))
				.thenSucceed();
	}

	/** Power wired to a module reaches the finder through the touching modules. */
	@GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 80, batch = BATCH)
	public static void powerReachesTheFinderThroughAModule(GameTestHelper helper) {
		modules(helper);
		power(helper, FINDER.west(), Direction.WEST); // Wire at (3,1,5), battery at (2,1,5).
		helper.startSequence()
				.thenIdle(3)
				.thenExecute(() -> {
					GameTestUtil.networkObject(helper, FINDER.west(3), BatteryNetworkObject.class).getStorage().setEnergy(100_000);
					finder(helper).togglePower();
				})
				.thenWaitUntil(() -> helper.assertTrue(finder(helper).isOn(), "power to a module does not reach the finder"))
				.thenExecute(() -> GameTestUtil.networkObject(helper, FINDER.west(3), BatteryNetworkObject.class).getStorage().setEnergy(0))
				.thenIdle(3)
				.thenWaitUntil(() -> helper.assertFalse(finder(helper).isOn(), "it keeps running on an empty battery"))
				.thenSucceed();
	}
}
