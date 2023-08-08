package dev.thedocruby.resounding.openal;

import dev.thedocruby.resounding.Engine;
import dev.thedocruby.resounding.Utils;
import dev.thedocruby.resounding.effects.Effect;
import dev.thedocruby.resounding.effects.Reverb;
import dev.thedocruby.resounding.toolbox.SlotProfile;
import dev.thedocruby.resounding.toolbox.SoundProfile;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.openal.EXTEfx;
import org.lwjgl.openal.EXTThreadLocalContext;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.IntPredicate;

import static dev.thedocruby.resounding.config.PrecomputedConfig.pC;


@Environment(EnvType.CLIENT)
public class Context extends Utils { // TODO: Create separate debug toggle for OpenAl EFX instead of using pC.dLog

	// default values
	private ALset[] contexts;
	public  boolean active  = false;
	public  boolean enabled = true ;
	public  boolean garbage = false; // for custom-garbage collector

	// context ids
	private long old  = -1;
	private long self = 0 ;

	private boolean bound = false;

	// optional values
	@Nullable
	public String id = null;
	// main interface
	// allow inheritance of settings via children contexts
	public  Context[] children;
	public  Effect[]  effects; // pipeline

	// context control {
	public boolean activate() {
		old = EXTThreadLocalContext.alcGetThreadContext();
		EXTThreadLocalContext.alcSetThreadContext(self);
		return !ALUtils.logAndConsumeError("Error while activating openAL context "+self+".");
	}
	public boolean deactivate() {
		if (old == -1) return false;
		EXTThreadLocalContext.alcSetThreadContext(old);
		return !ALUtils.logAndConsumeError("Error while reactivating openAL context "+old+".");
	}
	// }
	
	// INFO can't be reasonably made dynamic, don't try
	private void populateEffects() {
		// alphabetical
		// TODO implement other effects
		effects = new Effect[] {
		/*new Atmosphere() // TODO implement air-density
		, new Doppler()    // TODO add from OpenAL-MC mod
		, new EarDamage()  // TODO locate resources for ringing
		, new Echo()       // TODO - ray.long && power > distance * atmospheric density
		, new Occlusion()  // bunched in with reverb, for now
		, new Resonance()
		*/new Reverb()
		/*new Style()
		, new Travel()
		*/
		}; // cleaner syntax...
		contexts = new ALset[effects.length];
	}

	public         boolean bind(long context, @Nullable final String name) {
		bound = true;
		self = context;
		return setup(name);
	}

	public         boolean setup(@Nullable final String name) {
		if (active) return false;
		//if (children != null) clean(true);
		children = new Context[0];
		effects  = new Effect[0];
		garbage = false;
		id = name;
		// TODO create new context here
		if (bound) old  = EXTThreadLocalContext.alcGetThreadContext();
		else       self = EXTThreadLocalContext.alcGetThreadContext();
		activate();
		populateEffects();
		for (int i = 0; i<effects.length; i++) {
			contexts[i] = effects[i].setup(self);
			effects[i].init();
		}
		deactivate();
		active = true;
		return true;
	}
	public  boolean clean(final boolean force) {
		if (garbage) return true;
		Engine.LOGGER.info("{}: cleaning children[{}]", id, children.length);
		boolean success = cleanObjects();
		activate();
		garbage = force || success;
		active = false;
		if (pC.dLog) {
			if (success) Engine.LOGGER.info ("Cleaned context: {}.", id);
			else         Engine.LOGGER.error("Context remains: {}.", id);
		}
		return success;
	}

	public  boolean cleanObjects() {
		boolean success = true;
		for (ALset context : contexts) {
			success = cleanSlots  (context) && success;
			success = cleanEffects(context) && success;
			success = cleanFilters(context) && success;
			success = cleanDirect (context) && success;
		}
		return success;
	}
	// it's a pun! General function for cleaning slots/effects/filters
	private int[]   deleteAL(final String type, int[] set, Consumer<int[]> delete, IntPredicate verify) {
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
	private boolean cleanSlots(ALset context) {
		context.slots = deleteAL(
			"slot", context.slots,
			EXTEfx::alDeleteAuxiliaryEffectSlots,
			EXTEfx::alIsAuxiliaryEffectSlot
		);
		return context.slots.length == 0;
	}
	private boolean cleanEffects(ALset context) {
		context.effects = deleteAL(
			"effect", context.effects,
			EXTEfx::alDeleteEffects,
			EXTEfx::alIsEffect
		);
		return context.effects.length == 0;
	}
	private boolean cleanFilters(ALset context) {
		context.filters = deleteAL(
			"filter", context.filters,
			EXTEfx::alDeleteFilters,
			EXTEfx::alIsFilter
		);
		return context.filters.length == 0;
	}
	private boolean cleanDirect(ALset context) {
		EXTEfx.alDeleteFilters(context.direct);
		if (EXTEfx.alIsFilter(context.direct)) {
			Engine.LOGGER.error("Failed to delete direct filter object!"); return false;
		} else if (pC.dLog) {
			Engine.LOGGER.info("Direct filter object deleted with ID {}", context.direct);
		}
		return true;
	}

	public  boolean addChild(Context child) {
		final boolean query = queryChild(child.getID()) == -1;
		if (query) {
			children = ArrayUtils.add(children, child);
		}
		return query;
	}
	public  int queryChild(@Nullable String name) {
		for (int i = 0; i<children.length; i++) {
			if (children[i].getID(name)) return i;
		}
		return -1;
	}

	public  boolean getID(@Nullable String guess) {return Objects.equals(guess, id);}
	public  String  getID()                       {return id;}
	public  boolean isGarbage()                   {return garbage;}

	public  void update(SlotProfile slot, SoundProfile sound, boolean isGentle) {
		if (!(active && enabled)) return;
		activate();
		for (int i = 0; i<effects.length; i++) {
			contexts[i] = effects[i].update(slot, sound, isGentle);
		}
		deactivate();
	}

}
