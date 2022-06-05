package dev.thedocruby.resounding.openal;

import dev.thedocruby.resounding.Engine;
import dev.thedocruby.resounding.Utils;
import dev.thedocruby.resounding.effects.*;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.apache.commons.lang3.ArrayUtils;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.AL11;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.EXTEfx;
import org.lwjgl.openal.EXTThreadLocalContext;

import net.minecraft.util.math.MathHelper;

import static dev.thedocruby.resounding.config.PrecomputedConfig.pC;

import javax.annotation.Nullable;

import java.util.function.Function;
import java.util.function.IntPredicate;
import java.util.function.IntConsumer;
import java.util.function.Consumer;


@Environment(EnvType.CLIENT)
public class Context extends Utils { // TODO: Create separate debug toggle for OpenAl EFX instead of using pC.dLog

	// default values
	private static ALContext context = new ALContext();
	public  static boolean   active  = false          ;
	public  static boolean   enabled = true           ;
	public  static boolean   garbage = false          ; // for custom-garbage collector

	// optional values
	@Nullable public static String id = null;
	// main interface
	// allow inheritance of settings via children contexts
	public         Context[] children = new Context[0];
	public  static Effect[]  effects  = new Effect[0] ; // pipeline

	// context control {
	public static boolean activate() {
		context.old = EXTThreadLocalContext.alcGetThreadContext();
		EXTThreadLocalContext.alcSetThreadContext(context.self);
		return !ALUtils.checkErrors("Error while activating openAL context "+context.self+".");
	}
	public static boolean deactivate() {
		EXTThreadLocalContext.alcSetThreadContext(context.old);
		return !ALUtils.checkErrors("Error while reactivating openAL context "+context.old+".");
	}
	// }
	
	// INFO can't be reasonably made dynamic, don't try
	private static void populateEffects() {
		// alphabetical
		Effect[] temp =
		{ new Doppler()
		, new EarDamage()
		, new Echo()
		, new Occlusion()
		, new Resonance()
		, new Reverb()
		, new Style()
		, new Travel()
		}; // cleaner syntax...
		effects = temp;
	}

	public  static boolean bind(final long existing, final @Nullable String name) {
		if (enabled || !active || context.direct > 0) return true; // already setup?
		context.self = existing;
		id = name;
		if (!(
			setupSlots  () &&
			setupEffects() &&
			setupFilters() )) {
			Engine.LOGGER.error("Failed to create context: {}.", id);
			active = false;
			return false;
		}
		Engine.LOGGER.info("Created context: {}.", id);
		enabled = true;
		populateEffects();
		for (Effect effect : effects) effect.init(context);
		return enabled && active;
	}
	public  static boolean setup(@Nullable final String name) {
		// TODO create new context here
		final long newContext = 1; // placeholder
		return bind(newContext, name);
	}
	// it's a pun! General function for setup of slots/effects/filters
	private static int[]   generAL(final String type, Consumer<int[]> generate, IntPredicate verify, IntConsumer init) {
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
	private static boolean setupSlots() {
		context.slots = generAL(
			"slot",
			EXTEfx::alGenAuxiliaryEffectSlots,
			EXTEfx::alIsAuxiliaryEffectSlot,
			s -> EXTEfx.alAuxiliaryEffectSloti(s, EXTEfx.AL_EFFECTSLOT_AUXILIARY_SEND_AUTO, AL10.AL_TRUE)
		);
		return context.slots.length > 0;
	}
	private static boolean setupEffects() {
		context.effects = generAL(
			"effect",
			EXTEfx::alGenEffects,
			EXTEfx::alIsEffect,
			e -> EXTEfx.alEffecti(e, EXTEfx.AL_EFFECT_TYPE, EXTEfx.AL_EFFECT_EAXREVERB) // Set effect type to EAX Reverb
		);
		return context.effects.length > 0;
	}
	private static boolean setupFilters() {
		context.filters = generAL(
			"filter",
			EXTEfx::alGenFilters,
			EXTEfx::alIsFilter,
			f -> EXTEfx.alFilteri(f, EXTEfx.AL_FILTER_TYPE, EXTEfx.AL_FILTER_LOWPASS)
		);
		return context.filters.length > 0;
	}

	public  static boolean clean(@Nullable final boolean force) {
		if (!(
			cleanSlots  () &&
			cleanEffects() &&
			cleanFilters() )) {
			Engine.LOGGER.error("Context remains: {}.", id);
			if (force) enabled = false; garbage = true;
			return false;
		}
		Engine.LOGGER.info("Cleaned context: {}.", id);
		enabled = false; garbage = true;
		return true;
	}
	// it's a pun! General function for cleaning slots/effects/filters
	private static int[]   deleteAL(final String type, int[] set, Consumer<int[]> delete, IntPredicate verify) {
		if (pC.dLog) Engine.LOGGER.info("Removing {}[{}]", type, set.length);
		delete.accept(set.clone());
		// loop through slots
		for (int bit : set) {
			if (verify.test(bit)) { Engine.LOGGER.error("Failed to delete {}.{}", type, bit); continue; }
			set = ArrayUtils.removeElement(set, bit);
			if (pC.dLog) Engine.LOGGER.info("Deleting {}.{}", type, bit);
		}
		return set;
	}
	private static boolean cleanSlots() {
		context.slots = deleteAL(
			"slot", context.slots,
			EXTEfx::alDeleteAuxiliaryEffectSlots,
			EXTEfx::alIsAuxiliaryEffectSlot
		);
		return context.slots.length == 0;
	}
	private static boolean cleanEffects() {
		context.effects = deleteAL(
			"effect", context.effects,
			EXTEfx::alDeleteEffects,
			EXTEfx::alIsEffect
		);
		return context.effects.length == 0;
	}
	private static boolean cleanFilters() {
		context.filters = deleteAL(
			"filter", context.filters,
			EXTEfx::alDeleteFilters,
			EXTEfx::alIsFilter
		);
		return context.filters.length == 0;
	}

	public  static void    update() {
		if (!(active && enabled)) return;
		for (Effect effect : effects) effect.update();
	}

}
