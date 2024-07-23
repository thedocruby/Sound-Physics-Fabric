package dev.thedocruby.resounding;

import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import org.apache.commons.lang3.function.TriFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * {@code Memoizer} is used to memoize mapping calculation from one input to
 * another.
 * <p>
 * A mapping calculation maps a key-input pair into a key-output pair
 * and may require dependency from other keys (see {@code Memoizer}).
 * <p>
 * Calculation errors are collected but not thrown are collected as output from
 * calling {@code solve}. Following are possible errors:
 * <ul>
 * <li>circular or cyclic dependencies</li>
 * <li>dependency's input nor output value exist</li>
 * <li>calculation returned null</li>
 * </ul>
 * Runtime exceptions are not calculation errors and are propagated normally.
 *
 * @param <IN>  the input value type
 * @param <KEY>  the mapping key type
 * @param <OUT>  the output value type
 * @see #Memoizer(Map, TriFunction) Memoizer
 * @see #solve(Map, boolean, boolean) solve
 */
public class Memoizer<IN,KEY,OUT> {

    private Map<KEY,@NotNull IN> in;
    private final Map<KEY,OUT> output;
    private final List<String> errors = new LinkedList<>();

    private Function<KEY,@NotNull IN> getter;
    private BiConsumer<@NotNull KEY,OUT> setter;
    private final TriFunction<Function<KEY,@Nullable OUT>,@NotNull IN,@NotNull KEY,@Nullable OUT> calculator;
    private final ObjectLinkedOpenHashSet<KEY> path = new ObjectLinkedOpenHashSet<>();

    /**
     * Creates a new Memoizer with a backing map and value calculator.
     * <p>
     * The calculation strategy is provided two arguments and is similar to that
     * in {@code Memoizer(Map, TriFunction)} with the only difference being the
     * exclusion of the mapping key.
     *
     * @param output  the backing key-to-output map
     * @param calculator  the calculator
     * @see #Memoizer(Map, TriFunction)
     */
    public Memoizer(
            Map<KEY,OUT> output,
            BiFunction<Function<KEY,@Nullable OUT>,@NotNull IN,@Nullable OUT> calculator
    ) {
        this(output, (getter, raw, key) -> calculator.apply(getter, raw));
    }

    /**
     * Creates a new Memoizer with a backing map and value calculator.
     * <p>
     * The calculation strategy is provided three arguments:
     * <ul>
     * <li>an accessor function mapping a given key to an output. This forms
     *     dependencies between keys and should not be cyclic and should either
     *     their input or output values be present.</li>
     * <li>the input value mapped to the given key</li>
     * <li>the mapping key for both the input and output value</li>
     * </ul>
     * The calculation strategy may return null and is treated as a calculation
     * error and is recorded accordingly (see {@code solve}).
     *
     * @param output  the backing key-to-output map
     * @param calculator  the calculator
     * @see #solve(Map, boolean, boolean) solve
     */
    public Memoizer(
            Map<KEY,OUT> output,
            TriFunction<Function<KEY,@Nullable OUT>,@NotNull IN,@NotNull KEY,@Nullable OUT> calculator
    ) {
        this.output = output;
        this.calculator = calculator;
    }

    /**
     * Given a key-to-input mapping memoize output values for the given keys.
     * This is equivalent to {@code solve(in, false, false)}.
     *
     * @param in  the key-to-input map used for calculating new output values
     * @return a mutable list of errors emitted by this Memoizer
     * @see #solve(Map, boolean, boolean)
     */
    public List<String> solve(Map<KEY,@NotNull IN> in) {
        return solve(in, false, true);
    }

    /**
     * Given a key-to-input mapping memoize output values for the given keys.
     * <p>
     * Runtime exception from the calculator is not caught nor is recorded and
     * is instead forwarded back to the caller.
     *
     * @param in  the key-to-input map used for calculating new output values
     * @param deconstruct  should entries in the key-to-input map be consumed
     * @param ignoreNull  should a null key mapped to a null output be ignored
     * @return a mutable list of errors emitted by this Memoizer
     */
    public List<String> solve(Map<KEY,@NotNull IN> in, boolean deconstruct, boolean ignoreNull) {
        this.in = in;
        path.clear();
        getter = deconstruct ? in::remove : in::get;
        setter = ignoreNull ? (key, value) -> {if (key != value) output.put(key, value);} : output::put;

        in.keySet().stream().toList().forEach(this::get);

        return errors;
    }

    // specialized memoization for tag/material cache functionality
    private @Nullable OUT get(KEY key) {
        // when path contains elements and the current key has been visited
        if (path.contains(key)) {
            errors.add("Circular dependency detected: '" + path + "' requires '" + key.toString() + "'.");
            return null;
        }

        // return cached values
        if (output.containsKey(key))
            return output.get(key);

        path.add(key);
        OUT value = null;
        if (in.containsKey(key))
            value = calculator.apply(this::get, getter.apply(key), key);

        if (value == null)
            errors.add("Invalid path: '" + path + "'.");
        setter.accept(key, value);

        path.removeLast();
        return value;
    }
}
