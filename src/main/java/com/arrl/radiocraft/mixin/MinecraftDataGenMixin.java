package com.arrl.radiocraft.mixin;

import com.arrl.radiocraft.mixin_mock.DataGenMinecraft;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;

@Mixin(Minecraft.class)
public class MinecraftDataGenMixin {

    private static final Logger LOGGER = LoggerFactory.getLogger("RadioCraft-DataGen-Mixin");
    private static Minecraft fakeInstance = null;

    @Inject(method = "getInstance", at = @At("HEAD"), cancellable = true)
    private static void returnDummyInstanceDuringDataGen(CallbackInfoReturnable<Minecraft> cir) {
        if (isDataGeneration()) {
            LOGGER.info("Data generation detected! Minecraft.getInstance() mixin activated.");
            LOGGER.info("Preventing Simple Voice Chat crash by returning mock instance.");

            if (fakeInstance == null) {
                fakeInstance = createFakeMinecraftInstance();
            }

            cir.setReturnValue(fakeInstance);
        }
    }

    private static Minecraft createFakeMinecraftInstance() {
        try {
            LOGGER.debug("Creating fake Minecraft instance using Unsafe.allocateInstance");

            // Get Unsafe instance
            sun.misc.Unsafe unsafe = getUnsafe();

            // Create uninitialized Minecraft instance (doesn't call constructor)
            Minecraft fakeMinecraft = (Minecraft) unsafe.allocateInstance(DataGenMinecraft.class);

            LOGGER.debug("Successfully created fake Minecraft instance");
            return fakeMinecraft;

        } catch (Exception e) {
            LOGGER.error("Failed to create fake Minecraft instance", e);
            // Fallback to null if unsafe allocation fails
            return null;
        }
    }

    private static sun.misc.Unsafe getUnsafe() throws Exception {
        Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return (sun.misc.Unsafe) field.get(null);
    }

    private static boolean isDataGeneration() {
        // Check command line arguments first (most reliable)
        String command = System.getProperty("sun.java.command", "");
        if (command.contains("runData") || command.contains("data")) {
            LOGGER.debug("Data generation detected via command line: {}", command);
            return true;
        }

        // Check stack trace for data generation classes
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        for (StackTraceElement element : stack) {
            String className = element.getClassName();
            String methodName = element.getMethodName();

            if (className.contains("DataGenerator") ||
                    className.contains("DataProvider") ||
                    className.contains("datagen") ||
                    className.contains("GatherDataEvent") ||
                    methodName.contains("gatherData")) {
                LOGGER.debug("Data generation detected via stack trace: {}.{}", className, methodName);
                return true;
            }
        }

        // Check for NeoForge data generation context
        for (StackTraceElement element : stack) {
            if (element.getClassName().contains("net.neoforged.neoforge.data")) {
                LOGGER.debug("Data generation detected via NeoForge data classes: {}", element.getClassName());
                return true;
            }
        }

        return false;
    }
}