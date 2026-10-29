package com.arrl.radiocraft.common.blocks;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The parity union-find behind bundle turns: ties hold through chains, and an odd loop is refused. */
class BundleOrientationTest {

    private static int relation(BundleOrientation.Parity uf, int a, int b) {
        assertEquals(uf.find(a), uf.find(b), "not tied");
        return uf.parity(a) ^ uf.parity(b);
    }

    @Test
    void tiesCarryThroughAChain() {
        BundleOrientation.Parity uf = new BundleOrientation.Parity(6);
        assertTrue(uf.union(0, 1, 0));
        assertTrue(uf.union(1, 2, 1));
        assertTrue(uf.union(2, 3, 1));
        assertTrue(uf.union(4, 3, 0));
        assertEquals(0, relation(uf, 0, 1));
        assertEquals(1, relation(uf, 0, 2));
        assertEquals(0, relation(uf, 0, 3));
        assertEquals(0, relation(uf, 0, 4));
        assertEquals(1, relation(uf, 2, 4));
    }

    @Test
    void anOddLoopIsRefusedAndAnEvenOneAgrees() {
        BundleOrientation.Parity uf = new BundleOrientation.Parity(4);
        uf.union(0, 1, 1);
        uf.union(1, 2, 1);
        assertTrue(uf.union(2, 0, 0), "an even loop should agree");
        assertFalse(uf.union(2, 0, 1), "an odd loop should be refused");
        assertEquals(0, relation(uf, 0, 2)); // Unchanged by the refusal.
    }
}
