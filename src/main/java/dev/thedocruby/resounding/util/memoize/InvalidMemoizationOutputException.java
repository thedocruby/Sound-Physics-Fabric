package dev.thedocruby.resounding.util.memoize;

/**
 * Signals that a memoization output was invalid.
 *
 * @see MemoizationException
 * @deprecated Unused, prefer {@code MemoizationException}
 */
@Deprecated(forRemoval = true)
public class InvalidMemoizationOutputException extends MemoizationException {
    /**
     * Constructs a new invalid memoization output exception with the specified
     * cause and a detail message.
     *
     * @param cause  the cause
     * @see RuntimeException#RuntimeException(Throwable)
     *          new RuntimeException(Throwable)
     */
    public InvalidMemoizationOutputException(final Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new invalid memoization output exception with the specified
     * detail message and cause.
     *
     * @param message  the detail message
     * @param cause  the cause
     * @see RuntimeException#RuntimeException(String, Throwable)
     *          new RuntimeException(String, Throwable)
     */
    public InvalidMemoizationOutputException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new invalid memoization output exception with the specified
     * detail message.
     *
     * @param message  the detail message
     * @see RuntimeException#RuntimeException(String)
     *          new RuntimeException(String)
     */
    public InvalidMemoizationOutputException(final String message) {
        super(message);
    }

    /**
     * Constructs a new invalid memoization output runtime exception.
     *
     * @see RuntimeException#RuntimeException()
     *          new RuntimeException()
     */
    public InvalidMemoizationOutputException() {
    }
}
