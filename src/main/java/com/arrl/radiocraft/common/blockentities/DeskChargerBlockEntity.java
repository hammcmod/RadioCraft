package com.arrl.radiocraft.common.blockentities;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.Containers;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.block.state.BlockState;
import com.arrl.radiocraft.common.init.RadiocraftBlockEntities;
import com.arrl.radiocraft.Radiocraft;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;
import net.neoforged.neoforge.energy.EnergyStorage;
import com.arrl.radiocraft.common.capabilities.BasicEnergyStorage;
import net.minecraft.nbt.CompoundTag;

import javax.annotation.Nullable;

public class DeskChargerBlockEntity extends BlockEntity implements GeoBlockEntity, MenuProvider {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    public final ItemStackHandler inventory = new ItemStackHandler(1) {
        @Override
        protected int getStackLimit(int slot, ItemStack stack) {
            return 64;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    public final BasicEnergyStorage energyStorage = new BasicEnergyStorage(1000, 250, 250);
    private boolean infinite = false;

    public DeskChargerBlockEntity(BlockPos pos, BlockState state) {
        super(RadiocraftBlockEntities.DESK_CHARGER.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, DeskChargerBlockEntity be) {
        if (level.isClientSide()) return;

        net.neoforged.neoforge.energy.IEnergyStorage blockEnergy = be.energyStorage;
        if (blockEnergy == null) return;

        ItemStack radio = be.inventory.getStackInSlot(0);
        if (radio == null || radio.isEmpty()) return;

        net.neoforged.neoforge.energy.IEnergyStorage radioEnergy = radio.getCapability(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.ITEM);
        if (radioEnergy == null) return;

        int desiredTransfer = 50;

        int available = blockEnergy.getEnergyStored();
        if (available <= 0) return;

        int toAttempt = Math.min(desiredTransfer, available);
        if (be.infinite) {
            int accepted = radioEnergy.receiveEnergy(desiredTransfer, true);
            if (accepted <= 0) return;
            int received = radioEnergy.receiveEnergy(accepted, false);
            Radiocraft.LOGGER.debug("DeskCharger tick (infinite) @ {}: delivered={} to radio", pos, received);
                be.setChanged();
                level.sendBlockUpdated(pos, state, state, 3);
            return;
        }

        int accepted = radioEnergy.receiveEnergy(toAttempt, true);
        if (accepted <= 0) return;

        int extracted = blockEnergy.extractEnergy(accepted, false);
        if (extracted <= 0) return;

        int received = radioEnergy.receiveEnergy(extracted, false);
        if (received < extracted) {
            blockEnergy.receiveEnergy(extracted - received, false);
        }

        int radioStoredAfter = radioEnergy.getEnergyStored();
        Radiocraft.LOGGER.debug("DeskCharger tick @ {}: available={}, toAttempt={}, accepted(sim)={}, extracted={}, received={}, radioStoredAfter={}", pos, available, toAttempt, accepted, extracted, received, radioStoredAfter);

        be.setChanged();
        level.sendBlockUpdated(pos, state, state, 3);
    }

    public void drops() {
        SimpleContainer inv = new SimpleContainer(inventory.getSlots());
        for (int i = 0; i < inventory.getSlots(); i++) {
            inv.setItem(i, inventory.getStackInSlot(i));
        }
        Containers.dropContents(this.level, this.worldPosition, inv);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // No animations for now; placeholder for future controllers
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
    @Override
    public net.minecraft.nbt.CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public void saveAdditional(net.minecraft.nbt.CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", inventory.serializeNBT(registries));
        CompoundTag energyTag = new CompoundTag();
        energyStorage.saveAdditional(energyTag);
        tag.put("energy", energyTag);
        tag.putBoolean("infinite", this.infinite);
    }

    @Override
    public void loadAdditional(net.minecraft.nbt.CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("inventory")) {
            inventory.deserializeNBT(registries, tag.getCompound("inventory"));
        }
        if (tag.contains("energy")) {
            CompoundTag energyTag = tag.getCompound("energy");
            if (energyTag.contains("capacity")) {
                energyStorage.setMaxEnergy(energyTag.getInt("capacity"));
            }
            if (energyTag.contains("maxReceive")) {
                energyStorage.setMaxReceive(energyTag.getInt("maxReceive"));
            }
            if (energyTag.contains("maxExtract")) {
                energyStorage.setMaxExtract(energyTag.getInt("maxExtract"));
            }
            if (energyTag.contains("energy")) {
                energyStorage.setEnergy(energyTag.getInt("energy"));
            }
        }
        if (tag.contains("infinite")) {
            this.infinite = tag.getBoolean("infinite");
        }
    }

    public boolean isInfinite() {
        return this.infinite;
    }

    public void setInfinite(boolean value) {
        this.infinite = value;
        this.setChanged();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(Radiocraft.translationKey("screen", "desk_charger"));
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new com.arrl.radiocraft.common.menus.DeskChargerMenu(id, inventory, this);
    }
}
