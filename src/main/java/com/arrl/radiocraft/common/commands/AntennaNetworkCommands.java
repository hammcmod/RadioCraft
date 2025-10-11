package com.arrl.radiocraft.common.commands;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.api.antenna.IAntenna;
import com.arrl.radiocraft.common.radio.antenna.AntennaNetwork;
import com.arrl.radiocraft.common.radio.antenna.networks.AntennaNetworkManager;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public class AntennaNetworkCommands {
    public static final LiteralArgumentBuilder<CommandSourceStack> BUILDER = Commands.literal("antenna")
            .executes(command -> listAntennas(command.getSource()));

    private static String toStringEvenNull(Object o) {
        return o == null ? "null" : o.toString();
    }

    private static int listAntennas(CommandSourceStack source) {

        ArrayList<String> antennaInfos = new ArrayList<>();
        antennaInfos.add("Antenna Networks:");

        for (Map.Entry<ResourceLocation, AntennaNetwork> entry: AntennaNetworkManager.networks.entrySet()) {
            String entryName = entry.getKey().toString();

            Set<IAntenna> antennas = entry.getValue().allAntennas();

            Radiocraft.LOGGER.info(String.format("Antenna Network %s has %d antennas", entryName, antennas.size()));
            Radiocraft.LOGGER.info(antennas.toString());



            List<String> antennaData = antennas.stream().map((antenna) -> toStringEvenNull(antenna.getAntennaPos())).toList();

            antennaInfos.add(String.format("%s %s", entryName, antennaData));
        }

        Supplier<Component> combined = () -> Component.literal(String.join("\n", antennaInfos));

        source.sendSuccess(combined, true);
        return 1;
    }
}
