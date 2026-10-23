package com.arrl.radiocraft.common.items;

import com.arrl.radiocraft.CommonConfig;
import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.radio.crystal.CrystalQuality;
import net.minecraft.ChatFormatting;
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

    private final CrystalQuality crystal;

    /** A handheld built around a crystal of {@code crystal} quality (each quality is its own item). */
    public VHFHandheldItem(Properties properties, CrystalQuality crystal) {
        super(properties);
        this.crystal = crystal;
    }

    public CrystalQuality getCrystalQuality() {
        return crystal;
    }

    /** The crystal in the handheld {@code stack}, good if it is not a handheld. */
    public static CrystalQuality crystalOf(ItemStack stack) {
        return stack.getItem() instanceof VHFHandheldItem handheld ? handheld.crystal : CrystalQuality.GOOD;
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

                // Shift + use with battery in main hand = swap battery items between radio and hand
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
                        int consumption = calculateEnergyConsumption(cap, level.getGameTime());
                        if(consumption > 0)
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
     * The rate is usually under 1 FE a tick, so it is spread over ticks: this tick takes the whole FE the running total
     * has crossed since the last one (0 most ticks, 1 now and then).
     */
    private static int calculateEnergyConsumption(IVHFHandheldCapability cap, long gameTime) {
        int lifetimeTicks = cap.isPTTDown() ? 48000 : cap.getReceiveStrength() > 0 ? 144000 : 288000;
        return spreadDrain((double)CommonConfig.SMALL_BATTERY_CAPACITY.get() / lifetimeTicks, gameTime);
    }

    /** The whole FE to take at {@code gameTime} for a drain of {@code perTick} (often under 1): over N ticks they add up to N times it. */
    public static int spreadDrain(double perTick, long gameTime) {
        return (int)(Math.floor((gameTime + 1) * perTick) - Math.floor(gameTime * perTick));
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
        if(crystal != CrystalQuality.GOOD)
            tooltipComponents.add(Component.translatable(Radiocraft.translationKey("tooltip", "crystal_" + crystal.getId())).withStyle(ChatFormatting.GRAY));
    }

    @Override
    public void onCraftedBy(ItemStack stack, Level level, Player player) {
        super.onCraftedBy(stack, level, player);
        
    // New radio comes with 50-70% charge (like batteries)
        IEnergyStorage energyStorage = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        if (energyStorage != null) {
            int capacity = energyStorage.getMaxEnergyStored();
            int initialCharge = (int) (capacity * (0.5 + Math.random() * 0.2)); // 50-70%
            energyStorage.receiveEnergy(initialCharge, false);
        }
    }
}
