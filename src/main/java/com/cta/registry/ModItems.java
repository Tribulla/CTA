package com.cta.registry;

import com.cta.CTA;
import com.cta.item.MissileItem;
import com.cta.item.ScopeItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, CTA.MODID);

    // Scope - placeable entity, not a block
    public static final RegistryObject<Item> PANTHER_SCOPE = ITEMS.register("panther_scope", () -> new ScopeItem(new Item.Properties()));

    // Missiles - have fuel, fly with thrust in their facing direction
    public static final RegistryObject<Item> KALIBR_3M54 = ITEMS.register("3m54_kalibr", () -> new MissileItem(new Item.Properties(), "3m54_kalibr", false));
    public static final RegistryObject<Item> AGM_88 = ITEMS.register("agm_88", () -> new MissileItem(new Item.Properties(), "agm_88", false));
    public static final RegistryObject<Item> AIM_9 = ITEMS.register("aim_9", () -> new MissileItem(new Item.Properties(), "aim_9", false));
    public static final RegistryObject<Item> APKWS = ITEMS.register("apkws", () -> new MissileItem(new Item.Properties(), "apkws", false));
    public static final RegistryObject<Item> HELLFIRE = ITEMS.register("hellfire", () -> new MissileItem(new Item.Properties(), "hellfire", false));
    public static final RegistryObject<Item> HYDRA_70 = ITEMS.register("hydra_70", () -> new MissileItem(new Item.Properties(), "hydra_70", false));
    public static final RegistryObject<Item> KATYUSHA = ITEMS.register("katyusha", () -> new MissileItem(new Item.Properties(), "katyusha", false));
    public static final RegistryObject<Item> R_77 = ITEMS.register("r_77", () -> new MissileItem(new Item.Properties(), "r_77", false));
    public static final RegistryObject<Item> TOW_2 = ITEMS.register("tow_2", () -> new MissileItem(new Item.Properties(), "tow_2", false));
    
    // Bombs - no fuel, drop with gravity when released
    public static final RegistryObject<Item> CBU_87 = ITEMS.register("cbu_87", () -> new MissileItem(new Item.Properties(), "cbu_87", true));
    public static final RegistryObject<Item> GBU_12 = ITEMS.register("gbu_12", () -> new MissileItem(new Item.Properties(), "gbu_12", true));
    public static final RegistryObject<Item> GBU_24 = ITEMS.register("gbu_24", () -> new MissileItem(new Item.Properties(), "gbu_24", true));
    public static final RegistryObject<Item> M64 = ITEMS.register("m64", () -> new MissileItem(new Item.Properties(), "m64", true));
    public static final RegistryObject<Item> ROCKEYE = ITEMS.register("rockeye", () -> new MissileItem(new Item.Properties(), "rockeye", true));
    public static final RegistryObject<Item> SNAKEYE = ITEMS.register("snakeye", () -> new MissileItem(new Item.Properties(), "snakeye", true));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
