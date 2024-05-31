package com.arrl.radiocraft.common.menus.slots;

import net.minecraft.world.inventory.ContainerData;

public class IntSplitDataSlot implements ContainerData {

    private int value = 0;

    @Override
    public int get(int index) {
        return switch(index) {
            case 0 -> value & 0xFFFF0000; // 0 gets the most significant bits
            case 1 -> value & 0x0000FFFF; // 1 gets the least significant bits
            case 2 -> value; // 2 gets the real value, this isn't directly synchronised by the container.
            default -> 0;
        };
    }

    @Override
    public void set(int index, int value) {
        if(index == 0) { // Value is most significant bits
            int leastSignificant = this.value & 0x0000FFFF;
            this.value = leastSignificant + value;
        }
        else if(index == 1) { // Value is least significant bits
            int mostSignificant = this.value & 0xFFFF0000;
            this.value = mostSignificant + value;
        }
    }

    @Override
    public int getCount() {
        return 2;
    }

}
