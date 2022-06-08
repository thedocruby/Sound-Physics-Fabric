package dev.thedocruby.resounding.effects;

import dev.thedocruby.resounding.toolbox.*;
import dev.thedocruby.resounding.openal.*;
import dev.thedocruby.resounding.Engine;
import static dev.thedocruby.resounding.config.PrecomputedConfig.pC;
import net.minecraft.util.math.MathHelper;

import org.lwjgl.openal.AL10;
import org.lwjgl.openal.AL11;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.EXTEfx;

// this effect adds reverberation - sorta like echo, but instead of hearing the
// sound again, you're hearing it travel away from you.
public class Reverb extends Effect {

	public String id = "reverb";
//	public Reverb() {}

//	private ALset context;

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
		int slot   = context.slots  [id];
		int effect = context.effects[id];
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
		// iterate and apply them
		for (SIF options : effects) {
			EXTEfx.alEffectf(effect, options.s, options.t);
			ALUtils.errorSet("effect", options.f, effect, options.t);
		}
		//Attach updated effect object
		EXTEfx.alAuxiliaryEffectSloti(slot, EXTEfx.AL_EFFECTSLOT_EFFECT, effect);
		if (pC.dLog && !ALUtils.errorApply("effect", effect, "slot", slot)) {
			Engine.LOGGER.info("Initialized effect.{}", effect);
		}
	}

	public void lowpass(int   filter, float gain, float cutoff) {  // Set reverb send filter values and set source to send to all reverb fx slots
		if (Float.isNaN(gain  )) gain   = 1.0f;
		if (Float.isNaN(cutoff)) cutoff = 1.0f;
		EXTEfx.alFilterf(filter, EXTEfx.AL_LOWPASS_GAIN, gain);
		ALUtils.errorProperty("filter", filter, "gain", gain);

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
		boolean success = true;
		for(int i = 1; i <= pC.resolution; i++){
			double t = (double) i / pC.resolution;
			apply(i - 1,
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
		EXTEfx.alFilteri(context.direct, EXTEfx.AL_FILTER_TYPE, EXTEfx.AL_FILTER_LOWPASS);
		success &= !ALUtils.checkErrors("Failed to initialize direct filter object!");
		if (success) {
			if (pC.dLog) Engine.LOGGER.info("Finished initializing OpenAL Auxiliary Effect slots!");
			return success;
		}
		Engine.LOGGER.info("Failed to properly initialize OpenAL Auxiliary Effect slots. Aborting");
		// TODO ? what ?
		// efxEnabled = false;
		return success;
	}

}
