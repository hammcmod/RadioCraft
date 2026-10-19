package com.arrl.radiocraft.api.benetworks;

import com.arrl.radiocraft.api.capabilities.IBENetworks;
import com.arrl.radiocraft.common.be_networks.WireUtils;
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
     * cap, call {@link INetworkObjectProvider#initNetworkObject(IBENetworks, BlockPos)} to create one, then connect it
     * to any wire runs touching the block. Creation normally happens from {@code BlockEntity#onLoad}, which runs the
     * tick after placement and again after a chunk or world reload, so this is also what re-links devices after a
     * restart (networks are kept in memory only).
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
            if(networkObject == null) {
                networkObject = initNetworkObject(cap, pos);
                onNetworkObjectCreated(level, pos, networkObject);
                WireUtils.connectToWires(level, pos);
            }
            return networkObject;
        }

        return null;
    }

    /**
     * Called once for a freshly created {@link BENetworkObject}, before it is connected to any wires. Override to
     * restore state that lives on the object (for example a radio's power switch) from the block entity's saved data.
     *
     * @param level The {@link Level} the object is in.
     * @param pos The {@link BlockPos} of the object.
     * @param networkObject The object that was just created.
     */
    default void onNetworkObjectCreated(Level level, BlockPos pos, BENetworkObject networkObject) {}

    /**
     * Initialize a new {@link BENetworkObject} for this {@link INetworkObjectProvider}.
     * @param cap The {@link IBENetworks} capability to use.
     * @param pos The {@link BlockPos} of this {@link INetworkObjectProvider}.
     * @return The newly created {@link BENetworkObject}.
     */
    default BENetworkObject initNetworkObject(IBENetworks cap, BlockPos pos) {
        BENetworkObject networkObject = createNetworkObject();
        cap.setObject(pos, networkObject);
        return networkObject;
    }

}
