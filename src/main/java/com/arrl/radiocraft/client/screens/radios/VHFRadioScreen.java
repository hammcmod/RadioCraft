package com.arrl.radiocraft.client.screens.radios;

import com.arrl.radiocraft.client.RadiocraftClientValues;
import com.arrl.radiocraft.common.menus.RadioMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public abstract class VHFRadioScreen<T extends RadioMenu<?>> extends RadioScreen<T> {

    public VHFRadioScreen(T menu, Inventory inventory, Component title, ResourceLocation texture, ResourceLocation widgetsTexture) {
        super(menu, inventory, title, texture, widgetsTexture);
        RadiocraftClientValues.VOICE_ENABLED = true;
    }

}
