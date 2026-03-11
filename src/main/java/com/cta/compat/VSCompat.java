package com.cta.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.ModList;

import javax.annotation.Nullable;
import java.lang.reflect.Method;

public class VSCompat {
    private static Boolean vsLoaded = null;
    
    private static Class<?> vsGameUtilsClass = null;
    private static Class<?> shipClass = null;
    private static Class<?> shipTransformClass = null;
    private static Class<?> vectorConversionsClass = null;
    private static Class<?> matrix4dcClass = null;
    private static Class<?> vector3dClass = null;
    
    private static Method getShipManagingPosMethod = null;
    private static Method getShipToWorldMethod = null;
    private static Method getWorldToShipMethod = null;
    private static Method getTransformMethod = null;
    private static Method transformPositionMethod = null;
    private static Method transformDirectionMethod = null;
    private static Method toJOMLMethod = null;
    private static Method toMinecraftMethod = null;
    private static Method getPositionInWorldMethod = null;
    private static Method getVelocityMethod = null;
    private static Method getOmegaMethod = null;
    private static Method isBlockInShipyardMethod = null;
    private static Method getAllShipsMethod = null;
    private static Method getShipWorldMethod = null;
    private static Method getLoadedShipsMethod = null;
    private static Method getByChunkPosMethod = null;
    
    private static boolean reflectionInitialized = false;
    private static boolean reflectionFailed = false;

    public static boolean isVSLoaded() {
        if (vsLoaded == null) {
            vsLoaded = ModList.get().isLoaded("valkyrienskies");
        }
        return vsLoaded;
    }

    private static void initReflection() {
        if (reflectionInitialized || reflectionFailed) return;
        
        try {
            vsGameUtilsClass = Class.forName("org.valkyrienskies.mod.common.VSGameUtilsKt");
            shipClass = Class.forName("org.valkyrienskies.core.api.ships.Ship");
            shipTransformClass = Class.forName("org.valkyrienskies.core.api.ships.properties.ShipTransform");
            vectorConversionsClass = Class.forName("org.valkyrienskies.mod.common.util.VectorConversionsMCKt");
            matrix4dcClass = Class.forName("org.joml.Matrix4dc");
            vector3dClass = Class.forName("org.joml.Vector3d");
            
            getShipManagingPosMethod = vsGameUtilsClass.getMethod("getShipManagingPos", Level.class, BlockPos.class);
            getTransformMethod = shipClass.getMethod("getTransform");
            getShipToWorldMethod = shipTransformClass.getMethod("getShipToWorld");
            getWorldToShipMethod = shipTransformClass.getMethod("getWorldToShip");
            getPositionInWorldMethod = shipTransformClass.getMethod("getPositionInWorld");
            
            transformPositionMethod = matrix4dcClass.getMethod("transformPosition", vector3dClass);
            transformDirectionMethod = matrix4dcClass.getMethod("transformDirection", vector3dClass);
            
            toJOMLMethod = vectorConversionsClass.getMethod("toJOML", Vec3.class);
            toMinecraftMethod = vectorConversionsClass.getMethod("toMinecraft", Class.forName("org.joml.Vector3dc"));
            
            getVelocityMethod = shipClass.getMethod("getVelocity");
            getOmegaMethod = shipClass.getMethod("getOmega");
            
            isBlockInShipyardMethod = vsGameUtilsClass.getMethod("isBlockInShipyard", Level.class, BlockPos.class);
            
            getAllShipsMethod = vsGameUtilsClass.getMethod("getAllShips", Level.class);
            
            try {
                Class<?> queryableShipDataClass = Class.forName("org.valkyrienskies.core.internal.ships.VsiQueryableShipData");
                getByChunkPosMethod = queryableShipDataClass.getMethod("getByChunkPos", int.class, int.class);
            } catch (Exception e2) {
                System.err.println("[CTA] Could not init getByChunkPos: " + e2.getMessage());
            }
            
            try {
                getShipWorldMethod = vsGameUtilsClass.getMethod("getShipObjectWorld", Level.class);
                Class<?> vsiShipWorldClass = Class.forName("org.valkyrienskies.core.internal.world.VsiShipWorld");
                getLoadedShipsMethod = vsiShipWorldClass.getMethod("getLoadedShips");
            } catch (Exception e2) {
                System.err.println("[CTA] Could not init ship world iteration: " + e2.getMessage());
            }
            
            reflectionInitialized = true;
        } catch (Exception e) {
            reflectionFailed = true;
            System.err.println("[CTA] Failed to initialize VS reflection: " + e.getMessage());
        }
    }
    
    @Nullable
    public static Object getShipManagingPos(Level level, BlockPos pos) {
        if (!isVSLoaded()) return null;
        initReflection();
        if (reflectionFailed) return null;
        
        try {
            return getShipManagingPosMethod.invoke(null, level, pos);
        } catch (Exception e) {
            return null;
        }
    }
    
    public static boolean isOnShip(Level level, BlockPos pos) {
        if (!isVSLoaded()) return false;
        initReflection();
        if (reflectionFailed) return false;
        
        if (getShipManagingPos(level, pos) != null) return true;
        
        if (isBlockInShipyard(level, pos)) return true;
        
        return false;
    }

    @Nullable
    public static Object getShipForBlockPos(Level level, BlockPos pos) {
        if (!isVSLoaded()) return null;
        initReflection();
        if (reflectionFailed) return null;

        try {
            Object ship = getShipManagingPosMethod.invoke(null, level, pos);
            if (ship != null) return ship;
        } catch (Exception e) {
        }

        if (getAllShipsMethod != null && getByChunkPosMethod != null) {
            try {
                Object shipData = getAllShipsMethod.invoke(null, level);
                if (shipData != null) {
                    int chunkX = pos.getX() >> 4;
                    int chunkZ = pos.getZ() >> 4;
                    Object ship = getByChunkPosMethod.invoke(shipData, chunkX, chunkZ);
                    if (ship != null) return ship;
                }
            } catch (Exception e) {
            }
        }

        for (Object ship : getAllLoadedShips(level)) {
            try {
                Method getChunkClaim = ship.getClass().getMethod("getChunkClaim");
                Object chunkClaim = getChunkClaim.invoke(ship);
                if (chunkClaim != null) {
                    Method contains = chunkClaim.getClass().getMethod("contains", int.class, int.class);
                    int chunkX = pos.getX() >> 4;
                    int chunkZ = pos.getZ() >> 4;
                    Object result = contains.invoke(chunkClaim, chunkX, chunkZ);
                    if (result instanceof Boolean && (Boolean) result) {
                        return ship;
                    }
                }
            } catch (Exception e) {
            }
        }

        return null;
    }

    public static Vec3 toWorldCoordinatesRobust(Level level, BlockPos pos) {
        Vec3 shipLocalPos = Vec3.atCenterOf(pos);
        if (!isVSLoaded()) return shipLocalPos;
        initReflection();
        if (reflectionFailed) return shipLocalPos;

        Object ship = getShipForBlockPos(level, pos);
        if (ship == null) return shipLocalPos;

        return toWorldCoordinatesWithShip(ship, shipLocalPos);
    }
    
    public static boolean isBlockInShipyard(Level level, BlockPos pos) {
        if (!isVSLoaded()) return false;
        initReflection();
        if (reflectionFailed || isBlockInShipyardMethod == null) return false;
        
        try {
            Object result = isBlockInShipyardMethod.invoke(null, level, pos);
            return result instanceof Boolean && (Boolean) result;
        } catch (Exception e) {
            return false;
        }
    }
    
    public static BlockPos getShipLocalBlockPos(Level level, @Nullable BlockPos placementPos, Vec3 worldPos) {
        if (placementPos != null && isOnShip(level, placementPos)) {
            return placementPos;
        }
        
        return BlockPos.containing(worldPos);
    }
    
    public static boolean hasRedstoneSignal(Level level, @Nullable BlockPos placementPos, Vec3 worldPos) {
        BlockPos checkPos = getShipLocalBlockPos(level, placementPos, worldPos);
        return level.hasNeighborSignal(checkPos);
    }
    
    public static boolean updateEntityPositionOnShip(Entity entity, @Nullable BlockPos shipBlockPos, Vec3 shipLocalPos) {
        if (!isVSLoaded() || shipBlockPos == null) return false;
        
        Level level = entity.level();
        if (!isOnShip(level, shipBlockPos)) return false;
        
        Vec3 worldPos = toWorldCoordinates(level, shipBlockPos, shipLocalPos);
        
        entity.setPos(worldPos.x, worldPos.y, worldPos.z);
        return true;
    }
    
    public static Vec3 getShipVelocity(Level level, @Nullable BlockPos shipBlockPos) {
        if (!isVSLoaded() || shipBlockPos == null) return Vec3.ZERO;
        initReflection();
        if (reflectionFailed) return Vec3.ZERO;
        
        try {
            Object ship = getShipManagingPosMethod.invoke(null, level, shipBlockPos);
            if (ship == null) return Vec3.ZERO;
            
            if (getVelocityMethod != null) {
                Object velocity = getVelocityMethod.invoke(ship);
                if (velocity instanceof org.joml.Vector3d) {
                    org.joml.Vector3d vel = (org.joml.Vector3d) velocity;
                    return new Vec3(vel.x, vel.y, vel.z);
                }
            }
            return Vec3.ZERO;
        } catch (Exception e) {
            return Vec3.ZERO;
        }
    }
    
    public static Vec3 toWorldCoordinates(Level level, BlockPos shipBlockPos, Vec3 shipLocalPos) {
        if (!isVSLoaded()) return shipLocalPos;
        initReflection();
        if (reflectionFailed) return shipLocalPos;
        
        try {
            Object ship = getShipManagingPosMethod.invoke(null, level, shipBlockPos);
            if (ship == null) return shipLocalPos;
            
            Object localJoml = toJOMLMethod.invoke(null, shipLocalPos);
            
            Object transform = getTransformMethod.invoke(ship);
            Object shipToWorld = getShipToWorldMethod.invoke(transform);
            
            transformPositionMethod.invoke(shipToWorld, localJoml);
            
            return (Vec3) toMinecraftMethod.invoke(null, localJoml);
        } catch (Exception e) {
            return shipLocalPos;
        }
    }
    
    public static Vec3 transformDirectionToWorld(Level level, BlockPos shipBlockPos, Vec3 shipLocalDirection) {
        if (!isVSLoaded()) return shipLocalDirection;
        initReflection();
        if (reflectionFailed) return shipLocalDirection;
        
        try {
            Object ship = getShipManagingPosMethod.invoke(null, level, shipBlockPos);
            if (ship == null) return shipLocalDirection;
            
            Object dirJoml = toJOMLMethod.invoke(null, shipLocalDirection);
            
            Object transform = getTransformMethod.invoke(ship);
            Object shipToWorld = getShipToWorldMethod.invoke(transform);
            
            transformDirectionMethod.invoke(shipToWorld, dirJoml);
            
            return (Vec3) toMinecraftMethod.invoke(null, dirJoml);
        } catch (Exception e) {
            return shipLocalDirection;
        }
    }
    
    public static Vec3 transformDirectionToShip(Level level, BlockPos shipBlockPos, Vec3 worldDirection) {
        if (!isVSLoaded()) return worldDirection;
        initReflection();
        if (reflectionFailed) return worldDirection;
        
        try {
            Object ship = getShipManagingPosMethod.invoke(null, level, shipBlockPos);
            if (ship == null) return worldDirection;
            
            Object dirJoml = toJOMLMethod.invoke(null, worldDirection);
            
            Object transform = getTransformMethod.invoke(ship);
            Object worldToShip = getWorldToShipMethod.invoke(transform);
            
            transformDirectionMethod.invoke(worldToShip, dirJoml);
            
            return (Vec3) toMinecraftMethod.invoke(null, dirJoml);
        } catch (Exception e) {
            return worldDirection;
        }
    }
    
    public static float transformYawToShip(Level level, BlockPos shipBlockPos, float worldYaw) {
        if (!isVSLoaded()) return worldYaw;
        initReflection();
        if (reflectionFailed) return worldYaw;
        
        try {
            Object ship = getShipManagingPosMethod.invoke(null, level, shipBlockPos);
            if (ship == null) return worldYaw;
            
            double yawRad = Math.toRadians(worldYaw);
            Vec3 worldDir = new Vec3(-Math.sin(yawRad), 0, Math.cos(yawRad));
            
            Vec3 shipDir = transformDirectionToShip(level, shipBlockPos, worldDir);
            
            return (float) Math.toDegrees(Math.atan2(-shipDir.x, shipDir.z));
        } catch (Exception e) {
            return worldYaw;
        }
    }
    
    public static float transformPitchToShip(Level level, BlockPos shipBlockPos, float worldYaw, float worldPitch) {
        if (!isVSLoaded()) return worldPitch;
        initReflection();
        if (reflectionFailed) return worldPitch;
        
        try {
            Object ship = getShipManagingPosMethod.invoke(null, level, shipBlockPos);
            if (ship == null) return worldPitch;
            
            double yawRad = Math.toRadians(worldYaw);
            double pitchRad = Math.toRadians(worldPitch);
            double cosP = Math.cos(pitchRad);
            Vec3 worldDir = new Vec3(
                -Math.sin(yawRad) * cosP,
                -Math.sin(pitchRad),
                Math.cos(yawRad) * cosP
            );
            
            Vec3 shipDir = transformDirectionToShip(level, shipBlockPos, worldDir);
            
            double hDist = Math.sqrt(shipDir.x * shipDir.x + shipDir.z * shipDir.z);
            return (float) Math.toDegrees(Math.atan2(-shipDir.y, hDist));
        } catch (Exception e) {
            return worldPitch;
        }
    }
    
    public static Vec3 getShipVelocityAtPoint(Level level, BlockPos shipBlockPos, Vec3 worldPos) {
        if (!isVSLoaded()) return Vec3.ZERO;
        initReflection();
        if (reflectionFailed) return Vec3.ZERO;
        
        try {
            Object ship = getShipManagingPosMethod.invoke(null, level, shipBlockPos);
            if (ship == null) return Vec3.ZERO;
            
            Object transform = getTransformMethod.invoke(ship);
            
            Object shipCenterVec = getPositionInWorldMethod.invoke(transform);
            
            Object linearVelVec = getVelocityMethod.invoke(ship);
            
            Object omegaVec = getOmegaMethod.invoke(ship);
            
            Method getX = shipCenterVec.getClass().getMethod("x");
            Method getY = shipCenterVec.getClass().getMethod("y");
            Method getZ = shipCenterVec.getClass().getMethod("z");
            
            double centerX = (double) getX.invoke(shipCenterVec);
            double centerY = (double) getY.invoke(shipCenterVec);
            double centerZ = (double) getZ.invoke(shipCenterVec);
            
            double linVelX = (double) getX.invoke(linearVelVec);
            double linVelY = (double) getY.invoke(linearVelVec);
            double linVelZ = (double) getZ.invoke(linearVelVec);
            
            double omegaX = (double) getX.invoke(omegaVec);
            double omegaY = (double) getY.invoke(omegaVec);
            double omegaZ = (double) getZ.invoke(omegaVec);
            
            double relX = worldPos.x - centerX;
            double relY = worldPos.y - centerY;
            double relZ = worldPos.z - centerZ;
            
            double angContribX = omegaY * relZ - omegaZ * relY;
            double angContribY = omegaZ * relX - omegaX * relZ;
            double angContribZ = omegaX * relY - omegaY * relX;
            
            double totalX = (linVelX + angContribX) * 0.05;
            double totalY = (linVelY + angContribY) * 0.05;
            double totalZ = (linVelZ + angContribZ) * 0.05;
            
            return new Vec3(totalX, totalY, totalZ);
        } catch (Exception e) {
            return Vec3.ZERO;
        }
    }
    
    public static void applyShipVelocityToEntity(Level level, BlockPos shipBlockPos, Entity entity) {
        if (!isVSLoaded() || entity == null) return;
        
        try {
            Vec3 shipVel = getShipVelocityAtPoint(level, shipBlockPos, entity.position());
            entity.setDeltaMovement(entity.getDeltaMovement().add(shipVel));
        } catch (Exception e) {
        }
    }
    
    public static float transformYawToWorld(Level level, BlockPos shipBlockPos, float localYaw) {
        if (!isVSLoaded()) return localYaw;
        initReflection();
        if (reflectionFailed) return localYaw;
        
        try {
            Object ship = getShipManagingPosMethod.invoke(null, level, shipBlockPos);
            if (ship == null) return localYaw;
            
            double yawRad = Math.toRadians(localYaw);
            Vec3 localDir = new Vec3(-Math.sin(yawRad), 0, Math.cos(yawRad));
            
            Vec3 worldDir = transformDirectionToWorld(level, shipBlockPos, localDir);
            
            return (float) Math.toDegrees(Math.atan2(-worldDir.x, worldDir.z));
        } catch (Exception e) {
            return localYaw;
        }
    }
    
    public static float transformPitchToWorld(Level level, BlockPos shipBlockPos, float localYaw, float localPitch) {
        if (!isVSLoaded()) return localPitch;
        initReflection();
        if (reflectionFailed) return localPitch;
        
        try {
            Object ship = getShipManagingPosMethod.invoke(null, level, shipBlockPos);
            if (ship == null) return localPitch;
            
            double yawRad = Math.toRadians(localYaw);
            double pitchRad = Math.toRadians(localPitch);
            double cosP = Math.cos(pitchRad);
            Vec3 localDir = new Vec3(
                -Math.sin(yawRad) * cosP,
                -Math.sin(pitchRad),
                Math.cos(yawRad) * cosP
            );
            
            Vec3 worldDir = transformDirectionToWorld(level, shipBlockPos, localDir);
            
            double hDist = Math.sqrt(worldDir.x * worldDir.x + worldDir.z * worldDir.z);
            return (float) Math.toDegrees(Math.atan2(-worldDir.y, hDist));
        } catch (Exception e) {
            return localPitch;
        }
    }
    
    public static float transformRollToWorld(Level level, BlockPos shipBlockPos, float localRoll) {
        if (!isVSLoaded()) return localRoll;
        initReflection();
        if (reflectionFailed) return localRoll;
        
        try {
            Object ship = getShipManagingPosMethod.invoke(null, level, shipBlockPos);
            if (ship == null) return localRoll;
            
            Vec3 localUp = new Vec3(0.0, 1.0, 0.0);
            Vec3 worldUp = transformDirectionToWorld(level, shipBlockPos, localUp);
            
            Vec3 shipYAxis = transformDirectionToWorld(level, shipBlockPos, new Vec3(0.0, 1.0, 0.0));
            
            double rollRad = Math.toRadians(localRoll);
            Vec3 rollTest = new Vec3(
                0.0,
                Math.cos(rollRad),
                Math.sin(rollRad)
            );
            Vec3 transformedRoll = transformDirectionToWorld(level, shipBlockPos, rollTest);
            
            double dotProduct = transformedRoll.dot(worldUp) / (transformedRoll.length() * worldUp.length());
            dotProduct = Math.max(-1.0, Math.min(1.0, dotProduct));
            double angle = Math.acos(dotProduct);
            
            Vec3 shipForward = transformDirectionToWorld(level, shipBlockPos, new Vec3(0.0, 0.0, 1.0));
            Vec3 cross = worldUp.cross(transformedRoll);
            if (cross.dot(shipForward) < 0) {
                angle = -angle;
            }
            
            return (float) Math.toDegrees(angle);
        } catch (Exception e) {
            return localRoll;
        }
    }
    
    public static float transformRollToShip(Level level, BlockPos shipBlockPos, float worldRoll) {
        if (!isVSLoaded()) return worldRoll;
        initReflection();
        if (reflectionFailed) return worldRoll;
        
        try {
            Object ship = getShipManagingPosMethod.invoke(null, level, shipBlockPos);
            if (ship == null) return worldRoll;
            
            double rollRad = Math.toRadians(worldRoll);
            Vec3 rollTest = new Vec3(
                0.0,
                Math.cos(rollRad),
                Math.sin(rollRad)
            );
            
            Vec3 shipLocalRoll = transformDirectionToShip(level, shipBlockPos, rollTest);
            
            double angle = Math.atan2(shipLocalRoll.z, shipLocalRoll.y);
            return (float) Math.toDegrees(angle);
        } catch (Exception e) {
            return worldRoll;
        }
    }
    
    @SuppressWarnings("unchecked")
    public static Iterable<Object> getAllLoadedShips(Level level) {
        if (!isVSLoaded()) return java.util.Collections.emptyList();
        initReflection();
        if (reflectionFailed) return java.util.Collections.emptyList();
        
        if (getShipWorldMethod != null && getLoadedShipsMethod != null) {
            try {
                Object shipWorld = getShipWorldMethod.invoke(null, level);
                if (shipWorld != null) {
                    Object loadedShips = getLoadedShipsMethod.invoke(shipWorld);
                    if (loadedShips instanceof Iterable) {
                        return (Iterable<Object>) loadedShips;
                    }
                }
            } catch (Exception e) {
                System.err.println("[CTA] getShipObjectWorld failed: " + e.getMessage());
            }
        }
        
        if (getAllShipsMethod != null) {
            try {
                Object allShips = getAllShipsMethod.invoke(null, level);
                if (allShips instanceof Iterable) {
                    return (Iterable<Object>) allShips;
                }
            } catch (Exception e) {
                System.err.println("[CTA] getAllShips failed: " + e.getMessage());
            }
        }
        
        return java.util.Collections.emptyList();
    }
    
    public static Vec3 toWorldCoordinatesWithShip(Object ship, Vec3 shipLocalPos) {
        if (!isVSLoaded() || ship == null) return shipLocalPos;
        initReflection();
        if (reflectionFailed) return shipLocalPos;
        
        try {
            Object localJoml = toJOMLMethod.invoke(null, shipLocalPos);
            
            Object transform = getTransformMethod.invoke(ship);
            Object shipToWorld = getShipToWorldMethod.invoke(transform);
            
            transformPositionMethod.invoke(shipToWorld, localJoml);
            
            return (Vec3) toMinecraftMethod.invoke(null, localJoml);
        } catch (Exception e) {
            return shipLocalPos;
        }
    }

    
    public static Vec3 toShipCoordinatesWithShip(Object ship, Vec3 worldPos) {
        if (!isVSLoaded() || ship == null) return worldPos;
        initReflection();
        if (reflectionFailed) return worldPos;

        try {
            Object worldJoml = toJOMLMethod.invoke(null, worldPos);
            Object transform = getTransformMethod.invoke(ship);
            Object worldToShip = getWorldToShipMethod.invoke(transform);
            transformPositionMethod.invoke(worldToShip, worldJoml);
            return (Vec3) toMinecraftMethod.invoke(null, worldJoml);
        } catch (Exception e) {
            return worldPos;
        }
    }
}
