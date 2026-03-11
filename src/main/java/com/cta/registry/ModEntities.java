package com.cta.registry;

import com.cta.CTA;
import com.cta.entity.FragmentEntity;
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

    // Single missile entity type - warhead type (HE/HEAT/HEFRAG) is stored as data
    public static final RegistryObject<EntityType<MissileEntity>> MISSILE = ENTITIES.register("missile",
            () -> EntityType.Builder.<MissileEntity>of(MissileEntity::new, MobCategory.MISC)
                    .sized(0.5f, 0.5f)
                    .clientTrackingRange(32) // Track missiles up to 32 chunks (512 blocks) away
                    .updateInterval(1) // Update every tick for smooth flight
                    .build("missile"));

    public static final RegistryObject<EntityType<ScopeEntity>> SCOPE = ENTITIES.register("scope",
            () -> EntityType.Builder.<ScopeEntity>of(ScopeEntity::new, MobCategory.MISC)
                    .sized(0.5f, 0.5f)
                    .build("scope"));

    // Keep the legacy thermal scope id mapped to the standard scope entity for save compatibility.
    public static final RegistryObject<EntityType<ScopeEntity>> THERMAL_SCOPE = ENTITIES.register("thermal_scope",
            () -> EntityType.Builder.<ScopeEntity>of(ScopeEntity::new, MobCategory.MISC)
                    .sized(0.5f, 0.5f)
                    .build("thermal_scope"));

    // Fragment entity for HEFRAG warhead shrapnel
    public static final RegistryObject<EntityType<FragmentEntity>> FRAGMENT = ENTITIES.register("fragment",
            () -> EntityType.Builder.<FragmentEntity>of(FragmentEntity::new, MobCategory.MISC)
                    .sized(0.1f, 0.1f) // Small hitbox
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build("fragment"));

    public static void register(IEventBus eventBus) {
        ENTITIES.register(eventBus);
    }
}
