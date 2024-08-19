package com.arrl.radiocraft.api.capabilities;

import com.arrl.radiocraft.api.benetworks.BENetwork;
import com.arrl.radiocraft.api.benetworks.BENetworkObject;
import com.arrl.radiocraft.common.capabilities.RadiocraftCapabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.util.INBTSerializable;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Capability attached to {@link Level} containing every {@link BENetwork} for {@link BENetworkObject}s present within
 * that {@link Level}.
 */
public interface IBENetworks extends INBTSerializable<CompoundTag> {

    /**
     * Grab a {@link BENetworkObject} by its {@link BlockPos}.
     *
     * @param pos The {link BlockPos} to check
     *
     * @return {@link BENetworkObject} if an object is present, otherwise false.
     */
    BENetworkObject getObject(@NotNull BlockPos pos);

    /**
     * Set the {@link BENetworkObject} present at a given {@link BlockPos}.
     *
     * @param pos The {@link BlockPos} to set.
     * @param object The {@link BENetworkObject} to place in that position.
     */
    void setObject(@NotNull BlockPos pos, @NotNull BENetworkObject object);

    /**
     * Remove a {@link BENetworkObject} from this level and remove it from all of it's {@link BENetwork}s.
     *
     * @param pos The {@link BlockPos} of the {@link BENetworkObject} being removed.
     */
    void removeObject(@NotNull BlockPos pos);

    /**
     * Grab a {@link BENetwork} by its {@link UUID}.
     *
     * @param uuid The {@link UUID} of the desired network.
     *
     * @return The first {@link BENetwork} with a matching {@link UUID}, or null if none were found.
     */
    BENetwork getNetwork(UUID uuid);

    /**
     * Add a new {@link BENetwork} to the level.
     *
     * @param network The {@link BENetwork} to add.
     */
    void addNetwork(BENetwork network);

    /**
     * Removes a {@link BENetwork} from the level.
     * @param network The {@link BENetwork} to remove.
     */
    void removeNetwork(BENetwork network);

    /**
     * Tick all {@link BENetworkObject}s.
     */
    void tickNetworkObjects(Level level);

    /**
     * Grab a {@link BENetworkObject} by its {@link BlockPos}.
     *
     * @param level The {@link Level} to grab {@link IBENetworks} from.
     * @param pos The {link BlockPos} to check
     *
     * @return {@link BENetworkObject} if an object is present, otherwise false.
     */
    static BENetworkObject getObject(@NotNull Level level, @NotNull BlockPos pos) {
        IBENetworks cap = get(level);
        return cap != null ? cap.getObject(pos) : null;
    }

    /**
     * Set the {@link BENetworkObject} present at a given {@link BlockPos}.
     *
     * @param level The {@link Level} to grab {@link IBENetworks} from.
     * @param pos The {@link BlockPos} to set.
     * @param object The {@link BENetworkObject} to place in that position.
     */
    static void setObject(@NotNull Level level, @NotNull BlockPos pos, @NotNull BENetworkObject object) {
        IBENetworks cap = get(level);
        if(cap != null)
            cap.setObject(pos, object);
    }

    /**
     * Remove a {@link BENetworkObject} from this level and remove it from all of it's {@link BENetwork}s.
     *
     * @param level The {@link Level} to grab {@link IBENetworks} from.
     * @param pos The {@link BlockPos} of the {@link BENetworkObject} being removed.
     */
    static void removeObject(@NotNull Level level, @NotNull BlockPos pos) {
        IBENetworks cap = get(level);
        if(cap != null)
            cap.removeObject(pos);
    }

    /**
     * Grab a {@link BENetwork} by its {@link UUID}.
     *
     * @param level The {@link Level} to grab {@link IBENetworks} from.
     * @param uuid The {@link UUID} of the desired network.
     *
     * @return The first {@link BENetwork} with a matching {@link UUID}, or null if none were found.
     */
    static BENetwork getNetwork(@NotNull Level level, UUID uuid) {
        IBENetworks cap = get(level);
        return cap != null ? cap.getNetwork(uuid) : null;
    }

    /**
     * Add a new {@link BENetwork} to the level.
     *
     * @param level The {@link Level} to grab {@link IBENetworks} from.
     * @param network The {@link BENetwork} to add.
     */
    static void addNetwork(@NotNull Level level, BENetwork network) {
        IBENetworks cap = get(level);
        if(cap != null)
            cap.addNetwork(network);
    }

    /**
     * Removes a {@link BENetwork} from the level.
     *
     * @param level The {@link Level} to grab {@link IBENetworks} from.
     * @param network The {@link BENetwork} to remove.
     */
    static void removeNetwork(@NotNull Level level, BENetwork network) {
        IBENetworks cap = get(level);
        if(cap != null)
            cap.removeNetwork(network);
    }


    static IBENetworks get(@NotNull Level level) {
        return null;// level.getCapability(RadiocraftCapabilities.BE_NETWORKS).orElse(null);
    }

    static IBENetworks get(@NotNull Level level, @NotNull BlockPos pos) {
        return RadiocraftCapabilities.BE_NETWORKS.getCapability(level, pos, null, null, null);
    }

}
