package dev.thedocruby.resounding;

import org.apache.commons.lang3.ArrayUtils;

/**
* Utils
*/
public class Utils {

// java's type system sucks... overloading, ugh - even python could do better...
// an overloaded array length extender function boolean[], boolean[][], int[], int[][] {

public static int[]       extendArray (final int[]       old, final int min) {
	return ArrayUtils.addAll(old, new int    [Math.max(1,old.length - min)]  );
}

public static int[][]     extendArray (final int[][]     old, final int min) {
	return ArrayUtils.addAll(old, new int    [Math.max(1,old.length - min)][]);
}

public static boolean[]   extendArray (final boolean[]   old, final int min) {
	return ArrayUtils.addAll(old, new boolean[Math.max(1,old.length - min)]  );
}

public static boolean[][] extendArray (final boolean[][] old, final int min) {
	return ArrayUtils.addAll(old, new boolean[Math.max(1,old.length - min)][]);
}

// }

	// specialized tuple type for effects using float
	public record SIF(
		String f, // first
		int    s, // second
		float  t  // third
	) {}

}
