package com.cta.client.debug;

import com.cta.CTA;
import com.cta.command.CTADebugCommand;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Mod.EventBusSubscriber(modid = CTA.MODID, value = Dist.CLIENT)
public class MissileDebugRenderer {
    
    private static final List<FlightPath> flightPaths = new CopyOnWriteArrayList<>();
    private static final List<ExplosionDebug> explosions = new CopyOnWriteArrayList<>();
    private static final List<PenetrationDebug> penetrations = new CopyOnWriteArrayList<>();
    private static final List<FragmentDebug> fragments = new CopyOnWriteArrayList<>();
    
    private static final int VISUALIZATION_DURATION = 200; 
    
    public static void addFlightPoint(int entityId, Vec3 position, DebugColor color) {
        if (!CTADebugCommand.isDebugEnabled()) return;
        
        FlightPath path = flightPaths.stream()
                .filter(p -> p.entityId == entityId)
                .findFirst()
                .orElse(null);
        
        if (path == null) {
            path = new FlightPath(entityId, color);
            flightPaths.add(path);
        }
        
        path.addPoint(position);
    }
    
    public static void addExplosion(Vec3 center, float radius, DebugColor color) {
        if (!CTADebugCommand.isDebugEnabled()) return;
        explosions.add(new ExplosionDebug(center, radius, color, VISUALIZATION_DURATION));
    }
    
    public static void addPenetrationPath(Vec3 start, Vec3 end, boolean succeeded) {
        if (!CTADebugCommand.isDebugEnabled()) return;
        penetrations.add(new PenetrationDebug(start, end, succeeded, VISUALIZATION_DURATION));
    }
    
    public static void addFragment(Vec3 origin, Vec3 direction, double range, boolean hitEntity) {
        if (!CTADebugCommand.isDebugEnabled()) return;
        fragments.add(new FragmentDebug(origin, direction, range, hitEntity, VISUALIZATION_DURATION));
    }
    
    public static void clearAll() {
        flightPaths.clear();
        explosions.clear();
        penetrations.clear();
        fragments.clear();
    }
    
    public static void removeFlightPath(int entityId) {
        flightPaths.removeIf(p -> p.entityId == entityId);
    }
    
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!CTADebugCommand.isDebugEnabled()) return;
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        
        updateTimers();
    }
    
    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        if (!CTADebugCommand.isDebugEnabled()) return;
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        
        if (flightPaths.isEmpty() && explosions.isEmpty() && penetrations.isEmpty() && fragments.isEmpty()) {
            return;
        }
        
        Vec3 cameraPos = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        
        poseStack.pushPose();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.lineWidth(2.0f);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        
        Matrix4f matrix = poseStack.last().pose();
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        
        try {
            buffer.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
            
            for (FlightPath path : flightPaths) {
                List<Vec3> points = path.points;
                for (int i = 0; i < points.size() - 1; i++) {
                    Vec3 p1 = points.get(i);
                    Vec3 p2 = points.get(i + 1);
                    drawLine(buffer, matrix, p1, p2, path.color);
                }
            }
            
            for (ExplosionDebug explosion : explosions) {
                drawSphereWireframe(buffer, matrix, explosion.center, explosion.radius, explosion.color);
            }
            
            for (PenetrationDebug pen : penetrations) {
                DebugColor color = pen.succeeded ? DebugColor.ORANGE : DebugColor.DARK_RED;
                drawLine(buffer, matrix, pen.start, pen.end, color);
                if (!pen.succeeded) {
                    drawX(buffer, matrix, pen.end, 0.3, DebugColor.DARK_RED);
                }
            }
            
            for (FragmentDebug frag : fragments) {
                Vec3 end = frag.origin.add(frag.direction.scale(frag.range));
                DebugColor color = frag.hitEntity ? DebugColor.RED : DebugColor.YELLOW;
                drawLine(buffer, matrix, frag.origin, end, color);
            }
            
            tesselator.end();
        } catch (Exception e) {
            try {
                tesselator.end();
            } catch (Exception ignored) {}
        }
        
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.lineWidth(1.0f);
        
        poseStack.popPose();
    }
    
    private static void updateTimers() {
        explosions.removeIf(exp -> {
            exp.ticksRemaining--;
            return exp.ticksRemaining <= 0;
        });
        
        penetrations.removeIf(pen -> {
            pen.ticksRemaining--;
            return pen.ticksRemaining <= 0;
        });
        
        fragments.removeIf(frag -> {
            frag.ticksRemaining--;
            return frag.ticksRemaining <= 0;
        });
    }
    
    private static void drawLine(BufferBuilder buffer, Matrix4f matrix, Vec3 p1, Vec3 p2, DebugColor color) {
        buffer.vertex(matrix, (float) p1.x, (float) p1.y, (float) p1.z)
              .color(color.r, color.g, color.b, color.a).endVertex();
        buffer.vertex(matrix, (float) p2.x, (float) p2.y, (float) p2.z)
              .color(color.r, color.g, color.b, color.a).endVertex();
    }
    
    private static void drawX(BufferBuilder buffer, Matrix4f matrix, Vec3 center, double size, DebugColor color) {
        drawLine(buffer, matrix, 
            center.add(-size, 0, -size), center.add(size, 0, size), color);
        drawLine(buffer, matrix, 
            center.add(size, 0, -size), center.add(-size, 0, size), color);
        drawLine(buffer, matrix, 
            center.add(0, -size, -size), center.add(0, size, size), color);
    }
    
    private static void drawSphereWireframe(BufferBuilder buffer, Matrix4f matrix, Vec3 center, float radius, DebugColor color) {
        int segments = 16;
        
        for (int i = 0; i < segments; i++) {
            double angle1 = (i / (double) segments) * Math.PI * 2;
            double angle2 = ((i + 1) / (double) segments) * Math.PI * 2;
            
            Vec3 p1 = center.add(Math.cos(angle1) * radius, 0, Math.sin(angle1) * radius);
            Vec3 p2 = center.add(Math.cos(angle2) * radius, 0, Math.sin(angle2) * radius);
            drawLine(buffer, matrix, p1, p2, color);
            
            p1 = center.add(Math.cos(angle1) * radius, Math.sin(angle1) * radius, 0);
            p2 = center.add(Math.cos(angle2) * radius, Math.sin(angle2) * radius, 0);
            drawLine(buffer, matrix, p1, p2, color);
            
            p1 = center.add(0, Math.cos(angle1) * radius, Math.sin(angle1) * radius);
            p2 = center.add(0, Math.cos(angle2) * radius, Math.sin(angle2) * radius);
            drawLine(buffer, matrix, p1, p2, color);
        }
    }
    
    public enum DebugColor {
        RED(255, 0, 0, 255),
        DARK_RED(139, 0, 0, 255),
        ORANGE(255, 165, 0, 255),
        YELLOW(255, 255, 0, 255),
        GREEN(0, 255, 0, 255),
        BLUE(0, 100, 255, 255),
        CYAN(0, 255, 255, 255),
        WHITE(255, 255, 255, 255);
        
        final int r, g, b, a;
        
        DebugColor(int r, int g, int b, int a) {
            this.r = r;
            this.g = g;
            this.b = b;
            this.a = a;
        }
    }
    
    private static class FlightPath {
        final int entityId;
        final DebugColor color;
        final List<Vec3> points = new ArrayList<>();
        
        FlightPath(int entityId, DebugColor color) {
            this.entityId = entityId;
            this.color = color;
        }
        
        void addPoint(Vec3 point) {
            if (points.size() > 500) {
                points.remove(0);
            }
            points.add(point);
        }
    }
    
    private static class ExplosionDebug {
        final Vec3 center;
        final float radius;
        final DebugColor color;
        int ticksRemaining;
        
        ExplosionDebug(Vec3 center, float radius, DebugColor color, int duration) {
            this.center = center;
            this.radius = radius;
            this.color = color;
            this.ticksRemaining = duration;
        }
    }
    
    private static class PenetrationDebug {
        final Vec3 start;
        final Vec3 end;
        final boolean succeeded;
        int ticksRemaining;
        
        PenetrationDebug(Vec3 start, Vec3 end, boolean succeeded, int duration) {
            this.start = start;
            this.end = end;
            this.succeeded = succeeded;
            this.ticksRemaining = duration;
        }
    }
    
    private static class FragmentDebug {
        final Vec3 origin;
        final Vec3 direction;
        final double range;
        final boolean hitEntity;
        int ticksRemaining;
        
        FragmentDebug(Vec3 origin, Vec3 direction, double range, boolean hitEntity, int duration) {
            this.origin = origin;
            this.direction = direction;
            this.range = range;
            this.hitEntity = hitEntity;
            this.ticksRemaining = duration;
        }
    }
}
