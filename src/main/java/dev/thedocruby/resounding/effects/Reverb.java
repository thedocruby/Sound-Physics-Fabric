package dev.thedocruby.resounding.effects;

import dev.thedocruby.resounding.toolbox.*;
import dev.thedocruby.resounding.openal.*;

import static dev.thedocruby.resounding.config.PrecomputedConfig.pC;
import net.minecraft.util.math.MathHelper;

import org.lwjgl.openal.AL10;
import org.lwjgl.openal.AL11;
import org.lwjgl.openal.EXTEfx;

// this effect adds reverberation - sorta like echo, but instead of hearing the
// sound again, you're hearing it travel away from you.
public class Reverb extends Effect {

	public String id = "reverb";

	public void apply(
			int id,
			// Effect_properties {
			float decayTime,
			float density,
			float diffusion,
			float gainHF,
			float decayHFRatio,
			float reflectionsGain,
			float reflectionsDelay,
			float lateReverbGain,
			float lateReverbDelay
			// }
	)  {
		// define effects to be applied
		SIF[] effects = {
		new SIF("density"            , EXTEfx.AL_EAXREVERB_DENSITY              , density         ),
		new SIF("diffusion"          , EXTEfx.AL_EAXREVERB_DIFFUSION            , diffusion       ),
		new SIF("air_absorption_gain", EXTEfx.AL_EAXREVERB_AIR_ABSORPTION_GAINHF, 1f              ),
		new SIF("late_delay"         , EXTEfx.AL_EAXREVERB_LATE_REVERB_DELAY    , lateReverbDelay ),
		new SIF("late_gain"          , EXTEfx.AL_EAXREVERB_LATE_REVERB_GAIN     , lateReverbGain  ),
		new SIF("reflections_delay"  , EXTEfx.AL_EAXREVERB_REFLECTIONS_DELAY    , reflectionsDelay),
		new SIF("reflections_gain"   , EXTEfx.AL_EAXREVERB_REFLECTIONS_GAIN     , reflectionsGain ),
		new SIF("HF_decay_ratio"     , EXTEfx.AL_EAXREVERB_DECAY_HFRATIO        , decayHFRatio    ),
		new SIF("decay_time"         , EXTEfx.AL_EAXREVERB_DECAY_TIME           , decayTime       ),
		new SIF("HF_gain"            , EXTEfx.AL_EAXREVERB_GAINHF               , gainHF          )
		};
		final int effect = context.effects[id];
		// iterate and apply them
		for (SIF options : effects) {
			EXTEfx.alEffectf(effect, options.s(), options.t());
			ALUtils.errorSet("effect", options.f(), effect, options.t());
		}
		final int slot   = context.slots  [id];
		//Attach updated effect object
		EXTEfx.alAuxiliaryEffectSloti(slot, EXTEfx.AL_EFFECTSLOT_EFFECT, effect);
		if (pC.dLog && !ALUtils.errorApply("effect", effect, "slot", slot)) {
			LOGGER.info("Initialized effect.{}", effect);
		}
	}

	public void lowpass(int   filter, float gain, float cutoff) {  // Set reverb send filter values and set source to send to all reverb fx slots
		if (Float.isNaN(gain  )) gain   = 1.0f;
		EXTEfx.alFilterf(filter, EXTEfx.AL_LOWPASS_GAIN, gain);
		ALUtils.errorProperty("filter", filter, "gain", gain);

		if (Float.isNaN(cutoff)) cutoff = 1.0f;
		EXTEfx.alFilterf(filter, EXTEfx.AL_LOWPASS_GAINHF, cutoff);
		ALUtils.errorProperty("filter", filter, "cutoff", cutoff);
	}
	public void setFilter(int source, int id, float gain, float cutoff) {
		final int filter = context.filters[id];
		final int slot   = context.slots  [id];
		lowpass(filter, gain, cutoff);
		// TODO: figure out how to properly use `AL11.alSource3i(` so i don't have to predetermine reverb.
		AL11.alSource3i(source, EXTEfx.AL_AUXILIARY_SEND_FILTER, slot, 1, filter);
		ALUtils.errorApply(new String[]{"filter", "slot"}, new int[]{filter, slot}, "source", source);
	}

	public void setDirect(final int sourceID, float gain, float cutoff, boolean isGentle) {
		float min = isGentle ? 0.5f : 0;
		gain   = MathHelper.clamp(gain,   min, 1);
		cutoff = MathHelper.clamp(cutoff, min, 1);
		lowpass(context.direct, gain, cutoff);

		AL10.alSourcei(sourceID, EXTEfx.AL_DIRECT_FILTER, context.direct);
		ALUtils.errorApply("direct filter", context.direct, "source", sourceID);
	}

@Override
	public ALset update(SlotProfile slot, SoundProfile sound, boolean isGentle) {
		// Set reverb send filter values and set source to send to all reverb fx slots
		setFilter(sound.sourceID(), slot.slot(), (float) slot.gain(), (float) slot.cutoff());
		// Set direct filter values
		setDirect(sound.sourceID(), (float) sound.directGain(), (float) sound.directCutoff(), isGentle);
		return context;
	}


@Override
	public boolean init() {
		for(int i = 1; i <= pC.resolution; i++){
			double t = (double) i / pC.resolution;
			apply(i - 1,
					(float) Math.max(t * pC.maxDecayTime, 0.1),             // decayTime
					(float) (t * 0.5 + 0.5),                                // density
					(float) MathHelper.lerp(pC.rvrbDiff, 1-t, 1), // diffusion
					(float) (0.95 - (0.75 * t)),                            // gainHF
					(float) Math.max(0.95 - (0.3 * t), 0.1),                // decayHFRatio
					(float) Math.max(Math.pow(1 - t, 0.5) + 0.618, 0.1),    // reflectionsGain
					(float) (t * 0.01),                                     // reflectionsDelay
					(float) (Math.pow(t, 0.5) + 0.618),                     // lateReverbGain
					(float) (t * 0.01)                                      // lateReverbDelay
			);
		}
		EXTEfx.alFilteri(context.direct, EXTEfx.AL_FILTER_TYPE, EXTEfx.AL_FILTER_LOWPASS);
		final boolean success = !ALUtils.logAndConsumeError("Failed to properly initialize OpenAL Auxiliary Effect slots. Aborting");
		if (success && pC.dLog) LOGGER.info("Finished initializing OpenAL Auxiliary Effect slots!");
		// TODO ? what ?
		// efxEnabled = false;
		return success;
	}

}


/**
 * (from: Effects Guide (p. 66+) and other sources; altered)
 * Documentation:
 * Reverb Modal Density controls the coloration of the late reverb.
 * Lowering the value adds more coloration to the late reverb.
 * Modal density is defined as the expected number of modes per unit frequency, which is given by: n(ω)=∂N(ω)∂ω=∂∂ω(kL/π)=Lπcg
 * In music, normal modes of vibrating instruments (strings, air pipes, drums, etc.) are called "harmonics" or "overtones".
 * ∴ High-frequency "depth"?
 *<p>
 * Strutt|Building Acoustics|Room Modal Density allows the user to calculate the number of room modes present in a room up to a given frequency cut-off (number of modes) as well as the modal density (the number of modes per frequency band) using the Bolt and Morse formula.
 * The number of modes, N from 0 Hz up to f Hz is given by:
 * N=4πf³V/3c³+πf²S/4c²+fP/8c
 * where:
 * f is the frequency (Hz)
 * V is the room volume (m³)
 * S is the room surface area (m²)
 * P is the total room perimeter (m)
 * The above terms describe the number of oblique (∝f³), lateral (∝f²) and normal (∝f) modes in the room. Especially for high frequencies, the number of oblique modes is generally dominant.
 * The modal density is given by:
 * ⇒⇒⇒ dN/df=4πf²/Vc³+πfS/2c²+P/8c
 * Although derived for rectangular rooms, the above expressions give good estimates of the modal behaviour for rooms of arbitrary shape, provided that the aspect ratio of the room is not too extreme.
 * </p>
 * @param density				// min: 0.0f 	max: 1.0f   (?)                         default: 1.0f
 *
 * <p>
 * The Reverb Diffusion property controls the echo density in the reverberation decay.
 * It is set by default to 1.0, which provides the highest density.
 * Reducing diffusion gives the reverberation a more “grainy” character
 * that is especially noticeable with percussive (a rhythmic patterning of noise, ex. clapping, stomping) sound sources.
 * If you set a diffusion value of 0.0, the later reverberation sounds like a succession of distinct echoes.
 * ∴ 1.0 ⇒ uniform noise
 * #See other fields for more#
 * A commonly overlooked parameter on reverb plugins is diffusion.
 * This helps to control the initial build up of echo density and effects how you hear the reverb reflections.
 * As a guide, for clearer more natural sounding mixes, vocals and instruments use low parameter settings.
 * If you’re looking to enhance percussion and drum hits use medium to high settings.
 * </p>
 * @param diffusion				// min: 0.0f 	max: 1.0f   (a linear multiplier value) default: 1.0f
 *
 * <p>
 * The Reverb Gain property is the master volume control for the reflected sound
 * (both  early  reflections  and  reverberation), that the reverb effect adds to all sound sources.
 * It sets the maximum amount of reflections and reverberation added to the final sound mix.
 * The value of the Reverb Gain property ranges from 1.0 (0db) (the maximum amount)
 * to 0.0 (-100db) (no reflected sound at all).
 * </p>
 * @param gain					// min: 0.0f 	max: 1.0f   (Linear gain)               default: 0.32f
 *
 * <p>
 * The Reverb Gain HF property further tweaks reflected sound by attenuating (gradually fading) it at high frequencies.
 * It controls a low-pass filter that applies globally to the reflected sound of all sound sources feeding the particular instance of the reverb effect.
 * The value of the Reverb Gain HF property ranges from 1.0 (0db) (no filter) to 0.0 (-100db) (virtually no reflected sound).
 * #HF Reference# sets the frequency at which the value of this property is measured.
 * </p>
 * @param gainHF				// min: 0.0f 	max: 1.0f   (Linear gain)               default: 0.89f
 *
 * <p>
 * The Reverb Gain LF property further tweaks reflected sound by attenuating it at low frequencies.
 * It controls a high-pass filter that applies globally to the reflected sound of all sound sources feeding the particular instance  of the reverb effect.
 * The value of the Reverb Gain LF property ranges from 1.0 (0db) (no filter) to 0.0 (-100db) (virtually no reflected sound).
 * #LF Reference# sets the frequency at which the value of this property is measured.
 * </p>
 * @param gainLF				// min: 0.0f    max: 1.0f   (Linear gain)               default: 0.0f
 *
 * <p>
 *  The Decay Time property sets the reverberation decay time.
 *  It ranges from 0.1 (typically a small room with very dead surfaces)
 *  to 20.0 (typically a large room with very live surfaces).
 * </p>
 * @param decayTime				// min: 0.1f 	max: 20.0f  (Seconds)                   default: 1.49f
 *
 * <p>
 * The Decay HF Ratio property adjusts the spectral quality of the Decay Time parameter.
 * It is the ratio of high-frequency decay time relative to the time set by Decay Time.
 * The Decay HF Ratio value  1.0  is  neutral:  the  decay  time  is  equal  for  all  frequencies.
 * As  Decay  HF  Ratio  increases  above  1.0,  the  high-frequency  decay  time  increases,
 * so  it’s  longer  than  the  decay  time  at  mid-frequencies.
 * You hear a more brilliant reverberation with a longer decay at high frequencies.
 * As the Decay HF Ratio value decreases below 1.0, the high-frequency decay time decreases,
 * so it’s shorter than the decay time of the mid-frequencies.
 * You hear a more natural reverberation.
 * </p>
 * @param decayHFRatio			// min: 0.1f 	max: 20.0f  (Linear multiplier)         default: 0.83f
 *
 * <p>
 * The Decay LF Ratio property adjusts the spectral quality of the Decay Time parameter.
 * It is the ratio  of  low-frequency  decay  time  relative  to  the  time  set  by  Decay  Time.
 * The  Decay  LF  Ratio  value  1.0  is  neutral:  the  decay  time  is  equal  for  all  frequencies.
 * As  Decay  LF  Ratio  increases  above  1.0,  the  low-frequency  decay  time  increases,
 * so  it’s  longer  than  the  decay  time  at  mid-frequencies.
 * You hear a more booming reverberation with a longer decay at low frequencies.
 * As the Decay LF Ratio value decreases below 1.0, the low-frequency decay time decreases,
 * so it’s shorter than the decay time of the mid-frequencies.
 * You hear a more tinny reverberation.
 * </p>
 * @param decayLFRatio			// min: 0.1f 	max: 20.0f  (Linear multiplier)         default: 1.0f
 *
 * <p>
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
 * </p>
 * @param reflectionsGain		// min: 0.1f 	max: 3.16f  (Linear gain)               default: 0.05
 *
 * <p>
 * The Reflections Delay property is the amount of delay between the arrival time of the direct path from the source
 * to the first reflection from the source.  It ranges from 0 to 300 milliseconds.
 * You can reduce or increase "Reflections Delay" to simulate closer or more distant reflective surfaces,
 * and therefore control the perceived size of the room.
 * </p>
 * @param reflectionsDelay		// min: 0.0f 	max: 0.3f   (Seconds)                   default: 0.007
 *
 * <p>
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
 * </p>
 * @param reflectionsPan		// magnitude min: 0.0f   max: 1.0f   (Vector)                    default: [0f, 0f, 0f]
 *
 * <p>
 * The Late Reverb Gain property controls the overall amount of later reverberation relative to the Gain property.
 * (The Gain property sets the overall amount of both initial reflections and later reverberation.)
 * The value of Late Reverb Gain ranges from a maximum of 10.0 (+20 dB) to a minimum of 0.0 (-100 dB) (no late reverberation at all).
 * Note that Late Reverb Gain and Decay Time are independent properties:
 * If you adjust Decay Time without changing Late Reverb Gain, the total intensity
 * (the averaged square of the amplitude) of the late reverberation remains constant.
 * </p>
 * @param lateReverbGain		// min: 0.0f 	max: 10.0f  (Linear gain)               default: 1.26f
 *
 * <p>
 * The Late Reverb Delay property defines the beginning time of the late reverberation
 * relative to the time of the initial reflection (the first of the early reflections). It ranges from 0 to 100 milliseconds.
 * Reducing or increasing Late Reverb Delay is useful for simulating a smaller or larger room.
 * </p>
 * @param lateReverbDelay		// min: 0.0f 	max: 0.1f   (Seconds)                   default: 0.011f
 *
 * <p>
 * The Late Reverb Pan property is a 3D vector that controls the spatial distribution of the late reverb.
 * The direction of this vector controls the global direction of the reverb,
 * while its magnitude controls how focused the reverb are towards this direction.
 * The details under Reflections Pan (above) also apply to Late Reverb Pan.
 * </p>
 * @param lateReverbPan			// magnitude min: 0.0f    max: 1.0f   (Vector)           default: [0f, 0f, 0f]
 *
 * <p>
 * Echo Depth introduces a cyclic echo in the reverberation decay, which will be noticeable with transient or percussive sounds.
 * A larger value of Echo Depth will make this effect more prominent.
 * Echo Time controls the rate at which the cyclic echo repeats itself along the reverberation decay.
 * For example, the default setting for Echo Time is 250 ms., causing the echo to occur 4 times per second.
 * Therefore, if you were to clap your hands in this type of environment, you will hear four repetitions of clap per second.
 * Together with Reverb Diffusion, Echo Depth will control how long the echo effect will persist along the reverberation decay.
 * In a more diffuse environment, echoes will wash out more quickly after the direct sound.
 * In an environment that is less diffuse, you will be able to hear a larger number of repetitions of the echo,
 * which will wash out later in the reverberation decay.
 * If Diffusion is set to 0.0 and Echo Depth is set to 1.0, the echo will persist distinctly until the end of the reverberation decay.
 * </p>
 * @param echoTime				// min: 0.075f  max: 0.25f  (Seconds)                   default: 0.25f
 *
 * @param echoDepth				// min: 0.0f    max: 1.0f   (Linear multiplier)         default: 0.0f
 *
 * <p>
 * Using these two properties, you can create a pitch modulation in the reverberant sound.
 * This will be most noticeable applied to sources that have tonal color or pitch.
 * You can use this to make some trippy effects!
 * Modulation Time controls the speed of the vibrato (rate of periodic changes in pitch).
 * Modulation Depth controls the amount of pitch change.
 * Low values of Diffusion will contribute to reinforcing the perceived effect by
 * reducing the mixing of overlapping reflections in the reverberation decay.
 * </p>
 * @param modulationTime		// min: 0.004f  max: 4.0f   (Seconds)                   default: 0.25f
 *
 * @param modulationDepth		// min: 0.0f    max: 1.0f   (Linear multiplier)         default: 0.0f
 *
 * <p>
 * The properties "HF Reference" and "LF Reference" determine respectively the frequencies at which
 * the high-frequency effects and the low-frequency effects created by EAX Reverb properties are measured,
 * for example #Decay HF Ratio# and #Decay LF Ratio#.
 * Note that it is necessary to maintain a factor of at least 10 between these two reference frequencies so that
 * low frequency and high frequency properties can be accurately controlled and will produce independent effects.
 * In other words, the LF Reference value should be less than 1/10 of the HF Reference value.
 * </p>
 * @param HFReference			// min: 1000f   max: 20000f (Hertz)                     default: 5000f
 *
 * @param LFReference			// min: 20.0f   max: 1000f  (Hertz)                     default: 250f
 *
 * <p>
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
 * </p>
 * @param roomRolloffFactor		// min: 0.0f	max: 10.0f  (Linear multiplier)         default: 0.0f
 *
 * <p>
 * The Air Absorption Gain HF property controls the distance-dependent attenuation at high frequencies caused by the propagation medium.
 * It applies to reflected sound only.
 * You can use Air Absorption Gain HF to simulate sound transmission through foggy air, dry air, smoky atmosphere, and so on.
 * The default value is 0.994 (-0.05  dB) per meter, which roughly corresponds to typical condition of atmospheric humidity, temperature, and so on.
 * Lowering the value simulates a more absorbent medium (more humidity in the air, for example),
 * raising the value simulates a less absorbent medium (dry desert air, for example).
 * </p>
 * @param airAbsorptionGainHF	// min: 0.892f 	max: 1.0f   (Linear gain per meter)     default: 0.994f
 *
 * <p>
 * When this flag is set, the high-frequency decay time automatically stays below a limit value that is
 * derived from the setting of the property Air Absorption Gain HF.
 * This limit applies regardless of the setting of the property Decay HF Ratio, and the limit doesn't affect the value of Decay HF Ratio.
 * This limit, when on, maintains a natural sounding reverberation decay by allowing you to increase the
 * value of Decay Time without the risk of getting an unnaturally long decay time at high frequencies.
 * If this flag is set to AL_FALSE, high-frequency decay time isn’t automatically limited.
 * </p>
 * @param decayHFLimit			// min:AL_FALSE max:AL_TRUE (Boolean)                   default: true
 */


record DOCS(
		float density, float diffusion,
		float gain, float gainHF, float gainLF,
		float decayTime, float decayHFRatio, float decayLFRatio,
		float reflectionsGain, float reflectionsDelay, float[] reflectionsPan,
		float lateReverbGain, float lateReverbDelay, float[] lateReverbPan,
		float echoTime, float echoDepth,
		float modulationTime, float modulationDepth,
		float HFReference, float LFReference,
		float roomRolloffFactor,
		float airAbsorptionGainHF,
		int decayHFLimit
) {}