package dev.thedocruby.resounding.toolbox;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Objects;

@Environment(EnvType.CLIENT)
public record SoundProfile(
        int sourceID,
        double directGain,
        double directCutoff,
        double[] sendGain,
        double[] sendCutoff
) {
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SoundProfile profile = (SoundProfile) o;
        return               sourceID     == profile.sourceID
            &&               directGain   == profile.directGain
            &&               directCutoff == profile.directCutoff
            && Arrays.equals(sendGain,       profile.sendGain     )
            && Arrays.equals(sendCutoff,     profile.sendCutoff   );
    }

    @Override
    public int hashCode() {
        int hash = Objects.hash(sourceID, directGain, directCutoff);
        hash = 31 * hash + Arrays.hashCode(sendGain);
        hash = 31 * hash + Arrays.hashCode(sendCutoff);
        return hash;
    }

    @Override
    public @NotNull String toString() {
        return "    SoundProfile {\n"   +
                   "        sourceID = "     +                 sourceID       +
                ";\n        directGain = "   +                 directGain     +
                ";\n        directCutoff = " +                 directCutoff   +
                ";\n        sendGain = "     + Arrays.toString(sendGain     ) +
                ";\n        sendCutoff = "   + Arrays.toString(sendCutoff   ) +
                ";\n    }";
    }
}