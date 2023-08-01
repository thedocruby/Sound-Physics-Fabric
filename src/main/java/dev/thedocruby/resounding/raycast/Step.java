package dev.thedocruby.resounding.raycast;

import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import org.jetbrains.annotations.NotNull;

public record Step
        ( @NotNull Vec3d step
        , @NotNull Vec3i plane
) {}
