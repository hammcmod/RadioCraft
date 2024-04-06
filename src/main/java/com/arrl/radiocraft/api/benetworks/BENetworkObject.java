package com.arrl.radiocraft.api.benetworks;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.api.capabilities.IBENetworks;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

/**
 * Represents the presence of a {@link BlockEntity} within a {@link BENetwork}.
 */
public class BENetworkObject {

    public static final ResourceLocation DEFAULT_TYPE = Radiocraft.location("default");
    protected final Map<Direction, BENetwork> networks = new HashMap<>();

    public BENetwork getNetwork(@NotNull Direction side) {
        return networks.get(side);
    }

    public void setNetwork(@NotNull Direction side, BENetwork network) {
        networks.put(side, network);
    }

    public void clearNetworks() {
        for(BENetwork network : networks.values()) {
            network.remove(this);
        }
    }

    public ResourceLocation getType() {
        return DEFAULT_TYPE;
    }

    public void save(CompoundTag nbt) {
        CompoundTag networksTag = new CompoundTag();
        for(Entry<Direction, BENetwork> entry : networks.entrySet())
            networksTag.putUUID(entry.getKey().getName(), entry.getValue().getUUID());
        nbt.put("networks", networksTag);
    }

    public void load(IBENetworks cap, CompoundTag nbt) {
        CompoundTag tag = nbt.getCompound("networks");
        for(String key : tag.getAllKeys()) {
            BENetwork network = cap.getNetwork(UUID.fromString(key));
            network.add(this);
            networks.put(Direction.byName(key), network);
        }
    }

}
