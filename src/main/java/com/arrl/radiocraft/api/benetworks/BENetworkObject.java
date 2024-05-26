package com.arrl.radiocraft.api.benetworks;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.api.capabilities.IBENetworks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
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
    protected final Level level;
    protected final BlockPos pos;

    public BENetworkObject(Level level, BlockPos pos) {
        this.level = level;
        this.pos = pos;
    }

    public void tick(Level level, BlockPos pos) {}

    public BENetwork getNetwork(@NotNull Direction side) {
        return networks.get(side);
    }

    public void setNetwork(@NotNull Direction side, BENetwork network) {
        networks.put(side, network);
    }

    public void replaceNetwork(BENetwork original, BENetwork newNetwork) {
        for(Direction dir : networks.keySet()) {
            if(networks.get(dir) == original) {
                setNetwork(dir, newNetwork);
                return;
            }
        }
    }

    public void clearNetworks() {
        for(BENetwork network : networks.values())
            network.remove(this, true);
    }

    public ResourceLocation getType() {
        return DEFAULT_TYPE;
    }

    public BlockPos getPos() {
        return pos;
    }

    public Level getLevel() {
        return level;
    }

    /**
     * Called when a {@link BENetworkObject} is added to a {@link BENetwork} this {@link BENetworkObject} is in.
     *
     * @param network The {@link BENetwork} which updated.
     * @param object The {@link BENetworkObject} which was added.
     */
    public void onNetworkUpdateAdd(BENetwork network, BENetworkObject object) {}

    /**
     * Called when a {@link BENetworkObject} is removed from a {@link BENetwork} this {@link BENetworkObject} is in.
     *
     * @param network The {@link BENetwork} which updated.
     * @param object The {@link BENetworkObject} which was removed.
     */
    public void onNetworkUpdateRemove(BENetwork network, BENetworkObject object) {}

    /**
     * Called when this {@link BENetworkObject} is added to a {@link BENetwork}.
     *
     * @param network The {@link BENetwork} this object was added to.
     */
    public void onNetworkAdd(BENetwork network) {}

    /**
     * Called when this {@link BENetworkObject} is removed from a {@link BENetwork}.
     *
     * @param network The {@link BENetwork} this object was removed from.
     */
    public void onNetworkRemove(BENetwork network) {}

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

    /**
     * Gets called when this {@link BENetworkObject} is removed from the {@link Level} it is in. Destroys all references
     * to itself within networks and doesn't update itself while doing so.
     */
    public void discard() {
        for(BENetwork network : networks.values())
            network.remove(this, false);
    }

}
