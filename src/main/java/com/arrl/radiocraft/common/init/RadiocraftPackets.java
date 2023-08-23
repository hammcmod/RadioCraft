package com.arrl.radiocraft.common.init;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.network.RadiocraftPacket;
import com.arrl.radiocraft.common.network.packets.*;
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
		INSTANCE.registerMessage(id++, ClientboundNoisePacket.class, ClientboundNoisePacket::encode, ClientboundNoisePacket::decode, ClientboundNoisePacket::handle);
		INSTANCE.registerMessage(id++, ServerboundTogglePacket.class, ServerboundTogglePacket::encode, ServerboundTogglePacket::decode, ServerboundTogglePacket::handle);
		INSTANCE.registerMessage(id++, ClientboundAntennaWirePacket.class, ClientboundAntennaWirePacket::encode, ClientboundAntennaWirePacket::decode, ClientboundAntennaWirePacket::handle);
		INSTANCE.registerMessage(id++, ServerboundRadioPTTPacket.class, ServerboundRadioPTTPacket::encode, ServerboundRadioPTTPacket::decode, ServerboundRadioPTTPacket::handle);
		INSTANCE.registerMessage(id++, ServerboundRadioSSBPacket.class, ServerboundRadioSSBPacket::encode, ServerboundRadioSSBPacket::decode, ServerboundRadioSSBPacket::handle);
		INSTANCE.registerMessage(id++, ServerboundRadioCWPacket.class, ServerboundRadioCWPacket::encode, ServerboundRadioCWPacket::decode, ServerboundRadioCWPacket::handle);
		INSTANCE.registerMessage(id++, ServerboundFrequencyPacket.class, ServerboundFrequencyPacket::encode, ServerboundFrequencyPacket::decode, ServerboundFrequencyPacket::handle);
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
