package com.arrl.radiocraft.common.blockentities.radio;

import com.arrl.radiocraft.CommonConfig;
import com.arrl.radiocraft.api.benetworks.BENetworkObject;
import com.arrl.radiocraft.common.be_networks.network_objects.RadioNetworkObject;
import com.arrl.radiocraft.common.init.RadiocraftBlockEntities;
import com.arrl.radiocraft.common.menus.HFRadioAllBandMenu;
import com.arrl.radiocraft.common.radio.Band;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The all-band transceiver: one radio for 80, 40, 20 and 10m plus 2m, picked with the band knob, in SSB, CW or FM.
 * FM is a voice mode like SSB (the voice path is the same), but it is squelched: no background static between
 * transmissions, as on a real FM rig. On 2m it talks to VHF stations through a VHF antenna.
 */
public class HFRadioAllBandBlockEntity extends HFRadioBlockEntity {

    /** The knob's positions, in order from fully left to fully right: 3.5, 7, 14, 28 and 144 MHz. */
    public static final List<String> BANDS = List.of("80m", "40m", "20m", "10m", "2m");

    protected boolean fmEnabled = false;

    public HFRadioAllBandBlockEntity(BlockPos pos, BlockState state) {
        super(RadiocraftBlockEntities.HF_RADIO_ALL_BAND.get(), pos, state, Band.getBand("10m"));
    }

    /** Position of the current band on the knob (see {@link #BANDS}), or -1 if the band is not one of them. */
    public int getBandIndex() {
        return band == null ? -1 : BANDS.indexOf(band.name());
    }

    /** Turns the band knob to position {@code index}; the frequency goes to the bottom of the new band. */
    public void setBandIndex(int index) {
        if(index < 0 || index >= BANDS.size() || index == getBandIndex())
            return;
        Band newBand = Band.getBand(BANDS.get(index));
        if(newBand != null)
            changeBand(newBand);
    }

    public boolean getFMEnabled() {
        return fmEnabled;
    }

    /** Selects FM: voice like SSB, without the static. */
    public void setFMEnabled(boolean value) {
        if(fmEnabled == value)
            return;
        fmEnabled = value;
        if(value)
            super.setSSBEnabled(true); // Voice on (and CW off).
        updateBlock();
        setChanged();
    }

    /** SSB is on when the voice path is on and FM is not. */
    public boolean isSSBSelected() {
        return getSSBEnabled() && !fmEnabled;
    }

    @Override
    public void setSSBEnabled(boolean value) {
        if(value && fmEnabled) {
            fmEnabled = false;
            updateBlock();
            setChanged();
        }
        super.setSSBEnabled(value);
    }

    @Override
    public void setCWEnabled(boolean value) {
        if(value)
            fmEnabled = false;
        super.setCWEnabled(value);
    }

    @Override
    public boolean isFM() {
        return fmEnabled;
    }

    @Override
    public boolean shouldPlayStatic() {
        return !fmEnabled && super.shouldPlayStatic(); // FM is squelched.
    }

    @Override
    protected void setupSaveTag(CompoundTag nbt) {
        super.setupSaveTag(nbt);
        nbt.putBoolean("fmEnabled", fmEnabled);
    }

    @Override
    protected void readSaveTag(CompoundTag nbt) {
        super.readSaveTag(nbt);
        fmEnabled = nbt.getBoolean("fmEnabled");
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("container.hf_radio_all_band");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, @NotNull Inventory playerInventory, @NotNull Player player) {
        return new HFRadioAllBandMenu(id, this);
    }

    @Override
    public BENetworkObject createNetworkObject() {
        return new RadioNetworkObject(level, worldPosition, CommonConfig.HF_RADIO_ALL_BAND_TRANSMIT_TICK.get(), CommonConfig.HF_RADIO_ALL_BAND_RECEIVE_TICK.get());
    }

}
