package com.arrl.radiocraft.common.commands;

import com.arrl.radiocraft.common.init.RadiocraftItems;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * Commands for managing energy in RadioCraft items (batteries and radios).
 */
public class EnergyCommands {

    public static final LiteralArgumentBuilder<CommandSourceStack> BUILDER =
        Commands.literal("rcenergy")
            .requires(source -> source.hasPermission(3)) // Requires OP level 3 (operator)
                    
                    // /rcenergy drain - Drains all energy from item in main hand
                    .then(Commands.literal("drain")
                            .executes(command -> drainEnergy(command.getSource())))
                    
                    // /rcenergy fill - Fills item in main hand to maximum capacity
                    .then(Commands.literal("fill")
                            .executes(command -> fillEnergy(command.getSource())))
                    
                    // /rcenergy info - Shows energy information about item in hand
                    .then(Commands.literal("info")
                            .executes(command -> showEnergyInfo(command.getSource())));

    /**
     * Drains all energy from the item in the player's main hand.
     */
    private static int drainEnergy(CommandSourceStack source) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            ItemStack itemInHand = player.getItemInHand(InteractionHand.MAIN_HAND);
            
            if (itemInHand.isEmpty()) {
                source.sendFailure(Component.literal("You must be holding an item in your main hand!"));
                return 0;
            }
            
            IEnergyStorage energyStorage = itemInHand.getCapability(Capabilities.EnergyStorage.ITEM);
            
            if (energyStorage == null) {
                source.sendFailure(Component.literal("This item cannot store energy!"));
                return 0;
            }
            
            int currentEnergy = energyStorage.getEnergyStored();
            
            if (currentEnergy == 0) {
                source.sendFailure(Component.literal("This item is already empty!"));
                return 0;
            }
            
            // Drain all energy
            int drained = energyStorage.extractEnergy(currentEnergy, false);
            
            // Convert FE to Joules for display (2.5 FE = 1J)
            double joulesDrained = drained / 2.5;
            
            Component message = Component.literal("Drained ")
                    .withStyle(ChatFormatting.GREEN)
                    .append(Component.literal(String.format("%d FE (%.0f J)", drained, joulesDrained))
                            .withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal(" from ")
                            .withStyle(ChatFormatting.GREEN))
                    .append(itemInHand.getDisplayName())
                    .append(Component.literal("!")
                            .withStyle(ChatFormatting.GREEN));
            
            source.sendSuccess(() -> message, true);
            return 1;
            
        } catch (Exception e) {
            source.sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    /**
     * Fills the item in the player's main hand to maximum capacity.
     */
    private static int fillEnergy(CommandSourceStack source) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            ItemStack itemInHand = player.getItemInHand(InteractionHand.MAIN_HAND);
            
            if (itemInHand.isEmpty()) {
                source.sendFailure(Component.literal("You must be holding an item in your main hand!"));
                return 0;
            }
            
            IEnergyStorage energyStorage = itemInHand.getCapability(Capabilities.EnergyStorage.ITEM);
            
            if (energyStorage == null) {
                source.sendFailure(Component.literal("This item cannot store energy!"));
                return 0;
            }
            
            int maxEnergy = energyStorage.getMaxEnergyStored();
            int currentEnergy = energyStorage.getEnergyStored();
            int needed = maxEnergy - currentEnergy;
            
            if (needed == 0) {
                source.sendFailure(Component.literal("This item is already fully charged!"));
                return 0;
            }
            
            // Fill to maximum
            int filled = energyStorage.receiveEnergy(needed, false);
            
            // Convert FE to Joules for display (2.5 FE = 1J)
            double joulesFilled = filled / 2.5;
            
            Component message = Component.literal("Added ")
                    .withStyle(ChatFormatting.GREEN)
                    .append(Component.literal(String.format("%d FE (%.0f J)", filled, joulesFilled))
                            .withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal(" to ")
                            .withStyle(ChatFormatting.GREEN))
                    .append(itemInHand.getDisplayName())
                    .append(Component.literal("!")
                            .withStyle(ChatFormatting.GREEN));
            
            source.sendSuccess(() -> message, true);
            return 1;
            
        } catch (Exception e) {
            source.sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    /**
     * Shows detailed energy information about the item in the player's main hand.
     */
    private static int showEnergyInfo(CommandSourceStack source) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            ItemStack itemInHand = player.getItemInHand(InteractionHand.MAIN_HAND);
            
            if (itemInHand.isEmpty()) {
                source.sendFailure(Component.literal("You must be holding an item in your main hand!"));
                return 0;
            }
            
            IEnergyStorage energyStorage = itemInHand.getCapability(Capabilities.EnergyStorage.ITEM);
            
            if (energyStorage == null) {
                source.sendFailure(Component.literal("This item cannot store energy!"));
                return 0;
            }
            
            int stored = energyStorage.getEnergyStored();
            int max = energyStorage.getMaxEnergyStored();
            double percentage = max > 0 ? (double) stored / max * 100.0 : 0.0;
            
            // Convert FE to Joules (2.5 FE = 1J)
            double storedJoules = stored / 2.5;
            double maxJoules = max / 2.5;
            
            // Convert to Watt-hours (3600 J = 1 Wh)
            double storedWh = storedJoules / 3600.0;
            double maxWh = maxJoules / 3600.0;
            
            // Determine item type for additional info
            final String itemType;
            if (itemInHand.getItem() == RadiocraftItems.SMALL_BATTERY.get()) {
                itemType = "Small Battery";
            } else if (itemInHand.getItem() == RadiocraftItems.VHF_HANDHELD.get()) {
                itemType = "VHF Handheld Radio";
            } else {
                itemType = "Unknown";
            }
            
            // Build info message
            source.sendSuccess(() -> Component.literal("=== Energy Info ===")
                    .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
            
            source.sendSuccess(() -> Component.literal("Item: ")
                    .withStyle(ChatFormatting.GRAY)
                    .append(itemInHand.getDisplayName().copy().withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(" (" + itemType + ")")
                            .withStyle(ChatFormatting.DARK_GRAY)), false);
            
            source.sendSuccess(() -> Component.literal("Energy: ")
                    .withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(String.format("%d / %d FE", stored, max))
                            .withStyle(ChatFormatting.AQUA)), false);
            
            source.sendSuccess(() -> Component.literal("Joules: ")
                    .withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(String.format("%.0f / %.0f J", storedJoules, maxJoules))
                            .withStyle(ChatFormatting.AQUA)), false);
            
            source.sendSuccess(() -> Component.literal("Watt-hours: ")
                    .withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(String.format("%.2f / %.2f Wh", storedWh, maxWh))
                            .withStyle(ChatFormatting.AQUA)), false);
            
            // Color-coded percentage
            ChatFormatting percentColor;
            if (percentage > 75) {
                percentColor = ChatFormatting.GREEN;
            } else if (percentage > 50) {
                percentColor = ChatFormatting.YELLOW;
            } else if (percentage > 25) {
                percentColor = ChatFormatting.GOLD;
            } else {
                percentColor = ChatFormatting.RED;
            }
            
            source.sendSuccess(() -> Component.literal("Charge: ")
                    .withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(String.format("%.1f%%", percentage))
                            .withStyle(percentColor)), false);
            
            return 1;
            
        } catch (Exception e) {
            source.sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }
}
