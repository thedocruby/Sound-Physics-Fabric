package dev.thedocruby.resounding.openal;

// class containing AL context information
public class ALContext {
	public ALContext() {}

	// AL objects, intentionally non-static
	public static long   old     = -1; // context id
	public static long   self    = -1; // context id
	public static int    direct  = -1; // directFilter
	public static int[]  slots   = {};
	public static int[]  effects = {};
	public static int[]  filters = {};
}

