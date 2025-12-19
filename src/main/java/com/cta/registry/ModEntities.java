package com.cta.registry;

import com.cta.CTA;
import com.cta.entity.MissileEntity;
import com.cta.entity.ScopeEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, CTA.MODID);

    public static final RegistryObject<EntityType<MissileEntity>> MISSILE = ENTITIES.register("missile",
            () -> EntityType.Builder.<MissileEntity>of(MissileEntity::new, MobCategory.MISC)
                    .sized(0.5f, 0.5f)
                    .build("missile"));

    public static final RegistryObject<EntityType<ScopeEntity>> SCOPE = ENTITIES.register("scope",
            () -> EntityType.Builder.<ScopeEntity>of(ScopeEntity::new, MobCategory.MISC)
                    .sized(0.5f, 0.5f)
                    .build("scope"));

    public static void register(IEventBus eventBus) {
        ENTITIES.register(eventBus);
    }
}
