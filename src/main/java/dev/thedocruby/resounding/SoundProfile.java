package dev.thedocruby.resounding;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.Arrays;
import java.util.Objects;

@Environment(EnvType.CLIENT)
public record SoundProfile(
        double directGain,
        double directCutoff,
        double[] sendGain,
        double[] sendCutoff
) {
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SoundProfile result = (SoundProfile) o;
        return               directGain   == result.directGain
            &&               directCutoff == result.directCutoff
            && Arrays.equals(sendGain,       result.sendGain     )
            && Arrays.equals(sendCutoff,     result.sendCutoff   );
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(directGain, directCutoff);
        result = 31 * result + Arrays.hashCode(sendGain);
        result = 31 * result + Arrays.hashCode(sendCutoff);
        return result;
    }

    @Override
    public String toString() {
        return "SoundProfile{"   +
                "directGain="    +                 directGain     +
                ";directCutoff=" +                 directCutoff   +
                ";sendGain="     + Arrays.toString(sendGain     ) +
                ";sendCutoff="   + Arrays.toString(sendCutoff   ) +
                ";}";
    }
}