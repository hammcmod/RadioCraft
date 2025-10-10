package com.arrl.radiocraft.common.items;

import com.arrl.radiocraft.CommonConfig;
import com.arrl.radiocraft.api.capabilities.IVHFHandheldCapability;
import com.arrl.radiocraft.client.screens.radios.VHFHandheldScreen;
import com.arrl.radiocraft.common.init.RadiocraftItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.arrl.radiocraft.common.capabilities.RadiocraftCapabilities.VHF_HANDHELDS;

public class VHFHandheldItem extends Item {

    public VHFHandheldItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack item = player.getItemInHand(hand);

        if(hand == InteractionHand.MAIN_HAND) {
            if(level.isClientSide() && player.isCrouching()) {
                VHFHandheldScreen.open(player.getInventory().selected); // The open call is in a different class so the server doesn't try to load it.
            }
            if(!player.isCrouching()){
                return InteractionResultHolder.pass(item); //prevents bob animation on use key (aka right click)
            }
        }
        else { // OFFHAND
            if(!level.isClientSide()) {
                ItemStack mainItem = player.getItemInHand(InteractionHand.MAIN_HAND);

                // Shift + use with battery in main hand = swap battery energy between radio and battery
                if(player.isCrouching() && mainItem.getItem() == RadiocraftItems.SMALL_BATTERY.get()) {
                    swapBatteryEnergy(item, mainItem, player);
                    return InteractionResultHolder.success(item);
                }
            }
        }

        return super.use(level, player, hand);
    }

    /**
     * Called when this radio (in cursor) is clicked on another item in a slot.
     * Handles battery swap when clicking on a Small Battery.
     * Works in survival mode only - creative mode handled by ItemStackedOnOtherEvent.
     */
    @Override
    public boolean overrideStackedOnOther(ItemStack radio, net.minecraft.world.inventory.Slot slot, 
                                         net.minecraft.world.inventory.ClickAction action, Player player) {
        ItemStack slotStack = slot.getItem();
        
        if (action == net.minecraft.world.inventory.ClickAction.PRIMARY && !slotStack.isEmpty() && 
            slotStack.getItem() == RadiocraftItems.SMALL_BATTERY.get()) {
            
            if (!player.level().isClientSide()) {
                swapBatteryEnergy(radio, slotStack, player);
            }
            
            return true;
        }
        
        return false;
    }

    /**
     * Swaps energy between the radio and a battery item.
     * Radio's energy goes to the battery, battery's energy goes to the radio.
     * Public and static so it can be called from inventory click events.
     */
    public static void swapBatteryEnergy(ItemStack radio, ItemStack battery, Player player) {
        IEnergyStorage radioEnergy = radio.getCapability(Capabilities.EnergyStorage.ITEM);
        IEnergyStorage batteryEnergy = battery.getCapability(Capabilities.EnergyStorage.ITEM);
        
        if (radioEnergy != null && batteryEnergy != null) {
            int radioStored = radioEnergy.getEnergyStored();
            int batteryStored = batteryEnergy.getEnergyStored();
            
            // Swap energies
            radioEnergy.extractEnergy(radioStored, false);
            radioEnergy.receiveEnergy(batteryStored, false);
            
            batteryEnergy.extractEnergy(batteryStored, false);
            batteryEnergy.receiveEnergy(radioStored, false);
            
            // Sound and message feedback
            player.level().playSound(null, player.blockPosition(), 
                SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.PLAYERS, 
                1.0f, 1.0f);
            
            player.displayClientMessage(
                Component.translatable("message.radiocraft.battery_swapped"), 
                true
            );
        }
    }

    // prevents item bob on data update, which occurs every tick when receiving to update the RF meter
    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return false;
    }

    @Override
    public void inventoryTick(ItemStack item, Level level, Entity entity, int slot, boolean isSelected) {
        if(!level.isClientSide() && entity instanceof ServerPlayer player) {
            IVHFHandheldCapability cap = VHF_HANDHELDS.getCapability(item, null);
            
            if (cap != null && cap.isPowered()) {
                IEnergyStorage energyStorage = item.getCapability(Capabilities.EnergyStorage.ITEM);
                
                if (energyStorage != null) {
                    int storedEnergy = energyStorage.getEnergyStored();
                    
                    if (storedEnergy > 0) {
                        // Calculate consumption based on radio state
                        int consumption = calculateEnergyConsumption(cap);
                        
                        energyStorage.extractEnergy(consumption, false);
                    } else {
                        // No energy - turn off radio
                        cap.setPowered(false);
                        
                        // Empty battery message
                        player.displayClientMessage(
                            Component.translatable("message.radiocraft.radio_battery_empty"),
                            true
                        );
                    }
                }
            }
        }
    }

    /**
     * Calculates energy consumption per tick based on radio state.
     * Based on InitialProposal.md specifications:
     * - Idle: battery lasts 12 in-game days (288000 ticks)
     * - Receiving: battery lasts 6 in-game days (144000 ticks)
     * - Transmitting: battery lasts 2 in-game days (48000 ticks)
     */
    private int calculateEnergyConsumption(IVHFHandheldCapability cap) {
        int capacity = CommonConfig.SMALL_BATTERY_CAPACITY.get();
        
        if (cap.isPTTDown()) {
            return Math.max(1, capacity / 48000); // Transmitting (~3.5 FE/tick)
        } else if (cap.getReceiveStrength() > 0) {
            return Math.max(1, capacity / 144000); // Receiving (~1.16 FE/tick)
        } else {
            return Math.max(1, capacity / 288000); // Idle (~0.58 FE/tick)
        }
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        IEnergyStorage cap = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        if (cap != null) {
            return Math.round(((float) cap.getEnergyStored() / cap.getMaxEnergyStored()) * 13.0f);
        }
        return 0;
    }

    @Override
    public int getBarColor(ItemStack stack) {
        IEnergyStorage cap = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        if (cap != null) {
            float charge = (float) cap.getEnergyStored() / cap.getMaxEnergyStored();
            return Mth.hsvToRgb(Math.max(0.0f, charge) / 3.0f, 1.0f, 1.0f);
        }
        return Mth.hsvToRgb(0.0f, 1.0f, 1.0f);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, 
                               @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        
        IEnergyStorage energyStorage = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        if (energyStorage != null) {
            int storedFE = energyStorage.getEnergyStored();
            int maxFE = energyStorage.getMaxEnergyStored();
            
            double storedJoules = storedFE / 2.5;
            double maxJoules = maxFE / 2.5;
            double percentage = maxFE > 0 ? (double) storedFE / maxFE * 100.0 : 0.0;
            
            tooltipComponents.add(Component.translatable("tooltip.radiocraft.energy_stored_joules", 
                Math.round(storedJoules), Math.round(maxJoules)));
            tooltipComponents.add(Component.translatable("tooltip.radiocraft.battery_percentage", 
                String.format("%.1f", percentage)));
        }
        
        tooltipComponents.add(Component.translatable("tooltip.radiocraft.vhf_handheld_battery_swap"));
    }

    @Override
    public void onCraftedBy(ItemStack stack, Level level, Player player) {
        super.onCraftedBy(stack, level, player);
        
        // Rádio novo vem com 50-70% de carga (como as baterias)
        IEnergyStorage energyStorage = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        if (energyStorage != null) {
            int capacity = energyStorage.getMaxEnergyStored();
            int initialCharge = (int) (capacity * (0.5 + Math.random() * 0.2)); // 50-70%
            energyStorage.receiveEnergy(initialCharge, false);
        }
    }
}
