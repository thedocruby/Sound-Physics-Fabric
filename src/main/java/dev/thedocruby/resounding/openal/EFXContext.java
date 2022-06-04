package dev.thedocruby.resounding.openal;

import dev.thedocruby.resounding.Engine;
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
public class EFXContext { // TODO: Create separate debug toggle for OpenAl EFX instead of using pC.dLog

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

	// optional values
	@Nullable public static String id;
	// main interface
	// allow inheritance of settings via children contexts
	public         EFXContext[] children = new EFXContext[0];
	// pipeline
	public  static Effect[] effects = new Effect[0];

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

	public static boolean setup() {
		if (enabled || !active || context.direct > 0) return true; // already setup?
		initSlots  ();
		initEffects();
		initFilters();
		Engine.LOGGER.info("New context: {}.", id == null ? "<unnamed>" : id);

		return false;
	}
	public boolean clean() {return false;}

}
