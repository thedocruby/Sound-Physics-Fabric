package dev.thedocruby.resounding.raycast;

import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

public record Step (
        Vec3d step,
        Vec3i plane
) {}
