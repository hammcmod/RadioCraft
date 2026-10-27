package com.arrl.radiocraft.gametest;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.blockentities.LargeBatteryBlockEntity;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.compat.create.AlternatorBlock;
import com.arrl.radiocraft.compat.create.AlternatorBlockEntity;
import com.arrl.radiocraft.compat.create.CreateAlternator;
import com.arrl.radiocraft.compat.create.CreateCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The alternator, with Create installed (run with {@code -PwithCreate}). Without Create the alternator does not exist
 * and these pass at once.
 */
@GameTestHolder(Radiocraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class CreateGameTests {

	/** A turning alternator charges the large battery beside it. */
	@GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 100)
	public static void alternatorChargesBattery(GameTestHelper helper) {
		if(!CreateCompat.isActive()) {
			helper.succeed();
			return;
		}
		Alternator.chargesBattery(helper);
	}

	/** Kept apart so the test class loads without Create. */
	private static final class Alternator {

		static void chargesBattery(GameTestHelper helper) {
			BlockPos alternator = new BlockPos(4, 2, 4);
			BlockPos battery = alternator.east();
			helper.setBlock(alternator, CreateAlternator.BLOCK.get().defaultBlockState().setValue(AlternatorBlock.FACING, Direction.NORTH));
			helper.setBlock(battery, RadiocraftBlocks.LARGE_BATTERY.get());
			helper.runAfterDelay(2, () -> {
				AlternatorBlockEntity be = (AlternatorBlockEntity)helper.getBlockEntity(alternator);
				be.setSpeed(128);
			});
			helper.runAfterDelay(40, () -> {
				int expected = AlternatorBlockEntity.production(128);
				LargeBatteryBlockEntity large = (LargeBatteryBlockEntity)helper.getBlockEntity(battery);
				helper.assertTrue(expected > 0, "an alternator at 128 RPM makes nothing");
				helper.assertTrue(large.energyStorage.getEnergyStored() > expected * 10,
						"the battery holds " + large.energyStorage.getEnergyStored() + " FE after 38 ticks at " + expected + " FE/t");
				helper.succeed();
			});
		}
	}
}
