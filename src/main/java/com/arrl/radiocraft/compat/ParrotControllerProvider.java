package com.arrl.radiocraft.compat;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.blockentities.ParrotControllerBlockEntity;
import com.arrl.radiocraft.common.blocks.radios.ParrotControllerBlock;
import com.arrl.radiocraft.common.blocks.radios.ParrotControllerBlock.LinkSide;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/** Jade lines for the simplex repeat controller: delay, which side the radio is on, and what it is doing right now. */
public enum ParrotControllerProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if(data.contains("delayTicks"))
            tooltip.add(Component.literal(String.format("Repeat delay: %.1fs", data.getInt("delayTicks") / 20.0F)));

        BlockState state = accessor.getBlockState();
        LinkSide link = state.getValue(ParrotControllerBlock.LINK);
        tooltip.add(Component.literal(link == LinkSide.NONE ? "Radio: none linked" : "Radio: linked to the " + link.getSerializedName()));

        String activity = state.getValue(ParrotControllerBlock.TRANSMITTING) ? "Repeating"
                : state.getValue(ParrotControllerBlock.RECEIVING) ? "Receiving" : "Idle";
        tooltip.add(Component.literal("State: " + activity));
    }

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if(accessor.getBlockEntity() instanceof ParrotControllerBlockEntity be)
            data.putInt("delayTicks", be.getDelayTicks());
    }

    @Override
    public ResourceLocation getUid() {
        return Radiocraft.id("parrot_controller");
    }
}
