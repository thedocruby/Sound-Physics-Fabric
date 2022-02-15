package dev.thedocruby.resounding;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Objects;

@Environment(EnvType.CLIENT)
public record ReflectedRayResult(
        int lastBounce,
        double missed,
        double totalDistance,
        double totalReflectivity,
        double[] shared,
        double[] energyToPlayer,
        double[] bounceDistance,
        double[] totalBounceDistance,
        double[] bounceReflectivity,
        double[] totalBounceReflectivity
        ) {
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReflectedRayResult result = (ReflectedRayResult) o;
        return               lastBounce            == result.lastBounce
            &&               missed                == result.missed
            &&               totalDistance         == result.totalDistance
            &&               totalReflectivity     == result.totalReflectivity
            && Arrays.equals(shared,                  result.shared                 )
            && Arrays.equals(energyToPlayer,          result.energyToPlayer         )
            && Arrays.equals(bounceDistance,          result.bounceDistance         )
            && Arrays.equals(totalBounceDistance,     result.totalBounceDistance    )
            && Arrays.equals(bounceReflectivity,      result.bounceReflectivity     )
            && Arrays.equals(totalBounceReflectivity, result.totalBounceReflectivity);
    }

    @Override
    public int hashCode() {
        int hash = Objects.hash(lastBounce, missed, totalDistance, totalReflectivity);
        hash = 31 * hash + Arrays.hashCode(shared);
        hash = 31 * hash + Arrays.hashCode(energyToPlayer);
        hash = 31 * hash + Arrays.hashCode(bounceDistance);
        hash = 31 * hash + Arrays.hashCode(totalBounceDistance);
        hash = 31 * hash + Arrays.hashCode(bounceReflectivity);
        hash = 31 * hash + Arrays.hashCode(totalBounceReflectivity);
        return hash;
    }

    @Override
    public @NotNull String toString() {
        return "    ReflectedRayResult {\n" +
                   "        lastBounce = "              +                 lastBounce               +
                ";\n        missed = "                  +                 missed                   +
                ";\n        totalDistance = "           +                 totalDistance            +
                ";\n        totalReflectivity = "       +                 totalReflectivity        +
                ";\n        shared = "                  + Arrays.toString(shared                 ) +
                ";\n        energyToPlayer = "          + Arrays.toString(energyToPlayer         ) +
                ";\n        bounceDistance = "          + Arrays.toString(bounceDistance         ) +
                ";\n        totalBounceDistance = "     + Arrays.toString(totalBounceDistance    ) +
                ";\n        bounceReflectivity = "      + Arrays.toString(bounceReflectivity     ) +
                ";\n        totalBounceReflectivity = " + Arrays.toString(totalBounceReflectivity) +
                ";\n    }";
    }
}
