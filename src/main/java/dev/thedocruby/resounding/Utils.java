package dev.thedocruby.resounding;

import org.apache.commons.lang3.ArrayUtils;

import dev.thedocruby.resounding.effects.Effect;

/**
* Utils
*/
public class Utils {

// java's type system sucks... overloading, ugh - even python could do better...
// an overloaded array length extender function boolean[], boolean[][], int[], int[][] {

public static int[]       extendArray (final int[]       old, final int min) {
	return ArrayUtils.addAll(old, new int[Math.max(1,old.length - min)]);
}

public static int[][]     extendArray (final int[][]     old, final int min) {
	return ArrayUtils.addAll(old, new int[Math.max(1,old.length - min)][]);
}

public static boolean[]   extendArray (final boolean[]   old, final int min) {
	return ArrayUtils.addAll(old, new boolean[Math.max(1,old.length - min)]);
}

public static boolean[][] extendArray (final boolean[][] old, final int min) {
	return ArrayUtils.addAll(old, new boolean[Math.max(1,old.length - min)][]);
}

// }

	// specialized tuple type for effects using float
	public static class SIF {
		public static String f; // first
		public static int    s; // second
		public static float  t; // third

		public SIF(String a, int b, float c) {f = a; s = b; t = c;}
	}

}
