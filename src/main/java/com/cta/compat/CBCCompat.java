package com.cta.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CBCCompat {
    
    private static final boolean CBC_LOADED;
    private static Class<?> fuzeItemClass = null;
    
    public static final int FUZE_IMPACT = 0;
    public static final int FUZE_TIMED = 1;
    public static final int FUZE_PROXIMITY = 2;
    public static final int FUZE_DELAY = 3;
    public static final int FUZE_UNKNOWN = -1;
    
    static {
        boolean loaded = false;
        try {
            fuzeItemClass = Class.forName("rbasamoyai.createbigcannons.munitions.fuzes.FuzeItem");
            loaded = true;
        } catch (ClassNotFoundException e) {
            loaded = false;
        }
        CBC_LOADED = loaded;
    }
    
    public static boolean isCBCLoaded() {
        return CBC_LOADED;
    }
    
    public static boolean isFuzeItem(Item item) {
        if (!CBC_LOADED || fuzeItemClass == null) {
            return false;
        }
        return fuzeItemClass.isInstance(item);
    }
    
    public static boolean isFuzeItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return isFuzeItem(stack.getItem());
    }
    
    public static String getFuzeTypeName(int fuzeType) {
        return switch (fuzeType) {
            case FUZE_IMPACT -> "Impact";
            case FUZE_TIMED -> "Timed";
            case FUZE_PROXIMITY -> "Proximity";
            case FUZE_DELAY -> "Delay";
            default -> "Unknown";
        };
    }
    
    public static int getFuzeType(ItemStack stack) {
        if (stack.isEmpty() || !isFuzeItem(stack)) {
            return FUZE_UNKNOWN;
        }
        
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id == null) return FUZE_UNKNOWN;
        
        String name = id.getPath().toLowerCase();
        
        if (name.contains("timed") || name.contains("time_fuze")) {
            return FUZE_TIMED;
        }
        
        if (name.contains("proximity") || name.contains("prox")) {
            return FUZE_PROXIMITY;
        }
        
        if (name.contains("dead_man") || name.contains("deadman") || name.contains("delay")) {
            return FUZE_DELAY;
        }
        
        if (name.contains("impact")) {
            return FUZE_IMPACT;
        }
        
        return FUZE_IMPACT;
    }
    
    public static int getTimedFuzeDuration(ItemStack stack) {
        if (stack.isEmpty()) return 100;
        
        CompoundTag tag = stack.getTag();
        if (tag != null) {
            if (tag.contains("BlockEntityTag", 10)) {
                CompoundTag beTag = tag.getCompound("BlockEntityTag");
                if (beTag.contains("FuseTime")) {
                    return beTag.getInt("FuseTime") * 20;
                }
                if (beTag.contains("Timer")) {
                    return beTag.getInt("Timer");
                }
            }
            
            if (tag.contains("FuzeTimer")) {
                return tag.getInt("FuzeTimer");
            }
            if (tag.contains("FuseTime")) {
                return tag.getInt("FuseTime") * 20; 
            }
            if (tag.contains("Timer")) {
                return tag.getInt("Timer");
            }
            if (tag.contains("fuze_timer")) {
                return tag.getInt("fuze_timer");
            }
        }
        
        return 100;
    }
    
    public static double getProximityRange(ItemStack stack) {
        if (stack.isEmpty()) return 5.0;
        
        CompoundTag tag = stack.getTag();
        if (tag != null) {
            if (tag.contains("ProximityRange")) {
                return tag.getDouble("ProximityRange");
            }
            if (tag.contains("Range")) {
                return tag.getDouble("Range");
            }
            if (tag.contains("DetectionRange")) {
                return tag.getDouble("DetectionRange");
            }
        }
        
        return 5.0;
    }
    
    public static int getDelayDuration(ItemStack stack) {
        if (stack.isEmpty()) return 10;
        
        CompoundTag tag = stack.getTag();
        if (tag != null) {
            if (tag.contains("DelayTimer")) {
                return tag.getInt("DelayTimer");
            }
            if (tag.contains("Delay")) {
                return tag.getInt("Delay");
            }
            if (tag.contains("DetonationDelay")) {
                return tag.getInt("DetonationDelay");
            }
        }
        
        return 10;
    }
    
    private static Class<?> cannonMountClass = null;
    private static Class<?> abstractContraptionEntityClass = null;
    private static Class<?> mountedBigCannonContraptionClass = null;
    private static Class<?> abstractMountedCannonContraptionClass = null;
    private static Class<?> projectileBlockClass = null;
    private static Class<?> fuzedProjectileBlockClass = null;
    private static Class<?> powderChargeBlockClass = null;
    private static Class<?> bigCartridgeBlockClass = null;
    private static Class<?> handlerContainerClass = null;
    private static Class<?> dimensionPropertiesHandlerClass = null;
    private static Method getYawMethod = null;
    private static Method getPitchMethod = null;
    private static Method getMountedContraptionEntityMethod = null;
    private static Method getContraptionMethod = null;
    private static Method getBlocksMethod = null;
    private static Method initialOrientationMethod = null;
    private static Method getMaxSafeChargesMethod = null;
    private static Method getAssociatedEntityTypeMethod = null;
    private static Method powderChargePowerMethod = null;
    private static Method bigCartridgeChargePowerMethod = null;
    private static Method bigCartridgePowerFromDataMethod = null;
    private static Method getDimensionPropertiesMethod = null;
    private static Method dimensionGravityMultiplierMethod = null;
    private static Method dimensionDragMultiplierMethod = null;
    private static Object inertProjectileHandler = null;
    private static Object commonShellHandler = null;
    private static Object shrapnelShellHandler = null;
    private static Object fluidShellHandler = null;
    private static Object smokeShellHandler = null;
    private static Object mortarStoneHandler = null;
    private static Object dropMortarShellHandler = null;
    private static boolean mountClassLoaded = false;
    
    private static void loadCannonMountClass() {
        if (mountClassLoaded) return;
        mountClassLoaded = true;
        
        if (!CBC_LOADED) return;
        
        try {
            cannonMountClass = Class.forName("rbasamoyai.createbigcannons.cannon_control.cannon_mount.CannonMountBlockEntity");
            abstractContraptionEntityClass = Class.forName("com.simibubi.create.content.contraptions.AbstractContraptionEntity");
            abstractMountedCannonContraptionClass = Class.forName("rbasamoyai.createbigcannons.cannon_control.contraption.AbstractMountedCannonContraption");
            mountedBigCannonContraptionClass = Class.forName("rbasamoyai.createbigcannons.cannon_control.contraption.MountedBigCannonContraption");
            projectileBlockClass = Class.forName("rbasamoyai.createbigcannons.munitions.big_cannon.ProjectileBlock");
            fuzedProjectileBlockClass = Class.forName("rbasamoyai.createbigcannons.munitions.big_cannon.FuzedProjectileBlock");
            powderChargeBlockClass = Class.forName("rbasamoyai.createbigcannons.munitions.big_cannon.propellant.PowderChargeBlock");
            bigCartridgeBlockClass = Class.forName("rbasamoyai.createbigcannons.munitions.big_cannon.propellant.BigCartridgeBlock");
            handlerContainerClass = Class.forName("rbasamoyai.createbigcannons.index.CBCMunitionPropertiesHandlers");
            dimensionPropertiesHandlerClass = Class.forName("rbasamoyai.createbigcannons.munitions.config.DimensionMunitionPropertiesHandler");

            getYawMethod = cannonMountClass.getMethod("getYawOffset", float.class);
            getPitchMethod = cannonMountClass.getMethod("getPitchOffset", float.class);
            getMountedContraptionEntityMethod = cannonMountClass.getMethod("getContraption");
            getContraptionMethod = abstractContraptionEntityClass.getMethod("getContraption");
            getBlocksMethod = Class.forName("com.simibubi.create.content.contraptions.Contraption").getMethod("getBlocks");
            initialOrientationMethod = abstractMountedCannonContraptionClass.getMethod("initialOrientation");
            getMaxSafeChargesMethod = mountedBigCannonContraptionClass.getMethod("getMaxSafeCharges");
            getAssociatedEntityTypeMethod = projectileBlockClass.getMethod("getAssociatedEntityType");
            powderChargePowerMethod = powderChargeBlockClass.getMethod("getChargePower", StructureTemplate.StructureBlockInfo.class);
            bigCartridgeChargePowerMethod = bigCartridgeBlockClass.getMethod("getChargePower", StructureTemplate.StructureBlockInfo.class);
            bigCartridgePowerFromDataMethod = bigCartridgeBlockClass.getMethod("getPowerFromData", StructureTemplate.StructureBlockInfo.class);
            getDimensionPropertiesMethod = dimensionPropertiesHandlerClass.getMethod("getProperties", Level.class);

            Class<?> dimensionPropertiesClass = Class.forName("rbasamoyai.createbigcannons.munitions.config.DimensionMunitionProperties");
            dimensionGravityMultiplierMethod = dimensionPropertiesClass.getMethod("gravityMultiplier");
            dimensionDragMultiplierMethod = dimensionPropertiesClass.getMethod("dragMultiplier");

            inertProjectileHandler = getStaticField(handlerContainerClass, "INERT_BIG_CANNON_PROJECTILE");
            commonShellHandler = getStaticField(handlerContainerClass, "COMMON_SHELL_BIG_CANNON_PROJECTILE");
            shrapnelShellHandler = getStaticField(handlerContainerClass, "SHRAPNEL_SHELL");
            fluidShellHandler = getStaticField(handlerContainerClass, "FLUID_SHELL");
            smokeShellHandler = getStaticField(handlerContainerClass, "SMOKE_SHELL");
            mortarStoneHandler = getStaticField(handlerContainerClass, "MORTAR_STONE");
            dropMortarShellHandler = getStaticField(handlerContainerClass, "DROP_MORTAR_SHELL");
        } catch (Exception e) {
            cannonMountClass = null;
        }
    }
    
    public static boolean isCannonMount(Level level, BlockPos pos) {
        if (!CBC_LOADED) return false;
        loadCannonMountClass();
        if (cannonMountClass == null) return false;
        
        try {
            BlockEntity be = level.getBlockEntity(pos);
            return be != null && cannonMountClass.isInstance(be);
        } catch (Exception e) {
            return false;
        }
    }
    
    @Nullable
    public static Float getCannonMountYaw(Level level, BlockPos pos) {
        if (!CBC_LOADED) return null;
        loadCannonMountClass();
        if (cannonMountClass == null || getYawMethod == null) return null;
        
        try {
            BlockEntity be = level.getBlockEntity(pos);
            if (be == null || !cannonMountClass.isInstance(be)) return null;
            
            Object result = getYawMethod.invoke(be, 1.0f);
            if (result instanceof Number num) {
                return num.floatValue();
            }
        } catch (Exception e) {
        }
        
        return null;
    }
    
    @Nullable
    public static Float getCannonMountPitch(Level level, BlockPos pos) {
        if (!CBC_LOADED) return null;
        loadCannonMountClass();
        if (cannonMountClass == null || getPitchMethod == null) return null;
        
        try {
            BlockEntity be = level.getBlockEntity(pos);
            if (be == null || !cannonMountClass.isInstance(be)) return null;
            
            Object result = getPitchMethod.invoke(be, 1.0f);
            if (result instanceof Number num) {
                return num.floatValue();
            }
        } catch (Exception e) {
        }
        
        return null;
    }

    @Nullable
    public static CannonRangeData getCannonRangeData(Level level, BlockPos mountPos) {
        if (!CBC_LOADED) {
            return null;
        }
        loadCannonMountClass();
        if (cannonMountClass == null || getMountedContraptionEntityMethod == null || getContraptionMethod == null ||
                getBlocksMethod == null || projectileBlockClass == null) {
            return null;
        }

        Float cannonYaw = getCannonMountYaw(level, mountPos);
        Float cannonPitch = getCannonMountPitch(level, mountPos);
        if (cannonYaw == null || cannonPitch == null) {
            return null;
        }

        BlockEntity be = level.getBlockEntity(mountPos);
        if (be == null || !cannonMountClass.isInstance(be)) {
            return null;
        }

        try {
            Object contraptionEntity = getMountedContraptionEntityMethod.invoke(be);
            if (contraptionEntity == null) {
                return new CannonRangeData("No shell loaded", cannonYaw, cannonPitch, 0.0f, 0, 0.0f, 0.0f,
                        0.0f, 0, -1.0, List.of(), "No assembled cannon", false, 0.0, 0.0, false);
            }

            Object contraption = getContraptionMethod.invoke(contraptionEntity);
            if (contraption == null || !abstractMountedCannonContraptionClass.isInstance(contraption)) {
                return new CannonRangeData("No shell loaded", cannonYaw, cannonPitch, 0.0f, 0, 0.0f, 0.0f,
                        0.0f, 0, -1.0, List.of(), "No assembled cannon", false, 0.0, 0.0, false);
            }

            @SuppressWarnings("unchecked")
            Map<BlockPos, StructureTemplate.StructureBlockInfo> blocks =
                    (Map<BlockPos, StructureTemplate.StructureBlockInfo>) getBlocksMethod.invoke(contraption);
            if (blocks == null || blocks.isEmpty()) {
                return new CannonRangeData("No shell loaded", cannonYaw, cannonPitch, 0.0f, 0, 0.0f, 0.0f,
                        0.0f, 0, -1.0, List.of(), "No loaded munition", false, 0.0, 0.0, false);
            }

            Direction orientation = initialOrientationMethod != null
                    ? (Direction) initialOrientationMethod.invoke(contraption)
                    : Direction.NORTH;
            int maxSafeCharges = mountedBigCannonContraptionClass.isInstance(contraption) && getMaxSafeChargesMethod != null
                    ? ((Number) getMaxSafeChargesMethod.invoke(contraption)).intValue()
                    : 0;

            AmmoSnapshot ammo = inspectMountedAmmo(blocks.values(), orientation);
            if (ammo.projectileInfo == null) {
                return new CannonRangeData("No shell loaded", cannonYaw, cannonPitch, ammo.loadedPropellantPower,
                        ammo.loadedChargeLevels, ammo.powerPerCharge, 0.0f, 0.0f, maxSafeCharges, -1.0, List.of(),
                        ammo.loadedChargeLevels > 0 ? "No projectile loaded" : "No loaded munition", false,
                        0.0, 0.0, false);
            }

            ProjectileSnapshot projectile = readProjectileSnapshot(level, ammo.projectileInfo);
            if (projectile == null) {
                String shellName = ammo.projectileInfo.state().getBlock().getName().getString();
                return new CannonRangeData(shellName, cannonYaw, cannonPitch, ammo.loadedPropellantPower,
                        ammo.loadedChargeLevels, ammo.powerPerCharge, 0.0f, 0.0f, maxSafeCharges, -1.0, List.of(),
                        "Unsupported projectile data", false, 0.0, 0.0, false);
            }

            double gravityMultiplier = getDimensionMultiplier(level, dimensionGravityMultiplierMethod, 1.0);
            double dragMultiplier = getDimensionMultiplier(level, dimensionDragMultiplierMethod, 1.0);
            double effectiveGravity = projectile.gravity * gravityMultiplier;
            double effectiveDrag = projectile.drag * dragMultiplier;
            float loadedTotalCharge = ammo.loadedPropellantPower + projectile.addedChargePower;
            double currentRange = loadedTotalCharge >= projectile.minimumChargePower
                    ? simulateRange(loadedTotalCharge, cannonPitch, effectiveGravity, effectiveDrag, projectile.quadraticDrag)
                    : -1.0;

            int displayMax = Math.max(maxSafeCharges, ammo.loadedChargeLevels);
            List<RangeSample> samples = new ArrayList<>();
            if (displayMax > 0 && ammo.powerPerCharge > 0.0f) {
                for (int chargeLevel = 1; chargeLevel <= displayMax; chargeLevel++) {
                    float totalCharge = chargeLevel * ammo.powerPerCharge + projectile.addedChargePower;
                    boolean valid = totalCharge >= projectile.minimumChargePower;
                    double range = valid
                            ? simulateRange(totalCharge, cannonPitch, effectiveGravity, effectiveDrag, projectile.quadraticDrag)
                            : -1.0;
                    samples.add(new RangeSample(chargeLevel, range, chargeLevel == ammo.loadedChargeLevels, valid));
                }
            }

            String status;
            boolean ready;
            if (ammo.loadedChargeLevels <= 0) {
                status = "No propellant loaded";
                ready = false;
            } else if (loadedTotalCharge < projectile.minimumChargePower) {
                status = "Charge too low";
                ready = false;
            } else {
                status = "Ready";
                ready = true;
            }

            return new CannonRangeData(projectile.shellName, cannonYaw, cannonPitch, ammo.loadedPropellantPower,
                    ammo.loadedChargeLevels, ammo.powerPerCharge, projectile.addedChargePower,
                    projectile.minimumChargePower, maxSafeCharges, currentRange, List.copyOf(samples), status, ready,
                    effectiveGravity, effectiveDrag, projectile.quadraticDrag);
        } catch (Exception e) {
            return null;
        }
    }
    
    public static double calculateRange(Level level, BlockPos mountPos, int charges, String shellType) {
        if (!CBC_LOADED) return -1;

        CannonRangeData data = getCannonRangeData(level, mountPos);
        if (data == null || data.powerPerCharge <= 0.0f) {
            return -1;
        }

        float totalCharge = charges * data.powerPerCharge + data.projectileAddedChargePower;
        if (totalCharge < data.minimumChargePower) {
            return -1;
        }

        return simulateRange(totalCharge, data.cannonPitch, data.gravity, data.drag, data.quadraticDrag);
    }
    
    private static double getBaseVelocityForShell(String shellType) {
        if (shellType == null) return 20.0;
        
        return switch (shellType.toLowerCase()) {
            case "ap", "armor_piercing" -> 25.0;
            case "he", "high_explosive" -> 20.0;
            case "shot" -> 15.0;
            case "smoke" -> 18.0;
            case "flak" -> 22.0;
            case "mortar" -> 12.0;
            default -> 20.0;
        };
    }
    
    public static double calculateSimpleRange(Level level, BlockPos mountPos, int charges) {
        return calculateRange(level, mountPos, charges, "he");
    }

    private static AmmoSnapshot inspectMountedAmmo(Iterable<StructureTemplate.StructureBlockInfo> blocks, Direction orientation) {
        StructureTemplate.StructureBlockInfo projectileInfo = null;
        double projectileScore = Double.NEGATIVE_INFINITY;
        float loadedPropellantPower = 0.0f;
        float rawChargeLevels = 0.0f;

        for (StructureTemplate.StructureBlockInfo info : blocks) {
            Block block = info.state().getBlock();
            if (projectileBlockClass != null && projectileBlockClass.isInstance(block)) {
                double score = scoreAlongAxis(info.pos(), orientation);
                if (projectileInfo == null || score > projectileScore) {
                    projectileInfo = info;
                    projectileScore = score;
                }
                continue;
            }

            if (powderChargeBlockClass != null && powderChargeBlockClass.isInstance(block) && powderChargePowerMethod != null) {
                loadedPropellantPower += invokeFloat(pounderOrCartridgeTarget(block), powderChargePowerMethod, info);
                rawChargeLevels += 1.0f;
                continue;
            }

            if (bigCartridgeBlockClass != null && bigCartridgeBlockClass.isInstance(block) && bigCartridgeChargePowerMethod != null) {
                loadedPropellantPower += invokeFloat(pounderOrCartridgeTarget(block), bigCartridgeChargePowerMethod, info);
                float cartridgeLevels = bigCartridgePowerFromDataMethod != null
                        ? invokeFloat(null, bigCartridgePowerFromDataMethod, info)
                        : 1.0f;
                rawChargeLevels += cartridgeLevels > 0.0f ? cartridgeLevels : 1.0f;
            }
        }

        int loadedChargeLevels = rawChargeLevels > 0.0f ? Math.round(rawChargeLevels) : 0;
        float powerPerCharge = rawChargeLevels > 0.0f ? loadedPropellantPower / rawChargeLevels : 0.0f;
        return new AmmoSnapshot(projectileInfo, loadedPropellantPower, loadedChargeLevels, powerPerCharge);
    }

    @Nullable
    private static ProjectileSnapshot readProjectileSnapshot(Level level, StructureTemplate.StructureBlockInfo projectileInfo) {
        Block block = projectileInfo.state().getBlock();
        if (projectileBlockClass == null || !projectileBlockClass.isInstance(block) || getAssociatedEntityTypeMethod == null) {
            return null;
        }

        try {
            EntityType<?> entityType = (EntityType<?>) getAssociatedEntityTypeMethod.invoke(block);
            Object handler = selectProjectileHandler(block);
            if (handler == null || entityType == null) {
                return null;
            }

            Method getPropertiesOfMethod = findMethod(handler.getClass(), "getPropertiesOf", 1);
            if (getPropertiesOfMethod == null) {
                return null;
            }

            Object properties = getPropertiesOfMethod.invoke(handler, entityType);
            if (properties == null) {
                return null;
            }

            Object ballisticProperties = invokeNoArg(properties, "ballistics");
            Object bigCannonProperties = invokeNoArg(properties, "bigCannonProperties");
            if (ballisticProperties == null || bigCannonProperties == null) {
                return null;
            }

            return new ProjectileSnapshot(
                    block.getName().getString(),
                    invokeDouble(ballisticProperties, "gravity", 0.05),
                    invokeDouble(ballisticProperties, "drag", 0.0),
                    invokeBoolean(ballisticProperties, "isQuadraticDrag", false),
                    invokeFloat(bigCannonProperties, "addedChargePower", 0.0f),
                    invokeFloat(bigCannonProperties, "minimumChargePower", 0.0f),
                    invokeBoolean(bigCannonProperties, "canSquib", false));
        } catch (Exception e) {
            return null;
        }
    }

    @Nullable
    private static Object selectProjectileHandler(Block block) {
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(block);
        String key = id != null ? id.getPath() : block.getClass().getName();
        key = key.toLowerCase();

        if (key.contains("drop_mortar")) {
            return dropMortarShellHandler;
        }
        if (key.contains("mortar_stone")) {
            return mortarStoneHandler;
        }
        if (key.contains("smoke_shell")) {
            return smokeShellHandler;
        }
        if (key.contains("fluid_shell")) {
            return fluidShellHandler;
        }
        if (key.contains("shrapnel")) {
            return shrapnelShellHandler;
        }
        if (fuzedProjectileBlockClass != null && fuzedProjectileBlockClass.isInstance(block)) {
            return commonShellHandler;
        }
        if (key.contains("shell")) {
            return commonShellHandler;
        }
        return inertProjectileHandler;
    }

    private static double simulateRange(float chargePower, float cannonPitch, double gravity, double drag, boolean quadraticDrag) {
        if (chargePower <= 0.0f) {
            return -1.0;
        }

        double elevationRadians = Math.toRadians(-cannonPitch);
        double horizontalVelocity = Math.cos(elevationRadians) * chargePower;
        double verticalVelocity = Math.sin(elevationRadians) * chargePower;
        double x = 0.0;
        double y = 0.0;

        for (int tick = 0; tick < 1200; tick++) {
            x += horizontalVelocity;
            y += verticalVelocity;
            if (tick > 0 && y <= 0.0) {
                return Math.max(0.0, x);
            }

            double speed = Math.hypot(horizontalVelocity, verticalVelocity);
            if (speed <= 1.0e-6) {
                return Math.max(0.0, x);
            }

            double dragForce = drag * speed;
            if (quadraticDrag) {
                dragForce *= speed;
            }
            dragForce = Math.min(dragForce, speed);

            double dragX = -(horizontalVelocity / speed) * dragForce;
            double dragY = -(verticalVelocity / speed) * dragForce;
            horizontalVelocity += dragX;
            verticalVelocity += dragY - gravity;
        }

        return Math.max(0.0, x);
    }

    private static double scoreAlongAxis(BlockPos pos, Direction orientation) {
        int sign = orientation.getAxisDirection().getStep();
        return switch (orientation.getAxis()) {
            case X -> pos.getX() * sign;
            case Y -> pos.getY() * sign;
            case Z -> pos.getZ() * sign;
        };
    }

    @Nullable
    private static Object getStaticField(Class<?> owner, String name) {
        try {
            Field field = owner.getField(name);
            return field.get(null);
        } catch (Exception e) {
            return null;
        }
    }

    @Nullable
    private static Method findMethod(Class<?> owner, String name, int parameterCount) {
        for (Method method : owner.getMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == parameterCount) {
                return method;
            }
        }
        return null;
    }

    @Nullable
    private static Object invokeNoArg(Object target, String methodName) {
        try {
            Method method = target.getClass().getMethod(methodName);
            return method.invoke(target);
        } catch (Exception e) {
            return null;
        }
    }

    private static float invokeFloat(@Nullable Object target, Method method, Object... args) {
        try {
            Object result = method.invoke(target, args);
            if (result instanceof Number number) {
                return number.floatValue();
            }
        } catch (Exception e) {
        }
        return 0.0f;
    }

    private static float invokeFloat(Object target, String methodName, float fallback) {
        try {
            Method method = target.getClass().getMethod(methodName);
            Object result = method.invoke(target);
            if (result instanceof Number number) {
                return number.floatValue();
            }
        } catch (Exception e) {
        }
        return fallback;
    }

    private static double invokeDouble(Object target, String methodName, double fallback) {
        try {
            Method method = target.getClass().getMethod(methodName);
            Object result = method.invoke(target);
            if (result instanceof Number number) {
                return number.doubleValue();
            }
        } catch (Exception e) {
        }
        return fallback;
    }

    private static boolean invokeBoolean(Object target, String methodName, boolean fallback) {
        try {
            Method method = target.getClass().getMethod(methodName);
            Object result = method.invoke(target);
            if (result instanceof Boolean bool) {
                return bool;
            }
        } catch (Exception e) {
        }
        return fallback;
    }

    private static double getDimensionMultiplier(Level level, @Nullable Method accessor, double fallback) {
        if (getDimensionPropertiesMethod == null || accessor == null) {
            return fallback;
        }
        try {
            Object properties = getDimensionPropertiesMethod.invoke(null, level);
            if (properties == null) {
                return fallback;
            }
            Object result = accessor.invoke(properties);
            if (result instanceof Number number) {
                return number.doubleValue();
            }
        } catch (Exception e) {
        }
        return fallback;
    }

    private static Object pounderOrCartridgeTarget(Block block) {
        return block;
    }

    private record AmmoSnapshot(@Nullable StructureTemplate.StructureBlockInfo projectileInfo,
                                float loadedPropellantPower,
                                int loadedChargeLevels,
                                float powerPerCharge) {
    }

    private record ProjectileSnapshot(String shellName,
                                      double gravity,
                                      double drag,
                                      boolean quadraticDrag,
                                      float addedChargePower,
                                      float minimumChargePower,
                                      boolean canSquib) {
    }

    public record RangeSample(int chargeLevel, double range, boolean loaded, boolean valid) {
    }

    public record CannonRangeData(String shellName,
                                  float cannonYaw,
                                  float cannonPitch,
                                  float loadedPropellantPower,
                                  int loadedChargeLevels,
                                  float powerPerCharge,
                                  float projectileAddedChargePower,
                                  float minimumChargePower,
                                  int maxSafeCharges,
                                  double currentRange,
                                  List<RangeSample> samples,
                                  String status,
                                  boolean ready,
                                  double gravity,
                                  double drag,
                                  boolean quadraticDrag) {
    }
}
