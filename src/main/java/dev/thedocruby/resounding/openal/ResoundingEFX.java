package dev.thedocruby.resounding.openal;

import dev.thedocruby.resounding.Resounding;
import dev.thedocruby.resounding.ResoundingLog;
import dev.thedocruby.resounding.config.PrecomputedConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.EXTEfx;

import java.util.ArrayList;
import java.util.List;

/*
                                        !!!Documentation for OpenAL!!!
                * I am not responsible for anything that happens after you go to these links *
    - ExtEfx(aka Effects Extension) https://github.com/rtpHarry/Sokoban/blob/master/libraries/OpenAL%201.1%20SDK/docs/Effects%20Extension%20Guide.pdf or https://usermanual.wiki/Pdf/Effects20Extension20Guide.90272296/view
    - Core spec(aka OpenAL 1.1 Specification and Reference) https://www.openal.org/documentation/openal-1.1-specification.pdf
    - Core guide(aka OpenAL Programmer's Guide) http://openal.org/documentation/OpenAL_Programmers_Guide.pdf


    Source attributes(2&3): https://www.openal.org/documentation/openal-1.1-specification.pdf#page=34 & http://openal.org/documentation/OpenAL_Programmers_Guide.pdf#page=34
 */

@Environment(EnvType.CLIENT)
public class ResoundingEFX {

    private ResoundingEFX() {}

    private static final List<ReverbSlot> slots = new ArrayList<>();
    private static int directFilter0;

    public static void setupEFX() {
        //Get current context and device
        final long currentContext = ALC10.alcGetCurrentContext();
        final long currentDevice = ALC10.alcGetContextsDevice(currentContext);
        if (ALC10.alcIsExtensionPresent(currentDevice, "ALC_EXT_EFX")) {
            Resounding.LOGGER.info("EFX Extension recognized.");
        } else {
            Resounding.LOGGER.error("EFX Extension not found on current device. Aborting.");
            return;
        }
        // Delete previous filter if it was there
        if (slots.size() > 0 && slots.get(0).initialised){
            EXTEfx.alDeleteFilters(directFilter0);
            for (ReverbSlot slot : slots) { slot.delete(); }
            slots.clear();
        }

        // Create auxiliary effect slots
        // TODO: make this parametric so it can be iterated, allowing for the effect slot count to be variable
        slots.add(0, new ReverbSlot(0.15f , 0.0f, 1.0f, 0.2f, 0.99f, 0.8571429f, 2.5f, 0.001f, 1.26f, 0.011f, 0.994f, 0.16f).initialize());
        slots.add(1, new ReverbSlot(0.55f , 0.0f, 1.0f, 0.3f, 0.99f, 1         , 0.2f, 0.015f, 1.26f, 0.011f, 0.994f, 0.15f).initialize());
        slots.add(2, new ReverbSlot(1.68f , 0.1f, 1.0f, 0.5f, 0.99f, 1         , 0.0f, 0.021f, 1.26f, 0.021f, 0.994f, 0.13f).initialize());
        slots.add(3, new ReverbSlot(4.142f, 0.5f, 1.0f, 0.4f, 0.89f, 1         , 0.0f, 0.025f, 1.26f, 0.021f, 0.994f, 0.11f).initialize());

        // Create filters
        directFilter0 = EXTEfx.alGenFilters();
        EXTEfx.alFilteri(directFilter0, EXTEfx.AL_FILTER_TYPE, EXTEfx.AL_FILTER_LOWPASS);
        Resounding.LOGGER.info("Direct filter object created with ID {}", directFilter0);
    }

    /**
     * Registers the calculated reverb environment with OpenAL.
     *
     * @param sourceID ID of the source of the sound being processed
     * @param sendGain output gain of the reverb audio from the effect slots
     * @param sendCutoff output cutoff of the reverb audio from the effect slots
     * @param directGain output gain of the main audio of sound being processed
     * @param directCutoff output cutoff of the main audio of sound being processed
     * @throws IllegalArgumentException if the number of reverb audio parameters does not match the number of effect slots (sendGain.length, sendCutoff.length != resolution)
     */
    public static void setEnv(
            final int sourceID,
            final double @NotNull [] sendGain, final double @NotNull [] sendCutoff,
            final double directGain, final double directCutoff
    ) {
        if (sendGain.length != PrecomputedConfig.pC.resolution || sendCutoff.length != PrecomputedConfig.pC.resolution) {
            throw new IllegalArgumentException("Error: Reverb parameter count does not match reverb slot count!");
        }

        // Set reverb send filter values and set source to send to all reverb fx slots
        for(int i = 0; i < slots.size(); i++){ slots.get(i).applyFilter(sourceID, (float) sendGain[i], (float) sendCutoff[i]); }

        EXTEfx.alFilterf(directFilter0, EXTEfx.AL_LOWPASS_GAIN, (float) directGain);
        EXTEfx.alFilterf(directFilter0, EXTEfx.AL_LOWPASS_GAINHF, (float) directCutoff);
        AL10.alSourcei(sourceID, EXTEfx.AL_DIRECT_FILTER, directFilter0);
        ResoundingLog.checkErrorLog("Set Environment directFilter0:");

        AL10.alSourcef(sourceID, EXTEfx.AL_AIR_ABSORPTION_FACTOR, MathHelper.clamp(PrecomputedConfig.pC.airAbsorption, 0.0f, 10.0f));
        ResoundingLog.checkErrorLog("Set Environment airAbsorption:");
    }

    /* public static void setSoundPos(final int sourceID, final Vec3d pos) {
        if (pC.off) return;
        //System.out.println(pos);//TO DO
        AL10.alSourcefv(sourceID, 4100, new float[]{(float) pos.x, (float) pos.y, (float) pos.z});
    } */ // TODO: DirEval
}
