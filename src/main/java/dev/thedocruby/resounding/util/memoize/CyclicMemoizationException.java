package dev.thedocruby.resounding.util.memoize;

/**
 * Signals a cycle-related problem during memoization.
 */
public class CyclicMemoizationException extends MemoizationException {
    /**
     * Constructs a new cyclic memoization exception with the specified cause
     * and a detail message.
     *
     * @param cause  the cause
     * @see RuntimeException#RuntimeException(Throwable)
     *          new RuntimeException(Throwable)
     */
    public CyclicMemoizationException(final Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new cyclic memoization exception with the specified detail
     * message and cause.
     *
     * @param message  the detail message
     * @param cause  the cause
     * @see RuntimeException#RuntimeException(String, Throwable)
     *          new RuntimeException(String, Throwable)
     */
    public CyclicMemoizationException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new cyclic memoization exception with the specified detail
     * message.
     *
     * @param message  the detail message
     * @see RuntimeException#RuntimeException(String)
     *          new RuntimeException(String)
     */
    public CyclicMemoizationException(final String message) {
        super(message);
    }

    /**
     * Constructs a new cyclic memoization runtime exception.
     *
     * @see RuntimeException#RuntimeException()
     *          new RuntimeException()
     */
    public CyclicMemoizationException() {
    }
}
