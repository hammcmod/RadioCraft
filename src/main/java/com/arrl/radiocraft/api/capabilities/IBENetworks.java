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
 * Capability for antenna networks only. Power networks now use SavedData.
 * This interface is kept specifically for antenna/coaxial cable networks which have different requirements.
 */
public interface IBENetworks extends INBTSerializable<CompoundTag> {

    BENetworkObject getObject(@NotNull BlockPos pos);
    void setObject(@NotNull BlockPos pos, @NotNull BENetworkObject object);
    void removeObject(@NotNull BlockPos pos);
    BENetwork getNetwork(UUID uuid);
    void addNetwork(BENetwork network);
    void removeNetwork(BENetwork network);
    void tickNetworkObjects(Level level);

    // Static helper methods for antenna networks only
    static BENetworkObject getObject(@NotNull Level level, @NotNull BlockPos pos) {
        return get(level, pos) != null ? get(level, pos).getObject(pos) : null;
    }

    static void setObject(@NotNull Level level, @NotNull BlockPos pos, @NotNull BENetworkObject object) {
        IBENetworks cap = get(level, pos);
        if(cap != null)
            cap.setObject(pos, object);
    }

    static void removeObject(@NotNull Level level, @NotNull BlockPos pos) {
        IBENetworks cap = get(level, pos);
        if(cap != null)
            cap.removeObject(pos);
    }

    static BENetwork getNetwork(@NotNull Level level, UUID uuid) {
        IBENetworks cap = get(level, BlockPos.ZERO);
        return cap != null ? cap.getNetwork(uuid) : null;
    }

    static void addNetwork(@NotNull Level level, BENetwork network) {
        IBENetworks cap = get(level, BlockPos.ZERO);
        if(cap != null)
            cap.addNetwork(network);
    }

    static void removeNetwork(@NotNull Level level, BENetwork network) {
        IBENetworks cap = get(level, BlockPos.ZERO);
        if(cap != null)
            cap.removeNetwork(network);
    }

    static IBENetworks get(@NotNull Level level, @NotNull BlockPos pos) {
        return RadiocraftCapabilities.BE_NETWORKS.getCapability(level, pos, null, null, null);
    }

    static IBENetworks get(@NotNull Level level) {
        return get(level, BlockPos.ZERO);
    }
}