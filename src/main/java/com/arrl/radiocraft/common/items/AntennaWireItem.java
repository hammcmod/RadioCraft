package com.arrl.radiocraft.common.items;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.api.capabilities.IAntennaWireHolderCapability;
import com.arrl.radiocraft.common.capabilities.RadiocraftCapabilities;
import com.arrl.radiocraft.common.entities.AntennaWire;
import com.arrl.radiocraft.common.init.RadiocraftTags.Blocks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import org.jetbrains.annotations.NotNull;

public class AntennaWireItem extends Item {

    public AntennaWireItem(Properties properties) {
        super(properties);
    }

    /**
     * Called when this item is used when targeting a Block
     */
    public @NotNull InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        Player player = context.getPlayer();

        if(state.is(Blocks.ANTENNA_WIRE_HOLDERS)) {
            level.gameEvent(GameEvent.BLOCK_ATTACH, pos, GameEvent.Context.of(player));
            if (!level.isClientSide && player != null) {
                IAntennaWireHolderCapability cap = player.getCapability(RadiocraftCapabilities.ANTENNA_WIRE_HOLDERS);
                if (cap != null) {
                    BlockPos heldPos = cap.getHeldPos(player);
                    if(heldPos == null) {
                        AntennaWire entity = AntennaWire.createWire(level, pos, player);
                        player.sendSystemMessage(Component.literal("Antenna Wire started at " + pos));
                        entity.playSound(SoundEvents.LEASH_KNOT_PLACE, 1.0F, 1.0F);
                        level.playSound(null, entity.blockPosition(), SoundEvents.LEASH_KNOT_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
                        cap.setHeldPos(player, pos);
                    } else {
                        if(!pos.equals(cap.getHeldPos(player))) { // Do not allow wire to be created with identical start and end points.
                            player.sendSystemMessage(Component.literal("Antenna Wire ended at " + pos));
                            AntennaWire entity = AntennaWire.getFirstHeldWire(level, heldPos, player);
                            if(entity != null) {
                                entity.setEndPos(pos);
                                entity.setHolder(null);
                                level.playSound(null, entity.getEndPos(), SoundEvents.LEASH_KNOT_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
                                entity.updateAntennas();
                            } else {
                                Radiocraft.LOGGER.warn("Could not find player's first held wire.");
                            }
                            cap.setHeldPos(player, null);
                        } else {
                            player.sendSystemMessage(Component.literal("Antenna Wire cannot start and end at the same place"));
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
