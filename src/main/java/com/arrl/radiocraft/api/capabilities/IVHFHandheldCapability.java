package com.arrl.radiocraft.api.capabilities;

import com.arrl.radiocraft.api.antenna.IAntenna;
import com.arrl.radiocraft.api.blockentities.radio.IVoiceTransmitter;
import com.arrl.radiocraft.common.radio.IVoiceReceiver;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.AutoRegisterCapability;
import net.minecraftforge.common.util.INBTSerializable;

/**
 * Represents the inventory within a handheld radio.
 */
@AutoRegisterCapability
public interface IVHFHandheldCapability extends INBTSerializable<CompoundTag>, IVoiceTransmitter, IVoiceReceiver, IAntenna {

    /**
     * @return The item (battery) held by this handheld radio.
     */
    ItemStack getItem();

    void setItem(ItemStack item);

    int getFrequency();

    void setFrequency(int frequency);

    boolean isPowered();

    void setPowered(boolean value);

    boolean isPTTDown();

    void setPTTDown(boolean value);

    /**
     * @return The player holding this item (also the target for the VoiP channel)
     */
    Player getPlayer();

    void setPlayer(Player player);

}
