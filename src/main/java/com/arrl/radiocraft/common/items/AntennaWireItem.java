package com.arrl.radiocraft.common.items;

import com.arrl.radiocraft.common.entities.AntennaWire;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
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

    public AntennaWireItem(Properties properties) {
        super(properties);
    }

    /**
     * Called when this item is used when targeting a Block
     */
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        ItemStack stack = context.getItemInHand();

        if(state.getBlock() == RadiocraftBlocks.ANTENNA_CONNECTOR.get()) {
            Player player = context.getPlayer();
            level.gameEvent(GameEvent.BLOCK_ATTACH, pos, GameEvent.Context.of(player));

            if (!level.isClientSide && player != null) {
                if(stack.hasTag() && stack.getTag().contains("startPos")) {
                    CompoundTag tag = stack.getTag();
                    AntennaWire entity = AntennaWire.getFirstUnconnectedWire(level, BlockPos.of(tag.getLong("startPos")));

                    if(entity != null) {
                        entity.setEndPos(pos); // Set end pos of wire
                        entity.setHolder(null); // Set holder to null -- allows wire to be saved & render towards it's end part rather than holder
                    }
                    tag.remove("startPos");
                }
                else {
                    AntennaWire.createWire(level, pos, player);
                    stack.getOrCreateTag().putLong("startPos", pos.asLong());
                }
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        } else {
            return InteractionResult.PASS;
        }
    }
}
