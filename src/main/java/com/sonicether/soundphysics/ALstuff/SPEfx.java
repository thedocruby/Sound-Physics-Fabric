package com.sonicether.soundphysics.ALstuff;

import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.EXTEfx;

import java.util.ArrayList;
import java.util.List;

import static com.sonicether.soundphysics.SoundPhysics.mc;

import static com.sonicether.soundphysics.SPLog.*;
import static com.sonicether.soundphysics.config.PrecomputedConfig.pC;

/*
                                        !!!Documentation for OpenAL!!!
                * I am not responsible for anything that happens after you go to these links *
    - ExtEfx(aka Effects Extension) https://github.com/rtpHarry/Sokoban/blob/master/libraries/OpenAL%201.1%20SDK/docs/Effects%20Extension%20Guide.pdf or https://usermanual.wiki/Pdf/Effects20Extension20Guide.90272296/view
    - Core spec(aka OpenAL 1.1 Specification and Reference) https://www.openal.org/documentation/openal-1.1-specification.pdf
    - Core guide(aka OpenAL Programmer's Guide) http://openal.org/documentation/OpenAL_Programmers_Guide.pdf


    Source attributes(2&3): https://www.openal.org/documentation/openal-1.1-specification.pdf#page=34 & http://openal.org/documentation/OpenAL_Programmers_Guide.pdf#page=34
 */

public class SPEfx {

    private static final List<ReverbSlot> slots = new ArrayList<>();
    private static int directFilter0;
    private static final float rainDecayConstant = (float) (Math.log(2.0) / 1200);
    private static float rainAccumulator;
    private static boolean rainHasInitialValue;

    public static void syncReverbParams() {   //Set the global reverb parameters and apply them to the effect and effectslot
        if (slots.size() > 0 && slots.get(0).initialised){ for (ReverbSlot slot : slots) { slot.set(); } }
    }

    public static void setupEFX() {
        //Get current context and device
        final long currentContext = ALC10.alcGetCurrentContext();
        final long currentDevice = ALC10.alcGetContextsDevice(currentContext);
        if (ALC10.alcIsExtensionPresent(currentDevice, "ALC_EXT_EFX")) {
            log("EFX Extension recognized.");
        } else {
            logError("EFX Extension not found on current device. Aborting.");
            return;
        }
        // Delete previous filter if it was there
        if (slots.size() > 0 && slots.get(0).initialised){
            EXTEfx.alDeleteFilters(directFilter0);
            for (ReverbSlot slot : slots) {
                slot.delete();
            }
            slots.clear();
        }

        // Create auxiliary effect slots
        // TODO: make this parametric so it can be iterated, allowing for the effect slot count to be variable
        slots.add(0, new ReverbSlot(0.15f , 0.0f, 1.0f, 2, 0.99f, 0.8571429f, 2.5f, 0.001f, 1.26f, 0.011f, 0.994f, 0.16f).initialize());
        slots.add(1, new ReverbSlot(0.55f , 0.0f, 1.0f, 3, 0.99f, 1         , 0.2f, 0.015f, 1.26f, 0.011f, 0.994f, 0.15f).initialize());
        slots.add(2, new ReverbSlot(1.68f , 0.1f, 1.0f, 5, 0.99f, 1         , 0.0f, 0.021f, 1.26f, 0.021f, 0.994f, 0.13f).initialize());
        slots.add(3, new ReverbSlot(4.142f, 0.5f, 1.0f, 4, 0.89f, 1         , 0.0f, 0.025f, 1.26f, 0.021f, 0.994f, 0.11f).initialize());

        // Create filters
        directFilter0 = EXTEfx.alGenFilters();
        EXTEfx.alFilteri(directFilter0, EXTEfx.AL_FILTER_TYPE, EXTEfx.AL_FILTER_LOWPASS);
        logGeneral("directFilter0: " + directFilter0);
    }

    /**
     * Registers the calculated reverb environment with OpenAL.
     *
     * @param sourceID ID of the source of the sound being processed
     * @param sendGain  output gain of the reverb audio from the effect slots
     * @param sendCutoff output cutoff of the reverb audio from the effect slots
     * @param directGain output gain of the main audio of sound being processed
     * @param directCutoff output cutoff of the main audio of sound being processed
     * @throws IllegalArgumentException if the number of reverb audio parameters does not match the number of effect slots (sendGain.length, sendCutoff.length != slots.size)
     */
    public static void setEnvironment(
            final int sourceID,
            final double @NotNull [] sendGain, final double @NotNull [] sendCutoff,
            final double directGain, final double directCutoff
    ) {
        if (sendGain.length != slots.size() || sendCutoff.length != slots.size()) {
            throw new IllegalArgumentException("Error: Reverb parameter count does not match reverb slot count!");
        }
        if (pC.off) return;
        float absorptionHF = getAbsorptionHF();
        for (ReverbSlot slot : slots){ slot.airAbsorptionGainHF = absorptionHF; }

        syncReverbParams();

        // Set reverb send filter values and set source to send to all reverb fx slots
        for(int i = 0; i < slots.size(); i++){ slots.get(i).applyFilter(sourceID, (float) sendGain[i], (float) sendCutoff[i]); }

        EXTEfx.alFilterf(directFilter0, EXTEfx.AL_LOWPASS_GAIN, (float) directGain);
        EXTEfx.alFilterf(directFilter0, EXTEfx.AL_LOWPASS_GAINHF, (float) directCutoff);
        AL10.alSourcei(sourceID, EXTEfx.AL_DIRECT_FILTER, directFilter0);
        checkErrorLog("Set Environment directFilter0:");

        AL10.alSourcef(sourceID, EXTEfx.AL_AIR_ABSORPTION_FACTOR, MathHelper.clamp(pC.airAbsorption, 0.0f, 10.0f));
        checkErrorLog("Set Environment airAbsorption:");
    }

    // TODO: move rain/absorption code from ALstuff.SPEfx to extras.AdvancedAir
    public static float getAbsorptionHF() {
        if(mc == null || mc.world == null || mc.player == null)
            return 1.0f;
        double rain = getRain();
        double rainS = rainAccumulator;
        double biomeHumidity = mc.world.getBiome(mc.player.getBlockPos()).getDownfall();
        double biomeTemp = mc.world.getBiome(mc.player.getBlockPos()).getTemperature();
        double freq = 10000.0d;

        double relhum = 100.0d * MathHelper.lerp(Math.max(rain, rainS), Math.max(biomeHumidity, 0.2d), 1.0d); // convert biomeHumidity and rain gradients into a dynamic relative humidity value
        double tempK = 25.0d * biomeTemp + 273.15d; // Convert biomeTemp to degrees kelvin

        double hum = relhum*Math.pow(10.0d,4.6151d-6.8346d*Math.pow((273.15d/tempK),1.261d));
        double tempr = tempK/293.15d; // convert tempK to temperature relative to room temp

        double frO = (24+4.04E+4*hum*(0.02d+hum)/(0.391d+hum));
        double frN = Math.pow(tempr,-0.5)*(9+280*hum*Math.exp(-4.17d*(Math.pow(tempr,-1.0f/3.0f)-1)));
        double alpha = 8.686d*freq*freq*(1.84E-11*Math.sqrt(tempr)+Math.pow(tempr,-2.5)*(0.01275d*(Math.exp(-2239.1d/tempK)*1/(frO+freq*freq/frO))+0.1068d*(Math.exp(-3352/tempK)*1/(frN+freq*freq/frN))));

        return (float) Math.pow(10.0d, (alpha * -1.0d * pC.humidityAbsorption)/20.0d); // convert alpha (decibels per meter of attenuation) into airAbsorptionGainHF value and return
    }

    public static void setSoundPos(final int sourceID, final Vec3d pos) {
        if (pC.off) return;
        //System.out.println(pos);//TO DO
        AL10.alSourcefv(sourceID, 4100, new float[]{(float) pos.x, (float) pos.y, (float) pos.z});
    }

    public static float getRain(){
        float tickDelta = 1.0f;
        return (mc==null || mc.world==null) ? 0.0f : mc.world.getRainGradient(tickDelta);
    }

    public static void updateSmoothedRain() {
        if (!rainHasInitialValue) {
            // There is no smoothing on the first value.
            // This is not an optimal approach to choosing the initial value:
            // https://en.wikipedia.org/wiki/Exponential_smoothing#Choosing_the_initial_smoothed_value
            //
            // However, it works well enough for now.
            rainAccumulator = getRain();
            rainHasInitialValue = true;

            return;
        }

        // Implements the basic variant of exponential smoothing
        // https://en.wikipedia.org/wiki/Exponential_smoothing#Basic_(simple)_exponential_smoothing_(Holt_linear)

        // xₜ
        float newValue = getRain();

        // 𝚫t
        float tickDelta = 1.0f;

        // Compute the smoothing factor based on our
        // α = 1 - e^(-𝚫t/τ) = 1 - e^(-k𝚫t)
        float smoothingFactor = (float) (1.0f - Math.exp(-1*rainDecayConstant*tickDelta));

        // sₜ = αxₜ + (1 - α)sₜ₋₁
        rainAccumulator = MathHelper.lerp(smoothingFactor, rainAccumulator, newValue);
    }
}
