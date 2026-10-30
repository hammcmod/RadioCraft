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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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
     * Where an object is on a network: the side of the block the run touches, and whether it is the power network
     * there. A side has one slot for power and one for everything else (coax), so a bundle of power wire and coax into
     * one face puts the object on both (2026-10-28: with one slot a side, a bundle into a microwave radio's face left it
     * on its coax network only, and it never powered on).
     */
    public record Slot(Direction side, boolean power) {
        public static Slot of(Direction side, BENetwork network) {
            return new Slot(side, network instanceof PowerBENetwork);
        }

        /** The NBT key: the side's name, with "_power" for the power slot. */
        String key() {
            return power ? side.getName() + "_power" : side.getName();
        }
    }

    /**
     * The networks this {@link BENetworkObject} is in.
     */
    protected final Map<Slot, BENetwork> networks = new HashMap<>();

    /** The networks this object is in, by side and kind. Read only. */
    public Map<Slot, BENetwork> getNetworks() {
        return Collections.unmodifiableMap(networks);
    }
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
        return getNetwork(side, false);
    }

    /** The power network ({@code power}) or the other one (coax) on {@code side}, or null. */
    public BENetwork getNetwork(@NotNull Direction side, boolean power) {
        return networks.get(new Slot(side, power));
    }

    /**
     * Sets a network for this {@link BENetworkObject}.
     * @param side The {@link Direction} of the network.
     * @param network The {@link BENetwork} to set.
     */
    public void setNetwork(@NotNull Direction side, BENetwork network) {
        networks.put(Slot.of(side, network), network); // A power network goes in the side's power slot.
    }

    /**
     * Replaces a network with another network.
     * @param original The network to replace.
     * @param newNetwork The new network.
     */
    public void replaceNetwork(BENetwork original, BENetwork newNetwork) {
        for(Slot slot : List.copyOf(networks.keySet())) {
            if(networks.get(slot) == original)
                networks.put(slot, newNetwork); // The same run can touch more than one side, so replace every match.
        }
    }

    /**
     * Whether this object joins {@code other} just by touching it, with no wire in between. Solar panels link to each
     * other and to a charge controller this way; everything else needs wire. Either side saying yes is enough.
     */
    public boolean linksDirectlyTo(BENetworkObject other) {
        return false;
    }

    /**
     * {@link #linksDirectlyTo(BENetworkObject)} for one kind of network: {@code power} for the power walk, false for the
     * coax walk. By default the same for both; a device on both kinds (a direction finder module) can touch-link on
     * one only.
     */
    public boolean linksDirectlyTo(BENetworkObject other, boolean power) {
        return linksDirectlyTo(other);
    }

    /** Takes this object off its networks of one kind only: the power ones ({@code power}) or the others (coax). */
    public void clearNetworks(boolean power) {
        for(Slot slot : List.copyOf(networks.keySet())) {
            if(slot.power() != power)
                continue;
            BENetwork network = networks.remove(slot);
            if(!networks.containsValue(network))
                network.remove(this, true);
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
        for(Entry<Slot, BENetwork> entry : networks.entrySet())
            networksTag.putUUID(entry.getKey().key(), entry.getValue().getUUID());
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
            Direction side = Direction.byName(key.endsWith("_power") ? key.substring(0, key.length() - "_power".length()) : key);
            if(side != null && network != null)
                networks.put(Slot.of(side, network), network); // By the network's kind: saves before 2026-10-28 had the side only.
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
