package com.arrl.radiocraft.common.capabilities;

import com.arrl.radiocraft.api.capabilities.IAntennaWireHolderCapability;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;

public class AntennaWireHolderCapability implements IAntennaWireHolderCapability {

	public static HashMap<String, BlockPos> playerMap;

	public AntennaWireHolderCapability() {
		if (playerMap == null) {
			playerMap = new HashMap<>();
		}
	}

	private static String getPlayerUUID(Player player) {
		return player.getUUID().toString();
	}

	@Override
	public BlockPos getHeldPos(Player player) {
		String uuid = getPlayerUUID(player);
		/*
		Radiocraft.LOGGER.info("All wires in the playermap");
		playerMap.forEach((p, b) -> {
			if (b != null) {
				Radiocraft.LOGGER.info(p + " has a wire at " + b);
			} else {
				Radiocraft.LOGGER.info(p + " has a null wire");
			}
		});
		*/
		if (playerMap.get(uuid) == null) {
			return null;
		}
		return playerMap.get(uuid);
	}

	@Override
	public void setHeldPos(Player player, BlockPos pos) {
		if (player == null) {
			return;
		}
		String uuid = getPlayerUUID(player);
		while (playerMap.containsKey(uuid)) {
			playerMap.remove(uuid);
		}
		if (pos != null) {
			playerMap.put(uuid, pos);
		}
	}
}
