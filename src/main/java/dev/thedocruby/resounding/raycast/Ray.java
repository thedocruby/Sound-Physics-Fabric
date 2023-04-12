package dev.thedocruby.resounding.raycast;

import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record Ray (
        //MaterialData material,
        double power,
        @NotNull Vec3d position,
        @Nullable Vec3d vector,
        double length
) {
    
}
