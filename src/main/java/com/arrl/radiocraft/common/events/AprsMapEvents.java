package com.arrl.radiocraft.common.events;

import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Sneaking with an item in hand normally skips the block and uses the item, so empty maps sneak-used on an H19 would
 * just become an ordinary map. This lets the H19 have them instead, for a map wall (see
 * {@link com.arrl.radiocraft.common.blocks.AprsTerminalBlock}). It runs on both sides, so the client sends the use too.
 */
@EventBusSubscriber
public class AprsMapEvents {

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if(event.getItemStack().is(Items.MAP) && event.getEntity().isSecondaryUseActive()
                && event.getLevel().getBlockState(event.getPos()).is(RadiocraftBlocks.APRS_TERMINAL.get())) {
            event.setUseBlock(TriState.TRUE);
            event.setUseItem(TriState.FALSE);
        }
    }
}
