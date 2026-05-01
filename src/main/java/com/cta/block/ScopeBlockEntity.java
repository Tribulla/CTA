package com.cta.block;

import com.cta.compat.CBCCompat;
import com.cta.compat.VSCompat;
import com.cta.network.PacketHandler;
import com.cta.network.ScopeRangeDataPacket;
import com.cta.network.StartScopeViewPacket;
import com.cta.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.util.List;

public class ScopeBlockEntity extends BlockEntity {

    private float scopeYaw = 0;
    private float scopePitch = 0;

    private WeakReference<ServerPlayer> currentViewer = new WeakReference<>(null);
    private boolean beingViewed = false;
    private int rangeUpdateTick = 0;

    @Nullable
    private BlockPos boundCannonMount = null;

    public ScopeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SCOPE_BE.get(), pos, state);
        if (state.hasProperty(ScopeBlock.FACING)) {
            Direction facing = state.getValue(ScopeBlock.FACING);
            this.scopeYaw = facingToYaw(facing);
            this.scopePitch = facingToPitch(facing);
        }
    }

    private static float facingToYaw(Direction dir) {
        return switch (dir) {
            case SOUTH -> 0;
            case WEST -> 90;
            case NORTH -> 180;
            case EAST -> 270;
            default -> 0;
        };
    }

    private static float facingToPitch(Direction dir) {
        return switch (dir) {
            case UP -> -90;
            case DOWN -> 90;
            default -> 0;
        };
    }

    public float getScopeYaw() {
        return scopeYaw;
    }

    public float getScopePitch() {
        return scopePitch;
    }

    public boolean isBeingViewed() {
        return beingViewed;
    }

    public void setScopeRotation(float yaw, float pitch) {
        this.scopeYaw = yaw;
        this.scopePitch = pitch;
        setChanged();
    }

    @Nullable
    public BlockPos getBoundCannonMount() {
        return boundCannonMount;
    }

    public void bindToCannonMount(@Nullable BlockPos pos) {
        this.boundCannonMount = pos;
        sync();
    }

    public Vec3 getScopeEyePosition() {
        return new Vec3(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5);
    }

    public Vec3 getLookDirection() {
        double yawRad = Math.toRadians(-scopeYaw);
        double pitchRad = Math.toRadians(-scopePitch);
        double cosYaw = Math.cos(yawRad);
        double sinYaw = Math.sin(yawRad);
        double cosPitch = Math.cos(pitchRad);
        double sinPitch = Math.sin(pitchRad);
        return new Vec3(sinYaw * cosPitch, sinPitch, cosYaw * cosPitch).normalize();
    }

    public void startViewing(ServerPlayer player) {
        this.currentViewer = new WeakReference<>(player);
        this.beingViewed = true;
        setChanged();
        PacketHandler.INSTANCE.send(
                PacketDistributor.PLAYER.with(() -> player),
            new StartScopeViewPacket(this.worldPosition));
        sendRangeDataTo(player);
    }

    private void sendRangeDataTo(ServerPlayer player) {
        if (boundCannonMount == null) {
            PacketHandler.INSTANCE.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    new ScopeRangeDataPacket(false, "", -1, 0, new int[0], new double[0]));
            return;
        }
        CBCCompat.CannonRangeData data = CBCCompat.getCannonRangeData(level, boundCannonMount);
        if (data == null) {
            PacketHandler.INSTANCE.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    new ScopeRangeDataPacket(true, "?", -1, 0, new int[0], new double[0]));
            return;
        }
        List<CBCCompat.RangeSample> samples = data.samples();
        int[] lvls = new int[samples.size()];
        double[] rngs = new double[samples.size()];
        for (int i = 0; i < samples.size(); i++) {
            lvls[i] = samples.get(i).chargeLevel();
            rngs[i] = samples.get(i).range();
        }
        PacketHandler.INSTANCE.send(
                PacketDistributor.PLAYER.with(() -> player),
                new ScopeRangeDataPacket(true, data.shellName(), data.currentRange(),
                        data.loadedChargeLevels(), lvls, rngs));
    }

    public void stopViewing(ServerPlayer player) {
        this.currentViewer.clear();
        this.beingViewed = false;
        setChanged();
    }

    public void onRemoved() {
        ServerPlayer viewer = currentViewer.get();
        if (viewer != null) {
            stopViewing(viewer);
        }
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }


    @Nullable
    public BlockPos findNearbyCannonMount(int radius) {
        if (!CBCCompat.isCBCLoaded() || level == null)
            return null;
        BlockPos center = this.worldPosition;
        BlockPos closest = null;
        double closestDist = Double.MAX_VALUE;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos check = center.offset(x, y, z);
                    if (CBCCompat.isCannonMount(level, check)) {
                        double dist = center.distSqr(check);
                        if (dist < closestDist) {
                            closestDist = dist;
                            closest = check;
                        }
                    }
                }
            }
        }
        return closest;
    }


    public static void serverTick(Level level, BlockPos pos, BlockState state, ScopeBlockEntity be) {
        if (be.boundCannonMount != null && CBCCompat.isCBCLoaded()) {
            Float cannonYaw = CBCCompat.getCannonMountYaw(level, be.boundCannonMount);
            Float cannonPitch = CBCCompat.getCannonMountPitch(level, be.boundCannonMount);
            if (cannonYaw != null && cannonPitch != null) {
                be.scopeYaw = cannonYaw;
                be.scopePitch = cannonPitch;
                be.setChanged();
            } else {
                be.boundCannonMount = null;
                be.setChanged();
            }
        }

        // Periodically push range data to the viewing player
        if (be.beingViewed) {
            be.rangeUpdateTick++;
            if (be.rangeUpdateTick >= 20) {
                be.rangeUpdateTick = 0;
                ServerPlayer rangeViewer = be.currentViewer.get();
                if (rangeViewer != null) {
                    be.sendRangeDataTo(rangeViewer);
                }
            }
        }

        ServerPlayer viewer = be.currentViewer.get();
        if (viewer != null) {
            // FIX: Convert the block's Ship Space position into actual World Space 
            // so the distance check passes properly when mounted on a moving Valkyrien Skies ship.
            Vec3 worldPos = VSCompat.toWorldCoordinatesRobust(level, pos);
            
            if (viewer.isDeadOrDying() || viewer.level() != level ||
                    viewer.distanceToSqr(worldPos) > 128 * 128) {
                be.stopViewing(viewer);
            }
        } else if (be.beingViewed) {
            be.beingViewed = false;
            be.setChanged();
        }
    }


    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putFloat("ScopeYaw", scopeYaw);
        tag.putFloat("ScopePitch", scopePitch);
        if (boundCannonMount != null) {
            tag.putInt("BoundCannonX", boundCannonMount.getX());
            tag.putInt("BoundCannonY", boundCannonMount.getY());
            tag.putInt("BoundCannonZ", boundCannonMount.getZ());
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.scopeYaw = tag.getFloat("ScopeYaw");
        this.scopePitch = tag.getFloat("ScopePitch");
        if (tag.contains("BoundCannonX")) {
            this.boundCannonMount = new BlockPos(
                    tag.getInt("BoundCannonX"), tag.getInt("BoundCannonY"), tag.getInt("BoundCannonZ"));
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection net,
            net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket pkt) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            load(tag);
        }
    }
}