package com.arrl.radiocraft.common.blockentities;

import com.arrl.radiocraft.api.antenna.IAntennaPacket;
import com.arrl.radiocraft.api.benetworks.BENetworkObject;
import com.arrl.radiocraft.api.benetworks.INetworkObjectProvider;
import com.arrl.radiocraft.common.be_networks.network_objects.AntennaNetworkObject;
import com.arrl.radiocraft.common.be_networks.network_objects.DirectionFinderModuleNetworkObject;
import com.arrl.radiocraft.common.init.RadiocraftBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A direction finder module: one antenna input of a direction finder ({@link DirectionFinderBlockEntity}) it touches,
 * directly or through other modules. Signals its antenna hears are passed on to that direction finder.
 */
public class DirectionFinderModuleBlockEntity extends BlockEntity implements INetworkObjectProvider {

    /** The direction finder this module last belonged to; written on the game thread, read on the voice thread. */
    @Nullable
    private volatile BlockPos finder = null;

    public DirectionFinderModuleBlockEntity(BlockPos pos, BlockState state) {
        super(RadiocraftBlockEntities.DIRECTION_FINDER_MODULE.get(), pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if(level != null && !level.isClientSide())
            getNetworkObject(level, worldPosition); // Creates the object and joins the coax.
    }

    @Override
    public BENetworkObject createNetworkObject() {
        return new DirectionFinderModuleNetworkObject(level, worldPosition);
    }

    public void setFinder(@Nullable BlockPos finder) {
        this.finder = finder;
    }

    @Nullable
    public BlockPos getFinder() {
        return finder;
    }

    /**
     * The one antenna on this module's coax, null with none or several. Server only.
     */
    @Nullable
    public AntennaNetworkObject getAntenna() {
        if(level == null || !(getNetworkObject(level, worldPosition) instanceof DirectionFinderModuleNetworkObject module))
            return null;
        List<AntennaNetworkObject> antennas = module.getAntennas();
        return antennas.size() == 1 ? antennas.getFirst() : null;
    }

    /** The type of the antenna on this module's coax ({@code radiocraft:dipole} and so on), null if none is recognised. */
    @Nullable
    public ResourceLocation getAntennaType() {
        AntennaNetworkObject antenna = getAntenna();
        return antenna == null || antenna.getAntenna() == null ? null : antenna.getAntenna().type.getId();
    }

    /** A signal reached this module's antenna (any thread): it goes on to the direction finder, which decides. */
    public void hear(IAntennaPacket packet) {
        BlockPos at = finder;
        Level here = level;
        if(at == null || here == null || !here.isLoaded(at))
            return;
        // Off the server thread Level#getBlockEntity answers null, so go to the chunk, as the antennas do.
        if(here.getChunkAt(at).getBlockEntity(at, net.minecraft.world.level.chunk.LevelChunk.EntityCreationType.CHECK) instanceof DirectionFinderBlockEntity directionFinder)
            directionFinder.hear(worldPosition, packet);
    }
}
