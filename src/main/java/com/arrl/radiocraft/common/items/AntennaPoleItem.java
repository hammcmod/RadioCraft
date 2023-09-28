package com.arrl.radiocraft.common.items;

import javax.annotation.Nullable;

import com.arrl.radiocraft.common.blocks.AntennaPoleBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * @author MoreThanHidden
 * BEAntenna Pole Item (place from the bottom) based on ScaffoldingBlockItem
 */
public class AntennaPoleItem extends BlockItem {
    public AntennaPoleItem(Block pBlock, Item.Properties pProperties) {
        super(pBlock, pProperties);
    }

    @Nullable
    public BlockPlaceContext updatePlacementContext(BlockPlaceContext pContext) {
        BlockPos blockpos = pContext.getClickedPos();
        Level level = pContext.getLevel();
        BlockState blockstate = level.getBlockState(blockpos);
        Block block = this.getBlock();
        if (!blockstate.is(block)) {
            return AntennaPoleBlock.getDistance(level, blockpos) == AntennaPoleBlock.STABILITY_MAX_DISTANCE ? null : pContext;
        } else {
            Direction direction = Direction.UP;

            int i = 0;
            BlockPos.MutableBlockPos blockpos$mutableblockpos = blockpos.mutable().move(direction);

            while(i < AntennaPoleBlock.STABILITY_MAX_DISTANCE) {
                if (!level.isClientSide && !level.isInWorldBounds(blockpos$mutableblockpos)) {
                    Player player = pContext.getPlayer();
                    int j = level.getMaxBuildHeight();
                    if (player instanceof ServerPlayer && blockpos$mutableblockpos.getY() >= j) {
                        ((ServerPlayer)player).sendSystemMessage(Component.translatable("build.tooHigh", j - 1).withStyle(ChatFormatting.RED), true);
                    }
                    break;
                }

                blockstate = level.getBlockState(blockpos$mutableblockpos);
                if (!blockstate.is(this.getBlock())) {
                    if (blockstate.canBeReplaced(pContext)) {
                        return BlockPlaceContext.at(pContext, blockpos$mutableblockpos, direction);
                    }
                    break;
                }

                blockpos$mutableblockpos.move(direction);
                ++i;
            }

            return null;
        }
    }

    protected boolean mustSurvive() {
        return false;
    }
}