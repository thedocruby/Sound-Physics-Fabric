package dev.thedocruby.resounding;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.Arrays;
import java.util.Objects;

import static dev.thedocruby.resounding.config.PrecomputedConfig.pC;

@Environment(EnvType.CLIENT)
public record RayResult(
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
        RayResult result = (RayResult) o;
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
        int result = Objects.hash(lastBounce, missed, totalDistance, totalReflectivity);
        result = 31 * result + Arrays.hashCode(shared);
        result = 31 * result + Arrays.hashCode(energyToPlayer);
        result = 31 * result + Arrays.hashCode(bounceDistance);
        result = 31 * result + Arrays.hashCode(totalBounceDistance);
        result = 31 * result + Arrays.hashCode(bounceReflectivity);
        result = 31 * result + Arrays.hashCode(totalBounceReflectivity);
        return result;
    }

    @Override
    public String toString() {
        return "RayResult{" +
                "lastBounce="               +                 lastBounce               +
                ";missed="                  +                 missed                   +
                ";totalDistance="           +                 totalDistance            +
                ";totalReflectivity="       +                 totalReflectivity        +
                ";shared="                  + Arrays.toString(shared                 ) +
                ";energyToPlayer="          + Arrays.toString(energyToPlayer         ) +
                ";bounceDistance="          + Arrays.toString(bounceDistance         ) +
                ";totalBounceDistance="     + Arrays.toString(totalBounceDistance    ) +
                ";bounceReflectivity="      + Arrays.toString(bounceReflectivity     ) +
                ";totalBounceReflectivity=" + Arrays.toString(totalBounceReflectivity) +
                ";}";
    }
}
