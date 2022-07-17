package dev.thedocruby.resounding.toolbox;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Objects;

@Environment(EnvType.CLIENT)
public record ReflectedRayData(
		int size,
		double missed,
		double totalDistance,
		double totalReflectivity,
		double[] shared,
		double[] distToPlayer,
		double[] bounceDistance,
		double[] totalBounceDistance,
		double[] bounceReflectivity,
		double[] totalBounceEnergy
		) {
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ReflectedRayData data = (ReflectedRayData) o;
		return               size == data.size
			&&               missed                == data.missed
			&&               totalDistance         == data.totalDistance
			&&               totalReflectivity     == data.totalReflectivity
			&& Arrays.equals(shared,                  data.shared                 )
			&& Arrays.equals(distToPlayer,          data.distToPlayer)
			&& Arrays.equals(bounceDistance,          data.bounceDistance         )
			&& Arrays.equals(totalBounceDistance,     data.totalBounceDistance    )
			&& Arrays.equals(bounceReflectivity,      data.bounceReflectivity     )
			&& Arrays.equals(totalBounceEnergy, data.totalBounceEnergy);
	}

	@Override
	public int hashCode() {
		int hash = Objects.hash(size, missed, totalDistance, totalReflectivity);
		hash = 31 * hash + Arrays.hashCode(shared);
		hash = 31 * hash + Arrays.hashCode(distToPlayer);
		hash = 31 * hash + Arrays.hashCode(bounceDistance);
		hash = 31 * hash + Arrays.hashCode(totalBounceDistance);
		hash = 31 * hash + Arrays.hashCode(bounceReflectivity);
		hash = 31 * hash + Arrays.hashCode(totalBounceEnergy);
		return hash;
	}

	@Override
	public @NotNull String toString() {
		return  "    ReflectedRayData {\n" +
				   "        size = "              + size +
				";\n        missed = "                  +                 missed                   +
				";\n        totalDistance = "           +                 totalDistance            +
				";\n        totalReflectivity = "       +                 totalReflectivity        +
				";\n        shared = "                  + Arrays.toString(shared                 ) +
				";\n        distToPlayer = "          + Arrays.toString(distToPlayer) +
				";\n        bounceDistance = "          + Arrays.toString(bounceDistance         ) +
				";\n        totalBounceDistance = "     + Arrays.toString(totalBounceDistance    ) +
				";\n        bounceReflectivity = "      + Arrays.toString(bounceReflectivity     ) +
				";\n        totalBounceEnergy = " + Arrays.toString(totalBounceEnergy) +
				";\n    }";
	}
}
