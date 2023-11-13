package dev.thedocruby.resounding.raycast;

import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public record Hit(
        Vec3d position,  // end position of ray
        double length,   // length of ray (including prior bounces) when hit
        double shared,   // ??
        double distance, // distance from listener
        double segment,  // length of individual segment
        double reflect,  // reflectivity of surface
        double amplitude // amplitude of ray
) {

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Hit data = (Hit) o;
        return  // only need these to determine equality, all else will be moot based on rest of architecture
                length  () == data.length  ()
             && position() == data.position()
             && reflect () == data.reflect ();
    }

    @Override
    public int hashCode() {
        int hash = Objects.hash(length);
        hash = 31 * hash + Objects.hash(position);
        hash = 31 * hash + Objects.hash(reflect);
        return hash;
    }

    @Override
    public @NotNull String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("    ReflectedRayData ");
        sb.append(";\n        position   = ").append(position);
        sb.append(";\n        length     = ").append(length);
        sb.append(";\n        shared     = ").append(shared);
        sb.append(";\n        distance   = ").append(distance);
        sb.append(";\n        segment    = ").append(segment);
        sb.append(";\n        reflection = ").append(reflect);
        sb.append(";\n        amplitude  = ").append(amplitude);
        sb.append(";\n    }");
        return sb.toString();
    }
}
