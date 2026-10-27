package com.arrl.radiocraft.common.blockentities.radio;

import com.arrl.radiocraft.RadiocraftServerConfig;
import com.arrl.radiocraft.common.radio.Band;
import com.arrl.radiocraft.common.radio.voice.Ctcss;
import com.arrl.radiocraft.client.blockentity.RadioBlockEntityClientHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A 2m FM radio. Besides the frequency it has the two settings a repeater user needs: a CTCSS tone (sent under the
 * voice, and the receiver only opens for it) and a repeater offset (it listens on its frequency and transmits 600 kHz
 * below or above it).
 */
public abstract class VHFRadioBlockEntity extends RadioBlockEntity {

    protected float ctcssTone = Ctcss.NONE;
    protected int offsetHz = 0;

    @Override
    public boolean isFM() {
        return true;
    }

    /** VHF radios step by the channel step, like the handhelds. */
    @Override
    public int getFrequencyStep() {
        return RadiocraftServerConfig.VHF_FREQUENCY_STEP.get();
    }

    public VHFRadioBlockEntity(BlockEntityType<? extends RadioBlockEntity> type, BlockPos pos, BlockState state, Band band) {
        super(type, pos, state, band);
        ssbEnabled = true;
    }

    public float getCtcssTone() {
        return ctcssTone;
    }

    /** Sets the tone (snapped to a standard one, or none). */
    public void setCtcssTone(float tone) {
        float sane = Ctcss.sanitize(tone);
        if(sane != ctcssTone) {
            ctcssTone = sane;
            setChanged();
            updateBlock();
        }
    }

    public int getOffsetHz() {
        return offsetHz;
    }

    /** Sets the repeater offset: simplex (0) or 600 kHz either way. */
    public void setOffsetHz(int offset) {
        int sane = Ctcss.sanitizeOffset(offset);
        if(sane != offsetHz) {
            offsetHz = sane;
            setChanged();
            updateBlock();
        }
    }

    @Override
    public float getTransmitFrequency() {
        if(offsetHz == 0 || band == null)
            return frequency;
        return Math.max(band.minFrequency(), Math.min(band.maxFrequency(), frequency + offsetHz));
    }

    @Override
    public float getTransmitTone() {
        return ctcssTone;
    }

    @Override
    public float getToneSquelch() {
        return ctcssTone;
    }

    /** FM is squelched: no background static (a weak signal's hiss is in its audio, see RadioVoiceProcessor#fmFade; the static instance plays only a blown transmitter's roar). */
    @Override
    public boolean shouldPlayStatic() {
        return false;
    }

    @Override
    protected void setupSoundInstances() {
        super.setupSoundInstances();
        RadioBlockEntityClientHandler.startStaticSoundInstance(this);
    }

    @Override
    protected void setupSaveTag(CompoundTag nbt) {
        super.setupSaveTag(nbt);
        nbt.putFloat("ctcssTone", ctcssTone);
        nbt.putInt("offsetHz", offsetHz);
    }

    @Override
    protected void readSaveTag(CompoundTag nbt) {
        super.readSaveTag(nbt);
        ctcssTone = Ctcss.sanitize(nbt.getFloat("ctcssTone"));
        offsetHz = Ctcss.sanitizeOffset(nbt.getInt("offsetHz"));
    }

}
