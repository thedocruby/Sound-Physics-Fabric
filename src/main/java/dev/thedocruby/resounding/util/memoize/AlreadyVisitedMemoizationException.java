package dev.thedocruby.resounding.util.memoize;

/**
 * A visiting memoization visited more than what allowed. Usually visitation
 * limits are simply once or none at all.
 */
public class AlreadyVisitedMemoizationException extends MemoizationException {
    /**
     * Constructs a new already visited memoization exception with the specified
     * cause and a detail message.
     *
     * @param cause  the cause
     * @see RuntimeException#RuntimeException(Throwable)
     *          new RuntimeException(Throwable)
     */
    public AlreadyVisitedMemoizationException(final Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new already visited memoization exception with the specified
     * detail message and cause.
     *
     * @param message  the detail message
     * @param cause  the cause
     * @see RuntimeException#RuntimeException(String, Throwable)
     *          new RuntimeException(String, Throwable)
     */
    public AlreadyVisitedMemoizationException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new already visited memoization exception with the specified
     * detail message.
     *
     * @param message  the detail message
     * @see RuntimeException#RuntimeException(String)
     *          new RuntimeException(String)
     */
    public AlreadyVisitedMemoizationException(final String message) {
        super(message);
    }

    /**
     * Constructs a new already visited memoization runtime exception.
     *
     * @see RuntimeException#RuntimeException()
     *          new RuntimeException()
     */
    public AlreadyVisitedMemoizationException() {
    }
}
