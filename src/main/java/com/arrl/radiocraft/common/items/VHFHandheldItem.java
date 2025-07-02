package com.arrl.radiocraft.common.items;

import com.arrl.radiocraft.api.capabilities.IVHFHandheldCapability;
import com.arrl.radiocraft.client.screens.radios.VHFHandheldScreen;
import com.arrl.radiocraft.common.init.RadiocraftItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import static com.arrl.radiocraft.common.capabilities.RadiocraftCapabilities.VHF_HANDHELDS;

public class VHFHandheldItem extends Item {

    public VHFHandheldItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack item = player.getItemInHand(hand);

        if(hand == InteractionHand.MAIN_HAND) {
            if(level.isClientSide())
                VHFHandheldScreen.open(player.getInventory().selected); // The open call is in a different class so the server doesn't try to load it.
        }
        else {
            if(!level.isClientSide()) {
                ItemStack mainItem = player.getItemInHand(InteractionHand.MAIN_HAND);

                if(player.isCrouching() && (mainItem.getItem() == RadiocraftItems.SMALL_BATTERY.get() || mainItem.isEmpty())) { // Shift use with battery or air in main hand = swap batteries.

                    IVHFHandheldCapability cap = VHF_HANDHELDS.getCapability(item, null);

                    if (cap != null) {
                        player.setItemInHand(InteractionHand.MAIN_HAND, cap.getBattery());
                        cap.setBattery(mainItem);
                    }
                }

            }
        }

        return super.use(level, player, hand);
    }

    @Override
    public void inventoryTick(ItemStack item, Level level, Entity entity, int slot, boolean isSelected) {
        if(!level.isClientSide() && entity instanceof ServerPlayer player) {

            IVHFHandheldCapability cap = VHF_HANDHELDS.getCapability(item, null);

            if (cap != null) {
                if (cap.isPowered()) {
                    //entity.sendSystemMessage(Component.literal("Would consume power for device " + cap.getBattery() + " on player " + entity.getDisplayName()));
                    /*
                    TODO: Consume power on the item when held.

                    https://neoforged.net/news/20.5release/

                     this needs to use DataComponentType and instead of cap.getItem().getOrCreateTag(); you need to use

                     + DataComponentType<Integer> ENERGY = ...;

                    - int energy = stack.getOrCreateTag().getInt("energy");
                    + int energy = stack.getOrDefault(ENERGY, 0);

                    CompoundTag nbt = cap.getItem().getOrCreateTag();
                    if(!nbt.contains("charge"))
                        nbt.putInt("charge", 0);
                    int charge = nbt.getInt("charge");

                    if(!cap.getItem().isEmpty() && charge > 0) {
                        charge -= 1; // For now, just consume power whenever it's on.

                        nbt.putInt("charge", charge);
                    }
                    else {
                        cap.setPowered(false);
                        RadiocraftPackets.sendToPlayer(new CHandheldPowerPacket(slot, false), player);
                    }
                     */
                }
            }
        }
    }
}
