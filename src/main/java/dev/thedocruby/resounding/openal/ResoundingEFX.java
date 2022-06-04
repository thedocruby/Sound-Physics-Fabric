package dev.thedocruby.resounding.openal;

import dev.thedocruby.resounding.openal.Effect;

import dev.thedocruby.resounding.ResoundingEngine;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.apache.commons.lang3.ArrayUtils;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.AL11;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.EXTEfx;
import net.minecraft.util.math.MathHelper;

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
public class ResoundingEFX extends Effect { // TODO: Create separate debug toggle for OpenAl EFX instead of using pC.dLog

    private ResoundingEFX() {}

    private static int[][]  slots        = new int[0][];
    private static int[][]  effects      = new int[0][];
    private static int[][]  filters      = new int[0][];
    private static int[]    directFilter = new int[0];
    public static boolean[] initialized  = new boolean[0];
    public static boolean   efxEnabled   = false;

    // Effect_properties {
    public static void setEffect
    (
            final int context,
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
    ) // }
    {
        //<editor-fold desc="setReverbParams();">
        EXTEfx.alEffectf(effects[context][id], EXTEfx.AL_EAXREVERB_DENSITY, density);
        ALUtils.checkErrors("Error while assigning \"density\" property to Effect object "+effects[context][id]+"! Attempted to assign value of \""+density+"\".");

        EXTEfx.alEffectf(effects[context][id], EXTEfx.AL_EAXREVERB_DIFFUSION, diffusion);
        ALUtils.checkErrors("Error while assigning \"diffusion\" property to Effect object "+effects[context][id]+"! Attempted to assign value of \""+diffusion+"\".");

        //EXTEfx.alEffectf(effects[context][id], EXTEfx.AL_EAXREVERB_GAIN, 1.0f);
        //ALUtils.checkErrors("Error while assigning \"gain\" property to Effect object "+effects[context][id]+"! Attempted to assign value of \""+ 1.0f +"\".");

        EXTEfx.alEffectf(effects[context][id], EXTEfx.AL_EAXREVERB_GAINHF, gainHF);
        ALUtils.checkErrors("Error while assigning \"gainHF\" property to Effect object "+effects[context][id]+"! Attempted to assign value of \""+gainHF+"\".");

        EXTEfx.alEffectf(effects[context][id], EXTEfx.AL_EAXREVERB_DECAY_TIME, decayTime);
        ALUtils.checkErrors("Error while assigning \"decayTime\" property to Effect object "+effects[context][id]+"! Attempted to assign value of \""+decayTime+"\".");

        EXTEfx.alEffectf(effects[context][id], EXTEfx.AL_EAXREVERB_DECAY_HFRATIO, decayHFRatio);
        ALUtils.checkErrors("Error while assigning \"decayHFRatio\" property to Effect object "+effects[context][id]+"! Attempted to assign value of \""+decayHFRatio+"\".");

        EXTEfx.alEffectf(effects[context][id], EXTEfx.AL_EAXREVERB_REFLECTIONS_GAIN, reflectionsGain);
        ALUtils.checkErrors("Error while assigning \"reflectionsGain\" property to Effect object "+effects[context][id]+"! Attempted to assign value of \""+reflectionsGain+"\".");

        EXTEfx.alEffectf(effects[context][id], EXTEfx.AL_EAXREVERB_REFLECTIONS_DELAY, reflectionsDelay);
        ALUtils.checkErrors("Error while assigning \"reflectionsDelay\" property to Effect object "+effects[context][id]+"! Attempted to assign value of \""+reflectionsDelay+"\".");

        EXTEfx.alEffectf(effects[context][id], EXTEfx.AL_EAXREVERB_LATE_REVERB_GAIN, lateReverbGain);
        ALUtils.checkErrors("Error while assigning \"lateReverbGain\" property to Effect object "+effects[context][id]+"! Attempted to assign value of \""+lateReverbGain+"\".");

        EXTEfx.alEffectf(effects[context][id], EXTEfx.AL_EAXREVERB_LATE_REVERB_DELAY, lateReverbDelay);
        ALUtils.checkErrors("Error while assigning \"lateReverbDelay\" property to Effect object "+effects[context][id]+"! Attempted to assign value of \""+lateReverbDelay+"\".");

        EXTEfx.alEffectf(effects[context][id], EXTEfx.AL_EAXREVERB_AIR_ABSORPTION_GAINHF, 1f);
        ALUtils.checkErrors("Error while assigning \"density\" property to Effect object "+effects[context][id]+"! Attempted to assign value of \""+1f+"\".");
        //</editor-fold>

        //Attach updated effect object
        EXTEfx.alAuxiliaryEffectSloti(slots[context][id], EXTEfx.AL_EFFECTSLOT_EFFECT, effects[context][id]);
        if (!ALUtils.checkErrors("Error applying Effect object "+effects[context][id]+" to aux slot "+slots[context][id]+"!") && pC.dLog){
            ResoundingEngine.LOGGER.info("Successfully initialized Effect object {}!", effects[context][id]);
        }
    }

    public static void setFilter(final int context, int id, int sourceID, float gain, float cutoff) {  // Set reverb send filter values and set source to send to all reverb fx slots
        if (!(efxEnabled && initialized[context])) throw new IllegalStateException("EFX is not enabled/initialized! Cannot complete request.");
        EXTEfx.alFilterf(filters[context][id], EXTEfx.AL_LOWPASS_GAIN, gain);
        ALUtils.checkErrors("Error while assigning \"gain\" property to Effect object "+filters[context][id]+"! Attempted to assign value of \""+gain+"\".");
//ResoundingEngine.LOGGER.info("directFilter: {}", directFilter);

        EXTEfx.alFilterf(filters[context][id], EXTEfx.AL_LOWPASS_GAINHF, cutoff);
        ALUtils.checkErrors("Error while assigning \"cutoff\" property to Filter object "+filters[context][id]+"! Attempted to assign value of \""+cutoff+"\".");

        AL11.alSource3i(sourceID, EXTEfx.AL_AUXILIARY_SEND_FILTER, slots[context][id], 1, filters[context][id]); // TODO: figure out how to properly use `AL11.alSource3i(` so i don't have to predetermine reverb.
        ALUtils.checkErrors("Error applying Filter object "+filters[context][id]+" and aux slot "+slots[context][id]+" to source "+sourceID+"!");
    }

    private static void createAuxiliaryEffectSlots(final int context){       // Create new OpenAL Auxiliary Effect slots
		slots = increaseLengthMatrix(slots, context+1);
        slots[context] = new int[pC.resolution];
        if (pC.dLog) ResoundingEngine.LOGGER.info("Creating {} new Auxiliary Effect slots...", pC.resolution);
        EXTEfx.alGenAuxiliaryEffectSlots(slots[context]);
        for(int i = 0; i < pC.resolution; i++) {
            if(EXTEfx.alIsAuxiliaryEffectSlot(slots[context][i])){
                EXTEfx.alAuxiliaryEffectSloti(slots[context][i], EXTEfx.AL_EFFECTSLOT_AUXILIARY_SEND_AUTO, AL10.AL_TRUE);
                if (!ALUtils.checkErrors("Failed to initialize Auxiliary Effect slot "+slots[context][i]+"!")) {
                    if (pC.dLog) ResoundingEngine.LOGGER.info("Auxiliary Effect slot {} created!", slots[context][i]); continue;
                } initialized[context] = false; continue;
            } ResoundingEngine.LOGGER.error("Failed to create Auxiliary Effect slot! (index {})", i); initialized[context] = false;
        }
    }

    private static void deleteAuxiliaryEffectSlots(final int context){       // Remove OpenAL Auxiliary Effect slots
        if (pC.dLog) ResoundingEngine.LOGGER.info("Removing {} Auxiliary Effect slots...", slots[context].length);
        EXTEfx.alDeleteAuxiliaryEffectSlots(slots[context].clone());
        for (int j : slots[context]) {
            if (EXTEfx.alIsAuxiliaryEffectSlot(j)) { ResoundingEngine.LOGGER.error("Failed to delete Auxiliary Effect slot {}!", j); continue;}
            slots[context] = ArrayUtils.removeElement(slots[context], j);
            if (pC.dLog) { ResoundingEngine.LOGGER.info("Auxiliary Effect slot {} deleted.", j); }
        }
    }

    private static void createEffectObjects(final int context){       // Create new OpenAL Effect objects
		effects = increaseLengthMatrix(effects, context+1);
        effects[context] = new int[pC.resolution];
        if (pC.dLog) ResoundingEngine.LOGGER.info("Creating {} new Effect objects...", pC.resolution);
        EXTEfx.alGenEffects(effects[context]);
        for(int i = 0; i < pC.resolution; i++) {
            if(EXTEfx.alIsEffect(effects[context][i])){
                EXTEfx.alEffecti(effects[context][i], EXTEfx.AL_EFFECT_TYPE, EXTEfx.AL_EFFECT_EAXREVERB); // Set effect type to EAX Reverb
                if (!ALUtils.checkErrors("Failed to initialize Effect object "+effects[context][i]+"!")) {
                    if (pC.dLog) ResoundingEngine.LOGGER.info("Effect object {} created!", effects[context][i]); continue;
                } initialized[context] = false; continue;
            } ResoundingEngine.LOGGER.error("Failed to create Effect object! (index {})", i); initialized[context] = false;
        }
    }

    private static void deleteEffectObjects(final int context){       // Remove OpenAL Effect objects
        if (pC.dLog) ResoundingEngine.LOGGER.info("Removing {} Effect objects...", effects[context].length);
        EXTEfx.alDeleteEffects(effects[context].clone());
        for (int j : effects[context]) {
            if (EXTEfx.alIsEffect(j)) { ResoundingEngine.LOGGER.error("Failed to delete Effect object {}!", j); continue;}
            effects[context] = ArrayUtils.removeElement(effects[context], j);
            if (pC.dLog) { ResoundingEngine.LOGGER.info("Effect object {} deleted.", j); }
        }
    }

    private static void createFilterObjects(final int context){       // Create new OpenAL Filter objects
		filters = increaseLengthMatrix(filters, context+1);
        filters[context] = new int[pC.resolution];
        if (pC.dLog) ResoundingEngine.LOGGER.info("Creating {} new Filter objects...", pC.resolution);
        EXTEfx.alGenFilters(filters[context]);
        for(int i = 0; i < pC.resolution; i++) {
            if(EXTEfx.alIsFilter(filters[context][i])){
                EXTEfx.alFilteri(filters[context][i], EXTEfx.AL_FILTER_TYPE, EXTEfx.AL_FILTER_LOWPASS);
                if (!ALUtils.checkErrors("Failed to initialize Filter object "+filters[context][i]+"!")) {
                    if (pC.dLog) ResoundingEngine.LOGGER.info("Filter object {} created!", filters[context][i]); continue;
                } initialized[context] = false; continue;
            } ResoundingEngine.LOGGER.error("Failed to create Filter object! (index {})", i); initialized[context] = false;
        }
    }

    private static void deleteFilterObjects(final int context){      // Remove OpenAL Filter objects
        if (pC.dLog) ResoundingEngine.LOGGER.info("Removing {} Filter objects...", filters[context].length);
        EXTEfx.alDeleteFilters(filters[context].clone());
        for (int j : filters[context]) {
            if (EXTEfx.alIsFilter(j)) { ResoundingEngine.LOGGER.error("Failed to delete Filter object {}!", j); continue;}
            filters[context] = ArrayUtils.removeElement(filters[context], j);
            if (pC.dLog) { ResoundingEngine.LOGGER.info("Filter object {} deleted.", j); }
        }
    }


// java's type system sucks... overloading, ugh - even python could do better...
// an overloaded array length extender function boolean[], int[], int[][] {

// utility function for increasing an int[][]'s size whilst keeping content
private static int[][] increaseLengthMatrix (final int[][] old, final int min) {
	return ArrayUtils.addAll(old, new int[Math.max(1,old.length - min)][]);
}

// utility function for increasing an int[]'s size whilst keeping content
private static int[] increaseLengthArray (final int[] old, final int min) {
	return ArrayUtils.addAll(old, new int[Math.max(1,old.length - min)]);
}

// utility function for increasing a  boolean[]'s size whilst keeping content
private static boolean[] increaseLengthArray (final boolean[] old, final int min) {
	return ArrayUtils.addAll(old, new boolean[Math.max(1,old.length - min)]);
}

// }

private static void createContext(final int minContext) {
	// if enough contexts
	if (directFilter.length > minContext) return;
	directFilter = increaseLengthArray(directFilter, minContext+1);
	final int context = directFilter.length-1;
	createAuxiliaryEffectSlots(context);
	createEffectObjects       (context);
	createFilterObjects       (context);
	initialized  = increaseLengthArray(initialized, context);
	ResoundingEngine.LOGGER.info("initializing context: {}", context);
}

    private static void initEAXReverb(final int context){
		if (!efxEnabled) return;
        if (slots.length > context && pC.resolution == slots[context].length) return;
        createContext(context);
        if (initialized[context]) return;

        initialized[context] = true;

        for(int i = 1; i <= pC.resolution; i++){
            double t = (double) i / pC.resolution;
            setEffect(context, i - 1,
                    (float) Math.max(t * pC.maxDecayTime, 0.1),
                    (float) (t * 0.5 + 0.5),
                    (float) MathHelper.lerp(pC.rvrbDiff, 1-t, 1),
                    (float) (0.95 - (0.75 * t)),
                    (float) Math.max(0.95 - (0.3 * t), 0.1),
                    (float) Math.max(Math.pow(1 - t, 0.5) + 0.618, 0.1),
                    (float) (t * 0.01),
                    (float) (Math.pow(t, 0.5) + 0.618),
                    (float) (t * 0.01)
            );
        }
        directFilter[context] = EXTEfx.alGenFilters();
        if(!EXTEfx.alIsFilter(directFilter[context])) {
            ResoundingEngine.LOGGER.error("Failed to create direct filter object!");
            initialized[context] = false;
        }
        else if(pC.dLog){ ResoundingEngine.LOGGER.info("Direct filter object created with ID {}", directFilter[context]); }
        EXTEfx.alFilteri(directFilter[context], EXTEfx.AL_FILTER_TYPE, EXTEfx.AL_FILTER_LOWPASS);
        initialized[context] &= !ALUtils.checkErrors("Failed to initialize direct filter object!");
        if (initialized[context]) { ResoundingEngine.LOGGER.info("Finished initializing OpenAL Auxiliary Effect slots!"); return; }
        ResoundingEngine.LOGGER.info("Failed to properly initialize OpenAL Auxiliary Effect slots. Aborting");
        efxEnabled = false;
    }

    public static boolean setUpEXTEfx(final int context) {
        final long currentContext = ALC10.alcGetCurrentContext();
        final long currentDevice = ALC10.alcGetContextsDevice(currentContext);
        if (!ALC10.alcIsExtensionPresent(currentDevice, "ALC_EXT_EFX")) {
            ResoundingEngine.LOGGER.error("EFX Extension not found on current device, Aborting."); return false;
        }
        efxEnabled = true;
        ResoundingEngine.LOGGER.info("EFX Extension recognized! Initializing Auxiliary Effect slots...");
        initEAXReverb(context);
        return efxEnabled && initialized[context];
    }

    public static void cleanUpEXTEfx(final int context) {
        deleteAuxiliaryEffectSlots(context);
        deleteEffectObjects       (context);
        deleteFilterObjects       (context);
        EXTEfx.alDeleteFilters(directFilter[context]);
        if(EXTEfx.alIsFilter(directFilter[context])) { ResoundingEngine.LOGGER.error("Failed to delete direct filter object!"); }
        else if(pC.dLog){ ResoundingEngine.LOGGER.info("Direct filter object deleted with ID {}", directFilter[context]); }

        initialized[context] = false;
        efxEnabled = false;
    }

    public static void setDirectFilter(final int context, final int sourceID, float directGain, float directCutoff) {
        directGain = MathHelper.clamp(directGain, 0, 1);
        directCutoff = MathHelper.clamp(directCutoff, 0, 1);
        if (!(efxEnabled && initialized[context])) throw new IllegalStateException("EFX is not enabled/initialized! Cannot complete request.");
//      final int filter = directFilter[context];
        EXTEfx.alFilterf(directFilter[context], EXTEfx.AL_LOWPASS_GAIN, directGain);
        ALUtils.checkErrors("Error while assigning \"gain\" property to direct filter object! Attempted to assign value of \""+directGain+"\".");

        EXTEfx.alFilterf(directFilter[context], EXTEfx.AL_LOWPASS_GAINHF, directCutoff);
        ALUtils.checkErrors("Error while assigning \"cutoff\" property to direct filter object! Attempted to assign value of \""+directCutoff+"\".");

        AL10.alSourcei(sourceID, EXTEfx.AL_DIRECT_FILTER, directFilter[context]);
        ALUtils.checkErrors("Error applying direct filter object to source "+sourceID+"!");
    }

    /* public static void setSoundPos(final int sourceID, final Vec3d pos) {
        if (!(efxEnabled && initialized)) throw new IllegalStateException("EFX is not enabled/initialized! Cannot complete request.");
        if (pC.off) return;
        //System.out.println(pos);//TO DO
        AL10.alSourcefv(sourceID, 4100, new float[]{(float) pos.x, (float) pos.y, (float) pos.z});
    } */ // TODO: DirEval
}
