package com.arrl.radiocraft.common.init;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.network.RadiocraftPacket;
import com.arrl.radiocraft.common.network.packets.CWBufferPacket;
import com.arrl.radiocraft.common.network.packets.clientbound.CAntennaWirePacket;
import com.arrl.radiocraft.common.network.packets.clientbound.CNoisePacket;
import com.arrl.radiocraft.common.network.packets.serverbound.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.chunk.LevelChunk;
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
		INSTANCE.registerMessage(id++, CNoisePacket.class, CNoisePacket::encode, CNoisePacket::decode, CNoisePacket::handle);
		INSTANCE.registerMessage(id++, STogglePacket.class, STogglePacket::encode, STogglePacket::decode, STogglePacket::handle);
		INSTANCE.registerMessage(id++, CAntennaWirePacket.class, CAntennaWirePacket::encode, CAntennaWirePacket::decode, CAntennaWirePacket::handle);
		INSTANCE.registerMessage(id++, SRadioPTTPacket.class, SRadioPTTPacket::encode, SRadioPTTPacket::decode, SRadioPTTPacket::handle);
		INSTANCE.registerMessage(id++, SRadioSSBPacket.class, SRadioSSBPacket::encode, SRadioSSBPacket::decode, SRadioSSBPacket::handle);
		INSTANCE.registerMessage(id++, SRadioCWPacket.class, SRadioCWPacket::encode, SRadioCWPacket::decode, SRadioCWPacket::handle);
		INSTANCE.registerMessage(id++, SFrequencyPacket.class, SFrequencyPacket::encode, SFrequencyPacket::decode, SFrequencyPacket::handle);
		INSTANCE.registerMessage(id++, CWBufferPacket.class, CWBufferPacket::encode, CWBufferPacket::decode, CWBufferPacket::handle);
	}

	public static void sendToPlayer(RadiocraftPacket packet, ServerPlayer player) {
		INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), packet);
	}

	public static void sendToAllPlayers(RadiocraftPacket packet) {
		INSTANCE.send(PacketDistributor.ALL.noArg(), packet);
	}

	public static void sendToTrackingChunk(RadiocraftPacket packet, LevelChunk chunk) {
		INSTANCE.send(PacketDistributor.TRACKING_CHUNK.with(() -> chunk), packet);
	}

	public static void sendToServer(RadiocraftPacket packet) {
		INSTANCE.sendToServer(packet);
	}

	public static void sendToLevel(RadiocraftPacket packet, ServerLevel level) {
		INSTANCE.send(PacketDistributor.DIMENSION.with(level::dimension), packet);
	}

}
