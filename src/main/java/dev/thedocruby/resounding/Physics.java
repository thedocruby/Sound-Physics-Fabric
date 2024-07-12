package dev.thedocruby.resounding;

import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class Physics {

    @Contract("_, _ -> new")
    public static @NotNull Vec3d pseudoReflect(Vec3d ray, @NotNull Vec3i plane) { return pseudoReflect(ray,plane,2); }

    @Contract("_, _, _ -> new")
    public static @NotNull Vec3d pseudoReflect(Vec3d ray, @NotNull Vec3i plane, double fresnel) {
        // Fresnels on a 1-30 scale
        // TODO account for http://hyperphysics.phy-astr.gsu.edu/hbase/Tables/indrf.html

        final Vec3d planeD = new Vec3d(
                plane.getX(),
                plane.getY(),
                plane.getZ()
        );
        // ( ray - plane * (normal/air) * dot(ray,plane) ) / air * normal
        // https://blog.demofox.org/2017/01/09/raytracing-reflection-refraction-fresnel-total-internal-reflection-and-beers-law/
        // adjusted for refraction approximation
        // for a visualization, see: https://www.math3d.org/UYUQRza8n
        // assert fresnel != 0;
        // return ray.multiply(planeD.multiply(-1));
        //*
        return ray.subtract(
                planeD.multiply
                        (ray.multiply(planeD).multiply(fresnel))
        );
        // */
    }

    public static @NotNull Double reflection(@NotNull Double impedanceA, @NotNull Double impedanceB) {
        // difference of squares
        return impedanceA == impedanceB ? 0.0 : Math.pow( (impedanceA - impedanceB) / (impedanceA + impedanceB), 2);
    }
}
