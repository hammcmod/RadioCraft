package com.arrl.radiocraft.common.entities;

import com.arrl.radiocraft.api.capabilities.RadiocraftCapabilities;
import com.arrl.radiocraft.common.init.RadiocraftEntityTypes;
import com.arrl.radiocraft.common.init.RadiocraftItems;
import com.arrl.radiocraft.common.init.RadiocraftTags;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
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
public class AntennaWire extends Entity {

    private static final EntityDataAccessor<Optional<UUID>> DATA_HOLDER_UUID = SynchedEntityData.defineId(AntennaWire.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<BlockPos> DATA_END_POS = SynchedEntityData.defineId(AntennaWire.class, EntityDataSerializers.BLOCK_POS);

    private final AntennaWirePart[] parts;
    private final AntennaWirePart endPart;
    private Player holder = null;

    private int checkInterval = 0;

    public AntennaWire(EntityType<AntennaWire> type, Level level) {
        this(level, new BlockPos(0, 0, 0));
    }

    public AntennaWire(Level level, BlockPos pos) {
        super(RadiocraftEntityTypes.ANTENNA_WIRE.get(), level);

        this.endPart = new AntennaWirePart(this, "end");
        this.parts = new AntennaWirePart[] { endPart };
        endPart.setId(ENTITY_COUNTER.getAndAdd(2) + 1);

        this.noPhysics = true;
        this.noCulling = true; // Disable culling in case only one connector is behind a wall.

        this.setPos(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
        if(!level.isClientSide)
            setEndPos(pos);
    }

    /**
     * Create a wire entity at level, pos.
     */
    public static AntennaWire createWire(Level level, BlockPos pos, Player holder) {
        AntennaWire entity = new AntennaWire(level, pos);

        if(holder != null) {
            entity.setHolder(holder);
            holder.getCapability(RadiocraftCapabilities.ANTENNA_WIRE_HOLDERS).ifPresent((cap) -> {
                cap.setHeldPos(pos);
            });
        }

        level.addFreshEntity(entity);
        return entity;
    }

    /**
     * Gets the wire entities at the specified position or returns an empty list.
     */
    public static List<AntennaWire> getWires(Level level, BlockPos pos) {
        List<AntennaWire> wires = getAntennaWires(level, pos);
        List<AntennaWirePart> parts = getAntennaWireParts(level, pos);

        for(AntennaWirePart part : parts) {
            if(!wires.contains(part.parent))
                wires.add(part.parent);
        }

        return wires;
    }

    public static List<AntennaWire> getAntennaWires(Level level, BlockPos pos) {
        int posX = pos.getX();
        int posY = pos.getY();
        int posZ = pos.getZ();
        return level.getEntitiesOfClass(AntennaWire.class, new AABB(posX, posY, posZ, posX + 1.0D, posY + 1.0D, posZ + 1.0D));
    }

    public static List<AntennaWirePart> getAntennaWireParts(Level level, BlockPos pos) {
        int posX = pos.getX();
        int posY = pos.getY();
        int posZ = pos.getZ();
        return level.getEntitiesOfClass(AntennaWirePart.class, new AABB(posX, posY, posZ, posX + 1.0D, posY + 1.0D, posZ + 1.0D));
    }

    /**
     * Get the first wire entity at the specified position which is being held by Player
     */
    public static AntennaWire getFirstHeldWire(Level level, BlockPos pos, Player player) {
        List<AntennaWire> wires = getWires(level, pos);

        for(AntennaWire wire : wires) {
            if(wire.getWireHolder() == player)
                return wire;
        }

        return null;
    }

    @Override
    public void tick() {
        if (!level.isClientSide) {
            if (checkInterval++ == 60) {
                checkInterval = 0;
                Player holder = getWireHolder();
                if (!isRemoved() && !survives()) {
                    if(holder != null)
                        holder.getCapability(RadiocraftCapabilities.ANTENNA_WIRE_HOLDERS).ifPresent(cap -> cap.setHeldPos(null)); // Reset held pos.
                    discard();
                    playBreakSound();
                }
            }
        }
    }

    /**
     * Checks if the block can hold an antenna wire.
     */
    public boolean survives() {
        if(RadiocraftTags.isAntennaWireHolder(level.getBlockState(blockPosition()).getBlock())) { // Starts in valid block.
            Player holder = getWireHolder();
            if(holder != null)
                return !holder.isRemoved(); // Survives if has holder and holder is not removed.

            return RadiocraftTags.isAntennaWireHolder(level.getBlockState(getEndPos()).getBlock()); // Also survives if has no holder and end pos is in a valid block.
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
        endPart.setPos(new Vec3(endPos.getX() + 0.5D, endPos.getY() + 0.5D, endPos.getZ() + 0.5D));
    }

    public BlockPos getEndPos() {
        return endPart.blockPosition();
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
    protected void defineSynchedData() {
        this.entityData.define(DATA_HOLDER_UUID, Optional.empty());
        this.entityData.define(DATA_END_POS, BlockPos.ZERO);
    }

    @Override
    protected float getEyeHeight(Pose pose, EntityDimensions size) {
        return 0.0F;
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
            if (!isRemoved() && !level.isClientSide) {
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
        endPart.setId(id + 1);
    }

}
