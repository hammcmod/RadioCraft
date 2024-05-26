package com.arrl.radiocraft.common.capabilities;

import com.arrl.radiocraft.api.benetworks.BENetwork;
import com.arrl.radiocraft.api.benetworks.BENetworkObject;
import com.arrl.radiocraft.api.benetworks.BENetworkRegistry;
import com.arrl.radiocraft.api.capabilities.IBENetworks;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

public class BENetworksCapability implements IBENetworks {

    private final Map<BlockPos, BENetworkObject> networkObjects = new HashMap<>();
    private final Map<UUID, BENetwork> networks = new HashMap<>();
    private final Level level; // This is terrible code with a circular ref, but it shouldn't matter.

    public BENetworksCapability(Level level) {
        this.level = level;
    }

    @Override
    public BENetworkObject getObject(@NotNull BlockPos pos) {
        return networkObjects.get(pos);
    }

    @Override
    public void setObject(@NotNull BlockPos pos, @NotNull BENetworkObject object) {
        networkObjects.put(pos, object);
    }

    @Override
    public void removeObject(@NotNull BlockPos pos) {
        BENetworkObject obj = getObject(pos);
        if(obj != null)
            obj.discard();
        networkObjects.remove(pos);
    }

    @Override
    public BENetwork getNetwork(UUID uuid) {
        return networks.get(uuid);
    }

    @Override
    public void addNetwork(BENetwork network) {
        networks.put(network.getUUID(), network);
    }

    @Override
    public void removeNetwork(BENetwork network) {
        networks.remove(network.getUUID());
    }

    @Override
    public void tickNetworkObjects(Level level) {
        for(Entry<BlockPos, BENetworkObject> entry : networkObjects.entrySet())
            entry.getValue().tick(level, entry.getKey());
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();

        CompoundTag networks = new CompoundTag();
        for(Entry<UUID, BENetwork> entry : this.networks.entrySet())
            networks.putString(entry.getKey().toString(), entry.getValue().getType().toString());

        ListTag objects = new ListTag();
        for(Entry<BlockPos, BENetworkObject> entry : networkObjects.entrySet()) {
            CompoundTag nbtEntry = new CompoundTag();
            nbtEntry.putLong("pos", entry.getKey().asLong());
            nbtEntry.putString("type", entry.getValue().getType().toString());
            entry.getValue().save(nbtEntry);
        }

        nbt.put("networks", networks);
        nbt.put("networkObjects", objects);
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        CompoundTag networksTag = nbt.getCompound("networks");
        for(String key : networksTag.getAllKeys()) { // Make sure to load the networks themselves first.
            UUID uuid = UUID.fromString(key);
            networks.put(uuid, BENetworkRegistry.createNetwork(new ResourceLocation(networksTag.getString(key)), uuid));
        }

        ListTag objectsTag = nbt.getList("networkObjects", ListTag.TAG_COMPOUND);
        for(Tag t : objectsTag) {
            CompoundTag obj = (CompoundTag)t;
            BENetworkObject networkObject = BENetworkRegistry.createObject(new ResourceLocation(obj.getString("type")), level, BlockPos.of(obj.getLong("pos")));
            networkObjects.put(networkObject.getPos(), networkObject);
            networkObject.load(this, obj);
        }
    }

}
