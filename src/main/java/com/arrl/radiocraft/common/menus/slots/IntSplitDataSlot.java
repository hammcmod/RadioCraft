package com.arrl.radiocraft.common.menus.slots;

import net.minecraft.world.inventory.ContainerData;

public class IntSplitDataSlot implements ContainerData {

    private int value = 0;

    @Override
    public int get(int index) {
        return switch(index) {
            case 0 -> value & 0xffff;
            case 1 -> value & 0x0000ffff;
            case 3 -> value; // You can call case 3 on IntSplitDataSlot to get the real value, but this isn't actually synchronised by the container as count is 2.
            default -> 0;
        };
    }

    @Override
    public void set(int index, int value) {
        if(index == 0) {
            int frequency = value & 0xffff0000;
            this.value = frequency + (value & 0xffff);
        }
        else if(index == 1) {
            int frequency = value & 0x0000ffff;
            this.value = frequency + (value << 16);
        }
    }

    @Override
    public int getCount() {
        return 2;
    }

}
