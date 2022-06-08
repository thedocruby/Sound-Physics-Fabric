package dev.thedocruby.resounding.effects;

import dev.thedocruby.resounding.openal.*;
import dev.thedocruby.resounding.toolbox.*;
import dev.thedocruby.resounding.Engine;
import dev.thedocruby.resounding.Utils;
import dev.thedocruby.resounding.openal.ALset;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import org.lwjgl.openal.AL10;
import org.lwjgl.openal.AL11;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.EXTEfx;

import net.minecraft.util.math.MathHelper;

import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.apache.commons.lang3.ObjectUtils.Null;

import java.util.function.Function;
import java.util.function.IntPredicate;
import java.util.function.IntConsumer;
import java.util.function.Consumer;

import static dev.thedocruby.resounding.config.PrecomputedConfig.pC;

// TODO fill in later
public class Effect extends Utils {

	public  ALset   context       ; // instantiated
	public  String  name   = ""   ;
	public  boolean active = false;

	public  boolean init() {return true;};

	public  ALset   setup(final long id) {
		// TODO check if this just returns "loading effect Effect"
		if (pC.dLog) Engine.LOGGER.info("loading effect {}", name);
		ALset context = new ALset();
		context.self = id;

		active = true;
		if (!(
			setupSlots  () &&
			setupEffects() &&
			setupFilters() &&
			setupDirect () )) {
			Engine.LOGGER.error("Failed to setup effect: {}", name);
		} else {
			if (pC.dLog) Engine.LOGGER.info("Setup effect: {}", name);
			//active = true;
		}
		return context;
	}

	public  ALset update(SlotProfile slot, SoundProfile sound, boolean isGentle) {return context;}

	// it's a pun! General function for setup of slots/effects/filters
	private int[]   generAL(final String type, Consumer<int[]> generate, IntPredicate verify, IntConsumer init) {
			if (pC.dLog) Engine.LOGGER.info("Creating {}[{}]", type, pC.resolution);
			// create array
			int[] set = new int[pC.resolution];
			generate.accept(set);      // generate   set
			for(int bit : set) {       // loop over  set
				if(verify.test(bit)) { // verify     bit
					init.accept(bit);  // initialize bit
					// if successful (otherwise error)
					if (!ALUtils.checkErrors(
						s -> Engine.LOGGER.info(s+"Failed to create {}.{}", type, bit)
						)) {
						// log
						if (pC.dLog) Engine.LOGGER.info("Created {}.{}", type, bit); continue;
					} active = false; continue; // fail gracefully ← & ↓
				} Engine.LOGGER.error("Failed create {}.{}", type, bit); active = false;
			}
			return active ? set : new int[0];
		}
	private boolean setupSlots() {
		context.slots = generAL(
			"slot",
			EXTEfx::alGenAuxiliaryEffectSlots,
			EXTEfx::alIsAuxiliaryEffectSlot,
			s -> EXTEfx.alAuxiliaryEffectSloti(s, EXTEfx.AL_EFFECTSLOT_AUXILIARY_SEND_AUTO, AL10.AL_TRUE)
		);
		return context.slots.length > 0;
	}
	private boolean setupEffects() {
		context.effects = generAL(
			"effect",
			EXTEfx::alGenEffects,
			EXTEfx::alIsEffect,
			e -> EXTEfx.alEffecti(e, EXTEfx.AL_EFFECT_TYPE, EXTEfx.AL_EFFECT_EAXREVERB) // Set effect type to EAX Reverb
		);
		return context.effects.length > 0;
	}
	private boolean setupFilters() {
		context.filters = generAL(
			"filter",
			EXTEfx::alGenFilters,
			EXTEfx::alIsFilter,
			f -> EXTEfx.alFilteri(f, EXTEfx.AL_FILTER_TYPE, EXTEfx.AL_FILTER_LOWPASS)
		);
		return context.filters.length > 0;
	}
	private boolean setupDirect() {
		context.direct = EXTEfx.alGenFilters();
		if (!EXTEfx.alIsFilter(context.direct)) {
			Engine.LOGGER.error("Failed to create direct filter object!"); return false;
		} else if (pC.dLog) {
			Engine.LOGGER.info("Direct filter object created with ID {}", context.direct);
		}
		return true;
	}};
