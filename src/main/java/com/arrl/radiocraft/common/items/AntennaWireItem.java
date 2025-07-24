package com.arrl.radiocraft.common.items;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.api.capabilities.IAntennaWireHolderCapability;
import com.arrl.radiocraft.common.capabilities.RadiocraftCapabilities;
import com.arrl.radiocraft.common.entities.AntennaWire;
import com.arrl.radiocraft.common.init.RadiocraftTags.Blocks;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
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

        if(state.is(Blocks.ANTENNA_WIRE_HOLDERS)) {
            Player player = context.getPlayer();
            level.gameEvent(GameEvent.BLOCK_ATTACH, pos, GameEvent.Context.of(player));
            Radiocraft.LOGGER.info("Antenna Wire attachment started at " + pos);

            if (!level.isClientSide && player != null) {

                IAntennaWireHolderCapability cap = RadiocraftCapabilities.ANTENNA_WIRE_HOLDERS.getCapability(player, null);

                Radiocraft.LOGGER.info("Antenna Wire holder capability: " + cap);

                if (cap != null) {
                    BlockPos heldPos = cap.getHeldPos();
                    Radiocraft.LOGGER.info("Antenna Wire heldPos: " + heldPos);

                    if(heldPos == null) {
                        AntennaWire entity = AntennaWire.createWire(level, pos, player);
                        entity.playSound(SoundEvents.LEASH_KNOT_PLACE, 1.0F, 1.0F);
                        player.level().playSound(null, entity.blockPosition(), SoundEvents.LEASH_KNOT_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);

                        cap.setHeldPos(pos);
                    }
                    else {
                        if(!pos.equals(cap.getHeldPos())) { // Do not allow wire to be created with identical start and end points.
                            AntennaWire entity = AntennaWire.getFirstHeldWire(level, heldPos, player);

                            Radiocraft.LOGGER.info("Antenna Wire entity for end placement: " + entity);

                            if(entity != null) {
                                entity.setEndPos(pos);
                                Radiocraft.LOGGER.info("Antenna Wire entity end pos: " + entity.getEndPos());
                                entity.setHolder(null);
                                player.level().playSound(null, entity.getEndPos(), SoundEvents.LEASH_KNOT_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
                                entity.updateAntennas();
                            }

                            cap.setHeldPos(null);
                        }
                    }
                }

                return InteractionResult.SUCCESS;
            }

            return InteractionResult.sidedSuccess(level.isClientSide);
        } else {
            return InteractionResult.PASS;
        }
    }
}
