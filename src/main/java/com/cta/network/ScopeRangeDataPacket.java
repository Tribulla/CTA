package com.cta.network;

import com.cta.client.ScopeViewManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ScopeRangeDataPacket {
    private final boolean hasCannon;
    private final String shellName;
    private final double currentRange;
    private final int loadedCharges;
    private final int[] levels;
    private final double[] ranges;

    public ScopeRangeDataPacket(boolean hasCannon, String shellName, double currentRange,
                                 int loadedCharges, int[] levels, double[] ranges) {
        this.hasCannon = hasCannon;
        this.shellName = shellName;
        this.currentRange = currentRange;
        this.loadedCharges = loadedCharges;
        this.levels = levels;
        this.ranges = ranges;
    }

    public static void encode(ScopeRangeDataPacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.hasCannon);
        buf.writeUtf(msg.shellName);
        buf.writeDouble(msg.currentRange);
        buf.writeInt(msg.loadedCharges);
        buf.writeInt(msg.levels.length);
        for (int i = 0; i < msg.levels.length; i++) {
            buf.writeInt(msg.levels[i]);
            buf.writeDouble(msg.ranges[i]);
        }
    }

    public static ScopeRangeDataPacket decode(FriendlyByteBuf buf) {
        boolean hasCannon = buf.readBoolean();
        String shellName = buf.readUtf();
        double currentRange = buf.readDouble();
        int loadedCharges = buf.readInt();
        int count = buf.readInt();
        int[] levels = new int[count];
        double[] ranges = new double[count];
        for (int i = 0; i < count; i++) {
            levels[i] = buf.readInt();
            ranges[i] = buf.readDouble();
        }
        return new ScopeRangeDataPacket(hasCannon, shellName, currentRange, loadedCharges, levels, ranges);
    }

    public static void handle(ScopeRangeDataPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> handleClient(msg))
        );
        ctx.get().setPacketHandled(true);
    }

    private static void handleClient(ScopeRangeDataPacket msg) {
        ScopeViewManager.updateRangeData(msg.hasCannon, msg.shellName, msg.currentRange,
                msg.loadedCharges, msg.levels, msg.ranges);
    }
}
