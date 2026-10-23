package com.arrl.radiocraft.common.commands;

import com.arrl.radiocraft.common.items.VHFHandheldItem;
import com.arrl.radiocraft.api.benetworks.BENetworkObject;
import com.arrl.radiocraft.api.benetworks.PowerNetworkObject;
import com.arrl.radiocraft.api.capabilities.IBENetworks;
import com.arrl.radiocraft.common.blockentities.DeskChargerBlockEntity;
import com.arrl.radiocraft.common.init.RadiocraftItems;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

import java.util.function.BiFunction;

/**
 * Commands for managing energy in RadioCraft items (batteries, handhelds) and blocks (desk charger, and anything on
 * a power network: batteries, charge controllers, radios). Positions are block position arguments, so tab completion
 * offers the block being looked at, like {@code /setblock}; without a position the block under the player is used.
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
        // /rcenergy fillblock <amount> [pos] - set a block's stored energy (desk charger, battery, charge controller, radio)
        BUILDER.then(Commands.literal("fillblock")
            .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                .executes(ctx -> fillBlockEnergy(ctx.getSource(), posUnderPlayer(ctx), IntegerArgumentType.getInteger(ctx, "amount")))
                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                    .executes(ctx -> fillBlockEnergy(ctx.getSource(), BlockPosArgument.getLoadedBlockPos(ctx, "pos"), IntegerArgumentType.getInteger(ctx, "amount"))))));

        // /rcenergy blockinfo [pos]
        BUILDER.then(Commands.literal("blockinfo")
            .executes(ctx -> blockInfo(ctx.getSource(), posUnderPlayer(ctx)))
            .then(Commands.argument("pos", BlockPosArgument.blockPos())
                .executes(ctx -> blockInfo(ctx.getSource(), BlockPosArgument.getLoadedBlockPos(ctx, "pos")))));

        // /rcenergy transfer <amount> [pos] - immediate transfer from charger to radio in slot
        BUILDER.then(Commands.literal("transfer")
            .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                .executes(ctx -> transferToRadio(ctx.getSource(), posUnderPlayer(ctx), IntegerArgumentType.getInteger(ctx, "amount")))
                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                    .executes(ctx -> transferToRadio(ctx.getSource(), BlockPosArgument.getLoadedBlockPos(ctx, "pos"), IntegerArgumentType.getInteger(ctx, "amount"))))));

        // /rcenergy infinite <on|off> [pos] - toggle infinite mode for a desk charger
        BUILDER.then(Commands.literal("infinite")
            .then(onOff("on", true))
            .then(onOff("off", false)));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> onOff(String word, boolean on) {
        return Commands.literal(word)
            .executes(ctx -> infiniteMode(ctx.getSource(), on, posUnderPlayer(ctx)))
            .then(Commands.argument("pos", BlockPosArgument.blockPos())
                .executes(ctx -> infiniteMode(ctx.getSource(), on, BlockPosArgument.getLoadedBlockPos(ctx, "pos"))));
    }

    private static BlockPos posUnderPlayer(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        return ctx.getSource().getPlayerOrException().blockPosition().below();
    }

    private static int transferToRadio(CommandSourceStack source, BlockPos pos, int amount) {
        try {
            ServerPlayer player = source.getPlayerOrException();

            var be = player.level().getBlockEntity(pos);
            if (!(be instanceof DeskChargerBlockEntity desk)) {
                source.sendFailure(Component.literal("No DeskChargerBlockEntity at position."));
                return 0;
            }

            var radio = desk.inventory.getStackInSlot(0);
            if (radio == null || radio.isEmpty()) {
                source.sendFailure(Component.literal("No radio in charger slot."));
                return 0;
            }

            var radioCap = radio.getCapability(Capabilities.EnergyStorage.ITEM);
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
            source.sendSuccess(() -> Component.literal("Transferred " + received + " FE from desk charger to radio at " + pos.toShortString()), true);
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
        return withItemEnergy(source, (itemInHand, energyStorage) -> {
            int currentEnergy = energyStorage.getEnergyStored();
            if (currentEnergy == 0) {
                source.sendFailure(Component.literal("This item is already empty!"));
                return 0;
            }
            int drained = energyStorage.extractEnergy(currentEnergy, false);
            source.sendSuccess(() -> energyMessage("Drained ", drained, " from ", itemInHand), true);
            return 1;
        });
    }

    /**
     * Fills the item in the player's main hand to maximum capacity.
     */
    private static int fillEnergy(CommandSourceStack source) {
        return withItemEnergy(source, (itemInHand, energyStorage) -> {
            int needed = energyStorage.getMaxEnergyStored() - energyStorage.getEnergyStored();
            if (needed == 0) {
                source.sendFailure(Component.literal("This item is already fully charged!"));
                return 0;
            }
            int filled = energyStorage.receiveEnergy(needed, false);
            source.sendSuccess(() -> energyMessage("Added ", filled, " to ", itemInHand), true);
            return 1;
        });
    }

    private static Component energyMessage(String verb, int amount, String joiner, ItemStack item) {
        double joules = amount / 2.5; // 2.5 FE = 1 J
        return Component.literal(verb)
                .withStyle(ChatFormatting.GREEN)
                .append(Component.literal(String.format("%d FE (%.0f J)", amount, joules)).withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(joiner).withStyle(ChatFormatting.GREEN))
                .append(item.getDisplayName())
                .append(Component.literal("!").withStyle(ChatFormatting.GREEN));
    }

    private static int withItemEnergy(CommandSourceStack source, BiFunction<ItemStack, IEnergyStorage, Integer> action) {
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
            return action.apply(itemInHand, energyStorage);
        } catch (Exception e) {
            source.sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    /**
     * Shows detailed energy information about the item in the player's main hand.
     */
    private static int showEnergyInfo(CommandSourceStack source) {
        return withItemEnergy(source, (itemInHand, energyStorage) -> {
            int stored = energyStorage.getEnergyStored();
            int max = energyStorage.getMaxEnergyStored();
            double percentage = max > 0 ? (double) stored / max * 100.0 : 0.0;

            // Convert FE to Joules (2.5 FE = 1J) and to Watt-hours (3600 J = 1 Wh)
            double storedJoules = stored / 2.5;
            double maxJoules = max / 2.5;
            double storedWh = storedJoules / 3600.0;
            double maxWh = maxJoules / 3600.0;

            final String itemType;
            if (itemInHand.getItem() == RadiocraftItems.SMALL_BATTERY.get()) {
                itemType = "Small Battery";
            } else if (itemInHand.getItem() instanceof VHFHandheldItem) {
                itemType = "VHF Handheld Radio";
            } else {
                itemType = "Unknown";
            }

            source.sendSuccess(() -> Component.literal("=== Energy Info ===").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
            source.sendSuccess(() -> Component.literal("Item: ").withStyle(ChatFormatting.GRAY)
                    .append(itemInHand.getDisplayName().copy().withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(" (" + itemType + ")").withStyle(ChatFormatting.DARK_GRAY)), false);
            source.sendSuccess(() -> Component.literal("Energy: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(String.format("%d / %d FE", stored, max)).withStyle(ChatFormatting.AQUA)), false);
            source.sendSuccess(() -> Component.literal("Joules: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(String.format("%.0f / %.0f J", storedJoules, maxJoules)).withStyle(ChatFormatting.AQUA)), false);
            source.sendSuccess(() -> Component.literal("Watt-hours: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(String.format("%.2f / %.2f Wh", storedWh, maxWh)).withStyle(ChatFormatting.AQUA)), false);

            ChatFormatting percentColor = percentage > 75 ? ChatFormatting.GREEN : percentage > 50 ? ChatFormatting.YELLOW
                    : percentage > 25 ? ChatFormatting.GOLD : ChatFormatting.RED;
            source.sendSuccess(() -> Component.literal("Charge: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(String.format("%.1f%%", percentage)).withStyle(percentColor)), false);
            return 1;
        });
    }

    /**
     * Sets the energy stored in a block: a desk charger's own store, anything on a power network (battery, charge
     * controller, radio: the network object's storage, clamped to its capacity), or any block with an energy capability.
     */
    private static int fillBlockEnergy(CommandSourceStack source, BlockPos pos, int amount) {
        try {
            ServerLevel level = source.getLevel();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof DeskChargerBlockEntity desk) {
                int newEnergy = Math.max(0, Math.min(desk.energyStorage.getMaxEnergyStored(), amount));
                desk.energyStorage.setEnergy(newEnergy);
                desk.setChanged();
                level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);
                source.sendSuccess(() -> Component.literal("Set desk charger energy to " + newEnergy + " FE at " + pos.toShortString()), true);
                return 1;
            }

            BENetworkObject object = IBENetworks.getObject(level, pos);
            if (object instanceof PowerNetworkObject power) {
                int newEnergy = Math.max(0, Math.min(power.getStorage().getMaxEnergyStored(), amount));
                power.getStorage().setEnergy(newEnergy);
                if (be != null) be.setChanged();
                source.sendSuccess(() -> Component.literal("Set " + object.getType().getPath() + " energy to " + newEnergy + " / "
                        + power.getStorage().getMaxEnergyStored() + " FE at " + pos.toShortString()), true);
                return 1;
            }

            IEnergyStorage energyStorage = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, null);
            if (energyStorage == null) {
                source.sendFailure(Component.literal("No energy-capable block at " + pos.toShortString() + "."));
                return 0;
            }
            int added = energyStorage.receiveEnergy(amount, false);
            source.sendSuccess(() -> Component.literal("Added " + added + " FE to block at " + pos.toShortString()), true);
            if (be != null) be.setChanged();
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    private static int blockInfo(CommandSourceStack source, BlockPos pos) {
        try {
            ServerLevel level = source.getLevel();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof DeskChargerBlockEntity desk) {
                StringBuilder msg = new StringBuilder();
                msg.append("DeskCharger at ").append(pos.toShortString()).append(" energy=").append(desk.energyStorage.getEnergyStored())
                        .append(" / ").append(desk.energyStorage.getMaxEnergyStored()).append(" | infinite=").append(desk.isInfinite());
                var stack = desk.inventory.getStackInSlot(0);
                if (stack != null && !stack.isEmpty()) {
                    var radioCap = stack.getCapability(Capabilities.EnergyStorage.ITEM);
                    if (radioCap != null)
                        msg.append(" | Radio: ").append(radioCap.getEnergyStored()).append(" / ").append(radioCap.getMaxEnergyStored());
                }
                source.sendSuccess(() -> Component.literal(msg.toString()), false);
                return 1;
            }

            BENetworkObject object = IBENetworks.getObject(level, pos);
            if (object instanceof PowerNetworkObject power) {
                String msg = object.getType().getPath() + " at " + pos.toShortString() + " energy=" + power.getStorage().getEnergyStored()
                        + " / " + power.getStorage().getMaxEnergyStored() + " FE";
                source.sendSuccess(() -> Component.literal(msg), false);
                return 1;
            }
            source.sendFailure(Component.literal("No energy block at " + pos.toShortString() + "."));
            return 0;
        } catch (Exception e) {
            source.sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    private static int infiniteMode(CommandSourceStack source, boolean on, BlockPos pos) {
        try {
            ServerLevel level = source.getLevel();
            var be = level.getBlockEntity(pos);
            if (!(be instanceof DeskChargerBlockEntity desk)) {
                source.sendFailure(Component.literal("No desk charger at " + pos.toShortString() + " (infinite mode is for the desk charger; use fillblock for batteries and radios)."));
                return 0;
            }

            desk.setInfinite(on);
            desk.setChanged();
            level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);
            source.sendSuccess(() -> Component.literal("Set desk charger infinite=" + on + " at " + pos.toShortString()), true);
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }
}
