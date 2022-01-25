package com.thedocruby.soundphysics.ALstuff;

import org.lwjgl.openal.AL10;
import org.lwjgl.openal.AL11;
import org.lwjgl.openal.EXTEfx;

import static com.thedocruby.soundphysics.SPLog.*;
import static com.thedocruby.soundphysics.config.PrecomputedConfig.pC;
import static com.thedocruby.soundphysics.config.PrecomputedConfig.defaultAttenuationFactor;

@SuppressWarnings("DanglingJavadoc")//!!!!!!!!!!!! Ctrl + Shift + '-' !!!!!!!!!!!!
public class ReverbSlot {
    public boolean initialised = false;
    public int auxSlotId;
    public int effectId;
    public int filterId;

    //<editor-fold desc="Effect properties">
    // from: Effects Guide (p. 66+) and other sources; altered
    /**
     * Reverb Modal Density controls the coloration of the late reverb.
     * Lowering the value adds more coloration to the late reverb.
     *
     * Modal density is defined as the expected number of modes per unit frequency, which is given by: n(ω)=∂N(ω)∂ω=∂∂ω(kL/π)=Lπcg
     *
     * In music, normal modes of vibrating instruments (strings, air pipes, drums, etc.) are called "harmonics" or "overtones".
     *
     * ∴ High-frequency "depth"?
     *<p>
     * Strutt|Building Acoustics|Room Modal Density allows the user to calculate the number of room modes present in a room up to a given frequency cut-off (number of modes) as well as the modal density (the number of modes per frequency band) using the Bolt and Morse formula.
     *
     * The number of modes, N from 0 Hz up to f Hz is given by:
     *
     * N=4πf³V/3c³+πf²S/4c²+fP/8c
     *
     * where:
     * f is the frequency (Hz)
     * V is the room volume (m³)
     * S is the room surface area (m²)
     * P is the total room perimeter (m)
     *
     * The above terms describe the number of oblique (∝f³), lateral (∝f²) and normal (∝f) modes in the room. Especially for high frequencies, the number of oblique modes is generally dominant.
     *
     * The modal density is given by:
     *
     * ⇒⇒⇒ dN/df=4πf²/Vc³+πfS/2c²+P/8c
     *
     * Although derived for rectangular rooms, the above expressions give good estimates of the modal behaviour for rooms of arbitrary shape, provided that the aspect ratio of the room is not too extreme.
     * </p>
     */
    public final float density;				// min: 0.0f 	max: 1.0f   (?)                         default: 1.0f
    /**
     * The Reverb Diffusion property controls the echo density in the reverberation decay.
     * It is set by default to 1.0, which provides the highest density.
     * Reducing diffusion gives the reverberation a more “grainy” character
     * that is especially noticeable with percussive (a rhythmic patterning of noise, ex. clapping, stomping) sound sources.
     * If you set a diffusion value of 0.0, the later reverberation sounds like a succession of distinct echoes.
     * ∴ 1.0 ⇒ uniform noise
     *
     * #See other fields for more#
     *
     * A commonly overlooked parameter on reverb plugins is diffusion.
     * This helps to control the initial build up of echo density and effects how you hear the reverb reflections.
     * As a guide, for clearer more natural sounding mixes, vocals and instruments use low parameter settings.
     * If you’re looking to enhance percussion and drum hits use medium to high settings.
     */
    public final float diffusion;			// min: 0.0f 	max: 1.0f   (a linear multiplier value) default: 1.0f
    /**
     * The Reverb Gain property is the master volume control for the reflected sound
     * (both  early  reflections  and  reverberation), that the reverb effect adds to all sound sources.
     * It sets the maximum amount of reflections and reverberation added to the final sound mix.
     * The value of the Reverb Gain property ranges from 1.0 (0db) (the maximum amount)
     * to 0.0 (-100db) (no reflected sound at all).
     */
    public final float gain;				// min: 0.0f 	max: 1.0f   (Linear gain)               default: 0.32f
    /**
     * The Reverb Gain HF property further tweaks reflected sound by attenuating (gradually fading) it at high frequencies.
     * It controls a low-pass filter that applies globally to the reflected sound of all sound sources feeding the particular instance of the reverb effect.
     * The value of the Reverb Gain HF property ranges from 1.0 (0db) (no filter) to 0.0 (-100db) (virtually no reflected sound).
     * #HF Reference# sets the frequency at which the value of this property is measured.
     */
    public final float gainHF;			    // min: 0.0f 	max: 1.0f   (Linear gain)               default: 0.89f
    /**
     * The Reverb Gain LF property further tweaks reflected sound by attenuating it at low frequencies.
     * It controls a high-pass filter that applies globally to the reflected sound of all sound sources feeding the particular instance  of the reverb effect.
     * The value of the Reverb Gain LF property ranges from 1.0 (0db) (no filter) to 0.0 (-100db) (virtually no reflected sound).
     * #LF Reference# sets the frequency at which the value of this property is measured.
     */
    //public final float gainLF;            // min: 0.0f    max: 1.0f   (Linear gain)               default: 0.0f
    /**
     *  The Decay Time property sets the reverberation decay time.
     *  It ranges from 0.1 (typically a small room with very dead surfaces)
     *  to 20.0 (typically a large room with very live surfaces).
     */
    public final float decayTime;			// min: 0.1f 	max: 20.0f  (Seconds)                   default: 1.49f
    /**
     * The Decay HF Ratio property adjusts the spectral quality of the Decay Time parameter.
     * It is the ratio of high-frequency decay time relative to the time set by Decay Time.
     * The Decay HF Ratio value  1.0  is  neutral:  the  decay  time  is  equal  for  all  frequencies.
     *
     * As  Decay  HF  Ratio  increases  above  1.0,  the  high-frequency  decay  time  increases,
     * so  it’s  longer  than  the  decay  time  at  mid-frequencies.
     * You hear a more brilliant reverberation with a longer decay at high frequencies.
     *
     * As the Decay HF Ratio value decreases below 1.0, the high-frequency decay time decreases,
     * so it’s shorter than the decay time of the mid-frequencies.
     * You hear a more natural reverberation.
     */
    public final float decayHFRatio;		// min: 0.1f 	max: 20.0f  (Linear multiplier)         default: 0.83f
    /**
     * The Decay LF Ratio property adjusts the spectral quality of the Decay Time parameter.
     * It is the ratio  of  low-frequency  decay  time  relative  to  the  time  set  by  Decay  Time.
     * The  Decay  LF  Ratio  value  1.0  is  neutral:  the  decay  time  is  equal  for  all  frequencies.
     *
     * As  Decay  LF  Ratio  increases  above  1.0,  the  low-frequency  decay  time  increases,
     * so  it’s  longer  than  the  decay  time  at  mid-frequencies.
     * You hear a more booming reverberation with a longer decay at low frequencies.
     *
     * As the Decay LF Ratio value decreases below 1.0, the low-frequency decay time decreases,
     * so it’s shorter than the decay time of the mid-frequencies.
     * You hear a more tinny reverberation.
     */
    //public final float decayLFRatio;		// min: 0.1f 	max: 20.0f  (Linear multiplier)         default: 1.0f
    /**
     * The Reflections Gain property controls the overall amount of initial reflections relative to the Gain property.
     * (The Gain property sets the overall amount of reflected sound: both initial reflections and later reverberation.)
     * The value of Reflections Gain ranges from a maximum of 3.16 (+10 dB)
     * to a minimum of 0.0 (-100  dB) (no  initial  reflections  at  all), and is corrected by the value of the Gain property.
     * The Reflections Gain property does not affect the subsequent reverberation decay.
     * You can increase the amount of initial reflections to simulate a more narrow space or closer walls,
     * especially effective if you associate the initial reflections increase with a reduction in reflections delays
     * by lowering the value of the Reflection Delay property.
     * To simulate open or semi-open environments, you can maintain the amount of early reflections while reducing the value of
     * the Late Reverb Gain property, which controls later reflections.
     */
    public final float reflectionsGain;		// min: 0.1f 	max: 3.16f  (Linear gain)               default: 0.05
    /**
     * The Reflections Delay property is the amount of delay between the arrival time of the direct path from the source
     * to the first reflection from the source.  It ranges from 0 to 300 milliseconds.
     * You can reduce or increase "Reflections Delay" to simulate closer or more distant reflective surfaces,
     * and therefore control the perceived size of the room.
     */
    public final float reflectionsDelay;	// min: 0.0f 	max: 0.3f   (Seconds)                   default: 0.007
    /**
     * The Reflections Pan property is a 3D vector that controls the spatial distribution of the cluster of early reflections.
     * The direction of this vector controls the global direction of the reflections,
     * while its magnitude controls how focused the reflections are towards this direction.
     * It is important to note that the direction of the vector is interpreted in the coordinate system of the user,
     * without taking into account the orientation of the virtual listener.
     * For instance, assuming a four-point loudspeaker playback system,
     * setting Reflections Pan to (0.,  0.,  0.7) means that the reflections are panned to the front speaker pair,
     * whereas setting of (0.,  0.,  −0.7) pans the reflections towards the rear speakers.
     * These vectors follow a left-handed coordinate system, unlike OpenAL uses a right-handed coordinate system.
     * If the magnitude of Reflections Pan is zero (the default setting), the early reflections come evenly from  all  directions.
     * As the magnitude increases, the reflections become more focused in the direction pointed to by the vector.
     * A magnitude of 1.0 would represent the extreme case, where all reflections come from a single direction.
     */
    //public float[] reflectionsPan;//magnitude min: 0.0f   max: 1.0f   (Vector)                    default: [0f, 0f, 0f]
    /**
     * The Late Reverb Gain property controls the overall amount of later reverberation relative to the Gain property.
     * (The Gain property sets the overall amount of both initial reflections and later reverberation.)
     * The value of Late Reverb Gain ranges from a maximum of 10.0 (+20 dB) to a minimum of 0.0 (-100 dB) (no late reverberation at all).
     * Note that Late Reverb Gain and Decay Time are independent properties:
     * If you adjust Decay Time without changing Late Reverb Gain, the total intensity
     * (the averaged square of the amplitude) of the late reverberation remains constant.
     */
    public final float lateReverbGain;		// min: 0.0f 	max: 10.0f  (Linear gain)               default: 1.26f
    /**
     * The Late Reverb Delay property defines the beginning time of the late reverberation
     * relative to the time of the initial reflection (the first of the early reflections). It ranges from 0 to 100 milliseconds.
     * Reducing or increasing Late Reverb Delay is useful for simulating a smaller or larger room.
     */
    public final float lateReverbDelay;		// min: 0.0f 	max: 0.1f   (Seconds)                   default: 0.011f
    /**
     * The Late Reverb Pan property is a 3D vector that controls the spatial distribution of the late reverb.
     * The direction of this vector controls the global direction of the reverb,
     * while its magnitude controls how focused the reverb are towards this direction.
     * The details under Reflections Pan (above) also apply to Late Reverb Pan.
     */
    //public float[] lateReverbPan;//magnitude min: 0.0f    max: 1.0f   (Vector)                    default: [0f, 0f, 0f]
    /**
     * Echo Depth introduces a cyclic echo in the reverberation decay, which will be noticeable with transient or percussive sounds.
     * A larger value of Echo Depth will make this effect more prominent.
     *
     * Echo Time controls the rate at which the cyclic echo repeats itself along the reverberation decay.
     * For example, the default setting for Echo Time is 250 ms., causing the echo to occur 4 times per second.
     * Therefore, if you were to clap your hands in this type of environment, you will hear four repetitions of clap per second.
     * Together with Reverb Diffusion, Echo Depth will control how long the echo effect will persist along the reverberation decay.
     *
     * In a more diffuse environment, echoes will wash out more quickly after the direct sound.
     *
     * In an environment that is less diffuse, you will be able to hear a larger number of repetitions of the echo,
     * which will wash out later in the reverberation decay.
     *
     * If Diffusion is set to 0.0 and Echo Depth is set to 1.0, the echo will persist distinctly until the end of the reverberation decay.
     */
    //public float echoTime;                // min: 0.075f  max: 0.25f  (Seconds)                   default: 0.25f
    //public float echoDepth;               // min: 0.0f    max: 1.0f   (Linear multiplier)         default: 0.0f
    /**
     * Using these two properties, you can create a pitch modulation in the reverberant sound.
     * This will be most noticeable applied to sources that have tonal color or pitch.
     * You can use this to make some trippy effects!
     * Modulation Time controls the speed of the vibrato (rate of periodic changes in pitch).
     * Modulation Depth controls the amount of pitch change.
     * Low values of Diffusion will contribute to reinforcing the perceived effect by
     * reducing the mixing of overlapping reflections in the reverberation decay.
     */
    //public final float modulationTime;    // min: 0.004f  max: 4.0f   (Seconds)                   default: 0.25f
    //public final float modulationDepth;   // min: 0.0f    max: 1.0f   (Linear multiplier)         default: 0.0f
    /**
     * The properties "HF Reference" and "LF Reference" determine respectively the frequencies at which
     * the high-frequency effects and the low-frequency effects created by EAX Reverb properties are measured,
     * for example #Decay HF Ratio# and #Decay LF Ratio#.
     * Note that it is necessary to maintain a factor of at least 10 between these two reference frequencies so that
     * low frequency and high frequency properties can be accurately controlled and will produce independent effects.
     * In other words, the LF Reference value should be less than 1/10 of the HF Reference value.
     */
    //public final float HFReference;       // min: 1000f   max: 20000f (Hertz)                     default: 5000f
    //public final float LFReference;       // min: 20.0f   max: 1000f  (Hertz)                     default: 250f
    /**
     * The Room Rolloff Factor property is one of the two methods available to attenuate the reflected sound
     * (containing both reflections and reverberation) according to source-listener distance.
     * It is defined the same way as OpenAL’s Rolloff Factor, but operates on reverb sound instead of direct-path sound.
     * Setting the Room Rolloff Factor value to 1.0 specifies that the reflected sound will decay by 6 dB every time the distance doubles.
     * Any value other than 1.0 is equivalent to a scaling factor applied to the quantity specified by
     * ((Source  listener  distance) - (Reference Distance)).
     * Reference Distance is an OpenAL source parameter that specifies the inner border for distance rolloff effects:
     * if the source comes closer to the listener than the reference distance,
     * the direct-path sound isn’t increased as the source comes closer to the listener,
     * and neither is the reflected sound. The default value of Room Rolloff Factor is 0.0 because, by default,
     * the Effects Extension reverb effect naturally manages the reflected sound level automatically for each
     * sound source to simulate the natural rolloff of reflected sound vs. distance in typical rooms.
     * (Note that this isn’t the case if the source property flag AL_AUXILIARY_SEND_FILTER_GAIN_AUTO is set to AL_FALSE)
     * You can use Room Rolloff Factor as an option to automatic control, so you can exaggerate or replace the default automatically-controlled rolloff.
     */
    public final float roomRolloffFactor; 	// min: 0.0f	max: 10.0f  (Linear multiplier)         default: 0.0f
    /**
     * The Air Absorption Gain HF property controls the distance-dependent attenuation at high frequencies caused by the propagation medium.
     * It applies to reflected sound only.
     * You can use Air Absorption Gain HF to simulate sound transmission through foggy air, dry air, smoky atmosphere, and so on.
     * The default value is 0.994 (-0.05  dB) per meter, which roughly corresponds to typical condition of atmospheric humidity, temperature, and so on.
     * Lowering the value simulates a more absorbent medium (more humidity in the air, for example);
     * raising the value simulates a less absorbent medium (dry desert air, for example).
     */
    public float airAbsorptionGainHF;	// min: 0.892f 	max: 1.0f   (Linear gain per meter)     default: 0.994f
    /**
     * When this flag is set, the high-frequency decay time automatically stays below a limit value that is
     * derived from the setting of the property Air Absorption Gain HF.
     * This limit applies regardless of the setting of the property Decay HF Ratio, and the limit doesn't affect the value of Decay HF Ratio.
     * This limit, when on, maintains a natural sounding reverberation decay by allowing you to increase the
     * value of Decay Time without the risk of getting an unnaturally long decay time at high frequencies.
     * If this flag is set to AL_FALSE, high-frequency decay time isn’t automatically limited.
     */
    //public final int decayHFLimit;        // min:AL_FALSE max:AL_TRUE (Boolean)                   default: true
    //</editor-fold>

    public ReverbSlot(/*<editor-fold desc="Effect_properties">*/float decayTime, float density, float diffusion, float gain, float gainHF, float decayHFRatio, float reflectionsGain, float reflectionsDelay, float lateReverbGain, float lateReverbDelay, float airAbsorptionGainHF, float roomRolloffFactor/*</editor-fold>*/) {
        this.decayTime = decayTime;
        this.density = density;
        this.diffusion = diffusion;
        this.gain = gain;
        this.gainHF = gainHF;
        this.decayHFRatio = decayHFRatio;
        this.reflectionsGain = reflectionsGain;
        this.reflectionsDelay = reflectionsDelay;
        this.lateReverbGain = lateReverbGain;
        this.lateReverbDelay = lateReverbDelay;
        this.airAbsorptionGainHF = airAbsorptionGainHF;
        this.roomRolloffFactor = roomRolloffFactor;
    }

    public ReverbSlot initialize() {
        if (initialised) delete();

        //<editor-fold desc="auxSlotId = new EXTEfx.alAuxiliaryEffectSlot; effectId = new EXTEfx.alEffect; filterId = new EXTEfx.alFilter;">
        // Create auxiliary effect slot (secondary audio output)
        auxSlotId = EXTEfx.alGenAuxiliaryEffectSlots();
        logGeneral("Aux slot " + auxSlotId + " created");
        EXTEfx.alAuxiliaryEffectSloti(auxSlotId, EXTEfx.AL_EFFECTSLOT_AUXILIARY_SEND_AUTO, AL10.AL_TRUE);
        checkErrorLog("Failed creating reverb slot "+auxSlotId+"!");

        //Create effect objects
        effectId = EXTEfx.alGenEffects();												    //Create effect object
        EXTEfx.alEffecti(effectId, EXTEfx.AL_EFFECT_TYPE, EXTEfx.AL_EFFECT_EAXREVERB);		//Set effect object to be reverb
        checkErrorLog("Failed creating reverb effect "+effectId+"!");
        logGeneral("effect for "+auxSlotId+": "+effectId);

        filterId = EXTEfx.alGenFilters();
        EXTEfx.alFilteri(filterId, EXTEfx.AL_FILTER_TYPE, EXTEfx.AL_FILTER_LOWPASS);
        logGeneral("filter for "+auxSlotId+": "+filterId);
        //</editor-fold>

        initialised = true;
        return this.set();
    }

    public void delete() {
        EXTEfx.alDeleteAuxiliaryEffectSlots(auxSlotId);
        EXTEfx.alDeleteEffects(effectId);
        EXTEfx.alDeleteFilters(filterId);
        initialised = false;
    }

    public ReverbSlot set() {
        //<editor-fold desc="setReverbParams();">
        EXTEfx.alEffectf(effectId, EXTEfx.AL_EAXREVERB_DENSITY, density);
        checkErrorLog("Error while assigning reverb density: " + density);
        EXTEfx.alEffectf(effectId, EXTEfx.AL_EAXREVERB_DIFFUSION, diffusion);
        checkErrorLog("Error while assigning reverb diffusion: " + diffusion);
        EXTEfx.alEffectf(effectId, EXTEfx.AL_EAXREVERB_GAIN, gain);
        checkErrorLog("Error while assigning reverb gain: " + gain);
        EXTEfx.alEffectf(effectId, EXTEfx.AL_EAXREVERB_GAINHF, gainHF);
        checkErrorLog("Error while assigning reverb gainHF: " + gainHF);
        EXTEfx.alEffectf(effectId, EXTEfx.AL_EAXREVERB_DECAY_TIME, decayTime);
        checkErrorLog("Error while assigning reverb decayTime: " + decayTime);
        EXTEfx.alEffectf(effectId, EXTEfx.AL_EAXREVERB_DECAY_HFRATIO, decayHFRatio * pC.globalReverbBrightness);
        checkErrorLog("Error while assigning reverb decayHFRatio: " + decayHFRatio * pC.globalReverbBrightness);
        EXTEfx.alEffectf(effectId, EXTEfx.AL_EAXREVERB_REFLECTIONS_GAIN, reflectionsGain);
        checkErrorLog("Error while assigning reverb reflectionsGain: " + reflectionsGain);
        EXTEfx.alEffectf(effectId, EXTEfx.AL_EAXREVERB_REFLECTIONS_DELAY, reflectionsDelay);
        checkErrorLog("Error while assigning reverb reflectionsDelay: " + reflectionsDelay);
        EXTEfx.alEffectf(effectId, EXTEfx.AL_EAXREVERB_LATE_REVERB_GAIN, lateReverbGain);
        checkErrorLog("Error while assigning reverb lateReverbGain: " + lateReverbGain);
        EXTEfx.alEffectf(effectId, EXTEfx.AL_EAXREVERB_LATE_REVERB_DELAY, lateReverbDelay);
        checkErrorLog("Error while assigning reverb lateReverbDelay: " + lateReverbDelay);
        EXTEfx.alEffectf(effectId, EXTEfx.AL_EAXREVERB_AIR_ABSORPTION_GAINHF, airAbsorptionGainHF);
        checkErrorLog("Error while assigning reverb airAbsorptionGainHF: " + airAbsorptionGainHF);
        EXTEfx.alEffectf(effectId, EXTEfx.AL_EAXREVERB_ROOM_ROLLOFF_FACTOR, (float) (roomRolloffFactor * defaultAttenuationFactor));
        checkErrorLog("Error while assigning reverb roomRolloffFactor: " + roomRolloffFactor * defaultAttenuationFactor);
        //</editor-fold>

        //Attach updated effect object
        EXTEfx.alAuxiliaryEffectSloti(auxSlotId, EXTEfx.AL_EFFECTSLOT_EFFECT, effectId);
        return this;
    }

    public void applyFilter(int sourceID, float gain, float cutoff) {
        // Set reverb send filter values and set source to send to all reverb fx slots
        EXTEfx.alFilterf(filterId, EXTEfx.AL_LOWPASS_GAIN, gain * pC.globalReverbGain);
        EXTEfx.alFilterf(filterId, EXTEfx.AL_LOWPASS_GAINHF, cutoff);
        AL11.alSource3i(sourceID, EXTEfx.AL_AUXILIARY_SEND_FILTER, auxSlotId, 1, filterId);
        checkErrorLog("Set Environment filter"+filterId+" to "+auxSlotId+":");
    }
}
