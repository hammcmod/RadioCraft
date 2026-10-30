package com.arrl.radiocraft.gametest;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.datacomponents.ManpackState;
import com.arrl.radiocraft.common.init.RadiocraftDataComponent;
import com.arrl.radiocraft.common.init.RadiocraftItems;
import com.arrl.radiocraft.common.radio.rfi.CarriedRadioStrikes;
import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

/** Lightning striking a player fries the handheld and manpack radios they carry (maintainer, 2026-10-27). */
@GameTestHolder(Radiocraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class CarriedRadioStrikeGameTests {

	@GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 20)
	public static void lightningFriesCarriedRadios(GameTestHelper helper) {
		FakePlayer player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.nameUUIDFromBytes("struck".getBytes()), "struck"));
		player.getInventory().clearContent();
		player.setPos(helper.absoluteVec(new net.minecraft.world.phys.Vec3(4.5D, 2.0D, 4.5D)));
		player.getInventory().setItem(0, new ItemStack(RadiocraftItems.VHF_HANDHELD.get()));
		player.getInventory().setItem(1, new ItemStack(Items.COMPASS));
		ItemStack manpack = new ItemStack(RadiocraftItems.HF_MANPACK.get());
		manpack.set(RadiocraftDataComponent.MANPACK_STATE.get(), ManpackState.fresh(ManpackState.CAPACITY).withOn(true));
		player.setItemSlot(EquipmentSlot.CHEST, manpack);

		int broken = CarriedRadioStrikes.strike(helper.getLevel(), player);
		helper.assertTrue(broken == 2, "Expected the handheld and the manpack to break, broke " + broken);
		helper.assertTrue(player.getInventory().getItem(0).isEmpty(), "The handheld survived");
		helper.assertTrue(player.getItemBySlot(EquipmentSlot.CHEST).isEmpty(), "The manpack survived");
		helper.assertTrue(player.getInventory().getItem(1).is(Items.COMPASS), "Lightning took something that is not a radio");
		player.getInventory().clearContent();
		helper.succeed();
	}
}
