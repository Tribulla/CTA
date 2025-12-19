package com.cta.registry;

import com.cta.CTA;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CTA.MODID);

    public static final RegistryObject<CreativeModeTab> CTA_TAB = CREATIVE_MODE_TABS.register("cta_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ModItems.AIM_9.get()))
                    .title(Component.translatable("creativetab.cta_tab"))
                    .displayItems((pParameters, pOutput) -> {
                        ModItems.ITEMS.getEntries().forEach(reg -> pOutput.accept(reg.get()));
                        ModBlocks.BLOCKS.getEntries().forEach(reg -> pOutput.accept(reg.get()));
                    })
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
