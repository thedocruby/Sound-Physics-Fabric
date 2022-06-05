package dev.thedocruby.resounding.openal;

import dev.thedocruby.resounding.Engine;
import dev.thedocruby.resounding.Utils;
import dev.thedocruby.resounding.openal.ALContext;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.apache.commons.lang3.ArrayUtils;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.AL11;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.EXTEfx;
import net.minecraft.util.math.MathHelper;

import static dev.thedocruby.resounding.config.PrecomputedConfig.pC;

// TODO fill in later
public class Effect extends Utils {

	// TODO check if this just returns "loading effect Effect"
	public Effect() {if (pC.dLog) Engine.LOGGER.info("loading effect {}", this);}

	public boolean init(ALContext context){return true;}

	public void update(){}

};
