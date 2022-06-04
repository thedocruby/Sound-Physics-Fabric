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


@Environment(EnvType.CLIENT)
public class Context extends Utils { // TODO: Create separate debug toggle for OpenAl EFX instead of using pC.dLog

	// class containing AL context information
	private class ALContext {
		private ALContext() {}

		// AL objects, intentionally non-static
		public long   old     = -1        ; // context id
		public long   self    = -1        ; // context id
		public int    direct  = -1        ; // directFilter
		public int[]  slots   = new int[0];
		public int[]  effects = new int[0];
		public int[]  filters = new int[0];
	}

	// default values
	private static ALContext context        ;
	public  static boolean   active  = false;
	public  static boolean   enabled = true ;
	public  static boolean   garbage = false; // for custom-garbage collector

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
	
	// TODO (maybe?) make dynamic
	private static void populateEffects() {
		effects    = new Effect[8];
		// alphabetical
		effects[0] = new Doppler();
		effects[1] = new EarDamage();
		effects[2] = new Echo();
		effects[3] = new Occlusion();
		effects[4] = new Resonance();
		effects[5] = new Reverb();
		effects[6] = new Style();
		effects[7] = new Travel();
	}

	public static boolean setup(@Nullable final String name) {
		if (enabled || !active || context.direct > 0) return true; // already setup?
		id = name;
//		if (!(
//			setupSlots  () &&
//			setupEffects() &&
//			setupFilters() )) {
//			Engine.LOGGER.error("Failed context: {}.", id);
//			return false;
//		}
		Engine.LOGGER.info("Created context: {}.", id);
		enabled = true;
		populateEffects();
		for (int i = 0; i<effects.length; i++) effects[i].init();
		return enabled && active;
	}

	public static boolean clean(@Nullable final boolean force) {
		if (enabled || !active || context.direct > 0) return true; // already setup?
//		if (!(
//			cleanSlots  () &&
//			cleanEffects() &&
//			cleanFilters() )) {
//			Engine.LOGGER.error("Context remains: {}.", id);
//			if (force) enabled = false; garbage = true;
//			return false;
//		}
		Engine.LOGGER.info("Cleaned context: {}.", id);
		enabled = false; garbage = true;
		return true;
	}

}
