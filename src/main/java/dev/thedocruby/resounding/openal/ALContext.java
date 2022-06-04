package dev.thedocruby.resounding.openal;

// class containing AL context information
public class ALContext {
	public ALContext() {}

	// AL objects, intentionally non-static
	public long   old     = -1        ; // context id
	public long   self    = -1        ; // context id
	public int    direct  = -1        ; // directFilter
	public int[]  slots   = new int[0];
	public int[]  effects = new int[0];
	public int[]  filters = new int[0];
}

