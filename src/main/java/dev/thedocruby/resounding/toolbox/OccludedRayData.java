package dev.thedocruby.resounding.toolbox;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Objects;

@Environment(EnvType.CLIENT)
public record OccludedRayData
		( int lastLeg
		, double totalDistance
		, double totalOcclusion
		, double[] legDistance
		, double[] totalLegDistance
		, double[] legOcclusion
		, double[] totalLegOcclusion
) {
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		OccludedRayData data = (OccludedRayData) o;
		return                   lastLeg         == data.lastLeg
				&&               totalDistance   == data.totalDistance
				&&               totalOcclusion  == data.totalOcclusion
				&& Arrays.equals(legDistance,       data.legDistance      )
				&& Arrays.equals(totalLegDistance,  data.totalLegDistance )
				&& Arrays.equals(legOcclusion,      data.legOcclusion     )
				&& Arrays.equals(totalLegOcclusion, data.totalLegOcclusion);
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
		return "    OccludedRayData {\n" +
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
