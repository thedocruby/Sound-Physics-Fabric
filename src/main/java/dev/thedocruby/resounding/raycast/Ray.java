package dev.thedocruby.resounding.raycast;

import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Ray {
    public final double distance;
    public final Vec3d position;
    public final @Nullable Vec3d permeated;
    public final double permeation;
    public final @Nullable Vec3d reflected;
    public final double reflection;
    public Ray(double distance, @NotNull Vec3d position, @Nullable Vec3d permeated, double permeation, @Nullable Vec3d reflected, double reflection) {
        this.distance = distance;
        this.reflection = reflection;
        this.permeation = permeation;
        this.position = position;
        this.reflected = reflected;
        this.permeated = permeated;
    }
}
