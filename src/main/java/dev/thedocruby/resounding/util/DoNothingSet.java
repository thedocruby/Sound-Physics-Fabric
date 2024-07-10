package dev.thedocruby.resounding.util;

import java.util.AbstractSet;
import java.util.Collections;
import java.util.Iterator;
import org.jetbrains.annotations.NotNull;

/**
 * A set that does not store added elements.
 *
 * @param <E> the element type
 */
public class DoNothingSet<E> extends AbstractSet<E> {
    /**
     * The do-nothing set.
     */
    @SuppressWarnings("rawtypes")
    public static final DoNothingSet INSTANCE =  new DoNothingSet();

    /**
     * Returns a do-nothing set.
     *
     * @return a do-nothing set
     * @param <T> the element type
     */
    public static <T> DoNothingSet<T> getInstance() {
        @SuppressWarnings("unchecked")
        final var instance = (DoNothingSet<T>) INSTANCE;
        return instance;
    }

    // NOTE: hardcode implementations only if needed

    @Override
    public boolean add(final E ignored) {
        return true;
    }

    @Override
    public boolean contains(final Object ignored) {
        return false;
    }

    @Override
    public @NotNull Iterator<E> iterator() {
        return Collections.emptyIterator();
    }

    @Override
    public int size() {
        return 0;
    }
}
