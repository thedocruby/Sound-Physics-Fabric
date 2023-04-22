package dev.thedocruby.resounding.toolbox;

import static dev.thedocruby.resounding.config.PrecomputedConfig.pC;

public record MaterialData(
	String example,
	double reflectivity,
	double permeability
	) {
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		MaterialData data = (MaterialData) o;
		return (   Math.abs(this.reflectivity() - data.reflectivity()) <= pC.threshold
				&& Math.abs(this.permeability() - data.permeability()) <= pC.threshold
				);
	}
}