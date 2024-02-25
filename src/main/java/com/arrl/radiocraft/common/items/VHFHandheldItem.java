package com.arrl.radiocraft.common.items;

import com.arrl.radiocraft.api.capabilities.IVHFHandheldCapability;
import com.arrl.radiocraft.api.capabilities.RadiocraftCapabilities;
import com.arrl.radiocraft.client.screens.radios.VHFHandheldScreen;
import com.arrl.radiocraft.common.init.RadiocraftItems;
import com.arrl.radiocraft.common.init.RadiocraftPackets;
import com.arrl.radiocraft.common.network.packets.clientbound.CHandheldPowerPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.LazyOptional;

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
                    LazyOptional<IVHFHandheldCapability> optional = item.getCapability(RadiocraftCapabilities.VHF_HANDHELDS);

                    optional.ifPresent(cap -> {
                        player.setItemInHand(InteractionHand.MAIN_HAND, cap.getItem());
                        cap.setItem(mainItem);
                    });
                }

            }
        }

        return super.use(level, player, hand);
    }

    @Override
    public void inventoryTick(ItemStack item, Level level, Entity entity, int slot, boolean isSelected) {
        if(!level.isClientSide() && entity instanceof ServerPlayer player) {
            LazyOptional<IVHFHandheldCapability> optional = item.getCapability(RadiocraftCapabilities.VHF_HANDHELDS);
            optional.ifPresent(cap -> {
                if(cap.isPowered()) {
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
                }
            });
        }
    }
}
