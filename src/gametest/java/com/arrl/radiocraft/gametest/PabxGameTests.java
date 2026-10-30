package com.arrl.radiocraft.gametest;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.RadiocraftServerConfig;
import com.arrl.radiocraft.common.blockentities.MicrowaveRadioBlockEntity;
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
 * The phone PABX (2026-10-30): every phone needs one. Phones wired to a powered PABX call each other by extension,
 * numbered from its first extension; a PABX behind another is reached by its master number then the extension, and
 * calls out through the one above. Without a PABX, with it unpowered, or on a microwave radio alone, a phone has no
 * line. Calls between PABXes across a microwave link are in {@link MicrowaveGameTests}.
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

	private static PabxBlockEntity pabx(GameTestHelper helper, BlockPos pos) {
		return GameTestUtil.blockEntity(helper, pos, PabxBlockEntity.class);
	}

	private static String extension(GameTestHelper helper, BlockPos pos) {
		return Integer.toString(phone(helper, pos).getExtension());
	}

	/** The PABX (or {@code exchange} in its place), a phone touching it and one on its data wire. */
	private static void build(GameTestHelper helper, net.minecraft.world.level.block.Block exchange) {
		if(exchange != null)
			helper.setBlock(PABX, exchange);
		helper.setBlock(PHONE_A, RadiocraftBlocks.TELEPHONE.get());
		for(int i = 1; i < 4; i++)
			GameTestUtil.placeWire(helper, PABX.south(i), RadiocraftBlocks.DATA_WIRE.get());
		helper.setBlock(PHONE_B, RadiocraftBlocks.TELEPHONE.get());
	}

	/** A phone touching the PABX rings one on its data wire; the PABX gave them 100 and 101, and its lamp is lit. */
	@GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 200)
	public static void phonesCallThroughAPabx(GameTestHelper helper) {
		build(helper, RadiocraftBlocks.PABX.get());
		helper.startSequence()
				.thenWaitUntil(() -> {
					helper.assertTrue(pabx(helper, PABX).isPowered(), "the PABX is not up");
					int a = phone(helper, PHONE_A).getExtension(), b = phone(helper, PHONE_B).getExtension();
					helper.assertTrue(Math.min(a, b) == 100 && Math.max(a, b) == 101, "extensions " + a + " and " + b);
				})
				.thenExecute(() -> {
					helper.assertTrue(helper.getBlockState(PABX).getValue(PabxBlock.LIT), "the lamp is off");
					phone(helper, PHONE_A).dial(extension(helper, PHONE_B));
					helper.assertTrue(phone(helper, PHONE_B).getState() == TelephoneBlockEntity.State.RINGING, "B is not ringing: " + phone(helper, PHONE_A).getStatus());
				})
				.thenSucceed();
	}

	/** Setting the first extension numbers the phones again from it. */
	@GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 200)
	public static void firstExtensionRenumbers(GameTestHelper helper) {
		build(helper, RadiocraftBlocks.PABX.get());
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(phone(helper, PHONE_A).getExtension() >= 100 && phone(helper, PHONE_B).getExtension() >= 100, "not numbered yet"))
				.thenExecute(() -> pabx(helper, PABX).setFirstExtension(4000))
				.thenWaitUntil(() -> {
					int a = phone(helper, PHONE_A).getExtension(), b = phone(helper, PHONE_B).getExtension();
					helper.assertTrue(Math.min(a, b) == 4000 && Math.max(a, b) == 4001, "extensions " + a + " and " + b);
				})
				.thenSucceed();
	}

	/** Two phones on data wire with no PABX have no line. */
	@GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 100)
	public static void noPabxNoLine(GameTestHelper helper) {
		build(helper, null);
		helper.startSequence()
				.thenIdle(25)
				.thenExecute(() -> {
					phone(helper, PHONE_A).dial("0");
					helper.assertTrue(phone(helper, PHONE_B).getState() == TelephoneBlockEntity.State.IDLE, "B rang with no exchange");
					helper.assertTrue(phone(helper, PHONE_A).getStatus().startsWith("NO LINE"), "status: " + phone(helper, PHONE_A).getStatus());
				})
				.thenSucceed();
	}

	/** A powered microwave radio is no exchange: phones on it alone have no line. */
	@GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 100)
	public static void microwaveRadioAloneIsNoExchange(GameTestHelper helper) {
		build(helper, RadiocraftBlocks.MICROWAVE_RADIO.get());
		helper.startSequence()
				.thenExecute(() -> GameTestUtil.blockEntity(helper, PABX, MicrowaveRadioBlockEntity.class).togglePower())
				.thenWaitUntil(() -> helper.assertTrue(GameTestUtil.blockEntity(helper, PABX, MicrowaveRadioBlockEntity.class).isPowered(), "the radio is not up"))
				.thenExecute(() -> {
					phone(helper, PHONE_A).dial(extension(helper, PHONE_B));
					helper.assertTrue(phone(helper, PHONE_A).getStatus().startsWith("NO LINE"), "status: " + phone(helper, PHONE_A).getStatus());
				})
				.thenSucceed();
	}

	/**
	 * A PABX behind another (touching it), master number 7: from the front PABX's phone, 7 and the extension rings the
	 * phone behind; from behind, the front phone's bare extension rings it, through the PABX above.
	 */
	@GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 200)
	public static void pabxBehindAnother(GameTestHelper helper) {
		BlockPos front = PABX, back = PABX.east(), frontPhone = PABX.west(), backPhone = back.east();
		helper.setBlock(front, RadiocraftBlocks.PABX.get());
		helper.setBlock(back, RadiocraftBlocks.PABX.get());
		helper.setBlock(frontPhone, RadiocraftBlocks.TELEPHONE.get());
		helper.setBlock(backPhone, RadiocraftBlocks.TELEPHONE.get());
		helper.startSequence()
				.thenExecute(() -> {
					pabx(helper, front).setMaster("602");
					pabx(helper, back).setMaster("7");
					pabx(helper, back).setBehind(true);
					pabx(helper, back).setFirstExtension(300);
				})
				.thenWaitUntil(() -> helper.assertTrue(phone(helper, frontPhone).getExtension() == 100 && phone(helper, backPhone).getExtension() == 300,
						"extensions " + phone(helper, frontPhone).getExtension() + " and " + phone(helper, backPhone).getExtension()))
				.thenExecute(() -> {
					helper.assertTrue(pabx(helper, back).parent() == pabx(helper, front), "the back PABX is not behind the front one");
					helper.assertTrue(pabx(helper, back).fullPrefix().equals("6027"), "full number " + pabx(helper, back).fullPrefix());
					phone(helper, frontPhone).dial("300");
					helper.assertTrue(phone(helper, backPhone).getState() == TelephoneBlockEntity.State.IDLE, "a bare extension behind another PABX rang");
					phone(helper, frontPhone).dial("7300");
					helper.assertTrue(phone(helper, backPhone).getState() == TelephoneBlockEntity.State.RINGING, "7300 did not ring: " + phone(helper, frontPhone).getStatus());
					phone(helper, frontPhone).hangUp();
					phone(helper, backPhone).dial("100");
					helper.assertTrue(phone(helper, frontPhone).getState() == TelephoneBlockEntity.State.RINGING, "100 from behind did not ring: " + phone(helper, backPhone).getStatus());
					phone(helper, backPhone).hangUp();
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
		build(helper, RadiocraftBlocks.PABX.get());
		helper.startSequence()
				.thenIdle(25)
				.thenExecute(() -> {
					helper.assertFalse(helper.getBlockState(PABX).getValue(PabxBlock.LIT), "the lamp is lit with no power");
					phone(helper, PHONE_A).dial("101");
					helper.assertTrue(phone(helper, PHONE_A).getStatus().startsWith("NO LINE"), "status: " + phone(helper, PHONE_A).getStatus());
				})
				.thenSucceed();
	}
}
