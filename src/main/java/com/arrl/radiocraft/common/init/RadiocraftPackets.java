package com.arrl.radiocraft.common.init;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.network.RadiocraftPacket;
import com.arrl.radiocraft.common.network.packets.ClientboundNoisePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class RadiocraftPackets {

	private static final String PROTOCOL_VERSION = "1";
	public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
			Radiocraft.location("main"),
			() -> PROTOCOL_VERSION,
			PROTOCOL_VERSION::equals,
			PROTOCOL_VERSION::equals
	);

	public static void registerPackets() {
		int id = 0;
		INSTANCE.registerMessage(id++, ClientboundNoisePacket.class, ClientboundNoisePacket::encode, ClientboundNoisePacket::decode, ClientboundNoisePacket::handle);
	}

	public static void sendToPlayer(RadiocraftPacket packet, ServerPlayer player) {
		INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), packet);
	}

	public static void sendToAllPlayers(RadiocraftPacket packet) {
		INSTANCE.send(PacketDistributor.ALL.noArg(), packet);
	}

	public static void sendToServer(RadiocraftPacket packet) {
		INSTANCE.sendToServer(packet);
	}

	public static void sendToLevel(RadiocraftPacket packet, ServerLevel level) {
		INSTANCE.send(PacketDistributor.DIMENSION.with(level::dimension), packet);
	}

}
