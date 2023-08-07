package dev.thedocruby.resounding;

import org.jetbrains.annotations.Nullable;

public record RawMaterial(
        @Nullable Double   weight,       // weight of tag (atomic mass)
        @Nullable String   solvent,      // tag that solute / main material lives in
        @Nullable String[] solute,       // tags that comprise other values when empty
        @Nullable Double[] composition,  // how much importance is applied to each solute
        @Nullable Double   granularity,  // boundary count between solvent & solute
        @Nullable Double   melt,         // melting point (kelvin)
        @Nullable Double   boil,         // boiling point (kelvin)
        @Nullable Double   temperature,  // used to override atmospheric effects (lock s/lwave).

        @Nullable Double   density,      // density of tag (kg/m³ or %)
        @Nullable Double   swave,        // shear-wave velocity (for solids)
        @Nullable Double   lwave         // longitudinal-wave velocity (for liquids & gasses)
) {
}
