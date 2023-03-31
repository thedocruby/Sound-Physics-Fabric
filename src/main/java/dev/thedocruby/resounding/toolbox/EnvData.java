package dev.thedocruby.resounding.toolbox;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Set;

public record EnvData(
		Set<CastResults> reflRays,
		Set<OccludedRayData> occlRays
) {
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		EnvData data = (EnvData) o;
		return reflRays == data.reflRays && occlRays == data.occlRays;
	}

	@Override
	public int hashCode() { return Objects.hash(reflRays, occlRays); }

	@Override
	public @NotNull String toString() {
		return "EnvData {\n" +
				String.join("\n", reflRays.stream().sequential().map(CastResults::toString).toList()) + "\n" +
				String.join("\n", occlRays.stream().sequential().map( OccludedRayData::toString).toList()) + "\n}";
	}
}
