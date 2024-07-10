package dev.thedocruby.resounding.util.memoize;

/**
 * Signals a problem occurred during memoization.
 */
public class MemoizationException extends RuntimeException {
    /**
     * Constructs a new memoization exception with the specified cause and a
     * detail message.
     *
     * @param cause  the cause
     * @see RuntimeException#RuntimeException(Throwable)
     *          new RuntimeException(Throwable)
     */
    public MemoizationException(final Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new memoization exception with the specified detail message
     * and cause.
     *
     * @param message  the detail message
     * @param cause  the cause
     * @see RuntimeException#RuntimeException(String, Throwable)
     *          new RuntimeException(String, Throwable)
     */
    public MemoizationException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new memoization exception with the specified detail message.
     *
     * @param message  the detail message
     * @see RuntimeException#RuntimeException(String)
     *          new RuntimeException(String)
     */
    public MemoizationException(final String message) {
        super(message);
    }

    /**
     * Constructs a new memoization runtime exception.
     *
     * @see RuntimeException#RuntimeException()
     *          new RuntimeException()
     */
    public MemoizationException() {
    }
}
