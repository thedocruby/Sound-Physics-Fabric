package dev.thedocruby.resounding;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.Nullable;

public record RawMaterial(
        @SerializedName("weight")
        @Nullable Double   weight,       // weight of tag (atomic mass)

        @SerializedName("solvent")
        @Nullable String   solvent,      // tag that solute / main material lives in
        @SerializedName("solute")
        @Nullable String[] solute,       // tags that comprise other values when empty
        @SerializedName("composition")
        @Nullable Double[] composition,  // how much importance is applied to each solute
        @SerializedName("granularity")
        @Nullable Double   granularity,  // boundary count between solvent & solute
        @SerializedName("melt")
        @Nullable Double   melt,         // melting point (kelvin)
        @SerializedName("boil")
        @Nullable Double   boil,         // boiling point (kelvin)
        @SerializedName(value="temperature", alternate= {"temp"})
        @Nullable Double   temperature,  // used to override atmospheric effects (lock s/lwave).

        @SerializedName("density")
        @Nullable Double   density,      // density of tag (kg/m³ or %)
        @SerializedName(value="swave", alternate= {"solid"})
        @Nullable Double   swave,        // shear-wave velocity (for solids)
        @SerializedName(value="lwave", alternate= {"fluid"})
        @Nullable Double   lwave         // longitudinal-wave velocity (for liquids & gasses)
) {
}
