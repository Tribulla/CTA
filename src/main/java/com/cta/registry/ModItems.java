package com.cta.registry;

import com.cta.CTA;
import com.cta.entity.MissileEntity.WarheadType;
import com.cta.item.MissileItem;
import com.cta.item.ScopeItem;
import com.cta.item.WirelessConnectorItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, CTA.MODID);

    public static final RegistryObject<Item> PANTHER_SCOPE = ITEMS.register("panther_scope", () -> new ScopeItem(new Item.Properties()));

    public static final RegistryObject<Item> WIRELESS_CONNECTOR = ITEMS.register("wireless_connector",
            () -> new WirelessConnectorItem(new Item.Properties()));
    
    // 3M-54 Kalibr
    public static final RegistryObject<Item> KALIBR_3M54_HE = ITEMS.register("3m_54_kalibr_he", 
            () -> new MissileItem(new Item.Properties(), "3m54_kalibr", false, WarheadType.HE));
    public static final RegistryObject<Item> KALIBR_3M54_HEAT = ITEMS.register("3m_54_kalibr_heat", 
            () -> new MissileItem(new Item.Properties(), "3m54_kalibr", false, WarheadType.HEAT));
    public static final RegistryObject<Item> KALIBR_3M54_HEFRAG = ITEMS.register("3m_54_kalibr_hefrag", 
            () -> new MissileItem(new Item.Properties(), "3m54_kalibr", false, WarheadType.HEFRAG));
    
    // AGM-88 HARM
    public static final RegistryObject<Item> AGM_88_HE = ITEMS.register("agm_88_he", 
            () -> new MissileItem(new Item.Properties(), "agm_88", false, WarheadType.HE));
    public static final RegistryObject<Item> AGM_88_HEAT = ITEMS.register("agm_88_heat", 
            () -> new MissileItem(new Item.Properties(), "agm_88", false, WarheadType.HEAT));
    public static final RegistryObject<Item> AGM_88_HEFRAG = ITEMS.register("agm_88_hefrag", 
            () -> new MissileItem(new Item.Properties(), "agm_88", false, WarheadType.HEFRAG));
    
    // AIM-9 Sidewinder
    public static final RegistryObject<Item> AIM_9_HE = ITEMS.register("aim_9_he", 
            () -> new MissileItem(new Item.Properties(), "aim_9", false, WarheadType.HE));
    public static final RegistryObject<Item> AIM_9_HEAT = ITEMS.register("aim_9_heat", 
            () -> new MissileItem(new Item.Properties(), "aim_9", false, WarheadType.HEAT));
    public static final RegistryObject<Item> AIM_9_HEFRAG = ITEMS.register("aim_9_hefrag", 
            () -> new MissileItem(new Item.Properties(), "aim_9", false, WarheadType.HEFRAG));
    
    // APKWS
    public static final RegistryObject<Item> APKWS_HE = ITEMS.register("apkws_he", 
            () -> new MissileItem(new Item.Properties(), "apkws", false, WarheadType.HE));
    public static final RegistryObject<Item> APKWS_HEAT = ITEMS.register("apkws_heat", 
            () -> new MissileItem(new Item.Properties(), "apkws", false, WarheadType.HEAT));
    public static final RegistryObject<Item> APKWS_HEFRAG = ITEMS.register("apkws_hefrag", 
            () -> new MissileItem(new Item.Properties(), "apkws", false, WarheadType.HEFRAG));
    
    // Hellfire
    public static final RegistryObject<Item> HELLFIRE_HE = ITEMS.register("hellfire_he", 
            () -> new MissileItem(new Item.Properties(), "hellfire", false, WarheadType.HE));
    public static final RegistryObject<Item> HELLFIRE_HEAT = ITEMS.register("hellfire_heat", 
            () -> new MissileItem(new Item.Properties(), "hellfire", false, WarheadType.HEAT));
    public static final RegistryObject<Item> HELLFIRE_HEFRAG = ITEMS.register("hellfire_hefrag", 
            () -> new MissileItem(new Item.Properties(), "hellfire", false, WarheadType.HEFRAG));
    
    // Hydra-70
    public static final RegistryObject<Item> HYDRA_70_HE = ITEMS.register("hydra_70_he", 
            () -> new MissileItem(new Item.Properties(), "hydra_70", false, WarheadType.HE));
    public static final RegistryObject<Item> HYDRA_70_HEAT = ITEMS.register("hydra_70_heat", 
            () -> new MissileItem(new Item.Properties(), "hydra_70", false, WarheadType.HEAT));
    public static final RegistryObject<Item> HYDRA_70_HEFRAG = ITEMS.register("hydra_70_hefrag", 
            () -> new MissileItem(new Item.Properties(), "hydra_70", false, WarheadType.HEFRAG));
    
    // Katyusha
    public static final RegistryObject<Item> KATYUSHA_HE = ITEMS.register("katyusha_he", 
            () -> new MissileItem(new Item.Properties(), "katyusha", false, WarheadType.HE));
    public static final RegistryObject<Item> KATYUSHA_HEAT = ITEMS.register("katyusha_heat", 
            () -> new MissileItem(new Item.Properties(), "katyusha", false, WarheadType.HEAT));
    public static final RegistryObject<Item> KATYUSHA_HEFRAG = ITEMS.register("katyusha_hefrag", 
            () -> new MissileItem(new Item.Properties(), "katyusha", false, WarheadType.HEFRAG));
    
    // R-77
    public static final RegistryObject<Item> R_77_HE = ITEMS.register("r_77_he", 
            () -> new MissileItem(new Item.Properties(), "r_77", false, WarheadType.HE));
    public static final RegistryObject<Item> R_77_HEAT = ITEMS.register("r_77_heat", 
            () -> new MissileItem(new Item.Properties(), "r_77", false, WarheadType.HEAT));
    public static final RegistryObject<Item> R_77_HEFRAG = ITEMS.register("r_77_hefrag", 
            () -> new MissileItem(new Item.Properties(), "r_77", false, WarheadType.HEFRAG));
    
    // TOW-2
    public static final RegistryObject<Item> TOW_2_HE = ITEMS.register("tow_2_he", 
            () -> new MissileItem(new Item.Properties(), "tow_2", false, WarheadType.HE));
    public static final RegistryObject<Item> TOW_2_HEAT = ITEMS.register("tow_2_heat", 
            () -> new MissileItem(new Item.Properties(), "tow_2", false, WarheadType.HEAT));
    public static final RegistryObject<Item> TOW_2_HEFRAG = ITEMS.register("tow_2_hefrag", 
            () -> new MissileItem(new Item.Properties(), "tow_2", false, WarheadType.HEFRAG));
    
    // CBU-87
    public static final RegistryObject<Item> CBU_87_HE = ITEMS.register("cbu_87_he", 
            () -> new MissileItem(new Item.Properties(), "cbu_87", true, WarheadType.HE));
    public static final RegistryObject<Item> CBU_87_HEAT = ITEMS.register("cbu_87_heat", 
            () -> new MissileItem(new Item.Properties(), "cbu_87", true, WarheadType.HEAT));
    public static final RegistryObject<Item> CBU_87_HEFRAG = ITEMS.register("cbu_87_hefrag", 
            () -> new MissileItem(new Item.Properties(), "cbu_87", true, WarheadType.HEFRAG));
    
    // GBU-12
    public static final RegistryObject<Item> GBU_12_HE = ITEMS.register("gbu_12_he", 
            () -> new MissileItem(new Item.Properties(), "gbu_12", true, WarheadType.HE));
    public static final RegistryObject<Item> GBU_12_HEAT = ITEMS.register("gbu_12_heat", 
            () -> new MissileItem(new Item.Properties(), "gbu_12", true, WarheadType.HEAT));
    public static final RegistryObject<Item> GBU_12_HEFRAG = ITEMS.register("gbu_12_hefrag", 
            () -> new MissileItem(new Item.Properties(), "gbu_12", true, WarheadType.HEFRAG));
    
    // GBU-24
    public static final RegistryObject<Item> GBU_24_HE = ITEMS.register("gbu_24_he", 
            () -> new MissileItem(new Item.Properties(), "gbu_24", true, WarheadType.HE));
    public static final RegistryObject<Item> GBU_24_HEAT = ITEMS.register("gbu_24_heat", 
            () -> new MissileItem(new Item.Properties(), "gbu_24", true, WarheadType.HEAT));
    public static final RegistryObject<Item> GBU_24_HEFRAG = ITEMS.register("gbu_24_hefrag", 
            () -> new MissileItem(new Item.Properties(), "gbu_24", true, WarheadType.HEFRAG));
    
    // M64
    public static final RegistryObject<Item> M64_HE = ITEMS.register("m64_he", 
            () -> new MissileItem(new Item.Properties(), "m64", true, WarheadType.HE));
    
    // Rockeye
    public static final RegistryObject<Item> ROCKEYE_HE = ITEMS.register("rockeye_he", 
            () -> new MissileItem(new Item.Properties(), "rockeye", true, WarheadType.HE));
    public static final RegistryObject<Item> ROCKEYE_HEAT = ITEMS.register("rockeye_heat", 
            () -> new MissileItem(new Item.Properties(), "rockeye", true, WarheadType.HEAT));
    public static final RegistryObject<Item> ROCKEYE_HEFRAG = ITEMS.register("rockeye_hefrag", 
            () -> new MissileItem(new Item.Properties(), "rockeye", true, WarheadType.HEFRAG));
    
    // Snakeye
    public static final RegistryObject<Item> SNAKEYE_HE = ITEMS.register("snakeye_he", 
            () -> new MissileItem(new Item.Properties(), "snakeye", true, WarheadType.HE));
    public static final RegistryObject<Item> SNAKEYE_HEAT = ITEMS.register("snakeye_heat", 
            () -> new MissileItem(new Item.Properties(), "snakeye", true, WarheadType.HEAT));
    public static final RegistryObject<Item> SNAKEYE_HEFRAG = ITEMS.register("snakeye_hefrag", 
            () -> new MissileItem(new Item.Properties(), "snakeye", true, WarheadType.HEFRAG));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
