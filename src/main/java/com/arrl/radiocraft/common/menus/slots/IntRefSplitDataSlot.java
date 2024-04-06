package com.arrl.radiocraft.common.menus.slots;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class IntRefSplitDataSlot extends IntSplitDataSlot {

    private final Consumer<Integer> setter;
    private final Supplier<Integer> getter;

    public IntRefSplitDataSlot(Consumer<Integer> setter, Supplier<Integer> getter) {
        this.setter = setter;
        this.getter = getter;
    }

    @Override
    public int get(int index) {
        return switch(index) {
            case 0 -> getter.get() & 0xffff;
            case 1 -> getter.get() & 0x0000ffff;
            case 3 -> getter.get(); // You can call case 3 on IntSplitDataSlot to get the real value, but this isn't actually synchronised by the container as count is 2.
            default -> 0;
        };
    }

    @Override
    public void set(int index, int value) {
        if(index == 0) {
            int frequency = value & 0xffff0000;
            setter.accept(frequency + (value & 0xffff));
        }
        else if(index == 1) {
            int frequency = value & 0x0000ffff;
            setter.accept(frequency + (value << 16));
        }
    }

}
