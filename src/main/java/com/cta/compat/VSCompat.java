package com.cta.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.ModList;

import javax.annotation.Nullable;
import java.lang.reflect.Method;

/**
 * Valkyrien Skies compatibility helper
 * Uses reflection to avoid compile-time dependency on VS
 * Provides coordinate transformation utilities for launching projectiles from ships
 */
public class VSCompat {
    private static Boolean vsLoaded = null;
    
    // Cached reflection classes and methods
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
    
    private static boolean reflectionInitialized = false;
    private static boolean reflectionFailed = false;
    
    /**
     * Check if Valkyrien Skies is loaded
     */
    public static boolean isVSLoaded() {
        if (vsLoaded == null) {
            vsLoaded = ModList.get().isLoaded("valkyrienskies");
        }
        return vsLoaded;
    }
    
    /**
     * Initialize reflection for VS classes
     */
    private static void initReflection() {
        if (reflectionInitialized || reflectionFailed) return;
        
        try {
            // Core VS classes
            vsGameUtilsClass = Class.forName("org.valkyrienskies.mod.common.VSGameUtilsKt");
            shipClass = Class.forName("org.valkyrienskies.core.api.ships.Ship");
            shipTransformClass = Class.forName("org.valkyrienskies.core.api.ships.properties.ShipTransform");
            vectorConversionsClass = Class.forName("org.valkyrienskies.mod.common.util.VectorConversionsMCKt");
            matrix4dcClass = Class.forName("org.joml.Matrix4dc");
            vector3dClass = Class.forName("org.joml.Vector3d");
            
            // Get methods
            getShipManagingPosMethod = vsGameUtilsClass.getMethod("getShipManagingPos", Level.class, BlockPos.class);
            getTransformMethod = shipClass.getMethod("getTransform");
            getShipToWorldMethod = shipTransformClass.getMethod("getShipToWorld");
            getWorldToShipMethod = shipTransformClass.getMethod("getWorldToShip");
            getPositionInWorldMethod = shipTransformClass.getMethod("getPositionInWorld");
            
            // Matrix4dc methods
            transformPositionMethod = matrix4dcClass.getMethod("transformPosition", vector3dClass);
            transformDirectionMethod = matrix4dcClass.getMethod("transformDirection", vector3dClass);
            
            // Vector conversion methods
            toJOMLMethod = vectorConversionsClass.getMethod("toJOML", Vec3.class);
            toMinecraftMethod = vectorConversionsClass.getMethod("toMinecraft", Class.forName("org.joml.Vector3dc"));
            
            // Ship velocity methods
            getVelocityMethod = shipClass.getMethod("getVelocity");
            getOmegaMethod = shipClass.getMethod("getOmega");
            
            reflectionInitialized = true;
        } catch (Exception e) {
            reflectionFailed = true;
            System.err.println("[CTA] Failed to initialize VS reflection: " + e.getMessage());
        }
    }
    
    /**
     * Get the ship managing a block position, or null if not on a ship
     */
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
    
    /**
     * Check if a position is on a ship
     */
    public static boolean isOnShip(Level level, BlockPos pos) {
        return getShipManagingPos(level, pos) != null;
    }
    
    /**
     * Get the ship-local BlockPos for an entity that was placed on a ship
     * This is needed because VS ships exist in a "shipyard" dimension area
     * and redstone checks need to happen at the ship-local position, not world position
     * 
     * @param level The level
     * @param placementPos The original BlockPos where the entity was placed (ship-local)
     * @param worldPos The entity's current world position
     * @return The ship-local BlockPos for redstone checking, or a world BlockPos if not on ship
     */
    public static BlockPos getShipLocalBlockPos(Level level, @Nullable BlockPos placementPos, Vec3 worldPos) {
        // If we have a placement position and it's on a ship, use that
        // The placement position is already in ship-local coordinates
        if (placementPos != null && isOnShip(level, placementPos)) {
            return placementPos;
        }
        
        // Otherwise, just use the world position converted to BlockPos
        return BlockPos.containing(worldPos);
    }
    
    /**
     * Check for redstone signal at the correct position (ship-local or world)
     * This handles VS ship compatibility automatically
     * 
     * @param level The level
     * @param placementPos The original BlockPos where the entity was placed (may be ship-local)
     * @param worldPos The entity's current world position
     * @return true if there's a redstone signal at the relevant position
     */
    public static boolean hasRedstoneSignal(Level level, @Nullable BlockPos placementPos, Vec3 worldPos) {
        BlockPos checkPos = getShipLocalBlockPos(level, placementPos, worldPos);
        return level.hasNeighborSignal(checkPos);
    }
    
    /**
     * Update an entity's world position based on its stored ship-local position
     * Call this every tick for entities that should move with VS ships
     * 
     * @param entity The entity to update
     * @param shipBlockPos The ship-local BlockPos (used to find the ship)
     * @param shipLocalPos The entity's position in ship-local coordinates
     * @return true if the entity was updated (is on a ship), false otherwise
     */
    public static boolean updateEntityPositionOnShip(Entity entity, @Nullable BlockPos shipBlockPos, Vec3 shipLocalPos) {
        if (!isVSLoaded() || shipBlockPos == null) return false;
        
        Level level = entity.level();
        if (!isOnShip(level, shipBlockPos)) return false;
        
        // Transform ship-local position to world position
        Vec3 worldPos = toWorldCoordinates(level, shipBlockPos, shipLocalPos);
        
        // Update entity position
        entity.setPos(worldPos.x, worldPos.y, worldPos.z);
        return true;
    }
    
    /**
     * Get the ship's velocity vector
     * Returns zero vector if not on a ship
     */
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
    
    /**
     * Transform ship-local position to world coordinates
     * Returns original position if not on a ship
     */
    public static Vec3 toWorldCoordinates(Level level, BlockPos shipBlockPos, Vec3 shipLocalPos) {
        if (!isVSLoaded()) return shipLocalPos;
        initReflection();
        if (reflectionFailed) return shipLocalPos;
        
        try {
            Object ship = getShipManagingPosMethod.invoke(null, level, shipBlockPos);
            if (ship == null) return shipLocalPos;
            
            // Convert to JOML Vector3d
            Object localJoml = toJOMLMethod.invoke(null, shipLocalPos);
            
            // Get ship transform and matrix
            Object transform = getTransformMethod.invoke(ship);
            Object shipToWorld = getShipToWorldMethod.invoke(transform);
            
            // Transform position
            transformPositionMethod.invoke(shipToWorld, localJoml);
            
            // Convert back to Minecraft Vec3
            return (Vec3) toMinecraftMethod.invoke(null, localJoml);
        } catch (Exception e) {
            return shipLocalPos;
        }
    }
    
    /**
     * Transform a direction vector from ship-local to world space
     * Used for velocity/direction vectors (does not include position offset)
     * Returns original direction if not on a ship
     */
    public static Vec3 transformDirectionToWorld(Level level, BlockPos shipBlockPos, Vec3 shipLocalDirection) {
        if (!isVSLoaded()) return shipLocalDirection;
        initReflection();
        if (reflectionFailed) return shipLocalDirection;
        
        try {
            Object ship = getShipManagingPosMethod.invoke(null, level, shipBlockPos);
            if (ship == null) return shipLocalDirection;
            
            // Convert to JOML Vector3d
            Object dirJoml = toJOMLMethod.invoke(null, shipLocalDirection);
            
            // Get ship transform and matrix
            Object transform = getTransformMethod.invoke(ship);
            Object shipToWorld = getShipToWorldMethod.invoke(transform);
            
            // Transform direction
            transformDirectionMethod.invoke(shipToWorld, dirJoml);
            
            // Convert back to Minecraft Vec3
            return (Vec3) toMinecraftMethod.invoke(null, dirJoml);
        } catch (Exception e) {
            return shipLocalDirection;
        }
    }
    
    /**
     * Transform a direction vector from world space to ship-local space
     * Used to convert player look direction to ship-local coordinates
     * Returns original direction if not on a ship
     */
    public static Vec3 transformDirectionToShip(Level level, BlockPos shipBlockPos, Vec3 worldDirection) {
        if (!isVSLoaded()) return worldDirection;
        initReflection();
        if (reflectionFailed) return worldDirection;
        
        try {
            Object ship = getShipManagingPosMethod.invoke(null, level, shipBlockPos);
            if (ship == null) return worldDirection;
            
            // Convert to JOML Vector3d
            Object dirJoml = toJOMLMethod.invoke(null, worldDirection);
            
            // Get ship transform and WORLD TO SHIP matrix (inverse)
            Object transform = getTransformMethod.invoke(ship);
            Object worldToShip = getWorldToShipMethod.invoke(transform);
            
            // Transform direction from world to ship
            transformDirectionMethod.invoke(worldToShip, dirJoml);
            
            // Convert back to Minecraft Vec3
            return (Vec3) toMinecraftMethod.invoke(null, dirJoml);
        } catch (Exception e) {
            return worldDirection;
        }
    }
    
    /**
     * Transform yaw angle from world space to ship-local space
     * Use this when placing entities to store their rotation in ship-local coordinates
     */
    public static float transformYawToShip(Level level, BlockPos shipBlockPos, float worldYaw) {
        if (!isVSLoaded()) return worldYaw;
        initReflection();
        if (reflectionFailed) return worldYaw;
        
        try {
            Object ship = getShipManagingPosMethod.invoke(null, level, shipBlockPos);
            if (ship == null) return worldYaw;
            
            // Convert yaw to a direction vector in world space
            double yawRad = Math.toRadians(worldYaw);
            Vec3 worldDir = new Vec3(-Math.sin(yawRad), 0, Math.cos(yawRad));
            
            // Transform direction to ship-local space
            Vec3 shipDir = transformDirectionToShip(level, shipBlockPos, worldDir);
            
            // Convert back to yaw
            return (float) Math.toDegrees(Math.atan2(-shipDir.x, shipDir.z));
        } catch (Exception e) {
            return worldYaw;
        }
    }
    
    /**
     * Transform pitch angle from world space to ship-local space
     * Use this when placing entities to store their rotation in ship-local coordinates
     */
    public static float transformPitchToShip(Level level, BlockPos shipBlockPos, float worldYaw, float worldPitch) {
        if (!isVSLoaded()) return worldPitch;
        initReflection();
        if (reflectionFailed) return worldPitch;
        
        try {
            Object ship = getShipManagingPosMethod.invoke(null, level, shipBlockPos);
            if (ship == null) return worldPitch;
            
            // Convert yaw/pitch to a direction vector in world space
            double yawRad = Math.toRadians(worldYaw);
            double pitchRad = Math.toRadians(worldPitch);
            double cosP = Math.cos(pitchRad);
            Vec3 worldDir = new Vec3(
                -Math.sin(yawRad) * cosP,
                -Math.sin(pitchRad),
                Math.cos(yawRad) * cosP
            );
            
            // Transform direction to ship-local space
            Vec3 shipDir = transformDirectionToShip(level, shipBlockPos, worldDir);
            
            // Convert back to pitch
            double hDist = Math.sqrt(shipDir.x * shipDir.x + shipDir.z * shipDir.z);
            return (float) Math.toDegrees(Math.atan2(-shipDir.y, hDist));
        } catch (Exception e) {
            return worldPitch;
        }
    }
    
    /**
     * Get the ship's velocity at a given point (including angular velocity contribution)
     * Returns Vec3.ZERO if not on a ship
     * 
     * @param level The level
     * @param shipBlockPos A block position on the ship (for ship lookup)
     * @param worldPos The world position to calculate velocity at
     */
    public static Vec3 getShipVelocityAtPoint(Level level, BlockPos shipBlockPos, Vec3 worldPos) {
        if (!isVSLoaded()) return Vec3.ZERO;
        initReflection();
        if (reflectionFailed) return Vec3.ZERO;
        
        try {
            Object ship = getShipManagingPosMethod.invoke(null, level, shipBlockPos);
            if (ship == null) return Vec3.ZERO;
            
            // Get ship transform
            Object transform = getTransformMethod.invoke(ship);
            
            // Get ship center position (Vector3dc)
            Object shipCenterVec = getPositionInWorldMethod.invoke(transform);
            
            // Get linear velocity (Vector3dc)
            Object linearVelVec = getVelocityMethod.invoke(ship);
            
            // Get angular velocity (Vector3dc)
            Object omegaVec = getOmegaMethod.invoke(ship);
            
            // Extract values using Vector3dc getters
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
            
            // Calculate relative position from ship center
            double relX = worldPos.x - centerX;
            double relY = worldPos.y - centerY;
            double relZ = worldPos.z - centerZ;
            
            // omega cross r (angular velocity contribution)
            double angContribX = omegaY * relZ - omegaZ * relY;
            double angContribY = omegaZ * relX - omegaX * relZ;
            double angContribZ = omegaX * relY - omegaY * relX;
            
            // Total velocity at point (convert from m/s to blocks/tick by multiplying by 0.05)
            double totalX = (linVelX + angContribX) * 0.05;
            double totalY = (linVelY + angContribY) * 0.05;
            double totalZ = (linVelZ + angContribZ) * 0.05;
            
            return new Vec3(totalX, totalY, totalZ);
        } catch (Exception e) {
            return Vec3.ZERO;
        }
    }
    
    /**
     * Apply ship velocity to an entity (call after setting entity position)
     * Adds the ship's linear and angular velocity to the entity's current motion
     */
    public static void applyShipVelocityToEntity(Level level, BlockPos shipBlockPos, Entity entity) {
        if (!isVSLoaded() || entity == null) return;
        
        try {
            Vec3 shipVel = getShipVelocityAtPoint(level, shipBlockPos, entity.position());
            entity.setDeltaMovement(entity.getDeltaMovement().add(shipVel));
        } catch (Exception e) {
            // Silently fail if VS integration fails
        }
    }
    
    /**
     * Transform a yaw angle from ship-local to world space
     * @param level The level
     * @param shipBlockPos A block position on the ship
     * @param localYaw The yaw angle in ship-local space (degrees)
     * @return The yaw angle in world space (degrees)
     */
    public static float transformYawToWorld(Level level, BlockPos shipBlockPos, float localYaw) {
        if (!isVSLoaded()) return localYaw;
        initReflection();
        if (reflectionFailed) return localYaw;
        
        try {
            Object ship = getShipManagingPosMethod.invoke(null, level, shipBlockPos);
            if (ship == null) return localYaw;
            
            // Convert yaw to a direction vector
            double yawRad = Math.toRadians(localYaw);
            Vec3 localDir = new Vec3(-Math.sin(yawRad), 0, Math.cos(yawRad));
            
            // Transform direction to world space
            Vec3 worldDir = transformDirectionToWorld(level, shipBlockPos, localDir);
            
            // Convert back to yaw
            return (float) Math.toDegrees(Math.atan2(-worldDir.x, worldDir.z));
        } catch (Exception e) {
            return localYaw;
        }
    }
    
    /**
     * Transform pitch angle from ship-local to world space
     * Note: This is more complex due to 3D rotation; for most cases,
     * transforming the full direction vector is preferred
     */
    public static float transformPitchToWorld(Level level, BlockPos shipBlockPos, float localYaw, float localPitch) {
        if (!isVSLoaded()) return localPitch;
        initReflection();
        if (reflectionFailed) return localPitch;
        
        try {
            Object ship = getShipManagingPosMethod.invoke(null, level, shipBlockPos);
            if (ship == null) return localPitch;
            
            // Convert yaw/pitch to a direction vector
            double yawRad = Math.toRadians(localYaw);
            double pitchRad = Math.toRadians(localPitch);
            double cosP = Math.cos(pitchRad);
            Vec3 localDir = new Vec3(
                -Math.sin(yawRad) * cosP,
                -Math.sin(pitchRad),
                Math.cos(yawRad) * cosP
            );
            
            // Transform direction to world space
            Vec3 worldDir = transformDirectionToWorld(level, shipBlockPos, localDir);
            
            // Convert back to pitch
            double hDist = Math.sqrt(worldDir.x * worldDir.x + worldDir.z * worldDir.z);
            return (float) Math.toDegrees(Math.atan2(-worldDir.y, hDist));
        } catch (Exception e) {
            return localPitch;
        }
    }
    
    /**
     * Transform roll (bank angle) from ship-local to world space
     * Roll is rotation around the forward axis
     */
    public static float transformRollToWorld(Level level, BlockPos shipBlockPos, float localRoll) {
        if (!isVSLoaded()) return localRoll;
        initReflection();
        if (reflectionFailed) return localRoll;
        
        try {
            Object ship = getShipManagingPosMethod.invoke(null, level, shipBlockPos);
            if (ship == null) return localRoll;
            
            // Create a test vector perpendicular to the forward direction to track roll
            // Use "up" vector in local space and transform it
            Vec3 localUp = new Vec3(0.0, 1.0, 0.0);
            Vec3 worldUp = transformDirectionToWorld(level, shipBlockPos, localUp);
            
            // Calculate what the "up" direction should be without roll
            // This is the actual up vector of the transformed coordinate system
            Vec3 shipYAxis = transformDirectionToWorld(level, shipBlockPos, new Vec3(0.0, 1.0, 0.0));
            
            // Create a test vector for roll - perpendicular to forward, initially "up"
            double rollRad = Math.toRadians(localRoll);
            Vec3 rollTest = new Vec3(
                0.0,
                Math.cos(rollRad),
                Math.sin(rollRad)
            );
            Vec3 transformedRoll = transformDirectionToWorld(level, shipBlockPos, rollTest);
            
            // Calculate the angle from the Y axis
            double dotProduct = transformedRoll.dot(worldUp) / (transformedRoll.length() * worldUp.length());
            dotProduct = Math.max(-1.0, Math.min(1.0, dotProduct)); // Clamp to avoid NaN
            double angle = Math.acos(dotProduct);
            
            // Determine sign based on cross product direction relative to forward
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
    
    /**
     * Transform roll (bank angle) from world to ship-local space
     */
    public static float transformRollToShip(Level level, BlockPos shipBlockPos, float worldRoll) {
        if (!isVSLoaded()) return worldRoll;
        initReflection();
        if (reflectionFailed) return worldRoll;
        
        try {
            Object ship = getShipManagingPosMethod.invoke(null, level, shipBlockPos);
            if (ship == null) return worldRoll;
            
            // Similar to transformRollToWorld but in reverse
            // Create a test vector in world space and transform to ship-local
            double rollRad = Math.toRadians(worldRoll);
            Vec3 rollTest = new Vec3(
                0.0,
                Math.cos(rollRad),
                Math.sin(rollRad)
            );
            
            Vec3 shipLocalRoll = transformDirectionToShip(level, shipBlockPos, rollTest);
            
            // Convert back to angle
            double angle = Math.atan2(shipLocalRoll.z, shipLocalRoll.y);
            return (float) Math.toDegrees(angle);
        } catch (Exception e) {
            return worldRoll;
        }
    }
}
