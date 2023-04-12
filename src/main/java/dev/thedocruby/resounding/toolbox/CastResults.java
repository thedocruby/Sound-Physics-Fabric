package dev.thedocruby.resounding.toolbox;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Objects;

import static dev.thedocruby.resounding.config.PrecomputedConfig.pC;

@Environment(EnvType.CLIENT)
public class CastResults {
	public int bounces;        // number of bounces
	@Deprecated
	public double missed;
	public double length;      // total length along ray
	public double[] shared;    // ??
	public double[] distance;  // distance from listener
	public double[] segments;  // length of individual ray segments
	public double[] lengths;   // total ray length at each bounce
	public double[] surfaces;  // reflectivity of surfaces hit
	public double[] amplitude; // amplitude along ray

	public CastResults(int bounces, @Deprecated double missed, double length) {
			this.bounces = bounces;
			this.missed = missed;
			this.length = length;

			this.shared    = new double[pC.nRayBounces];
			this.distance  = new double[pC.nRayBounces];
			this.segments  = new double[pC.nRayBounces];
			this.lengths   = new double[pC.nRayBounces];
			this.surfaces  = new double[pC.nRayBounces];
			this.amplitude = new double[pC.nRayBounces];
	}

	public void add(double shared, double distance, double segment, double surface, double amplitude) {
		this.shared   [bounces] = shared;
		this.distance [bounces] = distance;
		this.segments [bounces] = segment;
		this.surfaces [bounces] = surface;
		this.amplitude[bounces] = amplitude;

		this.length += segment;
		this.lengths  [bounces] = length;
		bounces++;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		CastResults data = (CastResults) o;
		return               bounces == data.bounces
			&&               missed  == data.missed
			&&               length  == data.length
			&& Arrays.equals(shared,    data.shared   )
			&& Arrays.equals(distance,  data.distance )
			&& Arrays.equals(segments,  data.segments )
			&& Arrays.equals(lengths,   data.lengths  )
			&& Arrays.equals(surfaces,  data.surfaces )
			&& Arrays.equals(amplitude, data.amplitude);
	}

	@Override
	public int hashCode() {
		int hash = Objects.hash(bounces, missed, length);
		hash = 31 * hash + Arrays.hashCode(shared   );
		hash = 31 * hash + Arrays.hashCode(distance );
		hash = 31 * hash + Arrays.hashCode(segments );
		hash = 31 * hash + Arrays.hashCode(lengths  );
		hash = 31 * hash + Arrays.hashCode(surfaces );
		hash = 31 * hash + Arrays.hashCode(amplitude);
		return hash;
	}

	@Override
	public @NotNull String toString() {
		return     "    ReflectedRayData "                             +
				"{\n        bounces   = " + bounces                    +
				";\n        missed    = " + missed                     +
				";\n        length    = " + length                     +
				";\n        shared    = " + Arrays.toString(shared   ) +
				";\n        distance  = " + Arrays.toString(distance ) +
				";\n        segments  = " + Arrays.toString(segments ) +
				";\n        lengths   = " + Arrays.toString(lengths  ) +
				";\n        surfaces  = " + Arrays.toString(surfaces ) +
				";\n        amplitude = " + Arrays.toString(amplitude) +
				";\n    }";
	}
}
