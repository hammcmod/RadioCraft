package com.arrl.radiocraft.common.capabilities;

import com.arrl.radiocraft.api.benetworks.BENetwork;
import com.arrl.radiocraft.api.benetworks.BENetworkObject;
import com.arrl.radiocraft.api.capabilities.IBENetworks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

public class BENetworksCapability implements IBENetworks {

    private final Map<BlockPos, BENetworkObject> networkObjects = new HashMap<>();
    private final Map<UUID, BENetwork> networks = new HashMap<>();

    public BENetworksCapability() {}

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
    public @UnknownNullability CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag nbt = new CompoundTag();

        CompoundTag networks = new CompoundTag();

        for(Entry<UUID, BENetwork> entry : this.networks.entrySet()) {
            if(!entry.getValue().getNetworkObjects().isEmpty())
                networks.putString(entry.getKey().toString(), entry.getValue().getType().toString());
        }

        ListTag objects = new ListTag();
        for(Entry<BlockPos, BENetworkObject> entry : networkObjects.entrySet()) {
            CompoundTag nbtEntry = new CompoundTag();
            nbtEntry.putLong("pos", entry.getKey().asLong());
            nbtEntry.putString("type", entry.getValue().getType().toString());
            entry.getValue().save(nbtEntry);
            objects.add(nbtEntry);
        }

        nbt.put("networks", networks);
        nbt.put("networkObjects", objects);
        return nbt;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        CompoundTag networksTag = nbt.getCompound("networks");
        for(String key : networksTag.getAllKeys()) { // Make sure to load the networks themselves first.
            UUID uuid = UUID.fromString(key);
            //BENetworkRegistry.createNetwork(ResourceLocation.fromNamespaceAndPath(Radiocraft.MOD_ID, networksTag.getString(key)), uuid, level);
        }

        ListTag objectsTag = nbt.getList("networkObjects", ListTag.TAG_COMPOUND);
        for(Tag t : objectsTag) {
            CompoundTag obj = (CompoundTag)t;
            //BENetworkObject networkObject = BENetworkRegistry.createObject(ResourceLocation.fromNamespaceAndPath(Radiocraft.MOD_ID, obj.getString("type")), level, BlockPos.of(obj.getLong("pos")));
            //networkObject.load(this, obj);
        }
    }
}
