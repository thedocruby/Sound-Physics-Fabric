package dev.thedocruby.resounding;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.*;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static dev.thedocruby.resounding.config.PrecomputedConfig.pC;

@Environment(EnvType.CLIENT)
public class RaycastRenderer {

    private static final List<Ray> rays = Collections.synchronizedList(new ArrayList<>());

    public static void renderRays(double x, double y, double z, World world) {
        if (world == null) {
            return;
        }
        // ψ Get the name of the block you are standing on ψ
        //world.getPlayers().forEach((p) -> p.sendMessage(new LiteralText(world.getBlockState(p.getBlockPos().add(0,-1,0)).getBlock().getTranslationKey()),true));
        long gameTime = world.getTime();
        synchronized (rays) {
            for (Ray ray : rays) {
                if (ray.tickCreated == -1) ray.tickCreated = gameTime;
                renderRay(ray, x, y, z);
            }
            rays.removeIf(ray -> (gameTime - ray.tickCreated) > ray.lifespan || (gameTime - ray.tickCreated) < 0L);
        }
    }

    public static void addSoundBounceRay(Vec3d start, Vec3d end, int color) {
        if (!pC.dRays) {
            return;
        }
        addRay(start, end, color, false);
    }

    public static void addOcclusionRay(Vec3d start, Vec3d end, int color) {
        if (!pC.dRays) {
            return;
        }
        addRay(start, end, color, true);
    }

    public static void addRay(Vec3d start, Vec3d end, int color, boolean throughWalls) {
        synchronized (rays) {
            rays.add(new Ray(start, end, color, throughWalls));
        }
    }

    public static void renderRay(Ray ray, double x, double y, double z) {
        int red = getRed(ray.color);
        int green = getGreen(ray.color);
        int blue = getBlue(ray.color);

        if (!ray.throughWalls) {
            RenderSystem.enableDepthTest();
        }
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.getBuffer();
        RenderSystem.disableTexture();
        RenderSystem.disableBlend();
        RenderSystem.lineWidth(ray.throughWalls ? 3F : 0.25F);

        bufferBuilder.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);

        bufferBuilder.vertex(ray.start.x - x, ray.start.y - y, ray.start.z - z).color(red, green, blue, 255).next();
        bufferBuilder.vertex(ray.end.x - x, ray.end.y - y, ray.end.z - z).color(red, green, blue, 255).next();

        tessellator.draw();
        RenderSystem.lineWidth(1F);
        RenderSystem.enableBlend();
        RenderSystem.enableTexture();
    }

    private static int getRed(int argb) {
        return (argb >> 16) & 0xFF;
    }

    private static int getGreen(int argb) {
        return (argb >> 8) & 0xFF;
    }

    private static int getBlue(int argb) {
        return argb & 0xFF;
    }

    private static class Ray {
        private final Vec3d start;
        private final Vec3d end;
        private final int color;
        private long tickCreated;
        private final long lifespan;
        private final boolean throughWalls;

        public Ray(Vec3d start, Vec3d end, int color, boolean throughWalls) {
            this.start = start;
            this.end = end;
            this.color = color;
            this.throughWalls = throughWalls;
            this.tickCreated = -1;
            this.lifespan = 20 * 2;
        }
    }

}