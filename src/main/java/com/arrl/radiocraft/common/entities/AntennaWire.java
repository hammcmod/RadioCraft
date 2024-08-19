package com.arrl.radiocraft.common.entities;

import com.arrl.radiocraft.common.blockentities.AntennaBlockEntity;
import com.arrl.radiocraft.common.capabilities.RadiocraftCapabilities;
import com.arrl.radiocraft.common.init.RadiocraftEntityTypes;
import com.arrl.radiocraft.common.init.RadiocraftItems;
import com.arrl.radiocraft.common.init.RadiocraftTags;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import net.neoforged.neoforge.entity.PartEntity;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 @author MoreThanHidden//refactored by Favouriteless
 This class is used to create the antenna wire entity.
 @see net.minecraft.world.entity.decoration.LeashFenceKnotEntity
 */
public class AntennaWire extends Entity implements IAntennaWire, IEntityWithComplexSpawn {

    private static final EntityDataAccessor<Optional<UUID>> DATA_HOLDER_UUID = SynchedEntityData.defineId(AntennaWire.class, EntityDataSerializers.OPTIONAL_UUID);

    private final AntennaWirePart[] parts;
    private final AntennaWirePart endPart;
    private Player holder = null;

    private int checkInterval = 0;
    private boolean checkEnabled = true;

    public AntennaWire(EntityType<AntennaWire> type, Level level) {
        this(level, new BlockPos(0, 0, 0));
    }

    public AntennaWire(Level level, BlockPos pos) {
        super(RadiocraftEntityTypes.ANTENNA_WIRE.get(), level);

        this.endPart = new AntennaWirePart(this, "end");
        this.parts = new AntennaWirePart[] { endPart };
        setId(ENTITY_COUNTER.getAndAdd(getParts().length + 1) + 1);

        this.noPhysics = true;
        this.noCulling = true; // Disable culling in case only one connector is behind a wall.

        this.setPos(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
        endPart.setPos(position());
    }

    /**
     * Create a wire entity at level, pos.
     */
    public static AntennaWire createWire(Level level, BlockPos pos, Player holder) {
        AntennaWire entity = new AntennaWire(level, pos);

        if(holder != null) {
            entity.setHolder(holder);
            RadiocraftCapabilities.ANTENNA_WIRE_HOLDERS.getCapability(holder, null).setHeldPos(pos);

            /*holder.getCapability(RadiocraftCapabilities.ANTENNA_WIRE_HOLDERS).ifPresent((cap) -> {
                cap.setHeldPos(pos);
            });*/
        }

        level.addFreshEntity(entity);
        return entity;
    }

    /**
     * Gets the wire entities at the specified position or returns an empty list.
     */
    public static List<IAntennaWire> getWires(Level level, BlockPos pos) {
        int posX = pos.getX();
        int posY = pos.getY();
        int posZ = pos.getZ();

        AABB aabb = new AABB(posX, posY, posZ, posX + 1.0D, posY + 1.0D, posZ + 1.0D);
        List<AntennaWire> wires = level.getEntitiesOfClass(AntennaWire.class, aabb, entity -> !entity.isRemoved());
        List<AntennaWirePart> parts = level.getEntitiesOfClass(AntennaWirePart.class, aabb, entity -> !entity.isRemoved());

        List<IAntennaWire> out = new ArrayList<>();
        out.addAll(wires);
        out.addAll(parts);
        return out;
    }

    /**
     * Get the first wire entity at the specified position which is being held by Player
     */
    public static AntennaWire getFirstHeldWire(Level level, BlockPos pos, Player player) {
        int posX = pos.getX();
        int posY = pos.getY();
        int posZ = pos.getZ();
        List<AntennaWire> wires = level.getEntitiesOfClass(AntennaWire.class, new AABB(posX, posY, posZ, posX + 1.0D, posY + 1.0D, posZ + 1.0D));

        for(AntennaWire wire : wires) {
            if(wire.getWireHolder() == player)
                return wire;
        }

        return null;
    }

    public void updateAntennas() {
        if(checkEnabled) {
            checkEnabled = false;
            if(level().getBlockEntity(blockPosition()) instanceof AntennaBlockEntity be)
                be.markAntennaChanged();

            endPart.updateAntennas();
            for(IAntennaWire wire : getWires(level(), blockPosition()))
                wire.updateAntennas();

            checkEnabled = true;
        }
    }

    @Override
    public void tick() {
        if (!level().isClientSide) {
            if (checkInterval++ == 60) {
                checkInterval = 0;
                Player holder = getWireHolder();
                if (!isRemoved() && !survives()) {
                    if(holder != null)
                        RadiocraftCapabilities.ANTENNA_WIRE_HOLDERS.getCapability(holder, null).setHeldPos(null);
                        //holder.getCapability(RadiocraftCapabilities.ANTENNA_WIRE_HOLDERS).ifPresent(cap -> cap.setHeldPos(null)); // Reset held pos.
                    discard();
                    endPart.discard();
                    playBreakSound();
                    updateAntennas();
                }
            }
        }
    }

    /**
     * Checks if the block can hold an antenna wire.
     */
    public boolean survives() {
        if(level().getBlockState(blockPosition()).is(RadiocraftTags.Blocks.ANTENNA_WIRE_HOLDERS)) { // Starts in valid block.
            Player holder = getWireHolder();
            if(holder != null)
                return !holder.isRemoved(); // Survives if has holder and holder is not removed.

            return level().getBlockState(getEndPos()).is(RadiocraftTags.Blocks.ANTENNA_WIRE_HOLDERS); // Also survives if has no holder and end pos is in a valid block.
        }
        return false;
    }

    /**
     * Attempts to get the wire's holder. If a holder cannot be found, return null.
     */
    public Player getWireHolder() {
        Optional<UUID> optionalUUID = entityData.get(DATA_HOLDER_UUID);

        if(optionalUUID.isPresent()) {
            UUID uuid = optionalUUID.get();
            if(holder == null || !uuid.equals(holder.getUUID()))
                holder = level().getPlayerByUUID(uuid);

            return holder;
        }
        else
            return null;
    }

    /**
     * Sets the end part's position. On server, sends an update packet to all clients in the relevant level.
     */
    public void setEndPos(BlockPos endPos) {
        endPart.setPos(new Vec3(endPos.getX() + 0.5D, endPos.getY() + 0.5D, endPos.getZ() + 0.5D));
        if(!level().isClientSide);
            //RadiocraftPackets.sendToLevel(new CAntennaWirePacket(getId(), endPos), (ServerLevel)level());
    }

    public BlockPos getEndPos() {
        return endPart.blockPosition();
    }

    public BlockPos getStartPos() {
        return blockPosition();
    }

    public boolean isPairedWith(IAntennaWire other) {
        return other == endPart;
    }

    public AntennaWirePart getEndPart() {
        return endPart;
    }

    /**
     * Set the holder player for this wire entity, used for rendering a wire while the player is still connecting two points.
     */
    public void setHolder(Player player) {
        if(player != null)
            entityData.set(DATA_HOLDER_UUID, Optional.of(player.getUUID()));
        else
            entityData.set(DATA_HOLDER_UUID, Optional.empty());
    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        nbt.putLong("endPos", getEndPos().asLong());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        setEndPos(BlockPos.of(nbt.getLong("endPos")));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder pBuilder) {
        pBuilder.define(DATA_HOLDER_UUID, Optional.empty());
    }

    /**
     * Checks if the entity is in range to render.
     */
    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 1024.0D;
    }

    /**
     * Middle click result in creative mode.
     */
    @Override
    public ItemStack getPickResult() {
        return new ItemStack(RadiocraftItems.ANTENNA_WIRE.get());
    }

    // Should be treated as a multipart entity to ensure it can be detected at both connectors individually.
    @Override
    public boolean isMultipartEntity() {
        return true;
    }

    @Override
    public @Nullable PartEntity<?>[] getParts() {
        return parts;
    }

    @Override
    public Vec3 getRopeHoldPosition(float partialTicks) {
        return position();
    }

    @Override
    public Vec3 getLeashOffset(float partialTicks) {
        return Vec3.ZERO;
    }

    public boolean hurt(DamageSource source, float amount) {
        if (isInvulnerableTo(source)) {
            return false;
        } else {
            if (!isRemoved() && !level().isClientSide) {
                kill();
                markHurt();
                playBreakSound();
            }

            return true;
        }
    }

    public void playBreakSound() {
        playSound(SoundEvents.LEASH_KNOT_BREAK, 1.0F, 1.0F);
    }

    @Override
    public void setId(int id) {
        super.setId(id);
        int newId = id + 1;
        for(PartEntity<?> part : getParts())
            part.setId(newId++);
    }

    @Override
    public void writeSpawnData(RegistryFriendlyByteBuf buffer) {
        buffer.writeLong(endPart.blockPosition().asLong());
    }

    @Override
    public void readSpawnData(RegistryFriendlyByteBuf additionalData) {
        setEndPos(BlockPos.of(additionalData.readLong()));
    }
}
