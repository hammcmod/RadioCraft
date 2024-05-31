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
            case 0 -> getter.get() & 0xFFFF0000; // 0 gets the most significant bits
            case 1 -> getter.get() & 0x0000FFFF; // 1 gets the least significant bits
            case 3 -> getter.get(); // 2 gets the real value, this isn't directly synchronised by the container.
            default -> 0;
        };
    }

    @Override
    public void set(int index, int value) {
        if(index == 0) { // Value is most significant bits
            int leastSignificant = get(1);
            setter.accept(leastSignificant + value);
        }
        else if(index == 1) { // Value is least significant bits
            int mostSignificant = get(0);
            setter.accept(mostSignificant + value);
        }
    }

}
