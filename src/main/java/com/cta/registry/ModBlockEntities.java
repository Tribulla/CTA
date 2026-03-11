package com.cta.registry;

import com.cta.CTA;
import com.cta.block.ScopeBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registry for block entities
 */
public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = 
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, CTA.MODID);
    
    public static final RegistryObject<BlockEntityType<ScopeBlockEntity>> SCOPE_BE = 
            BLOCK_ENTITIES.register("scope", () -> 
                    BlockEntityType.Builder.of(ScopeBlockEntity::new, 
                            ModBlocks.SCOPE_BLOCK.get(), 
                            ModBlocks.THERMAL_SCOPE_BLOCK.get())
                    .build(null));
    
    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
