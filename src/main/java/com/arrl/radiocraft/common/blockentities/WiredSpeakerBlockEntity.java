package com.arrl.radiocraft.common.blockentities;

import com.arrl.radiocraft.api.benetworks.BENetworkObject;
import com.arrl.radiocraft.api.benetworks.INetworkObjectProvider;
import com.arrl.radiocraft.common.be_networks.network_objects.SpeakerNetworkObject;
import com.arrl.radiocraft.common.init.RadiocraftBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** A wall speaker or loudspeaker: only here to put the speaker on the power wire it touches. */
public class WiredSpeakerBlockEntity extends BlockEntity implements INetworkObjectProvider {

    public WiredSpeakerBlockEntity(BlockPos pos, BlockState state) {
        super(RadiocraftBlockEntities.WIRED_SPEAKER.get(), pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if(level != null && !level.isClientSide())
            getNetworkObject(level, worldPosition); // Creates the object and joins the wire.
    }

    @Override
    public BENetworkObject createNetworkObject() {
        return new SpeakerNetworkObject(level, worldPosition);
    }
}
