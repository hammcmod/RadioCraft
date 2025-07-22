package com.arrl.radiocraft.mixin_mock;

import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.RepositorySource;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

// Mock Minecraft class that overrides problematic methods
public class DataGenMinecraft extends Minecraft {

    private static final Logger LOGGER = LoggerFactory.getLogger("RadioCraft-DataGen-Mixin");
    private static PackRepository mockPackRepository;

    // This constructor will never be called due to Unsafe.allocateInstance
    public DataGenMinecraft() {
        super(null);
    }

    @Override
    public @NotNull PackRepository getResourcePackRepository() {
        LOGGER.debug("getResourcePackRepository() called on DataGenMinecraft - returning mock PackRepository");

        if (mockPackRepository == null) {
            mockPackRepository = createMockPackRepository();
        }

        return mockPackRepository;
    }

    private static PackRepository createMockPackRepository() {
        // Create a mock PackRepository that does nothing but doesn't crash
        return new PackRepository() {
            @Override
            public void addPackFinder(RepositorySource repositorySource) {
                LOGGER.debug("addPackFinder() called on mock PackRepository - ignoring");
            }

            @Override
            public void reload() {
                LOGGER.debug("reload() called on mock PackRepository - ignoring");
            }

            @Override
            public Set<String> getAvailableIds() {
                LOGGER.debug("getAvailableIds() called on mock PackRepository - returning empty set");
                return Set.of();
            }

            @Override
            public Set<String> getSelectedIds() {
                LOGGER.debug("getSelectedIds() called on mock PackRepository - returning empty set");
                return Set.of();
            }
        };
    }
}