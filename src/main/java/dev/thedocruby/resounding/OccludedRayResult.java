package dev.thedocruby.resounding;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Objects;

@Environment(EnvType.CLIENT)
public record OccludedRayResult(
        int lastLeg,
        double totalDistance,
        double totalOcclusion,
        double[] legDistance,
        double[] totalLegDistance,
        double[] legOcclusion,
        double[] totalLegOcclusion
) {
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OccludedRayResult result = (OccludedRayResult) o;
        return                   lastLeg         == result.lastLeg
                &&               totalDistance   == result.totalDistance
                &&               totalOcclusion  == result.totalOcclusion
                && Arrays.equals(legDistance,       result.legDistance      )
                && Arrays.equals(totalLegDistance,  result.totalLegDistance )
                && Arrays.equals(legOcclusion,      result.legOcclusion     )
                && Arrays.equals(totalLegOcclusion, result.totalLegOcclusion);
    }

    @Override
    public int hashCode() {
        int hash = Objects.hash(lastLeg, totalDistance, totalOcclusion);
        hash = 31 * hash + Arrays.hashCode(legDistance);
        hash = 31 * hash + Arrays.hashCode(totalLegDistance);
        hash = 31 * hash + Arrays.hashCode(legOcclusion);
        hash = 31 * hash + Arrays.hashCode(totalLegOcclusion);
        return hash;
    }

    @Override
    public @NotNull String toString() {
        return "    OccludedRayResult {\n" +
                   "        lastLeg = "           +                 lastLeg            +
                ";\n        totalDistance = "     +                 totalDistance      +
                ";\n        totalOcclusion = "    +                 totalOcclusion     +
                ";\n        legDistance = "       + Arrays.toString(legDistance      ) +
                ";\n        totalLegDistance = "  + Arrays.toString(totalLegDistance ) +
                ";\n        legOcclusion = "      + Arrays.toString(legOcclusion     ) +
                ";\n        totalLegOcclusion = " + Arrays.toString(totalLegOcclusion) +
                ";\n    }";
    }
}