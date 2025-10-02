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

/**
 * Represents the presence of a {@link BlockEntity} within a {@link BENetwork}.
 */
public class BENetworkObject {

    /**
     * The default type of a {@link BENetworkObject}.
     */
    public static final ResourceLocation DEFAULT_TYPE = Radiocraft.id("default");
    /**
     * The networks this {@link BENetworkObject} is in.
     */
    protected final Map<Direction, BENetwork> networks = new HashMap<>();
    /**
     * The {@link Level} this {@link BENetworkObject} is in.
     */
    protected final Level level;
    /**
     * The position of this {@link BENetworkObject}.
     */
    protected final BlockPos pos;

    /**
     * Creates a new {@link BENetworkObject}.
     * @param level The {@link Level} this object is in.
     * @param pos The {@link BlockPos} of this object.
     */
    public BENetworkObject(Level level, BlockPos pos) {
        this.level = level;
        this.pos = pos;
    }

    /**
     * Called every tick.
     * @param level The {@link Level} this object is in.
     * @param pos The {@link BlockPos} of this object.
     */
    public void tick(Level level, BlockPos pos) {}

    /**
     * Gets the network this {@link BENetworkObject} is in.
     * @param side The {@link Direction} of the network.
     * @return The network this {@link BENetworkObject} is in.
     */
    public BENetwork getNetwork(@NotNull Direction side) {
        return networks.get(side);
    }

    /**
     * Sets a network for this {@link BENetworkObject}.
     * @param side The {@link Direction} of the network.
     * @param network The {@link BENetwork} to set.
     */
    public void setNetwork(@NotNull Direction side, BENetwork network) {
        networks.put(side, network);
    }

    /**
     * Replaces a network with another network.
     * @param original The network to replace.
     * @param newNetwork The new network.
     */
    public void replaceNetwork(BENetwork original, BENetwork newNetwork) {
        for(Direction dir : networks.keySet()) {
            if(networks.get(dir) == original) {
                setNetwork(dir, newNetwork);
                return;
            }
        }
    }

    /**
     * Clears all networks this {@link BENetworkObject} is in.
     */
    public void clearNetworks() {
        for(BENetwork network : networks.values())
            network.remove(this, true);
        networks.clear();
    }

    /**
     * Gets the type of this {@link BENetworkObject}.
     * @return The type of this {@link BENetworkObject}.
     */
    public ResourceLocation getType() {
        return DEFAULT_TYPE;
    }

    /**
     * Gets the position of this {@link BENetworkObject}.
     * @return The position of this {@link BENetworkObject}.
     */
    public BlockPos getPos() {
        return pos;
    }

    /**
     * Gets the {@link Level} this {@link BENetworkObject} is in.
     * @return The {@link Level} this {@link BENetworkObject} is in.
     */
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

    /**
     * Saves the NBT.
     * @param nbt The {@link CompoundTag} to save to.
     */
    public void save(CompoundTag nbt) {
        CompoundTag networksTag = new CompoundTag();
        for(Entry<Direction, BENetwork> entry : networks.entrySet())
            networksTag.putUUID(entry.getKey().getName(), entry.getValue().getUUID());
        nbt.put("networks", networksTag);
    }

    /**
     * Loads the NBT
     * @param cap The {@link IBENetworks} capability to use.
     * @param nbt The {@link CompoundTag} to load from.
     */
    public void load(IBENetworks cap, CompoundTag nbt) {
        CompoundTag networksTag = nbt.getCompound("networks");
        for(String key : networksTag.getAllKeys()) {
            BENetwork network = cap.getNetwork(networksTag.getUUID(key));
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
