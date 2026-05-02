package com.cta.client;

import com.cta.network.OpenChannelScreenPacket;
import com.cta.network.ScopeRangeDataPacket;
import com.cta.network.SetCameraViewPacket;
import com.cta.network.StartScopeViewPacket;
import com.cta.client.screen.SetChannelScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ClientPacketHandler {

    public static void handleSetCameraViewPacket(SetCameraViewPacket msg) {
        Entity entity = null;
        if (msg.entityId != -1) {
            entity = Minecraft.getInstance().level.getEntity(msg.entityId);
        }
        Minecraft.getInstance().setCameraEntity(entity != null ? entity : Minecraft.getInstance().player);
    }

    public static void handleOpenChannelScreenPacket(OpenChannelScreenPacket msg) {
        SetChannelScreen.open(msg.peripheralPos, msg.targetPos);
    }

    public static void handleScopeRangeDataPacket(ScopeRangeDataPacket msg) {
        ScopeViewManager.updateRangeData(msg.hasCannon, msg.shellName, msg.currentRange,
                msg.loadedCharges, msg.levels, msg.ranges);
    }

    public static void handleStartScopeViewPacket(StartScopeViewPacket msg) {
        ScopeViewManager.startViewing(msg.scopePos);
    }
}
