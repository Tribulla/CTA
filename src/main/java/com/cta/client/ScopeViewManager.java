package com.cta.client;

import com.cta.block.ScopeBlockEntity;
import com.cta.compat.VSCompat;
import com.cta.network.PacketHandler;
import com.cta.network.StopViewingPacket;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;

@OnlyIn(Dist.CLIENT)
public class ScopeViewManager {

    @Nullable
    private static BlockPos viewedScopePos = null;
    private static Vec3 cameraPosition = Vec3.ZERO;
    private static float cameraYaw = 0;
    private static float cameraPitch = 0;
    private static CameraType storedCameraType = CameraType.FIRST_PERSON;
    private static boolean isViewing = false;
    private static final double MAX_FORWARD_DISTANCE = 16.0;

    // Range data received from the server
    private static boolean hasBoundCannon = false;
    private static double rangeCurrentRange = -1;
    private static int rangeLoadedCharges = 0;
    private static int[] rangeLevels = new int[0];
    private static double[] rangeRanges = new double[0];

    public static void startViewing(BlockPos scopePos) {
        if (scopePos == null)
            return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null)
            return;
        viewedScopePos = scopePos;
        isViewing = true;
        storedCameraType = mc.options.getCameraType();
        mc.options.setCameraType(CameraType.FIRST_PERSON);
        updateCameraPosition();
    }

    public static void stopViewing() {
        if (!isViewing)
            return;
        Minecraft mc = Minecraft.getInstance();
        mc.options.setCameraType(storedCameraType);
        PacketHandler.INSTANCE.sendToServer(new StopViewingPacket(viewedScopePos));
        viewedScopePos = null;
        isViewing = false;
        clearRangeData();
    }

    public static void updateRangeData(boolean hasCannon, String shellName, double currentRange,
                                        int loadedCharges, int[] levels, double[] ranges) {
        hasBoundCannon = hasCannon;
        rangeCurrentRange = currentRange;
        rangeLoadedCharges = loadedCharges;
        rangeLevels = levels;
        rangeRanges = ranges;
    }

    private static void clearRangeData() {
        hasBoundCannon = false;
        rangeCurrentRange = -1;
        rangeLoadedCharges = 0;
        rangeLevels = new int[0];
        rangeRanges = new double[0];
    }

    public static boolean hasBoundCannon() { return hasBoundCannon; }
    public static double getCurrentRange() { return rangeCurrentRange; }
    public static int getLoadedCharges() { return rangeLoadedCharges; }
    public static int[] getRangeLevels() { return rangeLevels; }
    public static double[] getRangeValues() { return rangeRanges; }

    public static boolean isViewingScope() {
        if (!isViewing || viewedScopePos == null)
            return false;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            isViewing = false;
            viewedScopePos = null;
            return false;
        }
        return true;
    }

    @Nullable
    public static BlockPos getViewedScopePos() {
        return viewedScopePos;
    }

    @Nullable
    public static ScopeBlockEntity getViewedScopeBE() {
        if (viewedScopePos == null)
            return null;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null)
            return null;
        BlockEntity be = mc.level.getBlockEntity(viewedScopePos);
        if (be instanceof ScopeBlockEntity scopeBE)
            return scopeBE;
        return null;
    }

    public static Vec3 getCameraPosition() {
        return cameraPosition;
    }

    public static float getCameraYaw() {
        return cameraYaw;
    }

    public static float getCameraPitch() {
        return cameraPitch;
    }

    public static void updateCameraPosition() {
        if (!isViewing || viewedScopePos == null)
            return;
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null)
            return;

        BlockEntity be = level.getBlockEntity(viewedScopePos);
        if (!(be instanceof ScopeBlockEntity scopeBE)) {
            stopViewing();
            return;
        }

        // 1. Get the local Ship Space variables from the BlockEntity
        Vec3 localScopePos = scopeBE.getScopeEyePosition();
        float localYaw = scopeBE.getScopeYaw();
        float localPitch = scopeBE.getScopePitch();
        Vec3 localLookDir = scopeBE.getLookDirection();

        // 2. Convert everything into absolute World Space using VSCompat
        Vec3 worldScopePos = VSCompat.toWorldCoordinates(level, viewedScopePos, localScopePos);
        Vec3 worldLookDir = VSCompat.transformDirectionToWorld(level, viewedScopePos, localLookDir);
        float worldYaw = VSCompat.transformYawToWorld(level, viewedScopePos, localYaw);
        float worldPitch = VSCompat.transformPitchToWorld(level, viewedScopePos, localYaw, localPitch);

        // 3. Apply the converted coordinates to the camera
        cameraYaw = worldYaw;
        cameraPitch = worldPitch;
        cameraPosition = findClearPosition(level, worldScopePos, worldLookDir);
    }

    private static Vec3 findClearPosition(Level level, Vec3 start, Vec3 direction) {
        BlockPos startBlock = BlockPos.containing(start);
        if (isAirOrTransparent(level, startBlock))
            return start;

        double stepSize = 0.25;
        Vec3 current = start;
        for (double dist = stepSize; dist <= MAX_FORWARD_DISTANCE; dist += stepSize) {
            current = start.add(direction.scale(dist));
            BlockPos blockPos = BlockPos.containing(current);
            if (isAirOrTransparent(level, blockPos))
                return current;
        }
        return start.add(direction.scale(MAX_FORWARD_DISTANCE));
    }

    private static boolean isAirOrTransparent(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.isAir() || !state.canOcclude() || state.getBlock().propagatesSkylightDown(state, level, pos);
    }

    public static void tick() {
        if (!isViewing)
            return;
        if (viewedScopePos == null) {
            stopViewing();
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            stopViewing();
            return;
        }

        BlockEntity be = mc.level.getBlockEntity(viewedScopePos);
        if (!(be instanceof ScopeBlockEntity)) {
            stopViewing();
            return;
        }

        // Fix: Convert the scope's position to World Space before calculating distance!
        Vec3 worldPos = VSCompat.toWorldCoordinatesRobust(mc.level, viewedScopePos);
        
        if (player.distanceToSqr(worldPos) > 128 * 128) {
            stopViewing();
            return;
        }

        updateCameraPosition();
    }

    public static void handleExitKey() {
        if (isViewing)
            stopViewing();
    }
}