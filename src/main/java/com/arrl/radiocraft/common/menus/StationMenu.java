package com.arrl.radiocraft.common.menus;

import com.arrl.radiocraft.common.blockentities.StationBus;
import com.arrl.radiocraft.common.network.serverbound.SStationPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

/** A station block's menu, which carries out what its screen sends in an {@link SStationPacket}. */
public interface StationMenu {

	BlockPos getBlockPos();

	/** Runs on the server for the player with this menu open. */
	void handleAction(ServerPlayer player, String action, CompoundTag data);

	/** Why a terminal could not send: no radio interface, no callsign on it, or no radio on. */
	static String failureKey(StationBus bus) {
		if(!bus.hasInterface())
			return "screen.radiocraft.station.no_interface";
		if(bus.callsign().isEmpty())
			return "screen.radiocraft.station.set_callsign_first";
		return bus.hasRadioOn() ? "screen.radiocraft.station.send_failed" : "screen.radiocraft.station.no_radio";
	}
}
