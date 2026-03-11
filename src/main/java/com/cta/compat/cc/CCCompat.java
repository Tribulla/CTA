package com.cta.compat.cc;

import com.cta.CTA;
import dan200.computercraft.api.ForgeComputerCraftAPI;
import net.minecraftforge.fml.ModList;

public class CCCompat {

    private static final boolean CC_LOADED = ModList.get().isLoaded("computercraft");

    public static boolean isLoaded() {
        return CC_LOADED;
    }

    public static void init() {
        if (!CC_LOADED) return;
        CTA.LOGGER.info("CC:Tweaked detected, registering wireless connector peripheral provider");
        ForgeComputerCraftAPI.registerPeripheralProvider(new CCPeripheralProvider());
    }
}
