package dev.thedocruby.resounding.toolbox;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Objects;

@Environment(EnvType.CLIENT)
public record RayData(
		int lastStep,
		double totalDistance,
		double totalValue,
		double[] stepDistance,
		double[] totalStepDistance,
		double[] stepValue,
		double[] totalStepValue
) {
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		RayData data = (RayData) o;
		return                   lastStep        == data.lastStep
				&&               totalDistance   == data.totalDistance
				&&               totalValue      == data.totalValue
				&& Arrays.equals(stepDistance,      data.stepDistance     )
				&& Arrays.equals(totalStepDistance, data.totalStepDistance)
				&& Arrays.equals(stepValue,         data.stepValue        )
				&& Arrays.equals(totalStepValue,	data.totalStepValue   );
	}

	@Override
	public int hashCode() {
		int hash = Objects.hash(lastStep, totalDistance, totalValue);
		hash = 31 * hash + Arrays.hashCode(stepDistance);
		hash = 31 * hash + Arrays.hashCode(totalStepDistance);
		hash = 31 * hash + Arrays.hashCode(stepValue);
		hash = 31 * hash + Arrays.hashCode(totalStepValue);
		return hash;
	}

	@Override
	public @NotNull String toString() {
		return "    RayData {\n" +
				   "        lastStep = "          +                 lastStep           +
				";\n        totalDistance = "     +                 totalDistance      +
				";\n        totalValue = "        +                 totalValue         +
				";\n        stepDistance = "      + Arrays.toString(stepDistance     ) +
				";\n        totalStepDistance = " + Arrays.toString(totalStepDistance) +
				";\n        stepValue = "         + Arrays.toString(stepValue        ) +
				";\n        totalStepValue = "    + Arrays.toString(totalStepValue   ) +
				";\n    }";
	}
}
