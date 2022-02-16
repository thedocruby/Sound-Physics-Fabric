package dev.thedocruby.resounding.toolbox;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Objects;

@Environment(EnvType.CLIENT)
public record ReflectedRayData(
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
        ReflectedRayData data = (ReflectedRayData) o;
        return               lastBounce            == data.lastBounce
            &&               missed                == data.missed
            &&               totalDistance         == data.totalDistance
            &&               totalReflectivity     == data.totalReflectivity
            && Arrays.equals(shared,                  data.shared                 )
            && Arrays.equals(energyToPlayer,          data.energyToPlayer         )
            && Arrays.equals(bounceDistance,          data.bounceDistance         )
            && Arrays.equals(totalBounceDistance,     data.totalBounceDistance    )
            && Arrays.equals(bounceReflectivity,      data.bounceReflectivity     )
            && Arrays.equals(totalBounceReflectivity, data.totalBounceReflectivity);
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
        return "    ReflectedRayData {\n" +
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
