package com.arrl.radiocraft.api.benetworks;

import com.arrl.radiocraft.Radiocraft;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents the presence of a {@link BlockEntity} within a {@link BENetwork}.
 */
public class BENetworkObject {

    public static final ResourceLocation DEFAULT_TYPE = Radiocraft.location("default");
    protected final Map<Direction, BENetwork<?>> networks = new HashMap<>();

    public BENetwork<?> getNetwork(@NotNull Direction side) {
        return networks.get(side);
    }

    public void setNetwork(@NotNull Direction side, BENetwork<?> network) {
        networks.put(side, network);
    }

    public ResourceLocation getType() {
        return DEFAULT_TYPE;
    }

    public void save(CompoundTag nbt) {
        nbt.putString("type", getType().toString());
    }

    public void load(CompoundTag nbt) {}

}
