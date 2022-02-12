package dev.thedocruby.resounding;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

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
    public String toString() {
        return "SoundProfile{"   +
                "sourceID="      +                 sourceID       +
                ";directGain="   +                 directGain     +
                ";directCutoff=" +                 directCutoff   +
                ";sendGain="     + Arrays.toString(sendGain     ) +
                ";sendCutoff="   + Arrays.toString(sendCutoff   ) +
                ";}";
    }
}