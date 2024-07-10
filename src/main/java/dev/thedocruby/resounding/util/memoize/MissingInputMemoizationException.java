package dev.thedocruby.resounding.util.memoize;

/**
 * An input required during memoization is missing or unassigned.
 */
public class MissingInputMemoizationException extends MemoizationException {
    /**
     * Constructs a new missing input memoization exception with the specified
     * cause and a detail message.
     *
     * @param cause  the cause
     * @see RuntimeException#RuntimeException(Throwable)
     *          new RuntimeException(Throwable)
     */
    public MissingInputMemoizationException(final Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new missing input memoization exception with the specified
     * detail message and cause.
     *
     * @param message  the detail message
     * @param cause  the cause
     * @see RuntimeException#RuntimeException(String, Throwable)
     *          new RuntimeException(String, Throwable)
     */
    public MissingInputMemoizationException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new missing input memoization exception with the specified
     * detail message.
     *
     * @param message  the detail message
     * @see RuntimeException#RuntimeException(String)
     *          new RuntimeException(String)
     */
    public MissingInputMemoizationException(final String message) {
        super(message);
    }

    /**
     * Constructs a new missing input memoization runtime exception.
     *
     * @see RuntimeException#RuntimeException()
     *          new RuntimeException()
     */
    public MissingInputMemoizationException() {
    }
}
