package dev.thedocruby.resounding;

import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import org.apache.commons.lang3.function.TriFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

public class Memoizer<IN,KEY,OUT> {

    private Map<KEY,@NotNull IN> in;
    private final Map<KEY,OUT> output;
    private final List<String> errors = new LinkedList<>();

    private Function<KEY,@NotNull IN> getter;
    private BiConsumer<@NotNull KEY,OUT> setter;
    private final TriFunction<Function<KEY,@Nullable OUT>,@NotNull IN,@NotNull KEY,@Nullable OUT> calculator;
    private final ObjectLinkedOpenHashSet<KEY> path = new ObjectLinkedOpenHashSet<>();

    public Memoizer(
            @NotNull Map<KEY,OUT> output,
            @NotNull BiFunction<Function<KEY,@Nullable OUT>,@NotNull IN,@Nullable OUT> calculator
    ) {
        this(output, Objects.requireNonNull(calculator, "Parameter calculator is null"), false);
    }

    private Memoizer(
            @NotNull Map<KEY,OUT> output,
            @NotNull BiFunction<Function<KEY,@Nullable OUT>,@NotNull IN,@Nullable OUT> calculator,
            boolean ignore
    ) {
        this(output, (getter, raw, key) -> calculator.apply(getter, raw));
    }

    public Memoizer(
            @NotNull Map<KEY,OUT> output,
            @NotNull TriFunction<Function<KEY,@Nullable OUT>,@NotNull IN,@NotNull KEY,@Nullable OUT> calculator
    ) {
        Objects.requireNonNull(output, "Parameter output is null");
        Objects.requireNonNull(calculator, "Parameter calculator is null");

        this.output = output;
        this.calculator = calculator;
    }

    public @NotNull List<String> solve(@NotNull Map<KEY,@NotNull IN> in) {
        return solve(in, false, true);
    }

    public @NotNull List<String> solve(@NotNull Map<KEY,@NotNull IN> in, boolean deconstruct, boolean ignoreNull) {
        this.in = Objects.requireNonNull(in, "Parameter in is null");
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
