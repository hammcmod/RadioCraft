package com.arrl.radiocraft.common.telephone;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Reading PABX numbers: master number lengths, master number then extension, names and the phone's label. */
class PabxNumbersTest {

    @Test
    void masterIsEmptyOrExactlyTheLength() {
        assertTrue(PabxNumbers.validMaster("", 3));
        assertTrue(PabxNumbers.validMaster("603", 3));
        assertTrue(PabxNumbers.validMaster("007", 3));
        assertFalse(PabxNumbers.validMaster("60", 3));
        assertFalse(PabxNumbers.validMaster("6031", 3));
        assertFalse(PabxNumbers.validMaster("6a3", 3));
        assertFalse(PabxNumbers.validMaster("60 ", 3));
        assertTrue(PabxNumbers.validMaster("6", 1));
    }

    @Test
    void extensionFollowsTheMasterNumber() {
        assertEquals("100", PabxNumbers.extensionAfter("603100", "603"));
        assertEquals("0", PabxNumbers.extensionAfter("6030", "603"));
        assertNull(PabxNumbers.extensionAfter("603", "603"), "the master number alone is no extension");
        assertNull(PabxNumbers.extensionAfter("602100", "603"));
        assertNull(PabxNumbers.extensionAfter("100", ""), "an empty master number cannot be dialed");
        assertNull(PabxNumbers.extensionAfter("60310a", "603"));
    }

    @Test
    void extensionsMatchExactly() {
        assertTrue(PabxNumbers.isExtension("100", 100));
        assertFalse(PabxNumbers.isExtension("0100", 100));
        assertFalse(PabxNumbers.isExtension("10", 100));
    }

    @Test
    void namesAreCleanedAndCut() {
        assertEquals("OFFICE", PabxNumbers.cleanName("office"));
        assertEquals("NORTH-1", PabxNumbers.cleanName(" north-1 "));
        assertEquals("ABCDEFGHIJ", PabxNumbers.cleanName("abcdefghijklm"));
        assertEquals("OFF ICE-1X", PabxNumbers.cleanName("off ice-1!x"));
        assertEquals("", PabxNumbers.cleanName("!!!"));
    }

    @Test
    void labelShowsTheNameWhenThereIsOne() {
        assertEquals("PABX 603: OFFICE", PabxNumbers.label("603", "OFFICE"));
        assertEquals("PABX 603", PabxNumbers.label("603", ""));
    }

    @Test
    void digitsAreAsciiOnly() {
        assertTrue(PabxNumbers.digits("0123456789"));
        assertFalse(PabxNumbers.digits(""));
        assertFalse(PabxNumbers.digits("١٢٣")); // Arabic-Indic digits are digits to Character.isDigit, not to a phone.
    }
}
