package com.arrl.radiocraft.common.commands;

import com.arrl.radiocraft.common.init.RadiocraftItems;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.arguments.IntegerArgumentType;
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

            static {
                // /rcenergy fillblock [amount] or [x y z amount]
                BUILDER.then(Commands.literal("fillblock")
                    .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                        .executes(ctx -> fillBlockEnergy(ctx.getSource(), null, IntegerArgumentType.getInteger(ctx, "amount")))
                    )
                    .then(Commands.argument("x", IntegerArgumentType.integer())
                        .then(Commands.argument("y", IntegerArgumentType.integer())
                            .then(Commands.argument("z", IntegerArgumentType.integer())
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                    .executes(ctx -> {
                                        int x = IntegerArgumentType.getInteger(ctx, "x");
                                        int y = IntegerArgumentType.getInteger(ctx, "y");
                                        int z = IntegerArgumentType.getInteger(ctx, "z");
                                        int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                        return fillBlockEnergy(ctx.getSource(), new int[]{x, y, z}, amount);
                                    })
                                )
                            )
                        )
                    )
                );

                // /rcenergy blockinfo x y z
                BUILDER.then(Commands.literal("blockinfo")
                    .then(Commands.argument("x", IntegerArgumentType.integer())
                        .then(Commands.argument("y", IntegerArgumentType.integer())
                            .then(Commands.argument("z", IntegerArgumentType.integer())
                                .executes(ctx -> blockInfo(ctx.getSource(), new int[]{IntegerArgumentType.getInteger(ctx, "x"), IntegerArgumentType.getInteger(ctx, "y"), IntegerArgumentType.getInteger(ctx, "z")}))
                            )
                        )
                    )
                );

                // /rcenergy transfer [amount] or [x y z amount] - immediate transfer from charger to radio in slot
                BUILDER.then(Commands.literal("transfer")
                    .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                        .executes(ctx -> transferToRadio(ctx.getSource(), null, IntegerArgumentType.getInteger(ctx, "amount")))
                    )
                    .then(Commands.argument("x", IntegerArgumentType.integer())
                        .then(Commands.argument("y", IntegerArgumentType.integer())
                            .then(Commands.argument("z", IntegerArgumentType.integer())
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                    .executes(ctx -> {
                                        int x = IntegerArgumentType.getInteger(ctx, "x");
                                        int y = IntegerArgumentType.getInteger(ctx, "y");
                                        int z = IntegerArgumentType.getInteger(ctx, "z");
                                        int amt = IntegerArgumentType.getInteger(ctx, "amount");
                                        return transferToRadio(ctx.getSource(), new int[]{x, y, z}, amt);
                                    })
                                )
                            )
                        )
                    )
                );

                // /rcenergy infinite <on|off> [x y z] - toggle infinite mode for desk charger
                BUILDER.then(Commands.literal("infinite")
                    .then(Commands.literal("on")
                        .executes(ctx -> infiniteMode(ctx.getSource(), true, null))
                        .then(Commands.argument("x", IntegerArgumentType.integer())
                            .then(Commands.argument("y", IntegerArgumentType.integer())
                                .then(Commands.argument("z", IntegerArgumentType.integer())
                                    .executes(ctx -> infiniteMode(ctx.getSource(), true, new int[]{IntegerArgumentType.getInteger(ctx, "x"), IntegerArgumentType.getInteger(ctx, "y"), IntegerArgumentType.getInteger(ctx, "z")}))
                                )
                            )
                        )
                    )
                    .then(Commands.literal("off")
                        .executes(ctx -> infiniteMode(ctx.getSource(), false, null))
                        .then(Commands.argument("x", IntegerArgumentType.integer())
                            .then(Commands.argument("y", IntegerArgumentType.integer())
                                .then(Commands.argument("z", IntegerArgumentType.integer())
                                    .executes(ctx -> infiniteMode(ctx.getSource(), false, new int[]{IntegerArgumentType.getInteger(ctx, "x"), IntegerArgumentType.getInteger(ctx, "y"), IntegerArgumentType.getInteger(ctx, "z")}))
                                )
                            )
                        )
                    )
                );
            }


        private static int transferToRadio(CommandSourceStack source, int[] coords, int amount) {
            try {
                ServerPlayer player = source.getPlayerOrException();
                net.minecraft.core.BlockPos pos;
                if (coords == null) pos = player.blockPosition().below(); else pos = new net.minecraft.core.BlockPos(coords[0], coords[1], coords[2]);

                var be = player.level().getBlockEntity(pos);
                if (!(be instanceof com.arrl.radiocraft.common.blockentities.DeskChargerBlockEntity desk)) {
                    source.sendFailure(Component.literal("No DeskChargerBlockEntity at position."));
                    return 0;
                }

                var radio = desk.inventory.getStackInSlot(0);
                if (radio == null || radio.isEmpty()) {
                    source.sendFailure(Component.literal("No radio in charger slot."));
                    return 0;
                }

                var radioCap = radio.getCapability(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.ITEM);
                if (radioCap == null) {
                    source.sendFailure(Component.literal("Radio has no energy capability."));
                    return 0;
                }

                int available = desk.energyStorage.getEnergyStored();
                int toSend = Math.min(available, amount);

                // Simulate receive to know how much will be accepted
                int acceptedSim = radioCap.receiveEnergy(toSend, true);
                int extracted = desk.energyStorage.extractEnergy(acceptedSim, false);
                int received = radioCap.receiveEnergy(extracted, false);
                if (received < extracted) {
                    desk.energyStorage.receiveEnergy(extracted - received, false);
                }

                desk.setChanged();
                player.level().sendBlockUpdated(pos, player.level().getBlockState(pos), player.level().getBlockState(pos), 3);
                source.sendSuccess(() -> Component.literal("Transferred " + received + " FE from desk charger to radio at " + pos), true);
                return 1;
            } catch (Exception e) {
                source.sendFailure(Component.literal("Error: " + e.getMessage()));
                return 0;
            }
        }
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

    /**
     * Fills energy into a block's energy capability for testing.
     * If coords is null, uses the block under the invoking player.
     */
    private static int fillBlockEnergy(CommandSourceStack source, int[] coords, int amount) {
        try {
            ServerPlayer player = source.getPlayerOrException();

            net.minecraft.core.BlockPos pos;
            if (coords == null) {
                pos = player.blockPosition().below();
            } else {
                pos = new net.minecraft.core.BlockPos(coords[0], coords[1], coords[2]);
            }

            // Try to find a BlockEntity at pos and if it's a DeskChargerBlockEntity, add directly to its storage
            var be = player.level().getBlockEntity(pos);
            if (be instanceof com.arrl.radiocraft.common.blockentities.DeskChargerBlockEntity desk) {
                // Directly set energy amount (clamped to capacity)
                int capacity = desk.energyStorage.getMaxEnergyStored();
                int newEnergy = Math.max(0, Math.min(capacity, amount));
                desk.energyStorage.setEnergy(newEnergy);
                desk.setChanged();
                // Notify neighbors/clients of the change
                player.level().sendBlockUpdated(pos, player.level().getBlockState(pos), player.level().getBlockState(pos), 3);
                source.sendSuccess(() -> Component.literal("Set desk charger energy to " + newEnergy + " FE at " + pos), true);
                return 1;
            }

            net.neoforged.neoforge.energy.IEnergyStorage energyStorage = player.level().getCapability(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK, pos, null);
            if (energyStorage == null) {
                source.sendFailure(Component.literal("No energy-capable block found at position."));
                return 0;
            }

            int added = energyStorage.receiveEnergy(amount, false);
            source.sendSuccess(() -> Component.literal("Added " + added + " FE to block at " + pos), true);
            // If there is a block entity, mark changed
            if (be != null) be.setChanged();
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    private static int blockInfo(CommandSourceStack source, int[] coords) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(coords[0], coords[1], coords[2]);
            var be = player.level().getBlockEntity(pos);
            if (be instanceof com.arrl.radiocraft.common.blockentities.DeskChargerBlockEntity desk) {
                int energy = desk.energyStorage.getEnergyStored();
                int cap = desk.energyStorage.getMaxEnergyStored();
                StringBuilder msg = new StringBuilder();
                msg.append("DeskCharger at ").append(pos).append(" energy=").append(energy).append(" / ").append(cap).append(" | infinite=").append(desk.isInfinite());

                var stack = desk.inventory.getStackInSlot(0);
                if (stack != null && !stack.isEmpty()) {
                    var radioCap = stack.getCapability(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.ITEM);
                    if (radioCap != null) {
                        msg.append(" | Radio: ").append(radioCap.getEnergyStored()).append(" / ").append(radioCap.getMaxEnergyStored());
                    }
                }

                source.sendSuccess(() -> Component.literal(msg.toString()), false);
                return 1;
            }
            source.sendFailure(Component.literal("No DeskChargerBlockEntity at position."));
            return 0;
        } catch (Exception e) {
            source.sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    private static int infiniteMode(CommandSourceStack source, boolean on, int[] coords) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            net.minecraft.core.BlockPos pos;
            if (coords == null) pos = player.blockPosition().below(); else pos = new net.minecraft.core.BlockPos(coords[0], coords[1], coords[2]);

            var be = player.level().getBlockEntity(pos);
            if (!(be instanceof com.arrl.radiocraft.common.blockentities.DeskChargerBlockEntity desk)) {
                source.sendFailure(Component.literal("No DeskChargerBlockEntity at position."));
                return 0;
            }

            desk.setInfinite(on);
            desk.setChanged();
            player.level().sendBlockUpdated(pos, player.level().getBlockState(pos), player.level().getBlockState(pos), 3);
            source.sendSuccess(() -> Component.literal("Set desk charger infinite=" + on + " at " + pos), true);
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }
}
