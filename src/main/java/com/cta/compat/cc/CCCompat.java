package com.cta.compat.cc;

import com.cta.CTA;
import net.minecraftforge.fml.ModList;

public class CCCompat {

    private static Boolean ccLoaded = null;

    public static boolean isLoaded() {
        if (ccLoaded == null) {
            ccLoaded = ModList.get().isLoaded("computercraft");
        }
        return ccLoaded;
    }

    public static void init() {
        // If CC is not loaded, we do absolutely nothing.
        // This prevents the inner class from ever being called.
        if (isLoaded()) {
            CTA.LOGGER.info("CC:Tweaked detected, registering wireless connector peripheral provider");
            CCPeripheralHandler.register();
        }
    }

    // =====================================================================
    // INNER CLASS: This is only loaded into memory if CC is actually installed.
    // By putting the CC code in here, we avoid the ClassNotFoundException crash!
    // =====================================================================
    private static class CCPeripheralHandler {
        static void register() {
            // Use the fully qualified name here so we don't need an 'import' at the top of the file
            dan200.computercraft.api.ForgeComputerCraftAPI.registerPeripheralProvider(new CCPeripheralProvider());
        }
    }
}