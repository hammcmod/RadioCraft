package com.arrl.radiocraft.common.items;

import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.entity.AntennaWireEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

public class AntennaWireItem extends Item {
    public AntennaWireItem(Item.Properties pProperties) {
        super(pProperties);
    }

    /**
     * Called when this item is used when targeting a Block
     */
    public InteractionResult useOn(UseOnContext pContext) {
        Level level = pContext.getLevel();
        BlockPos blockpos = pContext.getClickedPos();
        BlockState blockstate = level.getBlockState(blockpos);
        ItemStack itemInHand = pContext.getItemInHand();
        if (blockstate.is(RadiocraftBlocks.ANTENNA_CONNECTOR.get())) {
            Player player = pContext.getPlayer();
            if (!level.isClientSide() && player != null) {
                AntennaWireEntity wireEntity = AntennaWireEntity.getOrCreateWire(level, blockpos);
                level.gameEvent(GameEvent.BLOCK_ATTACH, blockpos, GameEvent.Context.of(player));
                if(itemInHand.hasTag() && itemInHand.getTag().contains("start")) {
                    BlockPos start = BlockPos.of(itemInHand.getTag().getLong("start"));
                    AntennaWireEntity oldWireEntity = AntennaWireEntity.getOrCreateWire(level, start);
                    oldWireEntity.setWireHolder(blockpos);
                    wireEntity.setWireHolder(blockpos);
                    itemInHand.getTag().remove("start");
                }else{
                    CompoundTag tag = new CompoundTag();
                    tag.putLong("start",  blockpos.asLong());
                    itemInHand.setTag(tag);
                    wireEntity.setWireHolder(null);
                    return InteractionResult.PASS;
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        } else {
            return InteractionResult.PASS;
        }
    }
}
