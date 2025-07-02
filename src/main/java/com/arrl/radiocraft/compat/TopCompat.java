package com.arrl.radiocraft.compat;

import mcjty.theoneprobe.api.*;
import net.neoforged.fml.InterModComms;
import net.neoforged.fml.ModList;

import javax.annotation.Nullable;
import java.util.function.Function;

public class TopCompat {

    public static void register() {
        if (!ModList.get().isLoaded("theoneprobe")) {
            return;
        }
        InterModComms.sendTo("theoneprobe", "getTheOneProbe", GetTheOneProbe::new);
    }

    public static class GetTheOneProbe implements Function<ITheOneProbe, Void> {

        public static ITheOneProbe probe;

        @Nullable
        @Override
        public Void apply(ITheOneProbe theOneProbe) {
            probe = theOneProbe;
            probe.registerProvider(TopProbe.getProbe());
            return null;
        }
    }
}