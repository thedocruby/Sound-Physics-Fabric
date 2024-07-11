package dev.thedocruby.resounding.util.memoize;

import dev.thedocruby.resounding.util.DoNothingSet;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Utility class for memoization.
 */
public final class Memoization {
    /**
     * Calculated an output and its dependencies for a given key. Visited inputs
     * during calculation will be removed, which is equivalent to calling {@code
     *     withDependenciesFirstAnyways(inputs, outputs, key, calculator, true)
     * }
     *
     * @param <IN>  the input type
     * @param <OUT>  the output type
     * @param inputs  the inputs (for read)
     * @param outputs  the outputs (for read and write)
     * @param key  the key to retrieve the output of
     * @param calculator  the output calculator
     * @return the output value
     * @throws MemoizationException  if memoization was unsuccessful
     * @see #withDependenciesFirstAnyways(Map, Map, String, BiFunction, boolean)
     */
    public static <IN, OUT> @NotNull OUT withDependenciesFirstRemoveVisited(
        final @NotNull Map<String, IN> inputs,
        final @NotNull Map<String, @NotNull OUT> outputs,
        final String key,
        final @NotNull BiFunction<
            @NotNull Function<String, @NotNull OUT>,
            @NotNull IN,
            OUT
        > calculator
    ) throws MemoizationException {
        return withDependenciesFirstAnyways(inputs, outputs, key, calculator, true);
    }

    /**
     * Calculated an output and its dependencies for a given key. Any changes to
     * {@code inputs} and {@code outputs} are kept on failure, which is
     * equivalent to calling {@code
     *     withDependenciesFirst(inputs, outputs, key, calculator, removeVisited, true)
     * }.
     *
     * @param <IN>  the input type
     * @param <OUT>  the output type
     * @param inputs  the inputs (for read)
     * @param outputs  the outputs (for read and write)
     * @param key  the key to retrieve the output of
     * @param calculator  the output calculator
     * @param removeVisited  should the visited inputs be removed
     * @return the output value
     * @throws MemoizationException  if memoization was unsuccessful
     * @see #withDependenciesFirst(Map, Map, String, BiFunction, boolean, boolean)
     */
    public static <IN, OUT> @NotNull OUT withDependenciesFirstAnyways(
        final @NotNull Map<String, IN> inputs,
        final @NotNull Map<String, @NotNull OUT> outputs,
        final String key,
        final @NotNull BiFunction<
            @NotNull Function<String, @NotNull OUT>,
            @NotNull IN,
            OUT
        > calculator,
        final boolean removeVisited
    ) throws MemoizationException {
        return withDependenciesFirst(inputs, outputs, key, calculator, removeVisited, false);
    }

    /**
     * Calculated an output and its dependencies for a given key.
     * <ul>
     * <li>If the parameter {@code removeVisited} is set to {@code false}, then
     * the parameter {@code inputs} will be unchanged; otherwise visited inputs
     * are removed.
     * <li>If the parameter {@code noChangeOnFail} is set to {@code true}, then
     * both parameters {@code inputs} and {@code outputs} will be unchanged;
     * otherwise visited inputs are removed and newly generated outputs are
     * retained on failure.
     * </ul>
     *
     * @param <IN>  the input type
     * @param <OUT>  the output type
     * @param inputs  the inputs (for read)
     * @param outputs  the outputs (for read and write)
     * @param key  the key to retrieve the output of
     * @param calculator  the output calculator
     * @param removeVisited  should the visited inputs be removed
     * @param noChangeOnFail  should the input and outputs be unchanged on fail
     * @return the output value
     * @throws MemoizationException  if memoization was unsuccessful
     */
    public static <IN, OUT> @NotNull OUT withDependenciesFirst(
        final @NotNull Map<String, IN> inputs,
        final @NotNull Map<String, @NotNull OUT> outputs,
        final String key,
        final @NotNull BiFunction<
            @NotNull Function<String, @NotNull OUT>,
            @NotNull IN,
            OUT
        > calculator,
        final boolean removeVisited,
        final boolean noChangeOnFail
    ) throws MemoizationException {
        final Set<String> visitedKeys = removeVisited
            ? new ObjectOpenHashSet<>()
            : DoNothingSet.getInstance();
        final Set<String> generatedKeys = noChangeOnFail
            ? new ObjectOpenHashSet<>()
            : DoNothingSet.getInstance();
        final var keyPath = new ObjectLinkedOpenHashSet<String>();

        try {
            final var output = withDependenciesFirstInternal(
                inputs,
                outputs,
                visitedKeys,
                generatedKeys,
                keyPath,
                key,
                calculator
            );

            // NOTE: only remove from inputs if no exceptions are caught
            inputs.keySet().removeAll(visitedKeys);

            return output;
        } catch (final RuntimeException caught) {
            if (!noChangeOnFail) {
                inputs.keySet().removeAll(visitedKeys);
            }

            // NOTE: removes generated outputs on exceptions
            outputs.keySet().removeAll(generatedKeys);

            throw caught;
        }
    }

    /**
     * Calculated an output and its dependencies for a given key.
     *
     * @param <IN>  the input type
     * @param <OUT>  the output type
     * @param inputs  the inputs (for read)
     * @param outputs  the outputs (for read and write)
     * @param visitedKeys  the visited keys (effectively removed input keys)
     * @param generatedKeys  the generated keys (outputs to remove on fail)
     * @param keyPath  the current key path (used to detect cyclic dependencies)
     * @param key  the key to retrieve the output of
     * @param calculator  the output calculator
     * @return the output value
     * @throws MemoizationException  if memoization was unsuccessful
     */
    private static <IN, OUT> @NotNull OUT withDependenciesFirstInternal(
        final @NotNull Map<String, IN> inputs,
        final @NotNull Map<String, @NotNull OUT> outputs,
        final @NotNull Set<String> visitedKeys,
        final @NotNull Set<String> generatedKeys,
        final @NotNull ObjectLinkedOpenHashSet<String> keyPath,
        final String key,
        final @NotNull BiFunction<
            @NotNull Function<String, @NotNull OUT>,
            @NotNull IN,
            OUT
        > calculator
    ) throws MemoizationException {
        if (!visitedKeys.add(key)) {
            throw new AlreadyVisitedMemoizationException("Already visited key '" + key + "'");
        }
        if (keyPath.contains(key)) {
            throw new CyclicMemoizationException(
                "Keys entered a cycle: " + String.join(" -> ", keyPath) + " -> " + key
            );
        }

        {
            final var output = outputs.get(key);
            if (output != null /* || outputs.containsKey(key) */) {
                return output;
            }
        }

        keyPath.add(key);

        final var input = inputs.get(key);
        if (input == null /* && !inputs.containsKey(key) */) {
            throw new MissingInputMemoizationException("Missing input value for key '" + key + "'");
        }

        final OUT output;
        try {
            // NOTE: only catch if the calculation fails
            output = calculator.apply(
                dependency -> withDependenciesFirstInternal(
                    inputs,
                    outputs,
                    visitedKeys,
                    generatedKeys,
                    keyPath,
                    dependency,
                    calculator
                ),
                input
            );
        } catch (final CyclicMemoizationException special) {
            // NOTE: special case as it SHOULD announce involved keys
            throw special;
        } catch (final RuntimeException cause) {
            // NOTE: intentionally nests MemoizationException(s)
            throw new MemoizationException(
                "Exception caught while calculating key '" + key + "'",
                cause
            );
        } finally {
            keyPath.removeLast();
        }

        if (output == null) {
            throw new MemoizationException(
                "Memoization calculation resulted with null for key'" + key + "'",
                new NullPointerException("The calculator function evaluated to null")
            );
        }

        generatedKeys.add(key);
        outputs.put(key, output);
        return output;
    }

    /**
     * @deprecated Utility classes need no instantiation.
     */
    @Deprecated
    private Memoization() {}
}
