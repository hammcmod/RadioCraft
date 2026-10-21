package com.arrl.radiocraft.common.be_networks.network_objects;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.api.benetworks.BENetworkObject;
import com.arrl.radiocraft.common.blocks.radios.VHFRepeaterBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The VHF repeater on the power network and the coax. It plugs straight into a duplexer on its back, top or bottom (no
 * coax needed between them); coax from the duplexer then runs to the antenna.
 */
public class RepeaterNetworkObject extends RadioNetworkObject {

    public static final ResourceLocation TYPE = Radiocraft.id("repeater");

    public RepeaterNetworkObject(Level level, BlockPos pos, int transmitUse, int receiveUse) {
        super(level, pos, transmitUse, receiveUse);
    }

    /** The sides a duplexer can sit on: the back, top and bottom of the repeater. */
    public static boolean isDuplexerSide(BlockState repeater, Direction side) {
        if(side == Direction.UP || side == Direction.DOWN)
            return true;
        return repeater.hasProperty(VHFRepeaterBlock.HORIZONTAL_FACING) && side == repeater.getValue(VHFRepeaterBlock.HORIZONTAL_FACING).getOpposite();
    }

    /** The side of the repeater a duplexer is plugged into, or null if none is. */
    public static Direction findDuplexerSide(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        for(Direction side : Direction.values()) {
            if(isDuplexerSide(state, side) && level.getBlockState(pos.relative(side)).getBlock() instanceof com.arrl.radiocraft.common.blocks.antennas.DuplexerBlock)
                return side;
        }
        return null;
    }

    @Override
    public boolean linksDirectlyTo(BENetworkObject other) {
        if(!(other instanceof DuplexerNetworkObject) || level == null)
            return false;
        BlockPos offset = other.getPos().subtract(pos);
        Direction side = Direction.fromDelta(offset.getX(), offset.getY(), offset.getZ());
        return side != null && isDuplexerSide(level.getBlockState(pos), side);
    }

    @Override
    public ResourceLocation getType() {
        return TYPE;
    }

}
