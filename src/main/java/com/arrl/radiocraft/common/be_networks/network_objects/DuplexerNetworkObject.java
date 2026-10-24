package com.arrl.radiocraft.common.be_networks.network_objects;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.api.benetworks.BENetworkObject;
import com.arrl.radiocraft.api.capabilities.IBENetworks;
import com.arrl.radiocraft.common.be_networks.ICoaxPassThrough;
import com.arrl.radiocraft.common.radio.Band;
import com.arrl.radiocraft.common.radio.DuplexerMath;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/**
 * The duplexer on a coax run. It lets a repeater transmit and receive through one antenna at the same time. Like the
 * antenna tuner the wire walk passes through it ({@link ICoaxPassThrough}), so coax from it reaches the antenna, and
 * a repeater joins it by touching it (see {@link RepeaterNetworkObject#linksDirectlyTo}).
 * <p>
 * Its six cavities ({@link DuplexerMath}) are each tuned by a screw to a frequency: the first three to the repeater's
 * output, the last three to its input. Out of the box it is factory tuned: it follows the repeater it sits on
 * ({@link #followRepeater}), so it just works. Once a player turns a screw the screws are theirs, and a repeater
 * frequency change means retuning by hand. {@link AntennaNetworkObject} applies the cavities to
 * what goes out ({@link #txPass}, {@link #swr}) and what comes in ({@link #rxPass}).
 */
public class DuplexerNetworkObject extends BENetworkObject implements ICoaxPassThrough {

    public static final ResourceLocation TYPE = Radiocraft.id("duplexer");

    private volatile int[] screwsHz = new int[DuplexerMath.SCREWS]; // 0 until first tuned. Replaced whole: read from the voice thread.
    private boolean manual = false; // A player has turned a screw: it no longer follows the repeater.

    public DuplexerNetworkObject(Level level, BlockPos pos) {
        super(level, pos);
    }

    public int getScrewHz(int index) {
        return screwsHz[index];
    }

    /** A player turns one screw: from now on the duplexer is tuned by hand. */
    public void turnScrew(int index, int hz) {
        manual = true;
        setScrewHz(index, hz);
    }

    /** True once a player has turned a screw. */
    public boolean isManual() {
        return manual;
    }

    /** Sets one screw's frequency, kept inside the 2m band and on the screw's step. */
    public void setScrewHz(int index, int hz) {
        if(index < 0 || index >= DuplexerMath.SCREWS)
            return;
        Band band = Band.getBand("2m");
        int min = (int)band.minFrequency(), max = (int)band.maxFrequency();
        int steps = Math.round((float)(Math.max(min, Math.min(max, hz)) - min) / DuplexerMath.SCREW_STEP_HZ);
        int[] next = screwsHz.clone();
        next[index] = min + steps * DuplexerMath.SCREW_STEP_HZ;
        screwsHz = next;
    }

    /**
     * Factory tuning: until a player turns a screw, the duplexer stays tuned for the repeater it is on, transmit
     * cavities on its output and receive cavities on its input.
     */
    public void followRepeater(float rxHz, float txHz) {
        if(manual)
            return;
        for(int i = 0; i < 3; i++) {
            setScrewHz(DuplexerMath.TX_FIRST + i, Math.round(txHz));
            setScrewHz(DuplexerMath.RX_FIRST + i, Math.round(rxHz));
        }
    }

    /** Share of the transmitter's power at {@code hz} that gets through to the antenna. */
    public double txPass(float hz) {
        return DuplexerMath.pass(screwsHz, DuplexerMath.TX_FIRST, hz);
    }

    /** Share of a received signal at {@code hz} that gets through to the receiver. */
    public double rxPass(float hz) {
        return DuplexerMath.pass(screwsHz, DuplexerMath.RX_FIRST, hz);
    }

    /** Share of a received signal left while the transmitter is up ({@link DuplexerMath#desense}). */
    public double desense(float rxHz, float txHz) {
        return DuplexerMath.desense(screwsHz, rxHz, txHz);
    }

    /** The SWR the transmit cavities show a transmitter at {@code hz}: 1 when they are tuned to it. */
    public double swr(float hz) {
        return DuplexerMath.swr(txPass(hz));
    }

    @Override
    public void save(CompoundTag nbt) {
        super.save(nbt);
        nbt.putIntArray("screws", screwsHz);
        nbt.putBoolean("manual", manual);
    }

    @Override
    public void load(IBENetworks cap, CompoundTag nbt) {
        super.load(cap, nbt);
        int[] saved = nbt.getIntArray("screws");
        if(saved.length == DuplexerMath.SCREWS)
            screwsHz = saved;
        manual = nbt.getBoolean("manual");
    }

    @Override
    public ResourceLocation getType() {
        return TYPE;
    }

}
