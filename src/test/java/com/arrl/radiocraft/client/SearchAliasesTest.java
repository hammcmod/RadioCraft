package com.arrl.radiocraft.client;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchAliasesTest {

    @Test
    void spellsNumbers() {
        assertEquals("twenty", SearchAliases.words(20));
        assertEquals("one hundred forty six", SearchAliases.words(146));
        assertEquals("three thousand five hundred", SearchAliases.words(3500));
        assertEquals("nineteen", SearchAliases.words(19));
    }

    @Test
    void bandsAlsoAsMeters() {
        List<String> aliases = SearchAliases.of("Premium 20m Radio");
        assertTrue(aliases.contains("twenty"));
        assertTrue(aliases.contains("twenty meter"));
        assertTrue(aliases.contains("twenty metres"));
    }

    @Test
    void numbersInsideNames() {
        assertTrue(SearchAliases.of("5-BTV Multiband HF Antenna").contains("five"));
        List<String> balun = SearchAliases.of("Balun (9:1)");
        assertTrue(balun.contains("nine") && balun.contains("one"));
        assertTrue(SearchAliases.of("Antenna Analyzer").isEmpty());
    }
}
