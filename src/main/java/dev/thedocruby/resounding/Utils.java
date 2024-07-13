package dev.thedocruby.resounding;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resource.InputSupplier;
import net.minecraft.resource.ResourcePack;
import net.minecraft.resource.ResourcePackProfile;
import org.apache.commons.compress.parallel.InputStreamSupplier;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static dev.thedocruby.resounding.Engine.mc;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
* Utils
*/
public class Utils {

    public static final Logger LOGGER = LogManager.getLogger("Resounding");

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
    public static <IN,OUT> OUT memoize(HashMap<String, IN> in, HashMap<String, OUT> out, String key, BiFunction<Function<String, OUT>, IN, OUT> calculate) {
        return memoize(in, out, key, calculate, true);
    }

    // specialized memoization for tag/material cache functionality
    public static <IN,OUT> OUT memoize(HashMap<String, IN> in, HashMap<String, OUT> out, String key, BiFunction<Function<String, OUT>, IN, OUT> calculate, boolean remove) {
        // return cached values
        if (out.containsKey(key))
            return out.get(key);
        // mark as in-progress
        // getter == null; should be scanned for in calculate to prevent cyclic references
        out.put(key, null);
        OUT value = null;
        if (in.containsKey(key))
            value = calculate.apply(
                x -> memoize(in, out, x, calculate, remove),
                remove ? in.remove(key) : in.get(key));
        out.put(key, value);
        if (value == null)
            LOGGER.error("{} is invalid or cyclical", key);
        return value;
    }

    public static Double when(Double value, Double coefficient) {
        return value == null ? null : value * coefficient;
    }

    // ba-d-ad jokes will never get old!
    // update + weight
    public static void updWeight(Double[] list, int index, @Nullable Double value, double coefficient) {
        if (list != null)
            list[index] = value == null ? 0 : value * coefficient;
    }

    // average values using weights
    public static Double unWeight(Double weight, Double[] values, Double fallback) {
        // density == 0 is for single-value modifications (shapes, lone values)
        return smartSum(values, fallback) / (weight == 0 ? values.length : weight);
    }

    // sum with specialized fallback
    public static Double smartSum(Double[] values, Double fallback) {
        if (values == null)
            return fallback;
        double output = 0;
        for (Double value : values)
            output += value == null ? 0 : value;
        return output;
    }

    // update a value in a particular type of hashmap (see signature)
    public static void update(HashMap<String, LinkedList<String>> map, String key, String value) {
        final LinkedList<String> list = map.getOrDefault(key, new LinkedList<>());
        list.add(value);
        map.put(key, list);
    }

    /* utility function */
    public static <T> double logBase(T x, T b) { return Math.log((Double) x) / Math.log((Double) b); }

    // Recalls arbitrary configuration from file
    // gets the config dir, opens the save file, parses it
    public static <T> HashMap<String, T> recall(String path, Type token, Function<LinkedTreeMap,T> deserializer) {
        HashMap<String, T> output = new HashMap<>();
        LinkedTreeMap<String, LinkedTreeMap> input = new LinkedTreeMap<>();
        try {
            String name = FabricLoader.getInstance().getConfigDir().toAbsolutePath().resolve(path).toString();
            FileReader reader = new FileReader(name);
            // parse JSON input
            input = new Gson().fromJson(reader, token);
        } catch (IOException e) {
            LOGGER.error("Failed recalling '" + token.toString() + "'s from config", e);
        }
        input.forEach((String key, LinkedTreeMap value) ->
                output.put(key, deserializer.apply(value))
        );
        return output;
    }

    public static <T> HashMap<String, T> resource(ResourcePack pack, String[] path, Type token, BiFunction<String, LinkedTreeMap, HashMap<String, T>> deserializer) {
        HashMap<String, T> output = new HashMap<>();
        InputStream input;
        // if not available, move on
        try { input = pack.openRoot(path).get(); }
        catch (NullPointerException | IOException e) { return output; }

        LinkedTreeMap<String, LinkedTreeMap> raw = new Gson().fromJson(new InputStreamReader(input, UTF_8), token);
        // place deserialized values into record.
        // this issue is fixed in GSON 2.10, but not in 2.8.9 (what 1.18.2 uses)
        raw.forEach((String key, LinkedTreeMap value) -> {
            HashMap<String, T> map = deserializer.apply(key, value);
            output.putAll(map);
        });

        return output;
    }

    public static Stream<String> granularFilter(List<String> items, Pattern[] patterns, String[] keys) {
        return items.stream().filter(
                // if matches any of the patterns
                item -> Arrays.stream(patterns)
                        .filter(Objects::nonNull)
                        .anyMatch(t -> t.matcher(item).find())
                     // or is explicitly stated
                     || Arrays.asList(keys).contains(item)
        );
    }

    // compile regular expressions
    static Pattern[] toPatterns(String[] patterns) {
        return Arrays.stream(patterns).map(Pattern::compile).toArray(Pattern[]::new);
    }

    // smart type conversion
    static <T> T[] asArray(T[] blank, @Nullable Object input) {
        if (input == null) return blank;
        if (input instanceof ArrayList)
            return ((ArrayList<T>) input).toArray(blank);
        else
            return (T[]) input;
    }

// }

	// specialized tuple type for effects using float
	public record SIF
		( String f // first
		, int    s // second
		, float  t  // third
	) {}

    // returns Tokenized Type for use in serialization, based on target type
    public static <T> Type token(T x) {
        return new TypeToken<T>(){}.getType();
    }

}
