package com.arrl.radiocraft.common.be_networks.network_objects;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.api.benetworks.BENetworkObject;
import com.arrl.radiocraft.common.be_networks.ICoaxPassThrough;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/**
 * The duplexer on a coax run. It lets a repeater transmit and receive through one antenna at the same time. Like the
 * antenna tuner the wire walk passes through it ({@link ICoaxPassThrough}), so coax from it reaches the antenna, and
 * a repeater joins it by touching it (see {@link RepeaterNetworkObject#linksDirectlyTo}).
 */
public class DuplexerNetworkObject extends BENetworkObject implements ICoaxPassThrough {

    public static final ResourceLocation TYPE = Radiocraft.id("duplexer");

    public DuplexerNetworkObject(Level level, BlockPos pos) {
        super(level, pos);
    }

    @Override
    public ResourceLocation getType() {
        return TYPE;
    }

}
