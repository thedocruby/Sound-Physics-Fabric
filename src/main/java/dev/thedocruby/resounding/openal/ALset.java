package dev.thedocruby.resounding.openal;

// class containing AL context information
public class ALset {
	public ALset() {
		slots   = new int[0];
		effects = new int[0];
		filters = new int[0];
	}
	// AL objects
	public static long  old = -1; // context id
	public static long  self    ; // context id
	public static int   direct  ; // directFilter
	public static int[] slots   ;
	public static int[] effects ;
	public static int[] filters ;
}

