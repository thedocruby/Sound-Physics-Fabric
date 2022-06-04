package dev.thedocruby.resounding.openal;

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

@Environment(EnvType.CLIENT)
public class EFXContext { // TODO: Create separate debug toggle for OpenAl EFX instead of using pC.dLog

	// allow inheritance of settings via children contexts
	public static EFXContext[] children = new EFXContext[0];

	// empty arrays
    private static int[]  slots        = new int[0];
    private static int[]  effects      = new int[0];
    private static int[]  filters      = new int[0];
	// default values
    private static int    directFilter = 0;
    public static boolean initialized = false;
    public static boolean efxEnabled  = true;

}
