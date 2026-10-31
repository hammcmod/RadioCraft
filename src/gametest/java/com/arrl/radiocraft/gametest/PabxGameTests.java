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
 * numbered from its first extension. PABXes on one data wire are peers (2026-10-31): a number that is no extension
 * here but starts with another's master number rings that extension there; a local extension wins. Master numbers
 * are exactly {@code pabx_master_digits} long (or none) and unique on a network. Without a PABX, with it unpowered, or
 * on a microwave radio alone, a phone has no line. Calls between PABXes across a microwave link are in
 * {@link MicrowaveGameTests}.
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
		helper.setBlock(PHONE_B, RadiocraftBlocks.TELEPHONE.get()); // Before the wire, so the wire is laid onto it.
		for(int i = 1; i < 4; i++)
			GameTestUtil.placeWire(helper, PABX.south(i), RadiocraftBlocks.DATA_WIRE.get());
	}

	/** Why a phone has no extension yet, for a failure message. */
	private static String why(GameTestHelper helper, BlockPos pos) {
		TelephoneBlockEntity phone = phone(helper, pos);
		PabxBlockEntity pabx = phone.pabx();
		return pabx == null ? "it has no PABX" : "its PABX " + (pabx.isPowered() ? "is up" : "has no power") + " and "
				+ (pabx.phones().contains(phone) ? "serves it" : "does not serve it");
	}

	/** Two PABXes on one data wire: {@link #FRONT} with a phone north of it, {@link #BACK} four blocks east with one north of it. */
	private static final BlockPos FRONT = PABX, BACK = PABX.east(4), FRONT_PHONE = FRONT.north(), BACK_PHONE = BACK.north();

	private static void buildPair(GameTestHelper helper) {
		helper.setBlock(FRONT, RadiocraftBlocks.PABX.get());
		helper.setBlock(BACK, RadiocraftBlocks.PABX.get());
		helper.setBlock(FRONT_PHONE, RadiocraftBlocks.TELEPHONE.get());
		helper.setBlock(BACK_PHONE, RadiocraftBlocks.TELEPHONE.get());
		for(int i = 1; i < 4; i++)
			GameTestUtil.placeWire(helper, FRONT.east(i), RadiocraftBlocks.DATA_WIRE.get());
	}

	/** A phone touching the PABX rings one on its data wire; the PABX gave them 100 and 101, and its lamp is lit. */
	@GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 200)
	public static void phonesCallThroughAPabx(GameTestHelper helper) {
		build(helper, RadiocraftBlocks.PABX.get());
		helper.startSequence()
				.thenWaitUntil(() -> {
					helper.assertTrue(pabx(helper, PABX).isPowered(), "the PABX is not up");
					int a = phone(helper, PHONE_A).getExtension(), b = phone(helper, PHONE_B).getExtension();
					helper.assertTrue(Math.min(a, b) == 100 && Math.max(a, b) == 101, "extensions " + a + " and " + b
							+ "; A: " + why(helper, PHONE_A) + "; B: " + why(helper, PHONE_B));
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
	 * Two PABXes on one data wire, 602 and 603 (named OFFICE, first extension 300): a bare extension of the other
	 * PABX rings nothing, its master number and the extension rings it, both ways. A phone keying 603 is shown
	 * "PABX 603: OFFICE", and its own number from elsewhere is its master number and extension.
	 */
	@GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 300)
	public static void wiredPabxesCallByMasterNumber(GameTestHelper helper) {
		buildPair(helper);
		helper.startSequence()
				.thenExecute(() -> {
					helper.assertTrue(pabx(helper, FRONT).setMaster("602") == null, "602 refused");
					helper.assertTrue(pabx(helper, BACK).save("603", "300", "office"), "the back PABX did not save: " + pabx(helper, BACK).getStatusKey());
				})
				.thenWaitUntil(() -> helper.assertTrue(phone(helper, FRONT_PHONE).getExtension() == 100 && phone(helper, BACK_PHONE).getExtension() == 300,
						"extensions " + phone(helper, FRONT_PHONE).getExtension() + " and " + phone(helper, BACK_PHONE).getExtension()))
				.thenExecute(() -> {
					helper.assertTrue(pabx(helper, BACK).getName().equals("OFFICE"), "name " + pabx(helper, BACK).getName());
					TelephoneBlockEntity front = phone(helper, FRONT_PHONE), back = phone(helper, BACK_PHONE);
					front.dial("300");
					helper.assertTrue(back.getState() == TelephoneBlockEntity.State.IDLE, "another PABX's bare extension rang");
					helper.assertTrue(front.getStatus().contains("NOT FOUND"), "status " + front.getStatus());
					front.dial("603300");
					helper.assertTrue(back.getState() == TelephoneBlockEntity.State.RINGING, "603300 did not ring: " + front.getStatus());
					helper.assertTrue(back.getPartnerNumber().equals("602100"), "the caller shows as " + back.getPartnerNumber());
					front.hangUp();
					back.dial("602100");
					helper.assertTrue(front.getState() == TelephoneBlockEntity.State.RINGING, "602100 did not ring: " + back.getStatus());
					back.hangUp();
				})
				.thenWaitUntil(() -> helper.assertTrue("PABX 603: OFFICE".equals(phone(helper, FRONT_PHONE).directoryLabel("603")),
						"keying 603 shows " + phone(helper, FRONT_PHONE).directoryLabel("603")))
				.thenExecute(() -> {
					helper.assertTrue(phone(helper, FRONT_PHONE).directoryLabel("60") == null, "a part of a master number named a PABX");
					helper.assertTrue(phone(helper, BACK_PHONE).getFullNumber().equals("603300"), "the back phone's number is " + phone(helper, BACK_PHONE).getFullNumber());
				})
				.thenSucceed();
	}

	/**
	 * A number that is an extension here rings it, even when it also reads as another PABX's master number and an
	 * extension there: 602's phones are 60330 and 60331, 603's is 30, and 60330 dialed at 602 rings 602's own phone.
	 */
	@GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 300)
	public static void localExtensionWins(GameTestHelper helper) {
		buildPair(helper);
		BlockPos second = FRONT.west();
		helper.setBlock(second, RadiocraftBlocks.TELEPHONE.get());
		helper.startSequence()
				.thenExecute(() -> {
					helper.assertTrue(pabx(helper, FRONT).save("602", "60330", ""), "the front PABX did not save");
					helper.assertTrue(pabx(helper, BACK).save("603", "30", ""), "the back PABX did not save");
				})
				.thenWaitUntil(() -> {
					int a = phone(helper, FRONT_PHONE).getExtension(), b = phone(helper, second).getExtension(), c = phone(helper, BACK_PHONE).getExtension();
					helper.assertTrue(Math.min(a, b) == 60330 && Math.max(a, b) == 60331 && c == 30, "extensions " + a + ", " + b + " and " + c);
				})
				.thenExecute(() -> {
					BlockPos called = phone(helper, FRONT_PHONE).getExtension() == 60330 ? FRONT_PHONE : second;
					BlockPos caller = called == FRONT_PHONE ? second : FRONT_PHONE;
					phone(helper, caller).dial("60330");
					helper.assertTrue(phone(helper, called).getState() == TelephoneBlockEntity.State.RINGING, "the local extension did not ring: " + phone(helper, caller).getStatus());
					helper.assertTrue(phone(helper, BACK_PHONE).getState() == TelephoneBlockEntity.State.IDLE, "603's extension 30 rang instead");
					phone(helper, caller).hangUp();
				})
				.thenSucceed();
	}

	/**
	 * Master numbers are exactly pabx_master_digits (3) long or empty, and no two PABXes on one network share one: a
	 * refused save changes nothing and says why. Names are cleaned to ten upper-case letters, digits, spaces and dashes.
	 */
	@GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 100)
	public static void masterNumberRules(GameTestHelper helper) {
		buildPair(helper);
		helper.startSequence()
				.thenIdle(5)
				.thenExecute(() -> {
					PabxBlockEntity front = pabx(helper, FRONT), back = pabx(helper, BACK);
					helper.assertTrue("screen.radiocraft.pabx.master_length".equals(front.setMaster("60")), "a two-digit master number was taken");
					helper.assertTrue("screen.radiocraft.pabx.master_length".equals(front.setMaster("6021")), "a four-digit master number was taken");
					helper.assertTrue("screen.radiocraft.pabx.master_length".equals(front.setMaster("6a2")), "letters were taken");
					helper.assertTrue(front.setMaster("602") == null && front.getMaster().equals("602"), "602 was refused");
					helper.assertTrue("screen.radiocraft.pabx.master_taken".equals(back.setMaster("602")), "a master number on the same wire was taken twice");
					helper.assertFalse(back.save("602", "200", "x"), "saving a taken master number succeeded");
					helper.assertTrue(back.getStatusKey().equals("screen.radiocraft.pabx.master_taken"), "status " + back.getStatusKey());
					helper.assertTrue(back.getMaster().isEmpty() && back.getFirstExtension() == 100 && back.getName().isEmpty(), "a refused save changed settings");
					helper.assertTrue(back.save("", "200", "off ice-1!x"), "an empty master number was refused");
					helper.assertTrue(back.getName().equals("OFF ICE-1X"), "name " + back.getName());
					helper.assertTrue(front.setMaster("") == null && front.getMaster().isEmpty(), "clearing the master number was refused");
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
