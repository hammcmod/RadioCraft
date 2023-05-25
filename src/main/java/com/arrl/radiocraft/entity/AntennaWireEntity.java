package com.arrl.radiocraft.entity;

import com.arrl.radiocraft.common.blocks.AntennaConnectorBlock;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.init.RadiocraftEntityTypes;
import com.arrl.radiocraft.common.init.RadiocraftItems;
import com.arrl.radiocraft.common.init.RadiocraftPackets;
import com.arrl.radiocraft.common.network.packets.ClientboundWireHolderPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.Nullable;

/**
 @author MoreThanHidden
 This class is used to create the antenna wire entity.
 @see net.minecraft.world.entity.decoration.LeashFenceKnotEntity
 */
public class AntennaWireEntity extends HangingEntity {


    private BlockPos tetherPos1;
    private BlockPos tetherPos2;




    private BlockPos targetPos;

    public AntennaWireEntity(EntityType<AntennaWireEntity> type, Level level) {
        super(type, level);
    }

    public AntennaWireEntity(Level level, BlockPos pos) {
        super(RadiocraftEntityTypes.ANTENNA_WIRE.get(), level, pos);
        this.setPos(pos.getX(), pos.getY(), pos.getZ());
    }


    public Entity getWireHolder() {
        if(targetPos == null && level.isClientSide) {
            return Minecraft.getInstance().player;
        } else if (targetPos == null) {
            return null;
        }
        return getWire(level, targetPos);
    }

    /**
     * Sets the wire holder position.
     * On server side, it sends a packet to the client to update the wire holder position.
     * @param targetPos
     */
    public void setWireHolder(BlockPos targetPos) {
        this.targetPos = targetPos;
        if (!level.isClientSide) {
            ClientboundWireHolderPacket packet = new ClientboundWireHolderPacket(getId(), targetPos);
            BlockPos pos = blockPosition();
            RadiocraftPackets.INSTANCE.send(PacketDistributor.NEAR.with(
                    PacketDistributor.TargetPoint.p(pos.getX(), pos.getY(), pos.getZ(), 2048.0D, level.dimension())
            ), packet);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putLong("wireHolder", targetPos.asLong());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        if(nbt.contains("wireHolder"))
            targetPos = BlockPos.of(nbt.getLong("wireHolder"));
    }

    /**
     * Tick probably isn't the best way to handle this but the client isn't available when readAdditionalSaveData is called.
     */
    @Override
    public void tick() {
        super.tick();
        setWireHolder(targetPos);
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
        return this.level.getBlockState(this.pos).is(RadiocraftBlocks.ANTENNA_CONNECTOR.get());
    }

    @Override
    public void playPlacementSound() {
        this.playSound(SoundEvents.LEASH_KNOT_PLACE, 1.0F, 1.0F);
    }

    /**
     * Gets or creates the wire entity at the specified position.
     */
    public static AntennaWireEntity getOrCreateWire(Level level, BlockPos pos) {
        if (!(level.getBlockState(pos).getBlock() instanceof AntennaConnectorBlock)){
            return null;
        }

        AntennaWireEntity wire = getWire(level, pos);

        if(wire != null) {
            return wire;
        }

        AntennaWireEntity antennaWireEntity1 = new AntennaWireEntity(level, pos);
        level.addFreshEntity(antennaWireEntity1);
        return antennaWireEntity1;
    }


    /**
     * Gets the wire entity at the specified position or returns null.
     */
    public static AntennaWireEntity getWire(Level level, BlockPos pos) {
        int posX = pos.getX();
        int posY = pos.getY();
        int posZ = pos.getZ();

        for(AntennaWireEntity antennaWireEntity : level.getEntitiesOfClass(AntennaWireEntity.class, new AABB((double)posX - 1.0D, (double)posY - 1.0D, (double)posZ - 1.0D, (double)posX + 1.0D, (double)posY + 1.0D, (double)posZ + 1.0D))) {
            if (antennaWireEntity.getPos().equals(pos)) {
                return antennaWireEntity;
            }
        }

        return null;
    }

    /**
     * Gets the start position of the antenna wire.
     */
    @Override
    public Vec3 getRopeHoldPosition(float partialTicks) {
        return this.getPosition(partialTicks).add(getLeashOffset());
    }

    @Override
    protected Vec3 getLeashOffset() {
        return new Vec3(0.0D, -0.12D, 0.5D);
    }

    /**
     * Middle click result in creative mode.
     */
    @Override
    public ItemStack getPickResult() {
        return new ItemStack(RadiocraftItems.ANTENNA_WIRE.get());
    }

}
