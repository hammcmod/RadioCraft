package com.arrl.radiocraft.common.entities;

import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.init.RadiocraftEntityTypes;
import com.arrl.radiocraft.common.init.RadiocraftItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.entity.PartEntity;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 @author MoreThanHidden//refactored by Favouriteless
 This class is used to create the antenna wire entity.
 @see net.minecraft.world.entity.decoration.LeashFenceKnotEntity
 */
public class AntennaWire extends HangingEntity {

    private static final EntityDataAccessor<Optional<UUID>> DATA_HOLDER_UUID = SynchedEntityData.defineId(AntennaWire.class, EntityDataSerializers.OPTIONAL_UUID);

    private AntennaWirePart[] parts;
    private AntennaWirePart endPart;
    private Player holder = null;

    public AntennaWire(EntityType<AntennaWire> type, Level level) {
        this(level, new BlockPos(0, 0, 0));
    }

    public AntennaWire(Level level, BlockPos pos) {
        super(RadiocraftEntityTypes.ANTENNA_WIRE.get(), level, pos);
        this.endPart = new AntennaWirePart(this, "end");
        this.parts = new AntennaWirePart[] { endPart };
        this.noPhysics = true;
        this.noCulling = true; // Disable culling in case only one connector is behind a wall.

        this.setPos(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
        endPart.setPos(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
    }

    /**
     * Attempts to get the wire's holder. If a holder cannot be found, return null.
     */
    public Player getWireHolder() {
        Optional<UUID> optionalUUID = entityData.get(DATA_HOLDER_UUID);

        if(optionalUUID.isPresent()) {
            UUID uuid = optionalUUID.get();
            if(holder == null || !uuid.equals(holder.getUUID()))
                holder = level.getPlayerByUUID(uuid);

            return holder;
        }
        else
            return null;
    }

    /**
     * Sets the end position.
     * On server side, it sends a packet to the client to update the wire holder position.
     * @param endPos
     */
    public void setEndPos(BlockPos endPos) {
        endPart.setPos(endPos.getX() + 0.5D, endPos.getY() + 0.5D, endPos.getZ() + 0.5D);
    }

    public BlockPos getEndPos() {
        return endPart.blockPosition();
    }

    public AntennaWirePart getEndPart() {
        return endPart;
    }

    /**
     * Create a wire entity at level, pos.
     */
    public static AntennaWire createWire(Level level, BlockPos pos, Player holder) {
        AntennaWire entity = new AntennaWire(level, pos);
        entity.setEndPos(pos);
        entity.setHolder(holder);
        level.addFreshEntity(entity);
        return entity;
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

    /**
     * Gets the wire entities at the specified position or returns an empty list.
     */
    public static List<AntennaWire> getWires(Level level, BlockPos pos) {
        int posX = pos.getX();
        int posY = pos.getY();
        int posZ = pos.getZ();

        List<AntennaWire> wires = level.getEntitiesOfClass(AntennaWire.class, new AABB(posX - 0.5D, posY - 0.5D, posZ - 0.5D, posX + 0.5D, posY + 0.5D, posZ + 0.5D));
        List<AntennaWirePart> parts = level.getEntitiesOfClass(AntennaWirePart.class, new AABB(posX - 0.5D, posY - 0.5D, posZ - 0.5D, posX + 0.5D, posY + 0.5D, posZ + 0.5D));

        for(AntennaWirePart part : parts) {
            if(!wires.contains(part.parent))
                wires.add(part.parent);
        }

        return wires;
    }

    public static AntennaWire getFirstUnconnectedWire(Level level, BlockPos pos) {
        List<AntennaWire> wires = getWires(level, pos);

        for(AntennaWire wire : wires) {
            if(wire.holder != null)
                return wire;
        }

        return null;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putLong("endPos", endPart.blockPosition().asLong());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        setEndPos(BlockPos.of(nbt.getLong("endPos")));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_HOLDER_UUID, Optional.empty());
    }

    @Override
    public int getWidth() {
        return 9;
    }

    @Override
    public int getHeight() {
        return 9;
    }

    @Override
    protected float getEyeHeight(Pose pose, EntityDimensions size) {
        return 0.0625F;
    }

    /**
     * Checks if the entity is in range to render.
     */
    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 1024.0D;
    }

    /**
     * Called when this entity is broken. Entity parameter may be null.
     */
    @Override
    public void dropItem(@Nullable Entity entity) {
        this.playSound(SoundEvents.LEASH_KNOT_BREAK, 1.0F, 1.0F);
    }

    /**
     * Checks if the block can hold an antenna wire.
     */
    @Override
    public boolean survives() {
        if(level.getBlockState(pos).getBlock() == RadiocraftBlocks.ANTENNA_CONNECTOR.get()) { // Survive if both endpoints are valid connectors
            if(endPart != null)
                return level.getBlockState(endPart.blockPosition()).getBlock() == RadiocraftBlocks.ANTENNA_CONNECTOR.get();
            return true;
        }
        return false;
    }

    @Override
    public void playPlacementSound() {
        this.playSound(SoundEvents.LEASH_KNOT_PLACE, 1.0F, 1.0F);
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
    public boolean shouldBeSaved() {
        return getWireHolder() != null;
    }

}
