package com.cta.registry;

import com.cta.CTA;
import com.cta.block.ScopeBlock;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, CTA.MODID);
    
    public static final RegistryObject<Block> SCOPE_BLOCK = BLOCKS.register("scope",
            ScopeBlock::new);
    
    public static final RegistryObject<Block> THERMAL_SCOPE_BLOCK = BLOCKS.register("thermal_scope",
            ScopeBlock::new);
    
    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
