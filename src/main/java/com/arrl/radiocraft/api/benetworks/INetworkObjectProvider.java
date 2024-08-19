package com.arrl.radiocraft.api.benetworks;

import com.arrl.radiocraft.api.capabilities.IBENetworks;
import com.arrl.radiocraft.common.capabilities.RadiocraftCapabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Interface representing a {@link BlockEntity} which is able to connect to {@link BENetwork}s.
 */
public interface INetworkObjectProvider {

    /**
     * Create the {@link BENetworkObject} for this {@link INetworkObjectProvider} and handle its setup.
     *
     * @return A new {@link BENetworkObject}.
     */
    BENetworkObject createNetworkObject();

    /**
     * Attempt to grab the {@link BENetworkObject} for this {@link INetworkObjectProvider}. If one is not found on the
     * cap, call {@link INetworkObjectProvider#initNetworkObject(IBENetworks, BlockPos)} and attempt to create a one.
     *
     * @param level The {@link Level} this {@link INetworkObjectProvider} is in.
     * @param pos This {@link INetworkObjectProvider}'s position.
     *
     * @return The {@link BENetworkObject} for level, pos, or the created one if there wasn't one present.
     */
    default BENetworkObject getNetworkObject(Level level, BlockPos pos) {

        IBENetworks cap = RadiocraftCapabilities.BE_NETWORKS.getCapability(level, pos, null, null, null);

        if(cap != null) {
            BENetworkObject networkObject = cap.getObject(pos);
            return networkObject != null ? networkObject : initNetworkObject(cap, pos);
        }

        return null;
    }

    default BENetworkObject initNetworkObject(IBENetworks cap, BlockPos pos) {
        BENetworkObject networkObject = createNetworkObject();
        cap.setObject(pos, networkObject);
        return networkObject;
    }

}
