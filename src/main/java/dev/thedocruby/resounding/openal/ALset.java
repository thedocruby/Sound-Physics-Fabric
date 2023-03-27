package dev.thedocruby.resounding.openal;

// class containing AL context information
public class ALset {
	public ALset() {
		slots   = new int[0];
		effects = new int[0];
		filters = new int[0];
	}
	// AL objects
	public long  old = -1; // context id
	public long  self    ; // context id
	public int   direct  ; // directFilter
	public int[] slots   ;
	public int[] effects ;
	public int[] filters ;
}

