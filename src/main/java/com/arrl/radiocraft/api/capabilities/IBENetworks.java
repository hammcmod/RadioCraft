package com.arrl.radiocraft.api.capabilities;

import com.arrl.radiocraft.api.benetworks.BENetwork;
import com.arrl.radiocraft.api.benetworks.BENetworkObject;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.AutoRegisterCapability;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Capability attached to {@link Level} containing every {@link BENetwork} for {@link BENetworkObject}s present within
 * that {@link Level}.
 */
@AutoRegisterCapability
public interface IBENetworks {

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

}
