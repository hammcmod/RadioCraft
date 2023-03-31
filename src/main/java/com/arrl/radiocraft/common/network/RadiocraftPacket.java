package com.arrl.radiocraft.common.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent.Context;

import java.util.function.Supplier;

public interface RadiocraftPacket {

	void encode(FriendlyByteBuf buffer);
	void handle(Supplier<Context> context);

}
