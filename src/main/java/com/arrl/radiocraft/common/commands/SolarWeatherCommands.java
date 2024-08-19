package com.arrl.radiocraft.common.commands;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.init.RadiocraftData;
import com.arrl.radiocraft.common.radio.solar.SolarEvent.SolarEventInstance;
import com.arrl.radiocraft.common.radio.solar.SolarEventManager;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class SolarWeatherCommands {

	public static final LiteralArgumentBuilder<CommandSourceStack> BUILDER =
			Commands.literal("solarweather").executes(command -> getCallsign(command.getSource()));

	public static int getCallsign(CommandSourceStack source) throws CommandSyntaxException {
		SolarEventInstance event = SolarEventManager.getEvent(source.getLevel());
		source.sendSuccess(() -> Component.translatable(Radiocraft.translationKey("commands", "solarweather.success"),
				RadiocraftData.SOLAR_EVENTS.getKey(event.getEvent()),
				event.getTicks(),
				event.getDuration(),
				event.getEvent().getNoise()
				).withStyle(ChatFormatting.GREEN), false);
		return 1;
	}
}
