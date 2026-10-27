package com.arrl.radiocraft.api.benetworks;

/** How a {@link PowerBENetwork} shares power out between its batteries and consumers: evenly. */
public final class PowerSharing {

    private PowerSharing() {}

    /**
     * Splits {@code amount} as evenly as it goes between takers that can each take at most {@code room[i]}: each
     * gets the same, except one with less room, which takes what it can and leaves the rest of its share to the
     * others. A remainder that does not divide goes one each to the first takers. Pure, so it is unit tested.
     *
     * @return How much each taker gets; the sum is {@code min(amount, total room)}.
     */
    public static int[] shareEvenly(int amount, int[] room) {
        int[] out = new int[room.length];
        int left = Math.max(0, amount);
        boolean[] full = new boolean[room.length];
        int open = 0;
        for(int i = 0; i < room.length; i++) {
            full[i] = room[i] <= 0;
            if(!full[i])
                open++;
        }
        while(left > 0 && open > 0) {
            int each = Math.max(1, left / open); // Less than one each: the remainder goes one at a time.
            for(int i = 0; i < room.length && left > 0; i++)
                if(!full[i]) {
                    int give = Math.min(each, room[i] - out[i]);
                    out[i] += give;
                    left -= give;
                    if(out[i] >= room[i]) {
                        full[i] = true;
                        open--;
                    }
                }
        }
        return out;
    }
}
