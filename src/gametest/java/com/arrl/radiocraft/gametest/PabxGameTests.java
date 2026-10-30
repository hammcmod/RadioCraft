package com.arrl.radiocraft.gametest;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.RadiocraftServerConfig;
import com.arrl.radiocraft.common.blockentities.PabxBlockEntity;
import com.arrl.radiocraft.common.blockentities.TelephoneBlockEntity;
import com.arrl.radiocraft.common.blocks.PabxBlock;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.AfterBatch;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The phone PABX (2026-10-30): phones wired to a powered PABX call each other with no microwave radio, and it numbers
 * them from 100; without one, or with it unpowered, a phone has no line.
 */
@GameTestHolder(Radiocraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class PabxGameTests {

	private static final String UNPOWERED = "pabx_unpowered";
	private static final BlockPos PABX = new BlockPos(5, 2, 5);
	private static final BlockPos PHONE_A = PABX.north();
	/** Four blocks off, on data wire. */
	private static final BlockPos PHONE_B = PABX.south(4);

	private static TelephoneBlockEntity phone(GameTestHelper helper, BlockPos pos) {
		return GameTestUtil.blockEntity(helper, pos, TelephoneBlockEntity.class);
	}

	private static void build(GameTestHelper helper, boolean withPabx) {
		if(withPabx)
			helper.setBlock(PABX, RadiocraftBlocks.PABX.get());
		helper.setBlock(PHONE_A, RadiocraftBlocks.TELEPHONE.get());
		for(int i = 1; i < 4; i++)
			GameTestUtil.placeWire(helper, PABX.south(i), RadiocraftBlocks.DATA_WIRE.get());
		helper.setBlock(PHONE_B, RadiocraftBlocks.TELEPHONE.get());
	}

	/** A phone touching the PABX rings one on its data wire; the PABX gave them 100 and 101, and its lamp is lit. */
	@GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 200)
	public static void phonesCallThroughAPabx(GameTestHelper helper) {
		build(helper, true);
		helper.startSequence()
				.thenWaitUntil(() -> {
					helper.assertTrue(GameTestUtil.blockEntity(helper, PABX, PabxBlockEntity.class).isPowered(), "the PABX is not up");
					int a = phone(helper, PHONE_A).getExtension(), b = phone(helper, PHONE_B).getExtension();
					helper.assertTrue(Math.min(a, b) == 100 && Math.max(a, b) == 101, "extensions " + a + " and " + b);
				})
				.thenExecute(() -> {
					helper.assertTrue(helper.getBlockState(PABX).getValue(PabxBlock.LIT), "the lamp is off");
					phone(helper, PHONE_A).dial(phone(helper, PHONE_B).getExtension());
					helper.assertTrue(phone(helper, PHONE_B).getState() == TelephoneBlockEntity.State.RINGING, "B is not ringing: " + phone(helper, PHONE_A).getStatus());
				})
				.thenSucceed();
	}

	/** Two phones on data wire with no PABX and no microwave radio have no line. */
	@GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 100)
	public static void noPabxNoLine(GameTestHelper helper) {
		build(helper, false);
		helper.startSequence()
				.thenIdle(25)
				.thenExecute(() -> {
					phone(helper, PHONE_A).dial(phone(helper, PHONE_B).getExtension());
					helper.assertTrue(phone(helper, PHONE_B).getState() == TelephoneBlockEntity.State.IDLE, "B rang with no exchange");
					helper.assertTrue(phone(helper, PHONE_A).getStatus().startsWith("NO LINE"), "status: " + phone(helper, PHONE_A).getStatus());
				})
				.thenSucceed();
	}

	@BeforeBatch(batch = UNPOWERED)
	public static void powerRequired(ServerLevel level) {
		RadiocraftServerConfig.RADIO_POWER_REQUIRED.set(true);
	}

	@AfterBatch(batch = UNPOWERED)
	public static void powerFree(ServerLevel level) {
		RadiocraftServerConfig.RADIO_POWER_REQUIRED.set(false);
	}

	/** A PABX with no power wire is no exchange: no line, lamp dark. */
	@GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 100, batch = UNPOWERED)
	public static void unpoweredPabxGivesNoLine(GameTestHelper helper) {
		build(helper, true);
		helper.startSequence()
				.thenIdle(25)
				.thenExecute(() -> {
					helper.assertFalse(helper.getBlockState(PABX).getValue(PabxBlock.LIT), "the lamp is lit with no power");
					phone(helper, PHONE_A).dial(phone(helper, PHONE_B).getExtension());
					helper.assertTrue(phone(helper, PHONE_A).getStatus().startsWith("NO LINE"), "status: " + phone(helper, PHONE_A).getStatus());
				})
				.thenSucceed();
	}
}
