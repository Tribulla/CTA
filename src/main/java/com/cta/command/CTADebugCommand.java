package com.cta.command;

import com.cta.CTA;
import com.cta.client.debug.MissileDebugRenderer;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CTA.MODID)
public class CTADebugCommand {
    
    private static boolean debugEnabled = false;
    
    public static boolean isDebugEnabled() {
        return debugEnabled;
    }
    
    public static void setDebugEnabled(boolean enabled) {
        debugEnabled = enabled;
        if (!enabled) {
            MissileDebugRenderer.clearAll();
        }
    }
    
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        
        dispatcher.register(
            Commands.literal("cta")
                .then(Commands.literal("debug")
                    .requires(source -> source.hasPermission(2))
                    .executes(context -> {
                        debugEnabled = !debugEnabled;
                        context.getSource().sendSuccess(() -> 
                            Component.literal("CTA Debug mode: " + (debugEnabled ? "§aON" : "§cOFF")), true);
                        if (!debugEnabled) {
                            MissileDebugRenderer.clearAll();
                        }
                        return 1;
                    })
                    .then(Commands.argument("enabled", BoolArgumentType.bool())
                        .executes(context -> {
                            debugEnabled = BoolArgumentType.getBool(context, "enabled");
                            context.getSource().sendSuccess(() -> 
                                Component.literal("CTA Debug mode: " + (debugEnabled ? "§aON" : "§cOFF")), true);
                            if (!debugEnabled) {
                                MissileDebugRenderer.clearAll();
                            }
                            return 1;
                        })
                    )
                )
                .then(Commands.literal("clear")
                    .requires(source -> source.hasPermission(2))
                    .executes(context -> {
                        MissileDebugRenderer.clearAll();
                        context.getSource().sendSuccess(() -> 
                            Component.literal("§eCleared all debug visualizations"), true);
                        return 1;
                    })
                )
        );
    }
}
