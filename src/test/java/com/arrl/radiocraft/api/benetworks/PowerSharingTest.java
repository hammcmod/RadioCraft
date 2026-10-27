package com.arrl.radiocraft.api.benetworks;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PowerSharingTest {

    @Test
    void sharesEvenlyBetweenTakersWithRoom() {
        assertArrayEquals(new int[] { 30, 30, 30 }, PowerSharing.shareEvenly(90, new int[] { 100, 100, 100 }));
    }

    @Test
    void aFullTakerPassesItsShareOn() {
        // The first has room for 10 only: the other two split the rest.
        assertArrayEquals(new int[] { 10, 40, 40 }, PowerSharing.shareEvenly(90, new int[] { 10, 100, 100 }));
        assertArrayEquals(new int[] { 0, 45, 45 }, PowerSharing.shareEvenly(90, new int[] { 0, 100, 100 }));
    }

    @Test
    void neverMoreThanAskedOrThanThereIsRoomFor() {
        assertEquals(25, Arrays.stream(PowerSharing.shareEvenly(100, new int[] { 5, 20 })).sum());
        int[] remainder = PowerSharing.shareEvenly(10, new int[] { 100, 100, 100 });
        assertEquals(10, Arrays.stream(remainder).sum());
        assertEquals(1, Arrays.stream(remainder).max().getAsInt() - Arrays.stream(remainder).min().getAsInt());
        assertArrayEquals(new int[0], PowerSharing.shareEvenly(50, new int[0]));
        assertArrayEquals(new int[] { 0, 0 }, PowerSharing.shareEvenly(0, new int[] { 5, 5 }));
    }
}
