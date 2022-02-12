package dev.thedocruby.resounding.openal;

import dev.thedocruby.resounding.Resounding;
import dev.thedocruby.resounding.ResoundingLog;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.AL11;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.EXTEfx;

import static dev.thedocruby.resounding.config.PrecomputedConfig.pC;

/*
                                        !!!Documentation for OpenAL!!!
                * I am not responsible for anything that happens after you go to these links *
    - ExtEfx(aka Effects Extension) https://github.com/rtpHarry/Sokoban/blob/master/libraries/OpenAL%201.1%20SDK/docs/Effects%20Extension%20Guide.pdf or https://usermanual.wiki/Pdf/Effects20Extension20Guide.90272296/view
    - Core spec(aka OpenAL 1.1 Specification and Reference) https://www.openal.org/documentation/openal-1.1-specification.pdf
    - Core guide(aka OpenAL Programmer's Guide) http://openal.org/documentation/OpenAL_Programmers_Guide.pdf


    Source attributes(2&3): https://www.openal.org/documentation/openal-1.1-specification.pdf#page=34 & http://openal.org/documentation/OpenAL_Programmers_Guide.pdf#page=34
 */

@Environment(EnvType.CLIENT)
public class ResoundingEFX { // TODO: Create separate debug toggle for OpenAl EFX instead of using pC.dLog

    private ResoundingEFX() {}

    private static int[] slots = new int[0];
    private static int[] effects = new int[0];
    private static int[] filters = new int[0];
    private static int directFilter;
    public static boolean efxEnabled = false;
    private static boolean initialized = false;
    static long lastDevice;

    public static void setEffect//<editor-fold desc="(Effect_properties)">
    (
            int id,
            float decayTime,
            float density,
            float diffusion,
            float gainHF,
            float decayHFRatio,
            float reflectionsGain,
            float reflectionsDelay,
            float lateReverbGain,
            float lateReverbDelay
    ) //</editor-fold>
    {
        //<editor-fold desc="setReverbParams();">
        EXTEfx.alEffectf(effects[id], EXTEfx.AL_EAXREVERB_DENSITY, density);
        ResoundingLog.checkErrorLog("Error while assigning \"density\" property to Effect object "+effects[id]+"! Attempted to assign value of \""+density+"\".");
        EXTEfx.alEffectf(effects[id], EXTEfx.AL_EAXREVERB_DIFFUSION, diffusion);
        ResoundingLog.checkErrorLog("Error while assigning \"diffusion\" property to Effect object "+effects[id]+"! Attempted to assign value of \""+diffusion+"\".");
        EXTEfx.alEffectf(effects[id], EXTEfx.AL_EAXREVERB_GAIN, pC.globalReverbGain);
        ResoundingLog.checkErrorLog("Error while assigning \"gain\" property to Effect object "+effects[id]+"! Attempted to assign value of \""+ pC.globalReverbGain+"\".");
        EXTEfx.alEffectf(effects[id], EXTEfx.AL_EAXREVERB_GAINHF, gainHF);
        ResoundingLog.checkErrorLog("Error while assigning \"gainHF\" property to Effect object "+effects[id]+"! Attempted to assign value of \""+gainHF+"\".");
        EXTEfx.alEffectf(effects[id], EXTEfx.AL_EAXREVERB_DECAY_TIME, decayTime);
        ResoundingLog.checkErrorLog("Error while assigning \"decayTime\" property to Effect object "+effects[id]+"! Attempted to assign value of \""+decayTime+"\".");
        EXTEfx.alEffectf(effects[id], EXTEfx.AL_EAXREVERB_DECAY_HFRATIO, decayHFRatio);
        ResoundingLog.checkErrorLog("Error while assigning \"decayHFRatio\" property to Effect object "+effects[id]+"! Attempted to assign value of \""+decayHFRatio+"\".");
        EXTEfx.alEffectf(effects[id], EXTEfx.AL_EAXREVERB_REFLECTIONS_GAIN, reflectionsGain);
        ResoundingLog.checkErrorLog("Error while assigning \"reflectionsGain\" property to Effect object "+effects[id]+"! Attempted to assign value of \""+reflectionsGain+"\".");
        EXTEfx.alEffectf(effects[id], EXTEfx.AL_EAXREVERB_REFLECTIONS_DELAY, reflectionsDelay);
        ResoundingLog.checkErrorLog("Error while assigning \"reflectionsDelay\" property to Effect object "+effects[id]+"! Attempted to assign value of \""+reflectionsDelay+"\".");
        EXTEfx.alEffectf(effects[id], EXTEfx.AL_EAXREVERB_LATE_REVERB_GAIN, lateReverbGain);
        ResoundingLog.checkErrorLog("Error while assigning \"lateReverbGain\" property to Effect object "+effects[id]+"! Attempted to assign value of \""+lateReverbGain+"\".");
        EXTEfx.alEffectf(effects[id], EXTEfx.AL_EAXREVERB_LATE_REVERB_DELAY, lateReverbDelay);
        ResoundingLog.checkErrorLog("Error while assigning \"lateReverbDelay\" property to Effect object "+effects[id]+"! Attempted to assign value of \""+lateReverbDelay+"\".");
        EXTEfx.alEffectf(effects[id], EXTEfx.AL_EAXREVERB_AIR_ABSORPTION_GAINHF, 1f);
        ResoundingLog.checkErrorLog("Error while assigning \"density\" property to Effect object "+effects[id]+"! Attempted to assign value of \""+1f+"\".");
        //</editor-fold>

        //Attach updated effect object
        EXTEfx.alAuxiliaryEffectSloti(slots[id], EXTEfx.AL_EFFECTSLOT_EFFECT, effects[id]);
        if (!ResoundingLog.checkErrorLog("Error applying Effect object "+effects[id]+" to aux slot "+slots[id]+"!") && pC.dLog){
            Resounding.LOGGER.info("Successfully initialized Effect object {}!", effects[id]);
        }
    }

    public static void setFilter(int id, int sourceID, float gain, float cutoff) {
        // Set reverb send filter values and set source to send to all reverb fx slots
        EXTEfx.alFilterf(filters[id], EXTEfx.AL_LOWPASS_GAIN, gain);
        ResoundingLog.checkErrorLog("Error while assigning \"gain\" property to Effect object "+filters[id]+"! Attempted to assign value of \""+gain+"\".");
        EXTEfx.alFilterf(filters[id], EXTEfx.AL_LOWPASS_GAINHF, cutoff);
        ResoundingLog.checkErrorLog("Error while assigning \"cutoff\" property to Filter object "+filters[id]+"! Attempted to assign value of \""+cutoff+"\".");
        AL11.alSource3i(sourceID, EXTEfx.AL_AUXILIARY_SEND_FILTER, slots[id], 1, filters[id]);
        ResoundingLog.checkErrorLog("Error applying Filter object "+filters[id]+" and aux slot "+slots[id]+" to source "+sourceID+"!");
    }

    private static int[] initAuxiliaryEffectSlots() {
        if (slots.length < pC.resolution) { return createAuxiliaryEffectSlots(); }
        return slots;
    }

    /*private static int[] deleteAuxiliaryEffectSlots(){       // Remove unused OpenAL Auxiliary Effect slots
        int[] slotsToRemove = ArrayUtils.subarray(slots, pC.resolution, slots.length);
        if (pC.dLog) Resounding.LOGGER.info("Removing {} extra Auxiliary Effect slots...", slotsToRemove.length);
        EXTEfx.alDeleteAuxiliaryEffectSlots(slotsToRemove);
        for (int j : slotsToRemove) {
            if (EXTEfx.alIsAuxiliaryEffectSlot(j)) { Resounding.LOGGER.error("Failed to delete Auxiliary Effect slot {}!", j); }
            else if (pC.dLog) { Resounding.LOGGER.info("Auxiliary Effect slot {} deleted.", j); }
        }
        return ArrayUtils.subarray(slots  , 0, pC.resolution);
    }*/

    private static int[] createAuxiliaryEffectSlots(){       // Create new OpenAL Auxiliary Effect slots
        int[] newSlots = new int[pC.resolution - slots.length];
        if (pC.dLog) Resounding.LOGGER.info("Creating {} new Auxiliary Effect slots...", newSlots.length);
        EXTEfx.alGenAuxiliaryEffectSlots(newSlots);
        for(int i = 0; i < newSlots.length; i++) {
            if(EXTEfx.alIsAuxiliaryEffectSlot(newSlots[i])){
                if (pC.dLog) Resounding.LOGGER.info("Auxiliary Effect slot {} created", newSlots[i]);
                EXTEfx.alAuxiliaryEffectSloti(newSlots[i], EXTEfx.AL_EFFECTSLOT_AUXILIARY_SEND_AUTO, AL10.AL_TRUE);
                ResoundingLog.checkErrorLog("Failed to initialize Auxiliary Effect slot "+newSlots[i]+"!");
            } else { Resounding.LOGGER.error("Failed to create Auxiliary Effect slot! (index {})", i); }
        }
        return ArrayUtils.addAll(slots, newSlots);
    }

    private static int[] initEffectObjects(){
        if (effects.length < pC.resolution) { return createEffectObjects(); }
        return effects;
    }

    /* private static int[] deleteEffectObjects(){       // Remove unused OpenAL Effect objects
        int[] effectsToRemove = ArrayUtils.subarray(effects, pC.resolution, effects.length);
        if (pC.dLog) Resounding.LOGGER.info("Removing {} extra Effect objects...", effectsToRemove.length);
        EXTEfx.alDeleteEffects(effectsToRemove);
        for (int j : effectsToRemove) {
            if (EXTEfx.alIsEffect(j)) { Resounding.LOGGER.error("Failed to delete Effect object {}!", j); }
            else if (pC.dLog) { Resounding.LOGGER.info("Effect object {} deleted.", j); }
        }
        return ArrayUtils.subarray(effects, 0, pC.resolution);
    }*/

    private static int[] createEffectObjects(){       // Create new OpenAL Effect objects
        int[] newEffects = new int[pC.resolution - effects.length];
        if (pC.dLog) Resounding.LOGGER.info("Creating {} new Effect objects...", newEffects.length);
        EXTEfx.alGenEffects(newEffects);
        for(int i = 0; i < newEffects.length; i++) {
            if(EXTEfx.alIsEffect(newEffects[i])){
                if (pC.dLog) Resounding.LOGGER.info("Effect object {} created!", newEffects[i]);
                EXTEfx.alEffecti(newEffects[i], EXTEfx.AL_EFFECT_TYPE, EXTEfx.AL_EFFECT_EAXREVERB);		//Set effect object to be reverb
                ResoundingLog.checkErrorLog("Failed to initialize Effect object "+newEffects[i]+"!");
            } else { Resounding.LOGGER.error("Failed to create Effect object! (index {})", i); }
        }
        return ArrayUtils.addAll(effects, newEffects);
    }

    private static int[] initFilterObjects(){
        if (filters.length < pC.resolution) { return createFilterObjects(); }
        return filters;
    }

    /* private static int[] deleteFilterObjects(){      // Remove unused OpenAL Filter objects
        int[] filtersToRemove = ArrayUtils.subarray(filters, pC.resolution, filters.length);
        if (pC.dLog) Resounding.LOGGER.info("Removing {} extra Filter objects...", filtersToRemove.length);
        EXTEfx.alDeleteFilters(filtersToRemove);
        for (int j : filtersToRemove) {
            if (EXTEfx.alIsFilter(j)) { Resounding.LOGGER.error("Failed to delete Filter object {}!", j); }
            else if (pC.dLog) { Resounding.LOGGER.info("Filter object {} deleted.", j); }
        }
        return ArrayUtils.subarray(filters, 0, pC.resolution);
    } */

    private static int[] createFilterObjects(){       // Create new OpenAL Filter objects
        int[] newFilters = new int[pC.resolution - filters.length];
        if (pC.dLog) Resounding.LOGGER.info("Creating {} new Filter objects...", newFilters.length);
        EXTEfx.alGenFilters(newFilters);
        for(int i = 0; i < newFilters.length; i++) {
            if(EXTEfx.alIsFilter(newFilters[i])){
                if (pC.dLog) Resounding.LOGGER.info("Filter object {} created!", newFilters[i]);
                EXTEfx.alFilteri(newFilters[i], EXTEfx.AL_FILTER_TYPE, EXTEfx.AL_FILTER_LOWPASS);
                ResoundingLog.checkErrorLog("Failed to initialize Filter object "+newFilters[i]+"!");
            } else { Resounding.LOGGER.error("Failed to create Filter object! (index {})", i); }
        }
        return ArrayUtils.addAll(filters, newFilters);
    }

    public static void initEAXReverb(){ // TODO: Figure out how to properly delete aux slots
        if (!efxEnabled || pC.off) return;

        slots   = initAuxiliaryEffectSlots();
        effects = initEffectObjects();
        filters = initFilterObjects();

        for(int i = 0; i < pC.resolution; i++){
            double t = Math.pow((double) i  / pC.resolution, pC.warpFactor);
            double t1 = Math.pow((double)(i + 1) / pC.resolution, pC.warpFactor);
            setEffect(i,
                    (float) Math.max(t1 * 4.142, 0.1),
                    (float) (t1 * 0.5),
                    (float) (0.95 - (pC.reverbCondensationFactor * t1)),
                    (float) (0.95 - (0.75 * t1)),
                    (float) Math.max(0.95 - (0.5 * t1), 0.1),
                    (float) Math.max(Math.pow(1 - t1, 5), 0.1),
                    (float) (t1 * 0.01),
                    (float) (Math.pow(t, 0.2) * 1.618),
                    (float) (t1 * 0.01)
            );
        }

        if (!initialized){
            // Create filters
            directFilter = EXTEfx.alGenFilters();
            if(!EXTEfx.alIsFilter(directFilter)) { Resounding.LOGGER.error("Failed to create direct filter object!"); }
            else if(pC.dLog){ Resounding.LOGGER.info("Direct filter object created with ID {}", directFilter); }
            EXTEfx.alFilteri(directFilter, EXTEfx.AL_FILTER_TYPE, EXTEfx.AL_FILTER_LOWPASS);
            ResoundingLog.checkErrorLog("Failed to initialize direct filter object!");

            initialized = true;
            Resounding.LOGGER.info("Finished initializing OpenAL Auxiliary Effect slots!");
        } else if (pC.dLog) { Resounding.LOGGER.info("Finished re-initializing OpenAL Auxiliary Effect slots!"); }
    }

    public static void setupEXTEfx() {
        //Get current context and device
        final long currentContext = ALC10.alcGetCurrentContext();
        final long currentDevice = ALC10.alcGetContextsDevice(currentContext);
        if(currentDevice != lastDevice){
            Resounding.LOGGER.info("OpenAL device change detected!");
            initialized = false;
            efxEnabled = false;
            slots = new int[0];
            effects = new int[0];
            filters = new int[0];
            directFilter = 0;
            lastDevice = currentDevice;
        }
        if (!ALC10.alcIsExtensionPresent(currentDevice, "ALC_EXT_EFX")) {
            Resounding.LOGGER.error("EFX Extension not found on current device, Aborting.");
        }
        Resounding.LOGGER.info("EFX Extension recognized! ");
        efxEnabled = true;
        initEAXReverb();
    }

    public static void setDirectFilter(int sourceID, float directGain, float directCutoff) {
        EXTEfx.alFilterf(directFilter, EXTEfx.AL_LOWPASS_GAIN, directGain);
        ResoundingLog.checkErrorLog("Error while assigning \"gain\" property to direct filter object! Attempted to assign value of \""+directGain+"\".");
        EXTEfx.alFilterf(directFilter, EXTEfx.AL_LOWPASS_GAINHF, directCutoff);
        ResoundingLog.checkErrorLog("Error while assigning \"cutoff\" property to direct filter object! Attempted to assign value of \""+directCutoff+"\".");
        AL10.alSourcei(sourceID, EXTEfx.AL_DIRECT_FILTER, directFilter);
        ResoundingLog.checkErrorLog("Error applying direct filter object to source "+sourceID+"!");
    }

    /* public static void setSoundPos(final int sourceID, final Vec3d pos) {
        if (pC.off) return;
        //System.out.println(pos);//TO DO
        AL10.alSourcefv(sourceID, 4100, new float[]{(float) pos.x, (float) pos.y, (float) pos.z});
    } */ // TODO: DirEval
}
